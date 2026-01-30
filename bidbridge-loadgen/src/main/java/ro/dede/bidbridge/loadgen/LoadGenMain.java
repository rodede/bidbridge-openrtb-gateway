package ro.dede.bidbridge.loadgen;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Minimal load generator for OpenRTB endpoints.
 * Supports fixed QPS sending with optional replay from a JSONL file.
 */
public final class LoadGenMain {
    public static void main(String[] args) throws Exception {
        var config = LoadGenConfig.parse(args);
        var payloads = config.replayFile == null
                ? List.of(Files.readString(Path.of(config.requestFile)))
                : loadReplayLines(config.replayFile);
        if (payloads.isEmpty()) {
            System.err.println("Replay file is empty.");
            System.exit(1);
        }
        var client = WebClient.builder().build();
        var metrics = new LoadGenMetrics();
        var payloadIndex = new AtomicLong();

        var periodNanos = Math.max(1, 1_000_000_000L / config.qps);
        var duration = Duration.ofSeconds(config.durationSeconds);
        var start = System.nanoTime();

        Flux.interval(Duration.ofNanos(periodNanos))
                .take(duration)
                .flatMap(ignored -> sendOnce(client, config, payloads, payloadIndex, metrics), config.concurrency)
                .blockLast();

        var elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        metrics.printSummary(elapsedMs);
    }

    private static Mono<Void> sendOnce(WebClient client,
                                       LoadGenConfig config,
                                       List<String> payloads,
                                       AtomicLong payloadIndex,
                                       LoadGenMetrics metrics) {
        return Mono.defer(() -> {
                    var start = System.nanoTime();
                    var payload = nextPayload(payloads, payloadIndex);
                    return client.post()
                            .uri(config.url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .bodyValue(payload)
                            .exchangeToMono(response -> response.releaseBody().thenReturn(response.statusCode().value()))
                            .timeout(Duration.ofMillis(config.timeoutMs))
                            .doOnNext(status -> metrics.record(status, elapsedMs(start)))
                            .doOnError(ex -> metrics.recordError(elapsedMs(start)))
                            .onErrorResume(ex -> Mono.empty())
                            .then();
                }
        );
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static List<String> loadReplayLines(String replayFile) throws Exception {
        var lines = Files.readAllLines(Path.of(replayFile));
        return lines.stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private static String nextPayload(List<String> payloads, AtomicLong payloadIndex) {
        var index = payloadIndex.getAndIncrement();
        return payloads.get((int) (index % payloads.size()));
    }

    private static final class LoadGenConfig {
        private final String url;
        private final String requestFile;
        private final String replayFile;
        private final int qps;
        private final int durationSeconds;
        private final int concurrency;
        private final int timeoutMs;

        private LoadGenConfig(String url,
                              String requestFile,
                              String replayFile,
                              int qps,
                              int durationSeconds,
                              int concurrency,
                              int timeoutMs) {
            this.url = url;
            this.requestFile = requestFile;
            this.replayFile = replayFile;
            this.qps = qps;
            this.durationSeconds = durationSeconds;
            this.concurrency = concurrency;
            this.timeoutMs = timeoutMs;
        }

        static LoadGenConfig parse(String[] args) {
            var url = value(args, "--url");
            var requestFile = value(args, "--request-file");
            var replayFile = value(args, "--replay-file");
            if (url == null || (requestFile == null && replayFile == null)
                    || (requestFile != null && replayFile != null)) {
                usageAndExit();
            }
            var qps = intValue(args, "--qps", 50);
            var durationSeconds = intValue(args, "--duration-seconds", 10);
            var concurrency = intValue(args, "--concurrency", Math.max(1, qps));
            var timeoutMs = intValue(args, "--timeout-ms", 500);
            if (qps <= 0 || durationSeconds <= 0 || concurrency <= 0 || timeoutMs <= 0) {
                usageAndExit();
            }
            return new LoadGenConfig(url, requestFile, replayFile, qps, durationSeconds, concurrency, timeoutMs);
        }

        private static String value(String[] args, String key) {
            for (var i = 0; i < args.length - 1; i++) {
                if (key.equals(args[i])) {
                    return args[i + 1];
                }
            }
            return null;
        }

        private static int intValue(String[] args, String key, int defaultValue) {
            var raw = value(args, key);
            if (raw == null) {
                return defaultValue;
            }
            return Integer.parseInt(raw);
        }

        private static void usageAndExit() {
            System.err.println("""
                    Usage:
                      --url <endpoint> (--request-file <path> | --replay-file <path>) [options]

                    Options:
                      --replay-file <path>     (JSON lines, one request per line)
                      --qps <int>               (default: 50)
                      --duration-seconds <int>  (default: 10)
                      --concurrency <int>       (default: qps)
                      --timeout-ms <int>        (default: 500)
                    """);
            System.exit(1);
        }
    }

    private static final class LoadGenMetrics {
        private final LongAdder total = new LongAdder();
        private final LongAdder errors = new LongAdder();
        private final LongAdder status2xx = new LongAdder();
        private final LongAdder status204 = new LongAdder();
        private final LongAdder status4xx = new LongAdder();
        private final LongAdder status5xx = new LongAdder();
        private final LongAdder latencySumMs = new LongAdder();
        private final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxLatencyMs = new AtomicLong(0);

        void record(int status, long latencyMs) {
            total.increment();
            latencySumMs.add(latencyMs);
            updateMin(minLatencyMs, latencyMs);
            updateMax(maxLatencyMs, latencyMs);
            if (status == 204) {
                status204.increment();
            } else if (status >= 200 && status < 300) {
                status2xx.increment();
            } else if (status >= 400 && status < 500) {
                status4xx.increment();
            } else if (status >= 500) {
                status5xx.increment();
            }
        }

        void recordError(long latencyMs) {
            total.increment();
            errors.increment();
            latencySumMs.add(latencyMs);
            updateMin(minLatencyMs, latencyMs);
            updateMax(maxLatencyMs, latencyMs);
        }

        void printSummary(long elapsedMs) {
            var totalCount = total.sum();
            var avgLatency = totalCount == 0 ? 0 : latencySumMs.sum() / totalCount;
            var minLatency = minLatencyMs.get() == Long.MAX_VALUE ? 0 : minLatencyMs.get();
            var maxLatency = maxLatencyMs.get();

            System.out.println("LoadGen summary");
            System.out.println("total=" + totalCount + " errors=" + errors.sum());
            System.out.println("2xx=" + status2xx.sum() + " 204=" + status204.sum()
                    + " 4xx=" + status4xx.sum() + " 5xx=" + status5xx.sum());
            System.out.println("latency_ms avg=" + avgLatency + " min=" + minLatency + " max=" + maxLatency);
            if (elapsedMs > 0) {
                var achievedQps = (totalCount * 1000.0) / elapsedMs;
                System.out.println("elapsed_ms=" + elapsedMs + " achieved_qps=" + String.format(Locale.ROOT, "%.2f", achievedQps));
            }
        }

        private static void updateMin(AtomicLong current, long value) {
            current.accumulateAndGet(value, Math::min);
        }

        private static void updateMax(AtomicLong current, long value) {
            current.accumulateAndGet(value, Math::max);
        }
    }
}
