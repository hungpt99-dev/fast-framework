package com.example.order.benchmark;

import com.example.order.dto.StartBenchmarkCmd;
import com.fast.cqrs.cqrs.annotation.Command;
import com.fast.cqrs.cqrs.annotation.HttpController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.order.handler.BenchmarkHandler;

@HttpController
public interface BenchmarkController {

    @GetMapping("/benchmark/run") // Changed URL to be cleaner
    @Command(handler = BenchmarkHandler.class)
    void runBenchmark(@ModelAttribute StartBenchmarkCmd cmd);
}
