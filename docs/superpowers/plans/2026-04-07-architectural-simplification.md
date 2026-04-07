# Architectural Simplification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove EventBus, WatchDog, REST client, online/offline state, confirmatory commands, dead config, and phantom sal-test module. Simplify exception hierarchy and consolidate ThreadLocals.

**Architecture:** Keep command-processing pipeline intact (CommandBus → RabbitMQ → CommandConsumer → handlers). Remove all broadcast/monitoring subsystems. Consolidate 3 ThreadLocal holders into 1 SalContext. Collapse 4 exception classes into 1 SalException + enum.

**Tech Stack:** Java 21, Spring Boot 3.4.5, RabbitMQ, Jackson

**Spec:** `docs/superpowers/specs/2026-04-07-architectural-simplification-design.md`

---

## File Structure

### Files to DELETE (sal-api)

Event subsystem (24 files):
- `sal-api/src/main/java/ru/copperside/sal/api/event/Event.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/EventBus.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/EventHandler.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/EventSource.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/adapter/AdapterDownEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/adapter/AdapterOfflineEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/adapter/AdapterOnlineEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/adapter/AdapterPRCChangedEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/adapter/AdapterPresentEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/adapter/AdapterUpEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/adapter/EnvironmentKeyChangedEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/adapter/ExceptionDetectEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/adapter/MonitorFailureEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/adapter/MonitorTimeOutEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/adapter/WhoisAdapterEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/endpoint/AdapterAvailableEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/endpoint/AdapterNotAvailableEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/endpoint/EndPointDownEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/endpoint/EndPointEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/endpoint/EndPointOfflineEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/endpoint/EndPointOnlineEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/endpoint/EndPointPRCChangedEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/endpoint/EndPointPresentEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/endpoint/EndPointUpEvent.java`
- `sal-api/src/main/java/ru/copperside/sal/api/event/endpoint/WhoisEndPointEvent.java`

WatchDog DTOs (2 files):
- `sal-api/src/main/java/ru/copperside/sal/api/watchdog/DependencyAdapter.java`
- `sal-api/src/main/java/ru/copperside/sal/api/watchdog/EndPointInfo.java`

Confirmatory + dead interfaces (4 files):
- `sal-api/src/main/java/ru/copperside/sal/api/command/ConfirmatoryCommandHandler.java`
- `sal-api/src/main/java/ru/copperside/sal/api/command/ConfirmatoryCommandHandlerAsync.java`
- `sal-api/src/main/java/ru/copperside/sal/api/command/ConfirmationResult.java`
- `sal-api/src/main/java/ru/copperside/sal/api/command/CommandResultHandler.java`

DTOs tied to removed subsystems (3 files):
- `sal-api/src/main/java/ru/copperside/sal/api/dto/ConfirmationResponse.java`
- `sal-api/src/main/java/ru/copperside/sal/api/dto/service/HostedRequest.java`
- `sal-api/src/main/java/ru/copperside/sal/api/dto/service/PingRequest.java`

Old exception classes (after replacement, 4 files):
- `sal-api/src/main/java/ru/copperside/sal/api/exception/SalBaseException.java`
- `sal-api/src/main/java/ru/copperside/sal/api/exception/ErrorException.java`
- `sal-api/src/main/java/ru/copperside/sal/api/exception/FatalException.java`
- `sal-api/src/main/java/ru/copperside/sal/api/exception/ValidationException.java`

### Files to DELETE (sal-spring-boot-starter)

Event (2 files):
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/event/DefaultEventBus.java`
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/event/EventBusAutoConfiguration.java`

WatchDog (5 files):
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/watchdog/EndPointRegistry.java`
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/watchdog/EndPointsAvailableScheduler.java`
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/watchdog/EndPointsRemoverScheduler.java`
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/watchdog/WatchDogService.java`
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/watchdog/WatchDogAutoConfiguration.java`

Health, client, online/offline (4 files):
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/health/AdapterStateHealthIndicator.java`
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/client/SalRestClient.java`
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/AdapterState.java`
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/OfflineCheckInterceptor.java`

Old ThreadLocal holders (after replacement, 3 files):
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/context/SessionHolder.java`
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/context/CommandContextHolder.java`
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/context/SalMdc.java`

### Files to DELETE (sal-example-adapter)

- `sal-example-adapter/src/main/java/ru/copperside/sal/example/switch_/SwitchController.java`
- `sal-example-adapter/src/main/java/ru/copperside/sal/example/watchdog/EndPointEventListener.java`

### Files to DELETE (modules)

- `sal-test/` — entire directory

### Files to CREATE

- `sal-api/src/main/java/ru/copperside/sal/api/exception/SalException.java`
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/context/SalContext.java`

### Files to MODIFY

- `pom.xml` — remove sal-test module
- `sal-api/src/main/java/ru/copperside/sal/api/command/CommandBus.java` — remove confirmatory methods
- `sal-api/src/main/java/ru/copperside/sal/api/exception/InfrastructureExceptionDTO.java` — sourceType field type: String (verify)
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/SalProperties.java` — remove dead properties
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/command/DefaultCommandBus.java` — remove confirmatory method
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/command/CommandConsumer.java` — use SalContext
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/command/CommandPublisher.java` — use SalContext
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/command/CommandTimeoutWatcher.java` — ErrorException → SalException
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/WebAutoConfiguration.java` — remove AdapterState, OfflineCheckInterceptor beans
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/WebMvcAutoConfiguration.java` — remove OfflineCheckInterceptor
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/SalContextFilter.java` — use SalContext
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/SalExceptionHandler.java` — SalBaseException → SalException
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/EnvironmentKeyInterceptor.java` — ErrorException → SalException
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/SessionFilter.java` — use SalContext
- `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/lifecycle/AdapterLifecycle.java` — remove AdapterState
- `sal-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — remove deleted configs
- `sal-spring-boot-starter/src/test/java/ru/copperside/sal/starter/command/DefaultCommandBusTest.java` — update to SalContext
- `sal-spring-boot-starter/src/test/java/ru/copperside/sal/starter/web/SalExceptionHandlerTest.java` — ErrorException → SalException
- `sal-example-adapter/src/main/java/ru/copperside/sal/example/ping/PingController.java` — remove AdapterState
- `sal-example-adapter/src/test/java/ru/copperside/sal/example/ExampleAdapterApplicationTest.java` — remove EventBus mock
- `sal-example-adapter/src/test/java/ru/copperside/sal/example/ping/PingControllerTest.java` — remove AdapterState mock

---

### Task 1: Remove sal-test module and dead sal-api files

Delete the phantom module and all event/watchdog/confirmatory/dto files from sal-api.

**Files:**
- Modify: `pom.xml:22-27` — remove `<module>sal-test</module>`
- Delete: `sal-test/` directory
- Delete: all files listed under "Files to DELETE (sal-api)" above EXCEPT exception classes (those change in Task 4)
- Modify: `sal-api/src/main/java/ru/copperside/sal/api/command/CommandBus.java` — remove confirmatory methods

- [ ] **Step 1: Remove sal-test from parent pom**

In `pom.xml`, remove the `<module>sal-test</module>` line:

```xml
    <modules>
        <module>sal-api</module>
        <module>sal-spring-boot-starter</module>
        <module>sal-example-adapter</module>
    </modules>
```

- [ ] **Step 2: Delete sal-test directory**

```bash
rm -rf sal-test
```

- [ ] **Step 3: Delete sal-api event packages**

```bash
rm -rf sal-api/src/main/java/ru/copperside/sal/api/event
```

- [ ] **Step 4: Delete sal-api watchdog package**

```bash
rm -rf sal-api/src/main/java/ru/copperside/sal/api/watchdog
```

- [ ] **Step 5: Delete confirmatory and dead command interfaces**

```bash
rm sal-api/src/main/java/ru/copperside/sal/api/command/ConfirmatoryCommandHandler.java
rm sal-api/src/main/java/ru/copperside/sal/api/command/ConfirmatoryCommandHandlerAsync.java
rm sal-api/src/main/java/ru/copperside/sal/api/command/ConfirmationResult.java
rm sal-api/src/main/java/ru/copperside/sal/api/command/CommandResultHandler.java
```

- [ ] **Step 6: Delete dto package (ConfirmationResponse, HostedRequest, PingRequest)**

Note: keep `PingResponse.java` — it's used by PingController. Move it to sal-example-adapter in a later task since it's adapter-specific.

```bash
rm sal-api/src/main/java/ru/copperside/sal/api/dto/ConfirmationResponse.java
rm sal-api/src/main/java/ru/copperside/sal/api/dto/service/HostedRequest.java
rm sal-api/src/main/java/ru/copperside/sal/api/dto/service/PingRequest.java
```

- [ ] **Step 7: Remove confirmatory methods from CommandBus interface**

Edit `sal-api/src/main/java/ru/copperside/sal/api/command/CommandBus.java`. Remove the last 6 lines (confirmatory methods). The file becomes:

```java
package ru.copperside.sal.api.command;

import ru.copperside.sal.api.message.RecordedMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Command Bus — send and execute commands across adapters via RabbitMQ.
 * <p>
 * C# origin: {@code TCB.Infrastructure.Command.ICommandBus}
 */
public interface CommandBus {

    /** Fire-and-forget: publish command, don't wait for result. */
    String publishCommand(Command command, String correlationId, CommandPriority priority);

    default String publishCommand(Command command) {
        return publishCommand(command, "", CommandPriority.Normal);
    }

    /** Publish a pre-built RecordedMessage as a command. */
    void publishCommand(RecordedMessage recordMessage, String commandName);

    /** Publish command result back to the sender. */
    void publishCommandResult(CommandResult result, String contextData);

    /** Publish a pre-built RecordedMessage as a command result. */
    void publishCommandResult(RecordedMessage result, String contextData);

    /** Request/reply: send command and wait for result with timeout. */
    <R extends CommandResult> CompletableFuture<R> executeCommandAsync(
            HaveResult<R> command, int timeoutSeconds, CommandPriority priority);

    default <R extends CommandResult> CompletableFuture<R> executeCommandAsync(HaveResult<R> command) {
        return executeCommandAsync(command, 120, CommandPriority.Normal);
    }
}
```

- [ ] **Step 8: Verify sal-api compiles**

```bash
mvn compile -pl sal-api -q
```

Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add -A && git commit -m "refactor: remove sal-test, event/watchdog/confirmatory from sal-api"
```

---

### Task 2: Remove starter subsystems (EventBus, WatchDog, client, online/offline)

Delete all starter files for removed subsystems and update auto-configuration registration.

**Files:**
- Delete: all files listed under "Files to DELETE (sal-spring-boot-starter)" EXCEPT context holders and exception classes
- Modify: `sal-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/WebAutoConfiguration.java`
- Modify: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/WebMvcAutoConfiguration.java`
- Modify: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/lifecycle/AdapterLifecycle.java`
- Modify: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/SalProperties.java`
- Modify: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/command/DefaultCommandBus.java`

- [ ] **Step 1: Delete event package from starter**

```bash
rm -rf sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/event
```

- [ ] **Step 2: Delete watchdog package from starter**

```bash
rm -rf sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/watchdog
```

- [ ] **Step 3: Delete health, client packages and online/offline files**

```bash
rm -rf sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/health
rm -rf sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/client
rm sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/AdapterState.java
rm sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/OfflineCheckInterceptor.java
```

- [ ] **Step 4: Update AutoConfiguration.imports**

Replace contents of `sal-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` with:

```
ru.copperside.sal.starter.SalAutoConfiguration
ru.copperside.sal.starter.serialization.SalSerializationAutoConfiguration
ru.copperside.sal.starter.rabbitmq.SalRabbitAutoConfiguration
ru.copperside.sal.starter.command.CommandBusAutoConfiguration
ru.copperside.sal.starter.command.CommandListenerAutoConfiguration
ru.copperside.sal.starter.web.WebAutoConfiguration
ru.copperside.sal.starter.web.WebMvcAutoConfiguration
```

Removed: `EventBusAutoConfiguration`, `WatchDogAutoConfiguration`.

- [ ] **Step 5: Simplify WebAutoConfiguration — remove AdapterState and OfflineCheckInterceptor beans**

```java
package ru.copperside.sal.starter.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.session.SessionSerializer;

@AutoConfiguration
@ConditionalOnWebApplication
public class WebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EnvironmentKeyInterceptor environmentKeyInterceptor(SalProperties properties) {
        return new EnvironmentKeyInterceptor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SalExceptionHandler salExceptionHandler(SalProperties properties) {
        return new SalExceptionHandler(properties);
    }

    @Bean
    public FilterRegistrationBean<SalContextFilter> salContextFilter(SalProperties properties) {
        FilterRegistrationBean<SalContextFilter> bean = new FilterRegistrationBean<>(new SalContextFilter(properties));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<SessionFilter> sessionFilter(SessionSerializer sessionSerializer) {
        FilterRegistrationBean<SessionFilter> bean = new FilterRegistrationBean<>(new SessionFilter(sessionSerializer));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> bean = new FilterRegistrationBean<>(new RequestLoggingFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return bean;
    }
}
```

- [ ] **Step 6: Simplify WebMvcAutoConfiguration — remove OfflineCheckInterceptor**

```java
package ru.copperside.sal.starter.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration(after = WebAutoConfiguration.class)
@ConditionalOnWebApplication
public class WebMvcAutoConfiguration implements WebMvcConfigurer {

    private final EnvironmentKeyInterceptor environmentKeyInterceptor;

    public WebMvcAutoConfiguration(EnvironmentKeyInterceptor environmentKeyInterceptor) {
        this.environmentKeyInterceptor = environmentKeyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(environmentKeyInterceptor)
                .excludePathPatterns("/actuator/**", "/ping");
    }
}
```

- [ ] **Step 7: Simplify AdapterLifecycle — remove AdapterState dependency**

```java
package ru.copperside.sal.starter.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import ru.copperside.sal.starter.SalProperties;

public class AdapterLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(AdapterLifecycle.class);
    private static final int PHASE = Integer.MAX_VALUE - 100;

    private final String adapterName;
    private volatile boolean running = false;

    public AdapterLifecycle(SalProperties properties) {
        this.adapterName = properties.getAdapter().getName();
    }

    @Override
    public void start() {
        running = true;
        log.info("SAL adapter started: name={}", adapterName);
    }

    @Override
    public void stop() {
        running = false;
        log.info("SAL adapter stopped: name={}", adapterName);
    }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public int getPhase() { return PHASE; }

    @Override
    public boolean isAutoStartup() { return true; }
}
```

- [ ] **Step 8: Clean SalProperties — remove dead properties**

```java
package ru.copperside.sal.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sal")
public class SalProperties {

    private final Adapter adapter = new Adapter();
    private final Service service = new Service();
    private final Client client = new Client();
    private final Command command = new Command();

    public Adapter getAdapter() { return adapter; }
    public Service getService() { return service; }
    public Client getClient() { return client; }
    public Command getCommand() { return command; }

    public static class Adapter {
        private String name = "unnamed-adapter";
        private String type = "GenericAdapter";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class Service {
        private String environmentKey = "";
        private int salVersion = 1;

        public String getEnvironmentKey() { return environmentKey; }
        public void setEnvironmentKey(String environmentKey) { this.environmentKey = environmentKey; }
        public int getSalVersion() { return salVersion; }
        public void setSalVersion(int salVersion) { this.salVersion = salVersion; }
    }

    public static class Client {
        private int requestTimeout = 120;
        private int serviceRequestTimeout = 10;

        public int getRequestTimeout() { return requestTimeout; }
        public void setRequestTimeout(int requestTimeout) { this.requestTimeout = requestTimeout; }
        public int getServiceRequestTimeout() { return serviceRequestTimeout; }
        public void setServiceRequestTimeout(int serviceRequestTimeout) { this.serviceRequestTimeout = serviceRequestTimeout; }
    }

    public static class Command {
        private int threads = 10;
        private int resultThreads = 10;

        public int getThreads() { return threads; }
        public void setThreads(int threads) { this.threads = threads; }
        public int getResultThreads() { return resultThreads; }
        public void setResultThreads(int resultThreads) { this.resultThreads = resultThreads; }
    }
}
```

- [ ] **Step 9: Remove confirmatory method from DefaultCommandBus**

In `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/command/DefaultCommandBus.java`, delete the entire `confirmatoryCommandAsync` method (lines 82-104) and remove the import of `ConfirmationResult`. Also remove unused imports: `ErrorException`, `SalErrorCodes` (if no longer used), `RecordedMessage` usage tied to confirmatory.

The `confirmatoryCommandAsync` method should be completely removed. Keep everything else.

- [ ] **Step 10: Update AdapterLifecycle bean creation**

The `AdapterLifecycle` bean is created in `WatchDogAutoConfiguration` (which we deleted). Move it to `WebAutoConfiguration` or `CommandBusAutoConfiguration`. Add to `CommandBusAutoConfiguration`:

```java
@Bean
@ConditionalOnMissingBean
public AdapterLifecycle adapterLifecycle(SalProperties properties) {
    return new AdapterLifecycle(properties);
}
```

Add import: `import ru.copperside.sal.starter.lifecycle.AdapterLifecycle;`

- [ ] **Step 11: Verify starter compiles**

```bash
mvn compile -pl sal-api,sal-spring-boot-starter -q
```

Expected: BUILD SUCCESS

- [ ] **Step 12: Commit**

```bash
git add -A && git commit -m "refactor: remove EventBus, WatchDog, client, online/offline from starter"
```

---

### Task 3: Simplify sal-example-adapter

Update example adapter to work without removed subsystems.

**Files:**
- Delete: `sal-example-adapter/src/main/java/ru/copperside/sal/example/switch_/SwitchController.java`
- Delete: `sal-example-adapter/src/main/java/ru/copperside/sal/example/watchdog/EndPointEventListener.java`
- Modify: `sal-example-adapter/src/main/java/ru/copperside/sal/example/ping/PingController.java`
- Modify: `sal-example-adapter/src/test/java/ru/copperside/sal/example/ExampleAdapterApplicationTest.java`
- Modify: `sal-example-adapter/src/test/java/ru/copperside/sal/example/ping/PingControllerTest.java`

- [ ] **Step 1: Delete switch and watchdog packages**

```bash
rm -rf sal-example-adapter/src/main/java/ru/copperside/sal/example/switch_
rm -rf sal-example-adapter/src/main/java/ru/copperside/sal/example/watchdog
```

- [ ] **Step 2: Simplify PingController — remove AdapterState**

```java
package ru.copperside.sal.example.ping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sal.api.dto.service.PingResponse;
import ru.copperside.sal.starter.SalProperties;

import java.time.OffsetDateTime;

@RestController
public class PingController {

    private final SalProperties properties;

    public PingController(SalProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        PingResponse response = new PingResponse();
        response.setOnline(true);
        response.setServerTime(OffsetDateTime.now());
        response.setRecipientServiceName(properties.getAdapter().getName());
        response.setSalVersion(properties.getService().getSalVersion());
        return response;
    }
}
```

- [ ] **Step 3: Simplify ExampleAdapterApplicationTest — remove EventBus mock**

```java
package ru.copperside.sal.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.copperside.sal.api.command.CommandBus;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
                "ru.copperside.sal.starter.rabbitmq.SalRabbitAutoConfiguration," +
                "ru.copperside.sal.starter.command.CommandBusAutoConfiguration," +
                "ru.copperside.sal.starter.command.CommandListenerAutoConfiguration"
})
class ExampleAdapterApplicationTest {

    @MockitoBean
    CommandBus commandBus;

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 4: Simplify PingControllerTest — remove AdapterState mock**

```java
package ru.copperside.sal.example.ping;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.copperside.sal.starter.SalProperties;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PingController.class,
        excludeAutoConfiguration = ru.copperside.sal.starter.web.WebAutoConfiguration.class)
class PingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SalProperties salProperties;

    @Test
    void pingReturnsAdapterName() throws Exception {
        SalProperties.Adapter adapter = new SalProperties.Adapter();
        adapter.setName("test-adapter");
        SalProperties.Service service = new SalProperties.Service();
        when(salProperties.getAdapter()).thenReturn(adapter);
        when(salProperties.getService()).thenReturn(service);

        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientServiceName").value("test-adapter"))
                .andExpect(jsonPath("$.online").value(true))
                .andExpect(jsonPath("$.serverTime").exists());
    }
}
```

- [ ] **Step 5: Run all tests**

```bash
mvn test
```

Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "refactor: simplify example adapter — remove WatchDog, EventBus, AdapterState"
```

---

### Task 4: Replace exception hierarchy — SalException + enum

Replace SalBaseException + 3 empty subclasses with a single SalException class.

**Files:**
- Create: `sal-api/src/main/java/ru/copperside/sal/api/exception/SalException.java`
- Delete: `sal-api/src/main/java/ru/copperside/sal/api/exception/SalBaseException.java`
- Delete: `sal-api/src/main/java/ru/copperside/sal/api/exception/ErrorException.java`
- Delete: `sal-api/src/main/java/ru/copperside/sal/api/exception/FatalException.java`
- Delete: `sal-api/src/main/java/ru/copperside/sal/api/exception/ValidationException.java`
- Modify: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/SalExceptionHandler.java`
- Modify: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/EnvironmentKeyInterceptor.java`
- Modify: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/command/CommandTimeoutWatcher.java`
- Modify: `sal-spring-boot-starter/src/test/java/ru/copperside/sal/starter/web/SalExceptionHandlerTest.java`

- [ ] **Step 1: Create SalException**

```java
package ru.copperside.sal.api.exception;

import java.time.Instant;

public class SalException extends RuntimeException {

    public enum Type { ERROR, FATAL, VALIDATION }

    private final Type type;
    private String code;
    private String codeDescription;
    private String adapterName;
    private String sourceType;
    private String sourcePath;
    private String sessionId;
    private String sourceId;
    private Instant timeStamp;
    private Object properties;

    private SalException(Type type, String message) {
        super(message);
        this.type = type;
    }

    private SalException(Type type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    private SalException(Type type, InfrastructureExceptionDTO data) {
        super(data.getMessage());
        this.type = type;
        this.code = data.getCode();
        this.codeDescription = data.getCodeDescription();
        this.adapterName = data.getAdapterName();
        this.sourceType = data.getSourceType();
        this.sessionId = data.getSessionId();
        this.sourceId = data.getSourceId();
        this.sourcePath = data.getSourcePath();
        this.timeStamp = data.getTimeStamp();
        this.properties = data.getProperties();
    }

    public static SalException error(String message) {
        return new SalException(Type.ERROR, message);
    }

    public static SalException error(String code, String message) {
        SalException ex = new SalException(Type.ERROR, message);
        ex.setCode(code);
        return ex;
    }

    public static SalException fatal(String message) {
        return new SalException(Type.FATAL, message);
    }

    public static SalException validation(String message) {
        return new SalException(Type.VALIDATION, message);
    }

    public static SalException fromDto(Type type, InfrastructureExceptionDTO data) {
        return new SalException(type, data);
    }

    public Type getType() { return type; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCodeDescription() { return codeDescription; }
    public void setCodeDescription(String codeDescription) { this.codeDescription = codeDescription; }

    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public Instant getTimeStamp() { return timeStamp; }
    public void setTimeStamp(Instant timeStamp) { this.timeStamp = timeStamp; }

    public Object getProperties() { return properties; }
    public void setProperties(Object properties) { this.properties = properties; }
}
```

- [ ] **Step 2: Delete old exception classes**

```bash
rm sal-api/src/main/java/ru/copperside/sal/api/exception/SalBaseException.java
rm sal-api/src/main/java/ru/copperside/sal/api/exception/ErrorException.java
rm sal-api/src/main/java/ru/copperside/sal/api/exception/FatalException.java
rm sal-api/src/main/java/ru/copperside/sal/api/exception/ValidationException.java
```

- [ ] **Step 3: Update SalExceptionHandler**

Replace `SalBaseException` → `SalException`:

```java
    @ExceptionHandler(SalException.class)
    public ResponseEntity<InfrastructureExceptionDTO> handleSalException(SalException ex) {
        log.warn("SAL exception [{}]: {}", ex.getCode(), ex.getMessage());
        InfrastructureExceptionDTO dto = toDto(ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);
    }
```

Update `toDto` method signature: `private InfrastructureExceptionDTO toDto(SalException ex)` — the body stays the same since SalException has the same getters.

- [ ] **Step 4: Update EnvironmentKeyInterceptor**

Replace `ErrorException` with `SalException.error(...)`:

```java
import ru.copperside.sal.api.exception.SalException;
// ...
            SalException ex = SalException.error(SalErrorCodes.MISMATCH_ENVIRONMENT_KEY, "Environment key mismatch");
            ex.setSourceType(ExceptionSourceType.SAL);
            throw ex;
```

- [ ] **Step 5: Update CommandTimeoutWatcher**

Replace `new ErrorException("Command execution timeout")` with `SalException.error(SalErrorCodes.COMMAND_EXECUTION_TIMEOUT, "Command execution timeout")`. Same for the abort case.

- [ ] **Step 6: Update SalExceptionHandlerTest**

```java
    @Test
    void salExceptionReturns500WithDto() {
        SalException ex = SalException.error(SalErrorCodes.ADAPTER_IS_OFFLINE, "Adapter offline");
        ex.setCode(SalErrorCodes.ADAPTER_IS_OFFLINE);

        var response = handler.handleSalException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        InfrastructureExceptionDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(SalErrorCodes.ADAPTER_IS_OFFLINE);
    }
```

- [ ] **Step 7: Verify compilation and tests**

```bash
mvn test -pl sal-api,sal-spring-boot-starter
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "refactor: collapse exception hierarchy into SalException + enum Type"
```

---

### Task 5: Consolidate ThreadLocal holders into SalContext

Replace SessionHolder, CommandContextHolder, and SalMdc with a single SalContext class.

**Files:**
- Create: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/context/SalContext.java`
- Delete: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/context/SessionHolder.java`
- Delete: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/context/CommandContextHolder.java`
- Delete: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/context/SalMdc.java`
- Modify: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/command/CommandConsumer.java`
- Modify: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/command/CommandPublisher.java`
- Modify: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/SalContextFilter.java`
- Modify: `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/web/SessionFilter.java`
- Modify: `sal-spring-boot-starter/src/test/java/ru/copperside/sal/starter/command/DefaultCommandBusTest.java`

- [ ] **Step 1: Create SalContext**

```java
package ru.copperside.sal.starter.context;

import org.slf4j.MDC;
import ru.copperside.sal.api.command.CommandContext;

import java.util.Map;

/**
 * Unified thread-local context for SAL request processing.
 * Replaces SessionHolder + CommandContextHolder + SalMdc.
 */
public final class SalContext {

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_SESSION_ID = "sessionId";
    public static final String MDC_ADAPTER_NAME = "adapterName";

    private static final ThreadLocal<Map<String, Object>> SESSION = new ThreadLocal<>();
    private static final ThreadLocal<CommandContext> COMMAND_CONTEXT = new ThreadLocal<>();

    private SalContext() {}

    // --- Session ---

    public static Map<String, Object> session() {
        return SESSION.get();
    }

    public static void setSession(Map<String, Object> session) {
        SESSION.set(session);
    }

    public static String sessionId() {
        Map<String, Object> session = SESSION.get();
        if (session == null) return null;
        Object sid = session.get("SessionId");
        return sid != null ? sid.toString() : null;
    }

    public static String operationId() {
        Map<String, Object> session = SESSION.get();
        if (session == null) return null;
        Object oid = session.get("OperationId");
        return oid != null ? oid.toString() : null;
    }

    // --- Command Context ---

    public static CommandContext commandContext() {
        return COMMAND_CONTEXT.get();
    }

    public static void setCommandContext(CommandContext ctx) {
        COMMAND_CONTEXT.set(ctx);
    }

    // --- MDC ---

    public static void setMdc(String correlationId, String sessionId, String adapterName) {
        if (correlationId != null) MDC.put(MDC_CORRELATION_ID, correlationId);
        if (sessionId != null) MDC.put(MDC_SESSION_ID, sessionId);
        if (adapterName != null) MDC.put(MDC_ADAPTER_NAME, adapterName);
    }

    // --- Lifecycle ---

    public static void clear() {
        SESSION.remove();
        COMMAND_CONTEXT.remove();
        MDC.remove(MDC_CORRELATION_ID);
        MDC.remove(MDC_SESSION_ID);
        MDC.remove(MDC_ADAPTER_NAME);
    }
}
```

- [ ] **Step 2: Delete old holders**

```bash
rm sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/context/SessionHolder.java
rm sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/context/CommandContextHolder.java
rm sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/context/SalMdc.java
```

- [ ] **Step 3: Update CommandConsumer to use SalContext**

Replace all occurrences:
- `SessionHolder.get()` → `SalContext.session()`
- `SessionHolder.set(...)` → `SalContext.setSession(...)`
- `SessionHolder.clear()` → (handled by `SalContext.clear()`)
- `SalMdc.set(...)` → `SalContext.setMdc(...)`
- `SalMdc.clear()` → (handled by `SalContext.clear()`)
- `CommandContextHolder.set(...)` → `SalContext.setCommandContext(...)`
- `CommandContextHolder.clear()` → (handled by `SalContext.clear()`)

In the finally block, replace 3 clear calls with single `SalContext.clear()`.

In the async callback (whenComplete), replace:
```java
if (capturedSession != null) SessionHolder.set(capturedSession);
SalMdc.set(capturedCorrelationId, null, null);
```
with:
```java
if (capturedSession != null) SalContext.setSession(capturedSession);
SalContext.setMdc(capturedCorrelationId, null, null);
```

And in the callback's finally:
```java
SalContext.clear();
```

Update imports: remove SessionHolder, CommandContextHolder, SalMdc. Add SalContext.

- [ ] **Step 4: Update CommandPublisher to use SalContext**

Replace `SessionHolder.get()` → `SalContext.session()`. Update import.

- [ ] **Step 5: Update SalContextFilter to use SalContext**

```java
package ru.copperside.sal.starter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.copperside.sal.api.constant.Headers;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.context.SalContext;

import java.io.IOException;
import java.util.UUID;

public class SalContextFilter extends OncePerRequestFilter {

    private final SalProperties properties;

    public SalContextFilter(SalProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(Headers.OPERATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        SalContext.setMdc(correlationId, SalContext.sessionId(), properties.getAdapter().getName());
        try {
            filterChain.doFilter(request, response);
        } finally {
            SalContext.clear();
        }
    }
}
```

- [ ] **Step 6: Update SessionFilter to use SalContext**

Replace `SessionHolder.set(session)` → `SalContext.setSession(session)`. Update import.

- [ ] **Step 7: Update DefaultCommandBusTest**

Replace tests for old holders with SalContext tests:

```java
    @Test
    void salContext_session_shouldBeThreadLocal() {
        SalContext.setSession(Map.of("SessionId", "test-123", "OperationId", 42));
        assertThat(SalContext.sessionId()).isEqualTo("test-123");
        assertThat(SalContext.operationId()).isEqualTo("42");

        SalContext.clear();
        assertThat(SalContext.session()).isNull();
    }

    @Test
    void salContext_commandContext_shouldBeThreadLocal() {
        var ctx = new ru.copperside.sal.api.command.CommandContext();
        ctx.setCorrelationId("test-cid");
        SalContext.setCommandContext(ctx);

        assertThat(SalContext.commandContext().getCorrelationId()).isEqualTo("test-cid");

        SalContext.clear();
        assertThat(SalContext.commandContext()).isNull();
    }

    @Test
    void salContext_mdc_shouldSetAndClear() {
        SalContext.setMdc("cid-1", "sid-1", "adapter-1");
        assertThat(org.slf4j.MDC.get("correlationId")).isEqualTo("cid-1");
        assertThat(org.slf4j.MDC.get("sessionId")).isEqualTo("sid-1");

        SalContext.clear();
        assertThat(org.slf4j.MDC.get("correlationId")).isNull();
    }
```

- [ ] **Step 8: Run all tests**

```bash
mvn test
```

Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add -A && git commit -m "refactor: consolidate SessionHolder + CommandContextHolder + SalMdc into SalContext"
```

---

### Task 6: Final verification and cleanup

Run full build, verify no dead imports or references remain.

**Files:** None new — verification only.

- [ ] **Step 1: Run full build with tests**

```bash
mvn clean test
```

Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 2: Check for dangling references**

```bash
grep -r "EventBus\|WatchDog\|AdapterState\|OfflineCheck\|SessionHolder\|CommandContextHolder\|SalMdc\|ErrorException\|FatalException\|ValidationException\|SalBaseException\|ConfirmatoryCommand\|CommandResultHandler\|ConfirmationResult" --include="*.java" sal-api/src sal-spring-boot-starter/src sal-example-adapter/src | grep -v "target/" | grep -v "Test"
```

Expected: No matches (or only comments/javadoc references that are acceptable).

- [ ] **Step 3: Clean up empty directories**

```bash
find sal-api/src sal-spring-boot-starter/src sal-example-adapter/src -type d -empty -delete
```

- [ ] **Step 4: Run tests one more time**

```bash
mvn test
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Final commit (if any cleanup was needed)**

```bash
git add -A && git commit -m "chore: final cleanup after architectural simplification"
```
