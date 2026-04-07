# Архитектурное упрощение tcb-sal

**Дата:** 2026-04-07
**Статус:** Draft
**Подход:** B — глубокая расчистка с сохранением wire-совместимости

## Цель

Упростить SAL-фреймворк, убрав подсистемы не связанные с обработкой команд:
EventBus, WatchDog, REST-клиент, online/offline state management. Параллельно
провести архитектурные упрощения (exception hierarchy, ThreadLocal консолидация).

Результат: кодовая база фокусируется на одной задаче — command processing через RabbitMQ.

## Ограничения

- Wire-совместимость с C# не нарушается (PascalCase JSON, AMQP property mapping, type mapping)
- Поток обработки команд (fire-and-forget, request/reply) не меняется
- Публичный контракт CommandBus сохраняется (кроме удаления confirmatory pattern)

## 1. Удаление подсистем

### 1.1 EventBus

**Удаляем:**
- `sal-api/event/` — весь пакет (~14 файлов): EventBus interface, Event base class, AdapterOnlineEvent, AdapterOfflineEvent, MonitorFailureEvent, EndPointUpEvent, EndPointDownEvent и т.д.
- `sal-spring-boot-starter/event/` — DefaultEventBus, EventBusAutoConfiguration

**Обоснование:** EventBus — pub/sub канал через RabbitMQ fanout exchanges. Команды используют отдельный point-to-point канал (direct exchange). Удаление EventBus не влияет на command pipeline.

### 1.2 WatchDog

**Удаляем:**
- `sal-spring-boot-starter/watchdog/` — весь пакет: EndPointRegistry, EndPointsAvailableScheduler, EndPointsRemoverScheduler, WatchDogService, WatchDogAutoConfiguration
- `sal-spring-boot-starter/health/AdapterStateHealthIndicator.java`
- `sal-api/watchdog/` — EndPointInfo, DependencyAdapter

**Обоснование:** WatchDog мониторит HTTP-эндпоинты пингами и управляет online/offline состоянием. Команды потребляются из RabbitMQ-очередей независимо от этого состояния.

### 1.3 REST-клиент

**Удаляем:**
- `sal-spring-boot-starter/client/SalRestClient.java`

**Обоснование:** Используется только WatchDog-подсистемой (EndPointsAvailableScheduler). После удаления WatchDog — не имеет потребителей.

### 1.4 Online/Offline state

**Удаляем:**
- `sal-spring-boot-starter/web/AdapterState.java`
- `sal-spring-boot-starter/web/OfflineCheckInterceptor.java`

**Упрощаем:**
- `AdapterLifecycle` — убираем зависимость от AdapterState, оставляем только graceful shutdown через volatile boolean (shutDown flag встраивается в сам AdapterLifecycle)

### 1.5 Confirmatory command pattern

**Удаляем:**
- `sal-api/command/ConfirmatoryCommandHandler.java`
- `sal-api/command/ConfirmatoryCommandHandlerAsync.java`
- `sal-api/command/CommandResultHandler.java`
- `sal-api/command/ConfirmationResult.java`
- `sal-api/dto/ConfirmationResponse.java`
- `CommandBus.confirmatoryCommandAsync()` — 2 перегрузки из интерфейса
- `DefaultCommandBus.confirmatoryCommandAsync()` — реализация

**Обоснование:** Ноль реализаций ConfirmatoryCommandHandler в кодовой базе.

### 1.6 sal-test модуль

**Удаляем:**
- `sal-test/` — весь модуль
- Ссылку из parent `pom.xml` `<modules>`

**Обоснование:** Модуль пуст — ни одного Java-файла. Phantom-модуль.

### 1.7 Мёртвые свойства SalProperties

**Удаляем поля:**
- `Service.mbMode`
- `Service.adapterDependency`
- `Service.dataPath`
- `Service.diskStorePath`
- `Service.enableOfflineMode` (OfflineCheckInterceptor удалён)
- `Service.minEpSalVersion`
- `Service.maxEpSalVersion`
- Вложенный класс `WatchDog` целиком (pingIntervalMs, removerIntervalMs)

**Остаётся:**
- `Adapter` (name, type)
- `Service` (environmentKey, salVersion)
- `Command` (threads, resultThreads)
- `Client` (requestTimeout, serviceRequestTimeout)
- `Event` — удаляем вместе с EventBus

### 1.8 Изменения в sal-example-adapter

**Удаляем:**
- `switch_/SwitchController.java` (зависел от WatchDogService)
- `watchdog/EndPointEventListener.java` (зависел от EndPointRegistry)

**Упрощаем:**
- `PingController` — убираем зависимость от AdapterState, оставляем простой health-check (имя адаптера + версия)
- `ExampleAdapterApplicationTest` — убираем проверку EventBus бина

## 2. Архитектурные упрощения

### 2.1 Exception hierarchy → SalException + enum

**Сейчас:**
```
SalBaseException (abstract, 9 полей)
  ├── ErrorException
  ├── FatalException
  └── ValidationException
```

**Станет:**
```java
public class SalException extends RuntimeException {
    public enum Type { ERROR, FATAL, VALIDATION }

    private final Type type;
    private int code;
    private String codeDescription;
    private String adapterName;
    private ExceptionSourceType sourceType;
    private String sourcePath;
    private String sourceId;
    private String sessionId;
    private Instant timeStamp;
    private Map<String, String> properties;

    // Static factory methods
    public static SalException error(int code) { ... }
    public static SalException fatal(int code) { ... }
    public static SalException validation(int code) { ... }
}
```

**Миграция:**
- `throw new ErrorException(code)` → `throw SalException.error(code)`
- `catch (ErrorException e)` → `catch (SalException e)` + проверка `e.getType()`
- `SalExceptionHandler` ловит один тип, различает по `getType()`

### 2.2 ThreadLocal консолидация → SalContext

**Сейчас:** SessionHolder, CommandContextHolder, SalMdc — три отдельных ThreadLocal.

**Станет:**
```java
public final class SalContext {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    public record Context(
        Map<String, Object> session,
        CommandContext commandContext,
        String correlationId
    ) {}

    // Lifecycle
    public static void set(Context ctx) {
        CURRENT.set(ctx);
        if (ctx.correlationId() != null) {
            MDC.put("correlationId", ctx.correlationId());
        }
    }

    public static void clear() {
        CURRENT.remove();
        MDC.remove("correlationId");
        MDC.remove("sessionId");
        MDC.remove("adapterName");
    }

    // Convenience accessors
    public static Context get() { return CURRENT.get(); }
    public static Map<String, Object> session() { ... }
    public static String sessionId() { ... }
    public static String operationId() { ... }
    public static String correlationId() { ... }
}
```

**Точки изменения:**
- `CommandConsumer.onMessage()` — set/clear SalContext вместо 3 отдельных вызовов
- `SessionFilter` — set SalContext (session + correlationId из HTTP)
- `SalContextFilter` — удаляется, логика MDC встроена в SalContext.set()
- `CommandPublisher`, `DefaultEventBus` — SessionHolder.get() → SalContext.session()
- Async callback в CommandConsumer — захватывает/восстанавливает один SalContext.Context

### 2.3 AdapterLifecycle упрощение

**Станет:**
```java
public class AdapterLifecycle implements SmartLifecycle {
    private volatile boolean running = false;
    private volatile boolean shutDown = false;

    public void start() {
        running = true;
        log.info("SAL adapter started: name={}", adapterName);
    }

    public void stop() {
        shutDown = true;
        running = false;
        log.info("SAL adapter stopped");
    }

    public boolean isShutDown() { return shutDown; }
}
```

Graceful shutdown сохраняется. Метод `isShutDown()` используется в web-фильтрах если нужно отклонять HTTP-запросы при остановке.

### 2.4 Auto-configuration слияние

**Сейчас (9):** SalAutoConfiguration, SalSerializationAutoConfiguration, SalRabbitAutoConfiguration, CommandBusAutoConfiguration, CommandListenerAutoConfiguration, EventBusAutoConfiguration, WebAutoConfiguration, WebMvcAutoConfiguration, WatchDogAutoConfiguration

**Станет (5):**

| Класс | Содержимое |
|---|---|
| SalAutoConfiguration | SalProperties bean |
| SalSerializationAutoConfiguration | WireObjectMapper, TypeMappingRegistry, TypeScannerRegistrar |
| SalRabbitAutoConfiguration | RabbitTemplate, ConnectionFactory настройка, SalMessageConverter, SalTopologyConfigurer |
| SalCommandAutoConfiguration | DefaultCommandBus, CommandPublisher, CommandConsumer, CommandHandlerRegistry, CommandListenerRegistrar, CommandResultConsumer, CommandTimeoutWatcher |
| SalWebAutoConfiguration | SessionFilter, SalContextFilter (или его замена), RequestLoggingFilter, SalExceptionHandler, WebMvc registrations |

## 3. Целевая структура файлов

### sal-api
```
ru.copperside.sal.api/
  annotation/
    CommandType.java
    ServiceMessage.java
  command/
    Command.java
    CommandBus.java              — без confirmatory methods
    CommandContext.java
    CommandHandler.java
    CommandHandlerAsync.java
    CommandPriority.java
    CommandResult.java
    FailedResult.java
    HaveResult.java
  constant/
    Headers.java                 — HTTP header names (wire-протокол)
    SessionKeys.java             — Session key names
  exception/
    SalException.java            — заменяет SalBaseException + 3 подкласса
    SalErrorCodes.java
    ExceptionSourceType.java
    InfrastructureExceptionDTO.java
  message/
    RecordedMessage.java
    MessageDataKeys.java
    MessageRejected.java
```

Удалённые пакеты: `event/` (целиком), `watchdog/` (целиком), `dto/` (целиком).

### sal-spring-boot-starter
```
ru.copperside.sal.starter/
  SalProperties.java             — без WatchDog, мёртвых полей, Event
  config/
    SalAutoConfiguration.java
    SalSerializationAutoConfiguration.java
    SalRabbitAutoConfiguration.java
    SalCommandAutoConfiguration.java
    SalWebAutoConfiguration.java
  context/
    SalContext.java               — единый ThreadLocal holder
  command/
    DefaultCommandBus.java
    CommandPublisher.java
    CommandConsumer.java
    CommandHandlerRegistry.java
    CommandListenerRegistrar.java
    CommandResultConsumer.java
    CommandTimeoutWatcher.java
    PendingCommand.java
  serialization/
    TypeMappingRegistry.java
    WireObjectMapper.java
    TypeScannerRegistrar.java
  session/
    SessionSerializer.java
  rabbitmq/
    SalMessageConverter.java
    SalRabbitConstants.java
    SalTopologyConfigurer.java
  web/
    SalExceptionHandler.java
    SessionFilter.java
    RequestLoggingFilter.java
  lifecycle/
    AdapterLifecycle.java
```

### sal-example-adapter
```
ru.copperside.sal.example/
  ExampleAdapterApplication.java
  echo/
    EchoCommand.java
    EchoResult.java
    EchoCommandHandler.java
  ping/
    PingController.java
```

## 4. Метрики

| Метрика | До | После | Δ |
|---|---|---|---|
| Java файлов | ~118 | ~45 | −62% |
| Строк кода | ~4,858 | ~2,800 | −42% |
| Auto-config классов | 9 | 5 | −44% |
| ThreadLocal holders | 3 | 1 | −67% |
| Exception классов | 4 | 1 | −75% |
| Config properties | 17 | 9 | −47% |
| Lifecycle hooks | 4 типа | 2 типа | −50% |

## 5. Риски

| Риск | Митигация |
|---|---|
| Адаптер-потребитель использовал EventBus | EventBus не имеет подписчиков в Java; C# события идут через свои каналы |
| Нужен мониторинг эндпоинтов в будущем | Spring Boot Actuator + k8s probes покрывают этот сценарий |
| Online/offline toggle для canary deploys | Управление трафиком на уровне инфраструктуры (k8s, load balancer) |
| Breaking change в CommandBus API (confirmatory) | Нет реализаций — нет потребителей |
