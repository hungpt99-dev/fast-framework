package com.fast.cqrs.cqrs.gateway;

import com.fast.cqrs.cqrs.CommandBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.beans.factory.DisposableBean;

/**
 * Default implementation of {@link CommandGateway}.
 * <p>
 * Wraps the {@link CommandBus} with additional features like
 * timeout, retry, async execution, and callbacks.
 */
@Component
public class DefaultCommandGateway implements CommandGateway, DisposableBean {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultCommandGateway.class);
    
    private final CommandBus commandBus;
    private final ExecutorService executor;
    
    public DefaultCommandGateway(CommandBus commandBus) {
        this.commandBus = commandBus;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }
    
    @Override
    public void destroy() {
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
    
    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Object command) {
        commandBus.dispatch(command);
        return null;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Object command, Duration timeout) {
        try {
            return (R) sendAsync(command).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new CommandGatewayException("Command execution failed", e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <R> CompletableFuture<R> sendAsync(Object command) {
        return CompletableFuture.supplyAsync(() -> {
            commandBus.dispatch(command);
            return null;
        }, executor);
    }
    
    @Override
    public void sendAndForget(Object command) {
        executor.submit(() -> {
            try {
                commandBus.dispatch(command);
            } catch (Exception e) {
                log.warn("Fire-and-forget command failed: {} - {}", 
                        command.getClass().getSimpleName(), e.getMessage());
            }
        });
    }
    
    @Override
    public CommandDispatch with(Object command) {
        return new DefaultCommandDispatch(command);
    }
    
    /**
     * Fluent builder implementation.
     */
    private class DefaultCommandDispatch implements CommandDispatch {
        
        private final Object command;
        private Duration timeout;
        private int retryAttempts = 0;
        private Duration retryBackoff = Duration.ofMillis(100);
        private Consumer<Object> onSuccess;
        private Consumer<Throwable> onError;
        
        DefaultCommandDispatch(Object command) {
            this.command = command;
        }
        
        @Override
        public CommandDispatch timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        @Override
        public CommandDispatch retry(int attempts) {
            this.retryAttempts = attempts;
            return this;
        }
        
        @Override
        public CommandDispatch retry(int attempts, Duration backoff) {
            this.retryAttempts = attempts;
            this.retryBackoff = backoff;
            return this;
        }
        
        @Override
        public CommandDispatch onSuccess(Consumer<Object> callback) {
            this.onSuccess = callback;
            return this;
        }
        
        @Override
        public CommandDispatch onError(Consumer<Throwable> callback) {
            this.onError = callback;
            return this;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <R> R send() {
            try {
                R result;
                if (timeout != null) {
                    result = (R) CompletableFuture.supplyAsync(
                            this::executeWithRetry, executor
                    ).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS).join();
                } else {
                    result = executeWithRetry();
                }
                if (onSuccess != null) {
                    onSuccess.accept(result);
                }
                return result;
            } catch (Exception e) {
                if (onError != null) {
                    onError.accept(e);
                }
                throw e;
            }
        }
        
        @Override
        public <R> CompletableFuture<R> sendAsync() {
            return CompletableFuture.supplyAsync(this::send, executor);
        }
        
        @Override
        public void sendAndForget() {
            executor.submit(() -> {
                try {
                    send();
                } catch (Exception e) {
                    log.warn("Fire-and-forget failed: {}", e.getMessage());
                }
            });
        }
        
        @SuppressWarnings("unchecked")
        private <R> R executeWithRetry() {
            int attempts = 0;
            Exception lastError = null;
            
            while (attempts <= retryAttempts) {
                try {
                    commandBus.dispatch(command);
                    return null;
                } catch (Exception e) {
                    lastError = e;
                    attempts++;
                    if (attempts <= retryAttempts) {
                        log.debug("Retry {}/{} for command {}", 
                                attempts, retryAttempts, command.getClass().getSimpleName());
                        sleep(retryBackoff);
                    }
                }
            }
            
            throw new CommandGatewayException("Command failed after " + retryAttempts + " retries", lastError);
        }
        
        private void sleep(Duration duration) {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
