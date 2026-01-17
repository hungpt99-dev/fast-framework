package com.example.order.dto;

public record StartBenchmarkCmd(
    String type, // "raw", "gateway", "direct"
    int iterations
) {}
