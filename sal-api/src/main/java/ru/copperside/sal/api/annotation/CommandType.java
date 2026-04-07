package ru.copperside.sal.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a Java command class to its C# full type name for wire-compatibility.
 * <p>
 * Example:
 * <pre>
 * {@code @CommandType("TCB.KCProcessing.Client.ReversalCommand")}
 * public class ReversalCommand implements Command<ReversalResult> { }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandType {

    /** C# full type name including namespace, e.g. "TCB.KCProcessing.Client.ReversalCommand". */
    String value();
}
