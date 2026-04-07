package ru.copperside.sal.starter.serialization;

import ru.copperside.sal.api.annotation.CommandType;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bidirectional mapping between C# full type names and Java classes.
 * <p>
 * Populated at startup by scanning classes annotated with {@link CommandType}.
 * Used by wire-format serialization to resolve types across C#/Java boundary.
 */
public class TypeMappingRegistry {

    private final Map<String, Class<?>> csharpToJava = new ConcurrentHashMap<>();
    private final Map<Class<?>, String> javaToCsharp = new ConcurrentHashMap<>();

    /**
     * Register a single mapping.
     *
     * @param csharpTypeName C# full type name, e.g. "TCB.KCProcessing.Client.ReversalCommand"
     * @param javaClass      corresponding Java class
     */
    public void register(String csharpTypeName, Class<?> javaClass) {
        csharpToJava.put(csharpTypeName, javaClass);
        javaToCsharp.put(javaClass, csharpTypeName);
    }

    /** Resolve Java class from C# type name. */
    public Optional<Class<?>> resolveJavaClass(String csharpTypeName) {
        return Optional.ofNullable(csharpToJava.get(csharpTypeName));
    }

    /** Resolve C# type name from Java class. */
    public Optional<String> resolveCsharpTypeName(Class<?> javaClass) {
        return Optional.ofNullable(javaToCsharp.get(javaClass));
    }

    /**
     * Scan a collection of classes for {@link CommandType} annotations and register them.
     */
    public void registerAnnotatedTypes(Collection<Class<?>> types) {
        for (Class<?> type : types) {
            CommandType annotation = type.getAnnotation(CommandType.class);
            if (annotation != null) {
                register(annotation.value(), type);
            }
        }
    }

    public int size() {
        return csharpToJava.size();
    }

    /**
     * Strip C# assembly qualifier from a type name.
     * "Namespace.Class, Assembly" → "Namespace.Class"
     */
    public static String stripAssemblyName(String typeName) {
        if (typeName == null) return null;
        int comma = typeName.indexOf(',');
        return comma >= 0 ? typeName.substring(0, comma).trim() : typeName;
    }
}
