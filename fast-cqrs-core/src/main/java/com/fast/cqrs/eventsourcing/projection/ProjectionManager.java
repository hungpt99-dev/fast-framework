package com.fast.cqrs.eventsourcing.projection;

import com.fast.cqrs.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages projections and dispatches events to them.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * ProjectionManager manager = new ProjectionManager();
 * manager.register(new OrderSummaryProjection());
 * 
 * // Process events
 * manager.process(event);
 * 
 * // Rebuild projection
 * manager.rebuild("order-summary", eventStore);
 * }</pre>
 */
public class ProjectionManager {

    private static final Logger log = LoggerFactory.getLogger(ProjectionManager.class);

    private final Map<String, Projection> projections = new ConcurrentHashMap<>();
    private final Map<String, ProjectionState> states = new ConcurrentHashMap<>();
    private final Map<Projection, Map<Class<?>, Method>> handlerCache = new ConcurrentHashMap<>();

    /**
     * Registers a projection.
     */
    public void register(Projection projection) {
        projections.put(projection.getName(), projection);
        states.put(projection.getName(), ProjectionState.initial(projection.getName()));
        cacheHandlers(projection);
        log.info("Registered projection: {}", projection.getName());
    }

    /**
     * Processes an event across all projections.
     */
    public void process(DomainEvent event) {
        process(event, false);
    }

    /**
     * Processes an event with replay flag.
     */
    public void process(DomainEvent event, boolean isReplay) {
        for (Projection projection : projections.values()) {
            if (!projection.canHandle(event.getClass()))
                continue;

            try {
                invokeHandler(projection, event);
            } catch (Exception e) {
                log.error("Error processing event {} in projection {}",
                        event.getEventType(), projection.getName(), e);
                states.put(projection.getName(),
                        states.get(projection.getName()).withError(e.getMessage()));
            }
        }
    }

    /**
     * Gets projection state.
     */
    public ProjectionState getState(String projectionName) {
        return states.get(projectionName);
    }

    /**
     * Gets all projections.
     */
    public Collection<Projection> getProjections() {
        return projections.values();
    }

    /**
     * Pauses a projection.
     */
    public void pause(String projectionName) {
        ProjectionState state = states.get(projectionName);
        if (state != null) {
            states.put(projectionName, new ProjectionState(
                    projectionName, state.lastProcessedPosition(),
                    state.lastProcessedAt(), ProjectionStatus.PAUSED, null));
        }
    }

    /**
     * Resumes a projection.
     */
    public void resume(String projectionName) {
        ProjectionState state = states.get(projectionName);
        if (state != null) {
            states.put(projectionName, new ProjectionState(
                    projectionName, state.lastProcessedPosition(),
                    state.lastProcessedAt(), ProjectionStatus.RUNNING, null));
        }
    }

    /**
     * Resets a projection for rebuild.
     */
    public void reset(String projectionName) {
        Projection projection = projections.get(projectionName);
        if (projection != null) {
            projection.reset();
            states.put(projectionName, ProjectionState.initial(projectionName));
        }
    }

    private void cacheHandlers(Projection projection) {
        Map<Class<?>, Method> handlers = new HashMap<>();
        for (Method method : projection.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(ProjectionHandler.class)) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && DomainEvent.class.isAssignableFrom(params[0])) {
                    method.setAccessible(true);
                    handlers.put(params[0], method);
                }
            }
        }
        handlerCache.put(projection, handlers);
    }

    private void invokeHandler(Projection projection, DomainEvent event) throws Exception {
        Map<Class<?>, Method> handlers = handlerCache.get(projection);
        if (handlers == null)
            return;

        Method handler = handlers.get(event.getClass());
        if (handler != null) {
            handler.invoke(projection, event);
        }
    }
}
