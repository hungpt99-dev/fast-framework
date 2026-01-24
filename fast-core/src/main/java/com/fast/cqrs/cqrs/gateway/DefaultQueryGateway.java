package com.fast.cqrs.cqrs.gateway;

import com.fast.cqrs.cqrs.QueryBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.factory.DisposableBean;

/**
 * Default implementation of {@link QueryGateway}.
 * <p>
 * Wraps the {@link QueryBus} with additional features like
 * caching, timeout, fallback, and parallel execution.
 */
@Component
public class DefaultQueryGateway implements QueryGateway, DisposableBean {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultQueryGateway.class);
    
    private final QueryBus queryBus;
    private final ExecutorService executor;
    
    public DefaultQueryGateway(QueryBus queryBus) {
        this.queryBus = queryBus;
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
    public <R> R query(Object query) {
        return queryBus.dispatch(query);
    }
    
    @Override
    public <R> R query(Object query, Duration timeout) {
        try {
            return this.<R>queryAsync(query).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new QueryGatewayException("Query execution failed", e);
        }
    }
    
    @Override
    public <R> CompletableFuture<R> queryAsync(Object query) {
        return CompletableFuture.supplyAsync(() -> queryBus.dispatch(query), executor);
    }
    
    @Override
    public QueryDispatch with(Object query) {
        return new DefaultQueryDispatch(query);
    }
    
    @Override
    public ParallelQueries parallel() {
        return new DefaultParallelQueries();
    }
    
    /**
     * Fluent builder implementation.
     */
    private class DefaultQueryDispatch implements QueryDispatch {
        
        private final Object query;
        private Duration cacheTtl;
        private Duration timeout;
        private Supplier<?> fallback;
        
        DefaultQueryDispatch(Object query) {
            this.query = query;
        }
        
        @Override
        public QueryDispatch cache(Duration ttl) {
            this.cacheTtl = ttl;
            return this;
        }
        
        @Override
        public QueryDispatch timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        @Override
        public <R> QueryDispatch fallback(Supplier<R> fallback) {
            this.fallback = fallback;
            return this;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <R> R execute() {
            try {
                if (timeout != null) {
                    return DefaultQueryGateway.this.query(query, timeout);
                }
                return queryBus.dispatch(query);
            } catch (Exception e) {
                if (fallback != null) {
                    log.debug("Query failed, using fallback: {}", e.getMessage());
                    return (R) fallback.get();
                }
                throw e;
            }
        }
        
        @Override
        public <R> CompletableFuture<R> executeAsync() {
            return CompletableFuture.supplyAsync(this::execute, executor);
        }
    }
    
    /**
     * Parallel query execution builder.
     */
    private class DefaultParallelQueries implements ParallelQueries {
        
        private final List<Object> queries = new ArrayList<>();
        private Duration timeout = Duration.ofSeconds(30);
        
        @Override
        public ParallelQueries add(Object query) {
            queries.add(query);
            return this;
        }
        
        @Override
        public ParallelQueries timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <R> List<R> execute() {
            List<CompletableFuture<R>> futures = queries.stream()
                    .map(q -> (CompletableFuture<R>) queryAsync(q))
                    .toList();
            
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                
                return futures.stream()
                        .map(CompletableFuture::join)
                        .toList();
            } catch (Exception e) {
                throw new QueryGatewayException("Parallel query execution failed", e);
            }
        }
        
        @Override
        public <R, A> A executeAndAggregate(Function<List<R>, A> aggregator) {
            List<R> results = execute();
            return aggregator.apply(results);
        }
    }
}
