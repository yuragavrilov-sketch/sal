package ru.copperside.sal.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a catch-all command result handler with a priority order.
 * <p>
 * C# origin: {@code [AnyCommandResultAttribute(order)]}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AnyCommandResults.class)
public @interface AnyCommandResult {
    int order();
}
