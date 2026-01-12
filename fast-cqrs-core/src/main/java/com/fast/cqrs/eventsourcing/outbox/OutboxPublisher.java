package com.fast.cqrs.eventsourcing.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Background worker that publishes outbox entries.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * OutboxPublisher publisher = new OutboxPublisher(outboxStore)
 *         .maxRetries(3)
 *         .batchSize(100)
 *         .onPublish(entry -> kafkaProducer.send(entry));
 * 
 * publisher.start();
 * }</pre>
 */
public class OutboxPublisher implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxStore outboxStore;
    private final ScheduledExecutorService scheduler;
    private Consumer<OutboxEntry> publishHandler;
    private int batchSize = 100;
    private int maxRetries = 3;
    private long pollIntervalMs = 1000;
    private volatile boolean running;

    public OutboxPublisher(OutboxStore outboxStore) {
        this.outboxStore = outboxStore;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "outbox-publisher");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Sets the publish handler.
     */
    public OutboxPublisher onPublish(Consumer<OutboxEntry> handler) {
        this.publishHandler = handler;
        return this;
    }

    /**
     * Sets the batch size.
     */
    public OutboxPublisher batchSize(int size) {
        this.batchSize = size;
        return this;
    }

    /**
     * Sets the max retries per entry.
     */
    public OutboxPublisher maxRetries(int retries) {
        this.maxRetries = retries;
        return this;
    }

    /**
     * Sets the poll interval.
     */
    public OutboxPublisher pollInterval(long ms) {
        this.pollIntervalMs = ms;
        return this;
    }

    /**
     * Starts the publisher.
     */
    public void start() {
        if (publishHandler == null) {
            throw new IllegalStateException("Publish handler not set");
        }

        running = true;
        scheduler.scheduleWithFixedDelay(this::poll, 0, pollIntervalMs, TimeUnit.MILLISECONDS);
        log.info("Outbox publisher started");
    }

    /**
     * Stops the publisher.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Outbox publisher stopped");
    }

    private void poll() {
        if (!running)
            return;

        try {
            List<OutboxEntry> entries = outboxStore.getPending(batchSize);

            for (OutboxEntry entry : entries) {
                try {
                    publishHandler.accept(entry);
                    outboxStore.update(entry.markSent());
                    log.debug("Published outbox entry: {}", entry.id());
                } catch (Exception e) {
                    log.error("Failed to publish outbox entry: {}", entry.id(), e);

                    if (entry.attempts() >= maxRetries) {
                        outboxStore.update(entry.markFailed(e.getMessage()));
                    } else {
                        outboxStore.update(entry.markFailed(e.getMessage()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error polling outbox", e);
        }
    }

    @Override
    public void close() {
        stop();
    }
}
