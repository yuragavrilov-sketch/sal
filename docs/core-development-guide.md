# Core Development Guide

Руководство для разработчиков, вносящих изменения в модули `sal-api` и `sal-spring-boot-starter`.

---

## 1. Архитектурные принципы

Три инварианта, нарушение которых ломает совместимость:

### 1.1 sal-api не зависит от Spring

`sal-api` содержит только чистые интерфейсы и DTO. Никаких аннотаций Spring (`@Component`, `@Service`, `@Autowired`), никаких `spring-*` зависимостей в `pom.xml`. Это намеренно: адаптеры должны иметь возможность использовать типы из `sal-api` без подтягивания Spring-контекста — например, в доменном слое или в тестах.

Допустимые зависимости `sal-api`: Jackson-annotations (нужны для wire-формата).

### 1.2 Wire-совместимость с C#

Любое изменение формата сообщений, AMQP-заголовков, сжатия сессии или названий полей JSON **требует координации с командой C#**. Java-сторона реализует ту же спецификацию, что и `TCB.Infrastructure` на C#. Изменение в одностороннем порядке приведёт к поломке продакшена без явной ошибки компиляции.

Детали контракта — см. [раздел 5](#5-wire-протокол-как-контракт).

### 1.3 @ConditionalOnMissingBean на всех бинах

Каждый бин в `sal-spring-boot-starter` объявлен с `@ConditionalOnMissingBean`. Это позволяет адаптеру переопределить **любой** компонент, объявив свою реализацию в `@Configuration`-классе. Нарушение этого правила (отсутствие `@ConditionalOnMissingBean`) лишает адаптеры возможности кастомизации без форков.

---

## 2. Сборка и локальная разработка

### Сборка всех модулей

```bash
mvn install
```

### Запуск example-адаптера

```bash
mvn -pl sal-example-adapter spring-boot:run
```

### Поднять RabbitMQ локально

```bash
docker-compose up -d
```

`docker-compose.yml` поднимает `rabbitmq:3-management` с:
- AMQP-порт: `5672`
- Management UI: `http://localhost:15672` (guest / guest)
- vhost: `dev`

### Отладка через RabbitMQ Management UI

В Management UI можно:
- Просматривать очереди и exchanges (`/queues`, `/exchanges`)
- Публиковать тестовые сообщения вручную (`Publish message`)
- Смотреть содержимое сообщений и заголовки (особенно `Additional-Data`)
- Проверять routing key и content-type каждого сообщения

---

## 3. Гид по модулям

### 3.1 sal-api

**Правила добавления новых типов:**

- Новый DTO или интерфейс — только в `sal-api`, не в `sal-spring-boot-starter`.
- Jackson-аннотации допустимы (`@JsonProperty`, `@JsonInclude`), Spring-аннотации — нет.
- Используй `@JsonProperty` для полей, где Java-имя расходится с wire-именем (обычно это тайпо из C#):

```java
// FailedResult.java — тайпо "Exeption" взято из C#, менять нельзя
@JsonProperty("Exeption")
private String exeption;
```

- Enum-значения должны совпадать по строкам с C#-аналогом (`CommandPriority.Normal` → `"Normal"`). `wireObjectMapper` сериализует enums через `toString()`.
- Новые типы для wire-обмена с C# регистрируй через `@CommandType("TCB.Namespace.ClassName")` — аннотацию из `sal-api`.

**Когда добавлять `@JsonProperty`:**
- Поле содержит тайпо, унаследованное из C# (см. [раздел 6](#6-намеренные-совместимости))
- Java-конвенция имени поля не совпадает с PascalCase-именем из C# по причинам, кроме простого преобразования `camelCase → PascalCase`

### 3.2 sal-spring-boot-starter

**Конвенции AutoConfiguration:**

Каждый `@AutoConfiguration`-класс должен:

1. Явно указывать порядок через `after = ...`, если зависит от другого Auto-конфига:

```java
@AutoConfiguration(after = SalRabbitAutoConfiguration.class)
```

2. Использовать `@ConditionalOnMissingBean` на каждом `@Bean`-методе — для возможности переопределения:

```java
@Bean
@ConditionalOnMissingBean
public TypeMappingRegistry typeMappingRegistry() { ... }
```

3. Для бинов по имени (не по типу) использовать `@ConditionalOnMissingBean(name = "beanName")`:

```java
@Bean
@ConditionalOnMissingBean(name = "wireObjectMapper")
public ObjectMapper wireObjectMapper() { ... }
```

**Инъекция `wireObjectMapper`:**

`wireObjectMapper` — это отдельный экземпляр `ObjectMapper`, настроенный для wire-формата (PascalCase, C#-совместимый). Он не заменяет стандартный Spring MVC `ObjectMapper` (camelCase). Инъецировать его нужно через `@Qualifier("wireObjectMapper")`:

```java
@Bean
public MyComponent myComponent(@Qualifier("wireObjectMapper") ObjectMapper wireObjectMapper) { ... }
```

Никогда не внедряй `wireObjectMapper` без квалификатора — Spring может выбрать неверный экземпляр.

---

## 4. Ключевые компоненты и их контракты

### 4.1 SalMessageConverter

**Что делает:** Конвертирует `RecordedMessage` ↔ Spring AMQP `Message`. Маппинг полей соответствует C# `RabbitMQEventBus`.

**AMQP property mapping (wire-контракт, нельзя менять):**

| AMQP-свойство | RecordedMessage-поле |
|---|---|
| Body | JSON-сериализация `Payload` (UTF-8) |
| ContentType | `PayloadType` (C# full type name) |
| MessageId | `MessageId` (as string) |
| CorrelationId | `CorrelationId` |
| Priority | `Priority` (byte) |
| DeliveryMode | `2` (persistent, всегда) |
| Timestamp | `TimeStamp` (epoch millis) |
| Expiration | миллисекунды от `TimeStamp` до `ExpireDate` |
| Headers["Additional-Data"] | JSON-байты `AdditionalData` + `SourceServiceId` |

**Что можно менять:** внутреннюю логику обработки ошибок, fallback при неизвестных типах.

**Что нельзя менять:** любой элемент из таблицы выше — это wire-контракт с C#.

### 4.2 TypeMappingRegistry

**Что делает:** Двунаправленный реестр соответствия между C#-именами типов (`"TCB.KCProcessing.Client.ReversalCommand"`) и Java-классами. Заполняется при старте сканированием `@CommandType`.

**Fallback-цепочка разрешения типов** (в `SalMessageConverter.fromMessage`):
1. Поиск по реестру по stripped имени (без assembly name)
2. Поиск по реестру по полному имени (с `", Assembly"`)
3. `Class.forName(typeName)` — для Java-to-Java сообщений, где класс известен локально
4. Fallback на `Map.class` — для неизвестных C#-типов

**Что можно менять:** добавлять новые маппинги через `register(csharpTypeName, javaClass)` или через аннотацию `@CommandType`.

**Что нельзя менять:** порядок и логику fallback-цепочки. Её изменение может привести к тому, что C#-типы перестанут разрешаться или начнут разрешаться в неверные классы.

### 4.3 SessionSerializer

**Что делает:** Сериализует сессию (`Map<String, Object>`) в компрессированную Base64-строку для передачи в `AdditionalData["Session"]`.

**Wire-контракт (формат фиксирован, совместим с C# BinaryWriter/DeflateStream):**

```
JSON string
  → UTF-8 bytes
  → prefix 7-bit encoded length (C# BinaryWriter.Write(string) format)
  → raw DEFLATE compression (без zlib-заголовка)
  → Base64
```

Этот формат точно воспроизводит поведение C# `BinaryWriter.Write(string)` + `DeflateStream`. Любое отклонение (например, zlib вместо raw DEFLATE, или стандартный length-prefix вместо 7-bit) приведёт к тому, что C#-сторона не сможет прочитать сессию.

**Что можно менять:** оптимизацию производительности внутри, при условии сохранения byte-точного output.

**Что нельзя менять:** алгоритм кодирования (7-bit length prefix, raw DEFLATE, Base64-encoding).

### 4.4 CommandConsumer

**Что делает:** Получает команды из RabbitMQ (`MessageListener`), диспетчеризует их в зарегистрированный `CommandHandler` или `CommandHandlerAsync`, отправляет результат обратно.

**Паттерн захвата контекста для async-обработчиков:**

Это критически важная деталь реализации. Единый ThreadLocal-фасад `SalContext` (session + commandContext + MDC `correlationId`) очищается в блоке `finally` вызывающего потока. Если async-обработчик вернёт `CompletableFuture`, колбэк `whenComplete` будет выполняться в другом потоке, где ThreadLocals уже будут пустыми.

Решение: контекст захватывается **до** начала async-выполнения в приватный record `ContextSnapshot` (объявлен внутри `CommandConsumer`), который копирует session, `CommandContext` и correlationId. Вызывающий поток при этом взводит внутренний флаг `asyncDispatched`, чтобы не очищать `SalContext` до завершения async-обработчика:

```java
// Capture full SalContext before async execution
ContextSnapshot snapshot = ContextSnapshot.capture();
asyncDispatched = true; // prevents outer finally-block from clearing SalContext

resultFuture = asyncHandler.executeAsync((Command) payload);

resultFuture.whenComplete((result, ex) -> {
    // Restore snapshot in the callback thread
    snapshot.restore();
    try {
        // ... send result
    } finally {
        SalContext.clear();
    }
});
```

`ContextSnapshot.capture()` вызывает `SalContext.session()` / `commandContext()` и текущий correlationId, а `restore()` раскладывает их обратно через `SalContext.setSession(...)`, `SalContext.setCommandContext(...)` и `SalContext.setCorrelationId(...)`. При добавлении новых полей в `SalContext` их нужно добавить и в `ContextSnapshot`.

**Что можно менять:** логику роутинга результатов, обработку ошибок.

**Что нельзя менять:** паттерн захвата контекста для async (иначе MDC и сессия будут пустыми в логах и хендлерах).

---

## 5. Wire-протокол как контракт

Следующие элементы **нельзя менять без координации с командой C#**:

| Элемент | Где определён | Описание |
|---|---|---|
| PascalCase naming strategy | `SalSerializationAutoConfiguration` | `PropertyNamingStrategies.UPPER_CAMEL_CASE` — C# Newtonsoft.Json default |
| Формат сжатия сессии | `SessionSerializer` | 7-bit length + raw DEFLATE + Base64; совместим с C# `BinaryWriter`/`DeflateStream` |
| Маппинг AMQP-свойств | `SalMessageConverter` | ContentType = PayloadType, MessageId = string, Additional-Data header |
| Имена exchanges и очередей | `SalRabbitConstants` | Фиксированные строки, соответствующие C#-топологии |
| Формат заголовка Additional-Data | `SalMessageConverter` | JSON-объект типа `Map<String, String>`, сериализованный в байты |
| Сохранённые тайпо | `FailedResult`, `DefaultCommandBus`, `CommandContext`, `SalErrorCodes` | Намеренные орфографические ошибки из C#, перечислены в разделе 6 |

---

## 6. Намеренные совместимости

Это **не баги**. Это намеренные несоответствия, унаследованные из C#-кода. Их исправление сломает wire-совместимость.

### 6.1 FailedResult.exeption (тайпо)

**Файл:** `sal-api/src/main/java/ru/copperside/sal/api/command/FailedResult.java`, строка 13–14

```java
@JsonProperty("Exeption")   // C# typo "Exeption" preserved for wire-compat
private String exeption;
```

C# тип `TCB.Infrastructure.Command.FailedResult` содержит поле `Exeption` (без "c"). Аннотация `@JsonProperty("Exeption")` форсирует это имя в JSON независимо от Java naming strategy.

### 6.2 AdditionalData["Confirmtation"] (тайпо)

**Файл:** `sal-spring-boot-starter/src/main/java/ru/copperside/sal/starter/command/DefaultCommandBus.java`, строка 99

```java
rm.getAdditionalData().put("Confirmtation", "true"); // C# typo preserved
```

C#-сторона читает именно ключ `"Confirmtation"` (без "i" в середине). Исправление написания сломает механизм confirmatory-команд.

### 6.3 SalErrorCodes.NOT_HANDLED_COMMAND_RESULT (leading space)

**Файл:** `sal-api/src/main/java/ru/copperside/sal/api/exception/SalErrorCodes.java`, строка 29

```java
public static final String NOT_HANDLED_COMMAND_RESULT = " NotHandledCommandResult"; // leading space preserved from C#
```

C# оригинал содержит ведущий пробел. C#-клиенты сравнивают строку точно, поэтому пробел должен быть сохранён.

---

## 7. Тестирование ядра

### Матрица тестов

| Тест-класс | Модуль | Что покрывает | Когда запускать |
|---|---|---|---|
| `WireCompatibilityTest` | `sal-api` | PascalCase-сериализация, round-trip ключевых типов (`RecordedMessage`, `CommandContext`, `InfrastructureExceptionDTO`), значения `CommandPriority` | При любом изменении типов в `sal-api` |
| `SalMessageConverterTest` | `sal-spring-boot-starter` | AMQP round-trip, маппинг всех AMQP-свойств, `SourceServiceId` в `Additional-Data` | При изменении `SalMessageConverter` |
| `DefaultCommandBusTest` | `sal-spring-boot-starter` | Pending commands: resolve, reject, expire по таймауту; `SalContext` (session, commandContext, correlationId) как единый ThreadLocal | При изменении `DefaultCommandBus`, `CommandTimeoutWatcher`, `SalContext` |
| `WireObjectMapperTest` | `sal-spring-boot-starter` | Настройки `wireObjectMapper`: PascalCase, non-null, string enums, ISO 8601 даты, tolerate unknown fields | При изменении `SalSerializationAutoConfiguration` |
| `SessionSerializerTest` | `sal-spring-boot-starter` | Compress/decompress round-trip, 7-bit encoding для малых и больших значений, Base64 output | При любом изменении `SessionSerializer` |

### Запуск тестов

```bash
# Все тесты проекта
mvn test

# Только тесты sal-api
mvn -pl sal-api test

# Только тесты sal-spring-boot-starter
mvn -pl sal-spring-boot-starter test

# Конкретный тест-класс
mvn -pl sal-api test -Dtest=WireCompatibilityTest
```

---

## 8. Известные технические долги

### 8.1 Пустой модуль sal-test

Модуль `sal-test` создан, но пуст. Планируемое содержимое:
- `TestCommandBus` — in-memory реализация `CommandBus` для тестов адаптеров без RabbitMQ
- `WireCompatibilityAssert` — AssertJ-assertions для проверки wire-совместимости при добавлении новых типов
- Testcontainers-конфигурация для интеграционных тестов с реальным RabbitMQ

До реализации адаптеры вынуждены мокировать `CommandBus` вручную.

### 8.2 Дублирование конфигурации ObjectMapper в тестах

`wireObjectMapper` настраивается идентично в четырёх тест-классах: `WireCompatibilityTest`, `SalMessageConverterTest`, `SessionSerializerTest`, `WireObjectMapperTest`. Каждый повторяет один и тот же `@BeforeEach setUp()` с одинаковыми вызовами конфигурации.

Решение: перенести в `sal-test` модуль как `WireObjectMapperFactory.create()` или как JUnit 5 extension.

### 8.3 DefaultEventBus использует Java FQDN вместо C# имени типа

`DefaultEventBus` при публикации событий использует `javaClass.getName()` в качестве имени exchange. Правильное поведение — использовать C# имя типа из `TypeMappingRegistry`. Это расхождение может привести к тому, что C#-подписчики не получат события, если exchange-имя не совпадёт с ожидаемым.

---

## 9. См. также

- [architecture.md](./architecture.md) — общая архитектура SAL, ADR-001–ADR-005
- [wire-protocol.md](./wire-protocol.md) — детальное описание wire-протокола, AMQP-топология, форматы сообщений
- [configuration-reference.md](./configuration-reference.md) — все `sal.*` properties с описаниями и дефолтами
- [adapter-development-guide.md](./adapter-development-guide.md) — как создать адаптер на базе `sal-spring-boot-starter`
- [glossary.md](./glossary.md) — термины: RecordedMessage, CommandBus и другие
