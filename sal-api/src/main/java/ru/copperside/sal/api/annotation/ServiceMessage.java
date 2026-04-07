package ru.copperside.sal.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an event as a service-level message (routed via "service" routing key).
 * <p>
 * C# origin: {@code [ServiceMessageAttribute]}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ServiceMessage {
}
