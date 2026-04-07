package ru.copperside.sal.starter.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import ru.copperside.sal.api.annotation.CommandType;
import ru.copperside.sal.api.command.CommandHandler;
import ru.copperside.sal.api.command.CommandHandlerAsync;
import ru.copperside.sal.starter.serialization.TypeMappingRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers and registers command handlers at startup (ADR-004).
 * <p>
 * Scans {@link CommandHandler} and {@link CommandHandlerAsync} beans,
 * extracts generic type parameters via {@link ResolvableType},
 * builds a map: commandTypeName → handler bean.
 * <p>
 * Replaces C# {@code CommandProxy.RegisterCommand()} reflection logic.
 */
public class CommandHandlerRegistry implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(CommandHandlerRegistry.class);

    private final ApplicationContext applicationContext;
    private final TypeMappingRegistry typeMappingRegistry;

    /** commandTypeName → handler bean name. */
    private final Map<String, String> handlerBeanNames = new ConcurrentHashMap<>();

    /** commandTypeName → command Java class. */
    private final Map<String, Class<?>> commandTypes = new ConcurrentHashMap<>();

    public CommandHandlerRegistry(ApplicationContext applicationContext, TypeMappingRegistry typeMappingRegistry) {
        this.applicationContext = applicationContext;
        this.typeMappingRegistry = typeMappingRegistry;
    }

    @Override
    public void afterSingletonsInstantiated() {
        scanHandlers(CommandHandler.class);
        scanHandlers(CommandHandlerAsync.class);
        log.info("Registered {} command handlers", handlerBeanNames.size());
    }

    private void scanHandlers(Class<?> handlerInterface) {
        String[] beanNames = applicationContext.getBeanNamesForType(
                ResolvableType.forClass(handlerInterface));

        for (String beanName : beanNames) {
            Class<?> beanType = applicationContext.getType(beanName);
            if (beanType == null) continue;

            ResolvableType[] interfaces = ResolvableType.forClass(beanType).getInterfaces();
            for (ResolvableType iface : interfaces) {
                if (iface.getRawClass() != null && handlerInterface.isAssignableFrom(iface.getRawClass())) {
                    ResolvableType[] generics = iface.getGenerics();
                    if (generics.length >= 1) {
                        Class<?> commandClass = generics[0].resolve();
                        if (commandClass == null) continue;

                        String commandTypeName = resolveCommandTypeName(commandClass);
                        handlerBeanNames.put(commandTypeName, beanName);
                        commandTypes.put(commandTypeName, commandClass);
                        typeMappingRegistry.register(commandTypeName, commandClass);
                        log.info("  Command handler: {} → {}", commandTypeName, beanType.getSimpleName());
                    }
                }
            }
        }
    }

    private String resolveCommandTypeName(Class<?> commandClass) {
        // Prefer @CommandType annotation (C# full type name)
        CommandType annotation = commandClass.getAnnotation(CommandType.class);
        if (annotation != null) {
            return annotation.value();
        }
        // Fallback: Java class name
        return commandClass.getName();
    }

    /** Get handler bean for a command type name, or null. */
    public Object resolveHandler(String commandTypeName) {
        String beanName = handlerBeanNames.get(commandTypeName);
        if (beanName == null) return null;
        return applicationContext.getBean(beanName);
    }

    /** Get command Java class for a command type name, or null. */
    public Class<?> getCommandClass(String commandTypeName) {
        return commandTypes.get(commandTypeName);
    }

    /** Check if a handler exists for the given command type name. */
    public boolean hasHandler(String commandTypeName) {
        return handlerBeanNames.containsKey(commandTypeName);
    }

    /** All registered command type names (for topology declaration). */
    public Iterable<String> getRegisteredCommandTypeNames() {
        return handlerBeanNames.keySet();
    }
}
