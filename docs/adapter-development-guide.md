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
10. [HTTP-контроллеры](#10-http-контроллеры)
11. [Регистрация типов для C# interop](#11-регистрация-типов-для-c-interop)
12. [Обработка ошибок](#12-обработка-ошибок)
13. [Тестирование](#13-тестирование)
14. [Чеклист перед деплоем](#14-чеклист-перед-деплоем)
15. [См. также](#15-см-также)

---

## 1. Quick Start

### Шаг 1 — pom.xml

Создайте Maven-проект с единственной зависимостью `sal-spring-boot-starter`. Всё остальное (Spring Boot, RabbitMQ, Jackson, Actuator) притягивается транзитивно.

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
  rabbitmq:
    host: localhost
    port: 5672
    virtual-host: dev
    username: guest
    password: guest
  lifecycle:
    timeout-per-shutdown-phase: 30s

server:
  port: 8080
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
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
INFO  o.s.b.w.e.tomcat.TomcatWebServer        : Tomcat started on port 8080
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
├── controller/                     # HTTP-эндпоинты (PingController обязателен)
│   ├── PingController.java
│   └── GreetController.java        # пример REST-контроллера
└── config/                         # дополнительная конфигурация (TypeMapping и т.п.)
    └── TypeMappingConfig.java
```

**Правило именования пакетов:** держите команды, события и контроллеры в отдельных пакетах — это облегчает поиск и уменьшает циклические зависимости.

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

`@CommandType` задаёт полное имя C#-типа, которое записывается в заголовок wire-сообщения. Без неё фреймворк не сможет смаппировать входящее сообщение на Java-класс и выбросит `ErrorException` с кодом `UnknownCommandResultType`.

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

`SessionHolder` хранит данные сессии в `ThreadLocal`. В синхронных обработчиках это работает прозрачно — поток тот же. Но в асинхронных коллбэках (`thenApply`, `thenCompose`, `handle`) поток может смениться:

```java
@Override
public CompletableFuture<MyResult> executeAsync(MyCommand command) {
    // ПРАВИЛЬНО: читаем сессию ДО перехода в другой поток
    String sessionId = SessionHolder.getSessionId();

    return someAsyncOp()
            .thenApply(data -> {
                // НЕПРАВИЛЬНО: SessionHolder.getSessionId() здесь может вернуть null
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

### Полный пример: HTTP → CommandBus → ответ клиенту

```java
@RestController
@RequestMapping("/echo")
public class EchoController {

    private final CommandBus commandBus;

    public EchoController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<String>> echo(
            @RequestParam(defaultValue = "hello") String payload) {

        EchoCommand command = new EchoCommand();
        command.setPayload(payload);

        return commandBus.<EchoResult>executeCommandAsync(command, 30, CommandPriority.Normal)
                .thenApply(result -> ResponseEntity.ok(result.getEcho()))
                .exceptionally(ex -> ResponseEntity.internalServerError()
                        .body("Command failed: " + ex.getMessage()));
    }
}
```

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

`SessionHolder` — thread-local хранилище данных сессии. Фреймворк автоматически заполняет его при получении команды или события через RabbitMQ (из заголовков wire-сообщения) и при HTTP-запросах (через `SessionFilter`). Сессия автоматически передаётся при отправке команд другим адаптерам.

### Доступ к данным сессии

```java
import ru.copperside.sal.starter.context.SessionHolder;

@Component
public class AuditService {

    public void logAction(String action) {
        String sessionId  = SessionHolder.getSessionId();    // ID текущей сессии
        String operationId = SessionHolder.getOperationId(); // ID текущей операции
        Map<String, Object> session = SessionHolder.get();   // весь Map сессии

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
SessionHolder.set(session);

// Очистить после завершения (фреймворк делает это автоматически в entry points):
SessionHolder.clear();
```

**Важно:** не вызывайте `SessionHolder.clear()` вручную внутри бизнес-логики — только если пишете собственный entry point (кастомный фильтр, Scheduled задача и т.п.). Фреймворк сам чистит ThreadLocal в finally-блоках HTTP-фильтров и RabbitMQ-консьюмеров.

---

## 10. HTTP-контроллеры

### PingController — обязателен для WatchDog

WatchDog других адаптеров регулярно вызывает `GET /ping` для проверки доступности. Без этого эндпоинта адаптер будет считаться недоступным.

```java
package com.example.myadapter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sal.api.dto.service.PingResponse;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.web.AdapterState;

import java.time.OffsetDateTime;

@RestController
public class PingController {

    private final SalProperties properties;
    private final AdapterState adapterState;

    public PingController(SalProperties properties, AdapterState adapterState) {
        this.properties = properties;
        this.adapterState = adapterState;
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        PingResponse response = new PingResponse();
        response.setOnline(adapterState.isOnline());
        response.setServerTime(OffsetDateTime.now());
        response.setRecipientServiceName(properties.getAdapter().getName());
        return response;
    }
}
```

### Кастомные контроллеры

Кастомные контроллеры ничем не отличаются от обычных Spring MVC контроллеров:

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final CommandBus commandBus;

    public OrderController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<CreateOrderResult>> createOrder(
            @RequestBody CreateOrderRequest request) {

        CreateOrderCommand command = new CreateOrderCommand();
        command.setCustomerId(request.getCustomerId());
        command.setItems(request.getItems());

        return commandBus.<CreateOrderResult>executeCommandAsync(command)
                .thenApply(result -> ResponseEntity.ok(result))
                .exceptionally(ex -> ResponseEntity.internalServerError().build());
    }
}
```

### EnvironmentKey — автоматическая валидация

Если в `application.yml` задан непустой `sal.service.environment-key`, фреймворк автоматически проверяет заголовок `X-Environment-Key` в каждом HTTP-запросе через `EnvironmentKeyInterceptor`. При несовпадении вернётся `ErrorException` с кодом `MismatchEnvironmentKey`.

Разработчику ничего делать не нужно — механизм включается автоматически при непустом значении `environment-key`.

---

## 11. Регистрация типов для C# interop

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
Wire-формат передаёт тип данных как строку C#-имени в заголовке AMQP-сообщения. При получении фреймворк ищет соответствующий Java-класс в `TypeMappingRegistry`. Если класс не найден — десериализация упадёт с `ErrorException(UnknownCommandResultType)`.

---

## 12. Обработка ошибок

### Иерархия исключений

```
RuntimeException
└── SalBaseException          ← базовый класс (код, описание, сессия, адаптер)
    ├── ErrorException        ← исправимая ошибка
    ├── ValidationException   ← ошибка валидации входных данных
    └── FatalException        ← критическая ошибка, требует вмешательства
```

### Как бросать исключения

```java
@Override
public ProcessResult execute(ProcessCommand command) {
    // Ошибка валидации:
    if (command.getAmount() == null || command.getAmount().signum() <= 0) {
        throw new ValidationException("Amount must be positive, got: " + command.getAmount());
    }

    // Исправимая бизнес-ошибка:
    Account account = accountRepo.findById(command.getAccountId()).orElse(null);
    if (account == null) {
        throw new ErrorException("Account not found: " + command.getAccountId());
    }

    // Критическая ошибка:
    try {
        return process(command, account);
    } catch (DatabaseConnectionException e) {
        throw new FatalException("Database unavailable", e);
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

### SalExceptionHandler — автоматическое преобразование

`SalExceptionHandler` (входит в стартер, активируется автоматически) перехватывает все `SalBaseException` и преобразует их в HTTP-ответ `500` с телом `InfrastructureExceptionDTO` в wire-формате (PascalCase JSON). C#-клиент корректно распакует DTO и бросит соответствующее исключение на своей стороне.

Неперехваченные `Exception` оборачиваются в `FatalException` с кодом `FatalException`.

Разработчику ничего настраивать не нужно — просто бросайте нужное исключение из обработчика.

---

## 13. Тестирование

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

### Интеграционный тест с @SpringBootTest

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PingControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void pingEndpointShouldReturnOnlineTrue() {
        var response = restTemplate.getForObject("/ping", java.util.Map.class);
        assertThat(response).containsKey("Online");
        assertThat(response.get("Online")).isEqualTo(true);
    }
}
```

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

## 14. Чеклист перед деплоем

| # | Что проверить | Критичность |
|---|---------------|-------------|
| 1 | `sal.adapter.name` уникален в кластере | Критично |
| 2 | `sal.adapter.type` совпадает с C#-конфигурацией | Критично |
| 3 | Все `@CommandType` значения точно совпадают с C#-именами | Критично |
| 4 | `PingController` реализован по пути `GET /ping` | Критично |
| 5 | Все типы результатов зарегистрированы в `TypeMappingRegistry` | Критично |
| 6 | `spring.rabbitmq.*` указывает на правильный брокер и vhost | Критично |
| 7 | Для каждого `CommandHandler` существует ровно один обработчик | Критично |
| 8 | `ErrorException`/`ValidationException`/`FatalException` бросаются с понятными сообщениями | Важно |
| 9 | Захват данных сессии из `SessionHolder` происходит до асинхронных операций | Важно |
| 10 | `sal.service.environment-key` одинаков на всех адаптерах среды | Важно |
| 11 | `sal.command.threads` и `sal.event.threads` настроены под нагрузку | Желательно |
| 12 | Wire-формат проверен тестом на PascalCase | Желательно |
| 13 | Actuator endpoints `health`, `info` доступны | Желательно |
| 14 | Graceful shutdown настроен (`server.shutdown: graceful`) | Желательно |

---

## 15. См. также

- [configuration-reference.md](configuration-reference.md) — полный справочник всех параметров `application.yml`
- [wire-protocol.md](wire-protocol.md) — описание wire-формата AMQP-сообщений, PascalCase JSON, заголовки
- [troubleshooting.md](troubleshooting.md) — диагностика типичных проблем (нет обработчика, таймаут, ошибки маппинга типов)
- [glossary.md](glossary.md) — термины и соответствия Java ↔ C# (CommandBus, EventBus, SessionHolder и др.)
- [architecture.md](architecture.md) — архитектурные решения (ADR), структура модулей, принципы взаимодействия
