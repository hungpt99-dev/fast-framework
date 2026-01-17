package com.example.order.handler;

import com.example.order.dto.CreateOrderCmd;
import com.example.order.dto.PerformanceCmd;
import com.example.order.dto.StartBenchmarkCmd;
import com.fast.cqrs.cqrs.CommandHandler;
import com.fast.cqrs.cqrs.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;

@Component
public class BenchmarkHandler implements CommandHandler<StartBenchmarkCmd> {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkHandler.class);
    private final CommandGateway gateway;

    public BenchmarkHandler(@org.springframework.context.annotation.Lazy CommandGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void handle(StartBenchmarkCmd cmd) {
        log.info("Starting benchmark: type={}, iterations={}", cmd.type(), cmd.iterations());
        long start = System.nanoTime();

        try {
            switch (cmd.type().toLowerCase()) {
                case "raw" -> runRaw(cmd.iterations());
                case "gateway" -> runGateway(cmd.iterations());
                case "async" -> runAsync(cmd.iterations());
                default -> throw new IllegalArgumentException("Unknown benchmark type: " + cmd.type());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long end = System.nanoTime();
        long durationMs = (end - start) / 1_000_000;
        double rps = durationMs > 0 ? (double) cmd.iterations() / (durationMs / 1000.0) : 0;
        
        log.info("Benchmark Result - {}: {} iterations in {} ms ({} ops/sec)", 
                cmd.type(), cmd.iterations(), durationMs, String.format("%.2f", rps));
    }

    private void runRaw(int iterations) {
        for (int i = 0; i < iterations; i++) {
            PerformanceCmd cmd = new PerformanceCmd("bench-" + i);
            gateway.send(cmd);
        }
    }

    private void runGateway(int iterations) {
        for (int i = 0; i < iterations; i++) {
            gateway.with(new CreateOrderCmd(null, "CUST-" + i, new BigDecimal("100")))
                   .send();
        }
    }
    
    private void runAsync(int iterations) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(iterations);
        for (int i = 0; i < iterations; i++) {
            gateway.with(new CreateOrderCmd(null, "CUST-" + i, new BigDecimal("100")))
                   .onError(e -> latch.countDown())
                   .onSuccess(r -> latch.countDown())
                   .sendAsync();
        }
        latch.await();
    }
}
