# Руководство по разработке адаптеров на базе sal-spring-boot-starter

## Содержание

1. [Quick Start — создание адаптера за 5 шагов](#1-quick-start)
2. [Структура проекта адаптера](#2-структура-проекта-адаптера)
3. [Работа с командами — определение типов](#3-работа-с-командами--определение-типов)
4. [Синхронный CommandHandler](#4-синхронный-commandhandler)
5. [Асинхронный CommandHandlerAsync](#5-асинхронный-commandhandlerasync)
6. [ConfirmatoryCommandHandler](#6-confirmatory-commandhandler)
7. [Отправка команд другим адаптерам](#7-отправка-команд-другим-адаптерам)
8. [Работа с событиями](#8-работа-с-событиями)
9. [Сессия](#9-сессия)
10. [Регистрация типов для C# interop](#10-регистрация-типов-для-c-interop)
11. [Обработка ошибок](#11-обработка-ошибок)
12. [Тестирование](#12-тестирование)
13. [Чеклист перед деплоем](#13-чеклист-перед-деплоем)
14. [См. также](#14-см-также)

---

## 1. Quick Start

### Шаг 1 — pom.xml

Создайте Maven-проект с единственной зависимостью `sal-spring-boot-starter`. Всё остальное (Spring Boot core, RabbitMQ, Jackson) притягивается транзитивно. Адаптер запускается как non-web Spring Boot приложение (`spring.main.web-application-type=none`): HTTP-слой, CORS и Actuator-эндпоинты не подключаются.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-adapter</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.5</version>
    </parent>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>ru.copperside</groupId>
            <artifactId>sal-spring-boot-starter</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### Шаг 2 — application.yml

Минимальная конфигурация. Обязательные поля: `sal.adapter.name`, `sal.adapter.type`, параметры RabbitMQ.

```yaml
sal:
  adapter:
    name: my-adapter          # уникальное имя адаптера в кластере
    type: MyAdapter           # тип (совпадает с C# AdapterType)
  service:
    environment-key: ""       # ключ среды; "" — не проверять
    enable-offline-mode: false
  client:
    request-timeout: 120      # секунды
  command:
    threads: 10
    result-threads: 10
  event:
    threads: 1

spring:
  application:
    name: ${sal.adapter.name}
  main:
    web-application-type: none   # адаптер — non-web Spring Boot приложение
  rabbitmq:
    host: localhost
    port: 5672
    virtual-host: dev
    username: guest
    password: guest
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### Шаг 3 — @SpringBootApplication

```java
package com.example.myadapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyAdapterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyAdapterApplication.class, args);
    }
}
```

### Шаг 4 — простой CommandHandler

```java
package com.example.myadapter.command;

import org.springframework.stereotype.Component;
import ru.copperside.sal.api.annotation.CommandType;
import ru.copperside.sal.api.command.CommandHandler;
import ru.copperside.sal.api.command.CommandResult;
import ru.copperside.sal.api.command.HaveResult;

// --- Команда ---
@CommandType("com.example.myadapter.GreetCommand")
public class GreetCommand implements HaveResult<GreetCommand.Result> {

    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public static class Result implements CommandResult {
        private String greeting;
        public String getGreeting() { return greeting; }
        public void setGreeting(String greeting) { this.greeting = greeting; }
    }
}

// --- Обработчик ---
@Component
public class GreetCommandHandler
        implements CommandHandler<GreetCommand, GreetCommand.Result> {

    @Override
    public GreetCommand.Result execute(GreetCommand command) {
        GreetCommand.Result result = new GreetCommand.Result();
        result.setGreeting("Hello, " + command.getName() + "!");
        return result;
    }
}
```

### Шаг 5 — запуск

```bash
# 1. Поднять RabbitMQ (если нет docker-compose в проекте — минимальный вариант):
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# 2. Запустить адаптер:
mvn spring-boot:run
```

Ожидаемый вывод при старте (фрагмент):

```
INFO  r.c.s.s.command.CommandHandlerRegistry  : Registered CommandHandler: GreetCommand -> GreetCommandHandler
INFO  r.c.sal.starter.SalAutoConfiguration    : SAL adapter 'my-adapter' (type=MyAdapter) started
INFO  r.c.s.s.command.CommandListenerRegistrar: Started RabbitMQ listeners for adapter 'my-adapter'
```

---

## 2. Структура проекта адаптера

Рекомендуемая раскладка пакетов:

```
com.example.myadapter/
├── MyAdapterApplication.java       # точка входа
├── command/                        # команды и их обработчики
│   ├── GreetCommand.java
│   └── GreetCommandHandler.java
├── event/                          # доменные события и подписчики
│   ├── OrderCreatedEvent.java
│   └── OrderCreatedEventHandler.java
└── config/                         # дополнительная конфигурация (TypeMapping и т.п.)
    └── TypeMappingConfig.java
```

**Правило именования пакетов:** держите команды и события в отдельных пакетах — это облегчает поиск и уменьшает циклические зависимости.

---

## 3. Работа с командами — определение типов

### Иерархия интерфейсов

```
Command                   ← маркерный интерфейс (все команды)
└── HaveResult<R>         ← команда с ответом типа R
```

```
CommandResult             ← маркерный интерфейс (все результаты)
├── EchoResult
├── ConfirmationResult    ← используется в двухфазном flow
└── Nothing               ← fire-and-forget без результата
```

### @CommandType — ключевая аннотация

`@CommandType` задаёт полное имя C#-типа, которое записывается в заголовок wire-сообщения. Без неё фреймворк не сможет смаппировать входящее сообщение на Java-класс и выбросит `SalException` типа `ERROR` с кодом `UnknownCommandResultType`.

```java
@CommandType("TCB.KCProcessing.Client.ReversalCommand")
public class ReversalCommand implements HaveResult<ReversalResult> {

    private String transactionId;
    private java.math.BigDecimal amount;

    // геттеры / сеттеры
}
```

**Правило:** значение в `@CommandType` должно точно совпадать со строкой, которую передаёт C#-клиент. Обычно это `<Namespace>.<ClassName>` без сборки.

### Команда без результата (fire-and-forget)

Для команды, которая не возвращает ответ, реализуйте `Command` напрямую (не `HaveResult`):

```java
@CommandType("com.example.myadapter.NotifyCommand")
public class NotifyCommand implements ru.copperside.sal.api.command.Command {

    private String message;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
```

Такую команду отправляют через `CommandBus.publishCommand()` — без ожидания ответа.

---

## 4. Синхронный CommandHandler

`CommandHandler<C, R>` — основной способ обработки команд. Реализуйте интерфейс и пометьте класс `@Component` — автоконфигурация зарегистрирует его автоматически при старте.

```java
package ru.copperside.sal.example.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.copperside.sal.api.command.CommandHandler;

@Component
public class EchoCommandHandler implements CommandHandler<EchoCommand, EchoResult> {

    private static final Logger log = LoggerFactory.getLogger(EchoCommandHandler.class);

    @Override
    public EchoResult execute(EchoCommand command) {
        log.info("Handling EchoCommand: {}", command.getPayload());
        EchoResult result = new EchoResult();
        result.setEcho(command.getPayload());
        return result;
    }
}
```

Где `EchoCommand` и `EchoResult`:

```java
@CommandType("ru.copperside.sal.example.EchoCommand")
public class EchoCommand implements HaveResult<EchoResult> {
    private String payload;
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}

public class EchoResult implements CommandResult {
    private String echo;
    public String getEcho() { return echo; }
    public void setEcho(String echo) { this.echo = echo; }
}
```

**Важно:** один тип команды — один обработчик. Если зарегистрировать два `CommandHandler` для одной команды, фреймворк бросит исключение при старте.

### Внедрение зависимостей

`CommandHandler` — обычный Spring Bean, поэтому можно внедрять репозитории, сервисы и любые другие компоненты:

```java
@Component
public class CreateOrderCommandHandler
        implements CommandHandler<CreateOrderCommand, CreateOrderResult> {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public CreateOrderCommandHandler(OrderRepository orderRepository,
                                     NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    @Override
    public CreateOrderResult execute(CreateOrderCommand command) {
        Order order = orderRepository.save(new Order(command.getItems()));
        notificationService.orderCreated(order.getId());
        CreateOrderResult result = new CreateOrderResult();
        result.setOrderId(order.getId().toString());
        return result;
    }
}
```

---

## 5. Асинхронный CommandHandlerAsync

Используйте `CommandHandlerAsync<C, R>` когда обработка команды:
- обращается к медленным I/O-операциям (внешний HTTP, база данных с реактивным драйвером),
- сама по себе требует ожидания нескольких параллельных задач,
- выполняется во внешнем пуле потоков.

```java
@Component
public class FetchRatesCommandHandler
        implements CommandHandlerAsync<FetchRatesCommand, FetchRatesResult> {

    private final RatesHttpClient httpClient;

    public FetchRatesCommandHandler(RatesHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public CompletableFuture<FetchRatesResult> executeAsync(FetchRatesCommand command) {
        return httpClient.getRatesAsync(command.getCurrencyCode())
                .thenApply(rates -> {
                    FetchRatesResult result = new FetchRatesResult();
                    result.setRates(rates);
                    return result;
                });
    }
}
```

### Предупреждение о ThreadLocal в async-коллбэках

`SalContext` хранит сессию, `CommandContext` и correlationId в `ThreadLocal`. В синхронных обработчиках это работает прозрачно — поток тот же. Но в асинхронных коллбэках (`thenApply`, `thenCompose`, `handle`) поток может смениться. `CommandConsumer` уже восстанавливает полный `ContextSnapshot` в финальном `whenComplete`, но если вы сами создаёте промежуточные callback-и — захватите нужные данные в локальные переменные заранее:

```java
@Override
public CompletableFuture<MyResult> executeAsync(MyCommand command) {
    // ПРАВИЛЬНО: читаем сессию ДО перехода в другой поток
    Map<String, Object> session = SalContext.session();
    String sessionId = session != null ? (String) session.get("SessionId") : null;

    return someAsyncOp()
            .thenApply(data -> {
                // НЕПРАВИЛЬНО: SalContext.session() здесь может вернуть null
                // ПРАВИЛЬНО: используем sessionId, захваченный выше
                MyResult result = new MyResult();
                result.setProcessedBy(sessionId);
                return result;
            });
}
```

**Правило:** если вам нужны данные сессии в коллбэке — захватите их в локальную переменную до начала асинхронных операций.

---

## 6. ConfirmatoryCommandHandler

Двухфазный flow используется, когда операция требует подтверждения пользователя (например, "Удалить? Да/Нет") или предварительной валидации перед исполнением.

**Фаза 1:** `confirmatoryExecute()` — проверка условий, возврат `ConfirmationResult`.
**Фаза 2:** `execute()` — выполнение самой операции после получения подтверждения.

```java
@Component
public class DeleteAccountCommandHandler
        implements ConfirmatoryCommandHandler<DeleteAccountCommand, DeleteAccountResult> {

    private final AccountService accountService;

    public DeleteAccountCommandHandler(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Фаза 1 — проверка, нужно ли подтверждение.
     */
    @Override
    public ConfirmationResult confirmatoryExecute(DeleteAccountCommand command) {
        boolean hasActiveOrders = accountService.hasActiveOrders(command.getAccountId());

        ConfirmationResult confirmation = new ConfirmationResult();
        confirmation.setConfirmationRequired(hasActiveOrders);
        if (hasActiveOrders) {
            confirmation.setConfirmationDataType("ActiveOrdersWarning");
            confirmation.setConfirmationData(
                    accountService.countActiveOrders(command.getAccountId()));
        }
        return confirmation;
    }

    /**
     * Фаза 2 — выполнение после подтверждения.
     */
    @Override
    public DeleteAccountResult execute(DeleteAccountCommand command) {
        accountService.delete(command.getAccountId());
        DeleteAccountResult result = new DeleteAccountResult();
        result.setDeleted(true);
        return result;
    }
}
```

`ConfirmationResult`:
- `confirmationRequired = false` → фреймворк сразу вызывает `execute()`,
- `confirmationRequired = true` → результат отправляется клиенту; после получения подтверждения клиент повторно вызывает команду, и фреймворк уже переходит к `execute()`.

---

## 7. Отправка команд другим адаптерам

Внедрите `CommandBus` и используйте нужный метод в зависимости от сценария.

### Fire-and-forget (без ожидания результата)

```java
@Component
public class OrderService {

    private final CommandBus commandBus;

    public OrderService(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    public void notifyShipping(String orderId) {
        ShipOrderCommand command = new ShipOrderCommand();
        command.setOrderId(orderId);

        // публикуем и не ждём ответа
        commandBus.publishCommand(command);

        // или с явным приоритетом и correlationId:
        commandBus.publishCommand(command, "corr-" + orderId, CommandPriority.High);
    }
}
```

### Request/reply (с ожиданием результата)

```java
public CompletableFuture<PaymentResult> processPayment(String orderId, BigDecimal amount) {
    PaymentCommand command = new PaymentCommand();
    command.setOrderId(orderId);
    command.setAmount(amount);

    return commandBus.<PaymentResult>executeCommandAsync(
            command,
            30,                      // таймаут в секундах
            CommandPriority.Normal   // приоритет
    );
}
```

### CommandPriority — значения

| Константа        | Числовое значение | Использование                              |
|------------------|-------------------|--------------------------------------------|
| `Idle`           | 0                 | Фоновые задачи низкой важности             |
| `BelowNormal`    | 4                 | Пакетная обработка                         |
| `Normal`         | 5                 | Обычные запросы (значение по умолчанию)    |
| `AboveNormal`    | 6                 | Важные пользовательские операции           |
| `High`           | 9                 | Срочные задачи                             |
| `RealTime`       | 10                | Критичные операции реального времени       |

### Полный пример: запуск демо-нагрузки при старте адаптера

Поскольку адаптер — это non-web приложение, «точка входа» для инициации команд — это, как правило, `ApplicationRunner`, `@EventListener(ApplicationReadyEvent.class)` или собственный scheduler. Ниже приведён пример в стиле `EchoRunner` из `sal-example-adapter`: после готовности контекста компонент в отдельном потоке публикует несколько команд и логирует их round-trip.

```java
@Component
@ConditionalOnProperty(prefix = "sal.example.echo-runner", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class EchoRunner {

    private static final Logger log = LoggerFactory.getLogger(EchoRunner.class);
    private final CommandBus commandBus;

    public EchoRunner(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        Thread worker = new Thread(this::runEchoLoop, "echo-runner");
        worker.setDaemon(true);
        worker.start();
    }

    private void runEchoLoop() {
        try {
            Thread.sleep(2_000); // дать listener-ам прогреться
            for (int i = 1; i <= 3; i++) {
                EchoCommand cmd = new EchoCommand();
                cmd.setPayload("ping-" + i);
                EchoResult result = commandBus
                        .<EchoResult>executeCommandAsync(cmd, 10, CommandPriority.Normal)
                        .get();
                log.info("Echo round-trip #{}: {}", i, result.getEcho());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("EchoRunner failed", e);
        }
    }
}
```

После отработки runner-а адаптер продолжает жить обычным процессом и обслуживать входящие команды. Поведение управляется флагом `sal.example.echo-runner.enabled` — так же, как эталонный `EchoRunner` из `sal-example-adapter`.

---

## 8. Работа с событиями

### Определение события

```java
import ru.copperside.sal.api.event.Event;
import ru.copperside.sal.api.annotation.ServiceMessage;

// Обычное доменное событие (маршрутизируется по типу адаптера):
public class OrderCreatedEvent implements Event {

    private String orderId;
    private String customerId;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
}

// Сервисное событие (маршрутизируется через routing key "service"):
@ServiceMessage
public class AdapterReadyEvent implements Event {
    private String adapterName;
    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }
}
```

Аннотация `@ServiceMessage` отмечает событие как сервисное — оно публикуется по специальному routing key `"service"` и доступно всем подписчикам в кластере, независимо от типа адаптера.

### Подписка на событие

```java
import org.springframework.stereotype.Component;
import ru.copperside.sal.api.event.EventHandler;
import ru.copperside.sal.api.event.EventSource;

@Component
public class OrderCreatedEventHandler implements EventHandler<OrderCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventHandler.class);

    @Override
    public void handle(OrderCreatedEvent event, EventSource source) {
        log.info("Order {} created by customer {}, source adapter: {}",
                event.getOrderId(),
                event.getCustomerId(),
                source.getEventSourceServiceId());

        // бизнес-логика обработки события
    }
}
```

`EventSource` содержит метаданные о происхождении события: `eventSourceServiceId`, `eventSessionId`, `eventOperationId`, `eventCorrelationId`, `eventTimeStamp`.

### Публикация события

```java
@Component
public class OrderProcessingService {

    private final EventBus eventBus;

    public OrderProcessingService(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void createOrder(String customerId, List<OrderItem> items) {
        // ... создаём заказ в БД ...

        // публикуем событие через RabbitMQ всем подписчикам
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(savedOrder.getId());
        event.setCustomerId(customerId);

        eventBus.publish(event);

        // можно опубликовать несколько событий одним вызовом:
        // eventBus.publish(event1, event2, event3);
    }
}
```

---

## 9. Сессия

`SalContext` — единый thread-local фасад для данных сессии, `CommandContext` и MDC `correlationId`. Фреймворк автоматически заполняет его при получении команды или события через RabbitMQ — сессия переносится в `RecordedMessage.additionalData[MessageDataKeys.SESSION]` и больше нигде (HTTP-транспорта в SAL нет). Сессия автоматически передаётся при отправке команд другим адаптерам.

### Доступ к данным сессии

```java
import ru.copperside.sal.starter.context.SalContext;
import ru.copperside.sal.api.constant.MessageDataKeys;

@Component
public class AuditService {

    public void logAction(String action) {
        Map<String, Object> session = SalContext.session(); // весь Map сессии
        String sessionId  = session != null ? (String) session.get(MessageDataKeys.SESSION_ID)   : null;
        String operationId = session != null ? (String) session.get(MessageDataKeys.OPERATION_ID) : null;

        log.info("Action '{}' by session={}, operation={}",
                action, sessionId, operationId);
    }
}
```

### Ручное управление сессией (для тестов и граничных случаев)

```java
// Установить сессию вручную:
Map<String, Object> session = new HashMap<>();
session.put("SessionId", "test-session-id");
session.put("OperationId", "42");
SalContext.setSession(session);

// Очистить после завершения (фреймворк делает это автоматически в entry points):
SalContext.clear();
```

**Важно:** не вызывайте `SalContext.clear()` вручную внутри бизнес-логики — только если пишете собственный entry point (например, `Scheduled`-задачу или кастомный consumer). Фреймворк сам чистит ThreadLocal в finally-блоках RabbitMQ-консьюмеров, а для `CommandHandlerAsync` — в `whenComplete`-callback через захват `ContextSnapshot`.

---

## 10. Регистрация типов для C# interop

`TypeMappingRegistry` хранит двунаправленное отображение между полными именами C#-типов и Java-классами. Это необходимо для корректной десериализации входящих wire-сообщений от C#-клиентов.

Классы, аннотированные `@CommandType`, автоматически регистрируются при старте. Но если нужно зарегистрировать типы, которые не имеют `@CommandType` (например, типы результатов, которые приходят из C# в нестандартных сценариях), создайте конфигурационный класс:

```java
package com.example.myadapter.config;

import org.springframework.context.annotation.Configuration;
import ru.copperside.sal.starter.serialization.TypeMappingRegistry;

import jakarta.annotation.PostConstruct;

@Configuration
public class TypeMappingConfig {

    private final TypeMappingRegistry registry;

    public TypeMappingConfig(TypeMappingRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void registerTypes() {
        // Регистрируем все типы, которые участвуют в wire-обмене с C#.
        // Первый аргумент — точное имя C#-типа (Namespace.ClassName).
        // Второй аргумент — соответствующий Java-класс.

        registry.register(
            "TCB.KCProcessing.Client.ReversalCommand",
            ReversalCommand.class
        );
        registry.register(
            "TCB.KCProcessing.Client.ReversalResult",
            ReversalResult.class
        );
    }
}
```

**Почему это важно:**
Wire-формат передаёт тип данных как строку C#-имени в заголовке AMQP-сообщения. При получении фреймворк ищет соответствующий Java-класс в `TypeMappingRegistry`. Если класс не найден — десериализация упадёт с `SalException.error(SalErrorCodes.UNKNOWN_COMMAND_RESULT_TYPE, ...)`.

---

## 11. Обработка ошибок

### Единый класс исключения

В SAL одно checked-исключение `SalException extends RuntimeException` с перечислением `SalException.Type`:

```
RuntimeException
└── SalException  (getCode(), getType(), getExceptionTypeName())
        └── Type { ERROR, FATAL, VALIDATION }
```

Создаётся через статические фабрики:

- `SalException.error(code, message)` — исправимая бизнес-ошибка (тип `ERROR`);
- `SalException.fatal(code, message)` — критическая ошибка, требует вмешательства (тип `FATAL`);
- `SalException.validation(code, message)` — ошибка валидации входных данных (тип `VALIDATION`).

`getExceptionTypeName()` возвращает C# wire-имя (`ErrorException`, `FatalException`, `ValidationException`) — оно попадает в `FailedResult` для совместимости с C#-клиентами.

### Как бросать исключения

```java
import ru.copperside.sal.api.exception.SalException;
import ru.copperside.sal.api.exception.SalErrorCodes;

@Override
public ProcessResult execute(ProcessCommand command) {
    // Ошибка валидации:
    if (command.getAmount() == null || command.getAmount().signum() <= 0) {
        throw SalException.validation(
                SalErrorCodes.VALIDATION_EXCEPTION,
                "Amount must be positive, got: " + command.getAmount());
    }

    // Исправимая бизнес-ошибка:
    Account account = accountRepo.findById(command.getAccountId()).orElse(null);
    if (account == null) {
        throw SalException.error(
                SalErrorCodes.ERROR_EXCEPTION,
                "Account not found: " + command.getAccountId());
    }

    // Критическая ошибка:
    try {
        return process(command, account);
    } catch (DatabaseConnectionException e) {
        throw SalException.fatal(
                SalErrorCodes.FATAL_EXCEPTION,
                "Database unavailable: " + e.getMessage());
    }
}
```

### SalErrorCodes — стандартные коды ошибок

Используйте константы `SalErrorCodes` вместо магических строк. Эти коды парсятся C#-клиентами на другой стороне.

```java
import ru.copperside.sal.api.exception.SalErrorCodes;

// Примеры кодов:
SalErrorCodes.FATAL_EXCEPTION            // "FatalException"
SalErrorCodes.COMMAND_EXECUTION_TIMEOUT  // "CommandExecutionTimeout"
SalErrorCodes.SESSION_REQUIRED           // "SessionRequired"
SalErrorCodes.NOT_HANDLED_COMMAND        // "NotHandledCommand"
SalErrorCodes.NO_AVAILABLE_ADAPTER       // "NoAvailableAdapter"
SalErrorCodes.MISMATCH_ENVIRONMENT_KEY   // "MismatchEnvironmentKey"
```

### Как SAL доставляет исключения вызывающему адаптеру

HTTP-маппера ошибок в SAL больше нет. Если `CommandHandler` бросает `SalException` (или любое другое необработанное исключение), `CommandConsumer` конвертирует его в `FailedResult` и отправляет в exchange `CommandCompletedExchange` — вызывающая сторона получит результат через обычный request/reply flow. Сообщение исходного исключения кладётся в поле `FailedResult.setExeption(String)` (опечатка `Exeption` сохранена для wire-совместимости с C#).

Разработчику ничего настраивать не нужно — просто бросайте нужное исключение из обработчика.

---

## 12. Тестирование

### Unit-тест обработчика команды

```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EchoCommandHandlerTest {

    private final EchoCommandHandler handler = new EchoCommandHandler();

    @Test
    void shouldEchoPayload() {
        EchoCommand command = new EchoCommand();
        command.setPayload("test message");

        EchoResult result = handler.execute(command);

        assertThat(result.getEcho()).isEqualTo("test message");
    }

    @Test
    void shouldHandleNullPayload() {
        EchoCommand command = new EchoCommand();
        command.setPayload(null);

        EchoResult result = handler.execute(command);

        assertThat(result.getEcho()).isNull();
    }
}
```

### Интеграционный тест с @SpringBootTest и Testcontainers RabbitMQ

У адаптера нет HTTP-эндпоинтов, поэтому полный интеграционный тест выполняется через реальный RabbitMQ (например, `RabbitMQContainer` из Testcontainers) и `CommandBus`:

```java
@SpringBootTest
@Testcontainers
class EchoCommandIntegrationTest {

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void rabbitProps(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired
    private CommandBus commandBus;

    @Test
    void shouldRoundTripEchoCommand() throws Exception {
        EchoCommand cmd = new EchoCommand();
        cmd.setPayload("hello");

        EchoResult result = commandBus
                .<EchoResult>executeCommandAsync(cmd, 10, CommandPriority.Normal)
                .get();

        assertThat(result.getEcho()).isEqualTo("hello");
    }
}
```

HTTP-слоя в адаптере нет, поэтому `TestRestTemplate`/`WebEnvironment.RANDOM_PORT` здесь не применимы — всё тестирование идёт через очереди.

### Тест совместимости wire-формата (PascalCase)

Критически важно убедиться, что JSON-сериализация использует PascalCase — именно это ожидает C#-сторона.

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WireFormatCompatibilityTest {

    @Autowired
    @Qualifier("wireObjectMapper")
    private ObjectMapper wireObjectMapper;

    @Test
    void echoResultShouldSerializeWithPascalCase() throws Exception {
        EchoResult result = new EchoResult();
        result.setEcho("hello");

        String json = wireObjectMapper.writeValueAsString(result);

        // C# ожидает "Echo", а не "echo"
        assertThat(json).contains("\"Echo\"");
        assertThat(json).doesNotContain("\"echo\"");
    }

    @Test
    void echoCommandShouldDeserializeFromPascalCase() throws Exception {
        String json = "{\"Payload\":\"world\"}";

        EchoCommand command = wireObjectMapper.readValue(json, EchoCommand.class);

        assertThat(command.getPayload()).isEqualTo("world");
    }
}
```

---

## 13. Чеклист перед деплоем

| # | Что проверить | Критичность |
|---|---------------|-------------|
| 1 | `sal.adapter.name` уникален в кластере | Критично |
| 2 | `sal.adapter.type` совпадает с C#-конфигурацией | Критично |
| 3 | Все `@CommandType` значения точно совпадают с C#-именами | Критично |
| 4 | Реализован `EchoRunner`/`ApplicationRunner`/event-listener для демо- или инициирующих сценариев (опционально) | Желательно |
| 5 | Все типы результатов зарегистрированы в `TypeMappingRegistry` | Критично |
| 6 | `spring.rabbitmq.*` указывает на правильный брокер и vhost | Критично |
| 7 | Для каждого `CommandHandler` существует ровно один обработчик | Критично |
| 8 | `SalException.error(...)`/`validation(...)`/`fatal(...)` бросаются с понятными сообщениями и кодом из `SalErrorCodes` | Важно |
| 9 | Захват данных сессии из `SalContext.session()` происходит до асинхронных операций | Важно |
| 10 | `sal.command.threads` и `sal.event.threads` настроены под нагрузку | Желательно |
| 11 | Wire-формат проверен тестом на PascalCase | Желательно |
| 12 | `spring.main.web-application-type: none` задан в `application.yml` | Желательно |
| 13 | Graceful shutdown настроен (`spring.lifecycle.timeout-per-shutdown-phase`) | Желательно |

---

## 14. См. также

- [configuration-reference.md](configuration-reference.md) — полный справочник всех параметров `application.yml`
- [wire-protocol.md](wire-protocol.md) — описание wire-формата AMQP-сообщений, PascalCase JSON, заголовки
- [troubleshooting.md](troubleshooting.md) — диагностика типичных проблем (нет обработчика, таймаут, ошибки маппинга типов)
- [glossary.md](glossary.md) — термины и соответствия Java ↔ C# (CommandBus, EventBus, SalContext и др.)
- [architecture.md](architecture.md) — архитектурные решения (ADR), структура модулей, принципы взаимодействия
