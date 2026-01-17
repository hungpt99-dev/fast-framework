package com.fast.cqrs.processor.util;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * Logging utility for annotation processors.
 * <p>
 * Wraps the {@link Messager} to provide a cleaner API for logging
 * during annotation processing.
 */
public class ProcessorLogger {

    private final Messager messager;
    private final String prefix;

    public ProcessorLogger(Messager messager) {
        this(messager, "[FastProcessor]");
    }

    public ProcessorLogger(Messager messager, String prefix) {
        this.messager = messager;
        this.prefix = prefix;
    }

    /**
     * Logs an error message. This will cause compilation to fail.
     */
    public void error(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, prefix + " " + message, element);
    }

    /**
     * Logs an error message without an associated element.
     */
    public void error(String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, prefix + " " + message);
    }

    /**
     * Logs a warning message.
     */
    public void warning(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.WARNING, prefix + " " + message, element);
    }

    /**
     * Logs a warning message without an associated element.
     */
    public void warning(String message) {
        messager.printMessage(Diagnostic.Kind.WARNING, prefix + " " + message);
    }

    /**
     * Logs an informational note.
     */
    public void note(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.NOTE, prefix + " " + message, element);
    }

    /**
     * Logs an informational note without an associated element.
     */
    public void note(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, prefix + " " + message);
    }

    /**
     * Logs a debug message (uses NOTE kind).
     */
    public void debug(String message) {
        // Only log in debug mode - can be controlled via processor options
        messager.printMessage(Diagnostic.Kind.OTHER, prefix + " [DEBUG] " + message);
    }
}
