package com.fast.cqrs.concurrent.resilience;

/**
 * Callback interface for custom rejection handling.
 */
public interface RejectHandler {
    
    /**
     * Called when a task is rejected.
     *
     * @param context Context about the rejection (method, args, etc.)
     * @return The fallback result or throw an exception
     */
    Object onReject(RejectContext context);

    record RejectContext(
        String key,
        Object[] args,
        String reason
    ) {}
}
