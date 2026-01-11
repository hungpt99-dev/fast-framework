package com.fast.cqrs.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Asynchronous event bus that publishes events in background threads.
 * <p>
 * Events are published non-blocking, allowing command handlers to
 * complete quickly while event handlers run asynchronously.
 */
public class AsyncEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(AsyncEventBus.class);

    private final Map<Class<?>, List<DomainEventHandler<?>>> handlers = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public AsyncEventBus() {
        this(Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "async-event-bus");
                t.setDaemon(true);
                return t;
            }
        ));
    }

    public AsyncEventBus(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends DomainEvent> void publish(E event) {
        log.debug("Async publishing event: {} (id={})", event.getEventType(), event.getEventId());

        List<DomainEventHandler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers == null || eventHandlers.isEmpty()) {
            log.debug("No handlers registered for event: {}", event.getEventType());
            return;
        }

        for (DomainEventHandler<?> handler : eventHandlers) {
            executor.submit(() -> {
                try {
                    ((DomainEventHandler<E>) handler).handle(event);
                    log.debug("Async handler {} processed event {}", 
                              handler.getClass().getSimpleName(), event.getEventId());
                } catch (Exception e) {
                    log.error("Error in async handler {} for event {}", 
                              handler.getClass().getSimpleName(), event.getEventId(), e);
                }
            });
        }
    }

    @Override
    public <E extends DomainEvent> void subscribe(Class<E> eventType, DomainEventHandler<E> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        log.info("Registered async handler {} for event {}", 
                 handler.getClass().getSimpleName(), eventType.getSimpleName());
    }

    /**
     * Shuts down the executor.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
