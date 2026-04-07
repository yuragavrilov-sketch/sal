package ru.copperside.sal.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a result handler to a specific command type with a priority order.
 * <p>
 * C# origin: {@code [CommandResultAttribute(commandType, order)]}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CommandResultBindings.class)
public @interface CommandResultBinding {
    Class<?> commandType();
    int order();
}
