package ro.dede.bidbridge.engine.observability;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import jakarta.annotation.PreDestroy;

/**
 * Propagates request ID from Reactor context into MDC for consistent log prefixing.
 */
@Component
public final class ReactorMdcConfiguration {

    private static final String HOOK_KEY = "requestIdMdc";

    public ReactorMdcConfiguration() {
        Hooks.onEachOperator(HOOK_KEY, Operators.lift((sc, subscriber) -> new MdcSubscriber(subscriber)));
    }

    @PreDestroy
    void cleanup() {
        Hooks.resetOnEachOperator(HOOK_KEY);
    }

    static final class MdcSubscriber<T> implements CoreSubscriber<T> {
        private final CoreSubscriber<T> delegate;

        MdcSubscriber(CoreSubscriber<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onSubscribe(org.reactivestreams.Subscription s) {
            delegate.onSubscribe(s);
        }

        @Override
        public void onNext(T t) {
            withMdc(delegate.currentContext(), () -> delegate.onNext(t));
        }

        @Override
        public void onError(Throwable t) {
            withMdc(delegate.currentContext(), () -> delegate.onError(t));
        }

        @Override
        public void onComplete() {
            withMdc(delegate.currentContext(), delegate::onComplete);
        }

        @Override
        public Context currentContext() {
            return delegate.currentContext();
        }

        private static void withMdc(Context context, Runnable action) {
            var previousRequestId = MDC.get(RequestLoggingFilter.REQUEST_ID_ATTR);
            var previousCaller = MDC.get(RequestLoggingFilter.CALLER_ATTR);
            var requestId = context.getOrDefault(RequestLoggingFilter.REQUEST_ID_ATTR, "unknown");
            MDC.put(RequestLoggingFilter.REQUEST_ID_ATTR, requestId);
            var caller = context.getOrDefault(RequestLoggingFilter.CALLER_ATTR, null);
            if (caller instanceof String callerValue && !callerValue.isBlank()) {
                MDC.put(RequestLoggingFilter.CALLER_ATTR, callerValue);
            }
            try {
                action.run();
            } finally {
                if (previousRequestId == null) {
                    MDC.remove(RequestLoggingFilter.REQUEST_ID_ATTR);
                } else {
                    MDC.put(RequestLoggingFilter.REQUEST_ID_ATTR, previousRequestId);
                }
                if (previousCaller == null) {
                    MDC.remove(RequestLoggingFilter.CALLER_ATTR);
                } else {
                    MDC.put(RequestLoggingFilter.CALLER_ATTR, previousCaller);
                }
            }
        }
    }
}
