package ro.dede.bidbridge.loadgen;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Minimal load generator for OpenRTB endpoints.
 * Supports fixed QPS sending with optional replay from a JSONL file.
 */
public final class LoadGenMain {
    private static final String BID_API_KEY_HEADER = "X-Api-Key";
    private static final String CALLER_HEADER = "X-Caller";

    public static void main(String[] args) throws Exception {
        var config = LoadGenConfig.parse(args);
        var payloads = config.replayFile == null
                ? List.of(Files.readString(Path.of(config.requestFile)))
                : loadReplayLines(config.replayFile);
        if (payloads.isEmpty()) {
            System.err.println("Replay file is empty.");
            System.exit(1);
        }
        // Use dedicated client resources so we can dispose cleanly after the run.
        var connectionProvider = ConnectionProvider.create("loadgen", config.concurrency * 2);
        var loopResources = LoopResources.create("loadgen", Math.min(2, config.concurrency), true);
        var httpClient = HttpClient.create(connectionProvider).runOn(loopResources);
        var client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        var metrics = new LoadGenMetrics();
        var payloadIndex = new AtomicLong();
        // Dedicated scheduler for timers to avoid lingering shared threads.
        var timerScheduler = Schedulers.newSingle("loadgen-timer");

        var periodNanos = Math.max(1, 1_000_000_000L / config.qps);
        var duration = Duration.ofSeconds(config.durationSeconds);
        var start = System.nanoTime();

        // Drive requests at target QPS with bounded concurrency.
        Flux.interval(Duration.ofNanos(periodNanos), timerScheduler)
                .take(duration)
                .flatMap(ignored -> sendOnce(client, config, payloads, payloadIndex, metrics, timerScheduler), config.concurrency)
                .blockLast();

        var elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        metrics.printSummary(elapsedMs);
        // Dispose resources explicitly to avoid lingering threads in exec:java.
        timerScheduler.dispose();
        connectionProvider.disposeLater().block();
        loopResources.disposeLater().block();
    }

    private static Mono<Void> sendOnce(WebClient client,
                                       LoadGenConfig config,
                                       List<String> payloads,
                                       AtomicLong payloadIndex,
                                       LoadGenMetrics metrics,
                                       Scheduler timerScheduler) {
        return Mono.defer(() -> {
                    var start = System.nanoTime();
                    var payload = nextPayload(payloads, payloadIndex);
                    return client.post()
                            .uri(config.url)
                            .headers(httpHeaders -> {
                                if (config.bidApiKey != null) {
                                    httpHeaders.set(BID_API_KEY_HEADER, config.bidApiKey);
                                }
                                if (config.xCaller != null) {
                                    httpHeaders.set(CALLER_HEADER, config.xCaller);
                                }
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .bodyValue(payload)
                            .exchangeToMono(response -> response.releaseBody().thenReturn(response.statusCode().value()))
                            .timeout(Duration.ofMillis(config.timeoutMs), timerScheduler)
                            .doOnNext(status -> metrics.record(status, elapsedMs(start)))
                            .doOnError(ex -> {
                                if (ex instanceof TimeoutException) {
                                    metrics.recordTimeout(elapsedMs(start));
                                } else {
                                    metrics.recordError(elapsedMs(start));
                                }
                            })
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
        private final String bidApiKey;
        private final String xCaller;

        private LoadGenConfig(String url,
                              String requestFile,
                              String replayFile,
                              int qps,
                              int durationSeconds,
                              int concurrency,
                              int timeoutMs,
                              String bidApiKey,
                              String xCaller) {
            this.url = url;
            this.requestFile = requestFile;
            this.replayFile = replayFile;
            this.qps = qps;
            this.durationSeconds = durationSeconds;
            this.concurrency = concurrency;
            this.timeoutMs = timeoutMs;
            this.bidApiKey = bidApiKey;
            this.xCaller = xCaller;
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
            var bidApiKey = value(args, "--bid-api-key");
            var xCaller = value(args, "--x-caller");
            if (qps <= 0 || durationSeconds <= 0 || concurrency <= 0 || timeoutMs <= 0) {
                usageAndExit();
            }
            return new LoadGenConfig(url, requestFile, replayFile, qps, durationSeconds, concurrency, timeoutMs, bidApiKey, xCaller);
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
                      --bid-api-key <value>     (header X-Api-Key, fallback env BID_API_KEY)
                      --x-caller <value>        (header X-Caller, fallback env X_CALLER)
                    """);
            System.exit(1);
        }
    }

    private static final class LoadGenMetrics {
        private final LongAdder total = new LongAdder();
        private final LongAdder errors = new LongAdder();
        private final LongAdder timeouts = new LongAdder();
        private final LongAdder status200 = new LongAdder();
        private final LongAdder status2xx = new LongAdder();
        private final LongAdder status204 = new LongAdder();
        private final LongAdder status4xx = new LongAdder();
        private final LongAdder status5xx = new LongAdder();
        private final LongAdder statusOther = new LongAdder();
        private final LongAdder latencySumMs = new LongAdder();
        private final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxLatencyMs = new AtomicLong(0);

        void record(int status, long latencyMs) {
            total.increment();
            latencySumMs.add(latencyMs);
            updateMin(minLatencyMs, latencyMs);
            updateMax(maxLatencyMs, latencyMs);
            switch (status) {
                case 200 -> status200.increment();
                case 204 -> status204.increment();
                default -> {
                    if (status > 200 && status < 300) {
                        status2xx.increment();
                    } else if (status >= 400 && status < 500) {
                        status4xx.increment();
                    } else if (status >= 500) {
                        status5xx.increment();
                    } else {
                        statusOther.increment();
                    }
                }
            }
        }

        void recordError(long latencyMs) {
            total.increment();
            errors.increment();
            latencySumMs.add(latencyMs);
            updateMin(minLatencyMs, latencyMs);
            updateMax(maxLatencyMs, latencyMs);
        }

        void recordTimeout(long latencyMs) {
            total.increment();
            timeouts.increment();
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
            System.out.println("total=" + totalCount + " errors=" + errors.sum() + " timeouts(client)=" + timeouts.sum());
            System.out.println("200=" + status200.sum() + " 2xx=" + status2xx.sum()
                    + " 204=" + status204.sum() + " 4xx=" + status4xx.sum()
                    + " 5xx=" + status5xx.sum() + " other=" + statusOther.sum());
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
