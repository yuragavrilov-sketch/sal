# Troubleshooting Guide — TCB-SAL

Руководство по диагностике и устранению типовых проблем при работе с фреймворком TCB-SAL.

Формат каждой проблемы: **Симптомы → Причина → Решение**.

---

## Содержание

1. [Проблемы запуска](#1-проблемы-запуска)
2. [Проблемы доставки команд](#2-проблемы-доставки-команд)
3. [Проблемы результатов команд](#3-проблемы-результатов-команд)
4. [Проблемы сессии](#4-проблемы-сессии)
5. [Проблемы WatchDog и online/offline](#5-проблемы-watchdog-и-onlineoffline)
6. [Проблемы C#/Java interop](#6-проблемы-cjava-interop)
7. [Диагностические инструменты](#7-диагностические-инструменты)
8. [См. также](#8-см-также)

---

## 1. Проблемы запуска

### 1.1 `Connection refused: localhost:5672`

**Симптомы**

```
com.rabbitmq.client.AlreadyClosedException: connection is already closed
  due to connection error; cause: java.net.ConnectException:
  Connection refused: localhost/127.0.0.1:5672
```

Приложение падает сразу после старта, не успевая объявить очереди.

**Причина**

RabbitMQ не запущен или недоступен по адресу, указанному в конфигурации.

**Решение**

Запустите RabbitMQ через Docker Compose:

```bash
docker-compose up -d
```

Проверьте, что брокер поднялся:

```bash
docker-compose ps
# rabbitmq   Up   5672/tcp, 15672/tcp
```

Или убедитесь, что хост/порт в `application.yml` совпадают с реальным адресом брокера:

```yaml
spring:
  rabbitmq:
    host: localhost   # замените на актуальный хост
    port: 5672
```

---

### 1.2 `ACCESS_REFUSED - Login was refused`

**Симптомы**

```
com.rabbitmq.client.AuthenticationFailureException:
  ACCESS_REFUSED - Login was refused using authentication mechanism PLAIN.
  For details see the broker logfile.
```

Соединение устанавливается, но сразу разрывается.

**Причина**

Неверные учётные данные или неправильный vhost. RabbitMQ отклоняет аутентификацию.

**Решение**

Проверьте параметры подключения в `application.yml`:

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest      # должно совпадать с пользователем RabbitMQ
    password: guest      # проверьте пароль
    virtual-host: /dev   # убедитесь, что vhost существует
```

Убедитесь, что vhost создан в RabbitMQ Management UI (`http://localhost:15672` → Admin → Virtual Hosts). Пользователь должен иметь права на этот vhost.

---

### 1.3 `release version 21 not supported`

**Симптомы**

```
error: release version 21 not supported
```

Сборка завершается с ошибкой компилятора. Maven/Gradle не может скомпилировать исходники.

**Причина**

Установлена версия JDK ниже 21. TCB-SAL требует Java 21+.

**Решение**

Установите JDK 21 (например, через SDKMAN):

```bash
sdk install java 21.0.3-tem
sdk use java 21.0.3-tem
java -version
# java version "21.0.3" ...
```

Или скачайте дистрибутив вручную с [adoptium.net](https://adoptium.net) и пропишите `JAVA_HOME`.

---

### 1.4 `Bean creation exception: wireObjectMapper`

**Симптомы**

```
org.springframework.beans.factory.BeanCreationException:
  Error creating bean with name 'wireObjectMapper':
  expected single matching bean but found 2: wireObjectMapper, objectMapper
```

Или: сообщения сериализуются неверно (отсутствует camelCase / тип не тот).

**Причина**

В контексте Spring зарегистрирован ещё один `ObjectMapper` без квалификатора. Spring не знает, какой бин инжектировать.

**Решение**

При инжекции `ObjectMapper` в собственном коде всегда используйте квалификатор `@Qualifier("wireObjectMapper")`:

```java
@Autowired
@Qualifier("wireObjectMapper")
private ObjectMapper wireObjectMapper;
```

Если вы объявляете собственный `ObjectMapper`-бин, не называйте его `wireObjectMapper` и пометьте `@Primary` явно только там, где это необходимо.

---

## 2. Проблемы доставки команд

### 2.1 `No handler registered for command 'X'`

**Симптомы**

```
[WARN] No handler registered for command 'ru.example.MyCommand'
```

Команда принята из очереди, но фреймворк не знает, кому её передать.

**Причина**

Аннотация `@CommandType` на классе команды не совпадает с `content_type` входящего сообщения RabbitMQ, либо хендлер не зарегистрирован в `CommandHandlerRegistry`.

**Решение**

1. Проверьте логи при старте приложения — там выводится список зарегистрированных хендлеров:
   ```
   [BUS] Registered command handler: ru.example.MyCommand -> MyCommandHandler
   ```
2. Убедитесь, что `@CommandType` на классе команды точно совпадает (включая регистр) с `content_type` сообщения:
   ```java
   @CommandType("ru.example.MyCommand")
   public class MyCommand { ... }
   ```
3. Убедитесь, что класс хендлера является Spring-бином (`@Component`, `@Service` и т.д.).

---

### 2.2 Команда не доставляется (тишина в логах)

**Симптомы**

Сообщение опубликовано в RabbitMQ, но хендлер не вызывается. Никаких ошибок в логах нет.

**Причина**

Неверный routing key, exchange не объявлен, или binding между exchange и очередью отсутствует.

**Решение**

Откройте RabbitMQ Management UI (`http://localhost:15672`):

1. **Exchanges** → найдите `CommandExchange` → проверьте, что он существует.
2. **Exchanges** → `CommandExchange` → вкладка **Bindings** → убедитесь, что binding с нужным routing key ведёт к нужной очереди.
3. **Queues** → найдите очередь адаптера → проверьте **Message rates** и **Consumers count**.

Routing key должен совпадать со значением `@CommandType`:
```
routing_key: "ru.example.MyCommand"
```

---

### 2.3 Команда доставлена, но `payload` — `LinkedHashMap`

**Симптомы**

```java
// В хендлере вместо MyPayload получаем LinkedHashMap
Object payload = command.getPayload();
// payload instanceof LinkedHashMap == true  (ожидалось MyPayload)
```

**Причина**

Тип `MyPayload` не зарегистрирован в `TypeMappingRegistry`. Jackson десериализует JSON в `LinkedHashMap` как fallback.

**Решение**

Зарегистрируйте тип при инициализации приложения:

```java
@PostConstruct
public void registerTypes() {
    registry.register("ru.example.MyCommand", MyCommand.class);
    registry.register("ru.example.MyPayload", MyPayload.class);
}
```

Или воспользуйтесь механизмом автоматической регистрации, если он предусмотрен вашей конфигурацией.

---

## 3. Проблемы результатов команд

### 3.1 `CompletableFuture` зависает

**Симптомы**

Вызов `commandBus.send(...).get()` или `await()` не завершается. Приложение зависает на ожидании результата.

**Причина**

Очередь для получения результатов не была объявлена или не создана корректно. Ответ уходит в никуда.

**Решение**

Проверьте логи при старте — должна быть строка вида:

```
[BUS] Declared result queue: adapter-results.my-adapter
```

Если строки нет:
- Убедитесь, что конфигурация `sal.service.result-queue` задана корректно.
- Проверьте, что у пользователя RabbitMQ есть права на создание очередей в vhost.
- В RabbitMQ Management UI → **Queues** проверьте наличие result-очереди.

---

### 3.2 `CommandExecutionTimeout`

**Симптомы**

```
ru.copperside.tcbsal.exception.CommandExecutionTimeoutException:
  Command 'ru.example.SlowCommand' timed out after 30000ms
```

**Причина**

Хендлер команды выполняется дольше настроенного таймаута.

**Решение**

**Вариант 1 — увеличить таймаут** (быстрое решение):

```yaml
sal:
  command:
    timeout-ms: 60000  # 60 секунд
```

Или на уровне конкретного вызова:

```java
commandBus.send(command, Duration.ofSeconds(60));
```

**Вариант 2 — оптимизировать хендлер**: проанализируйте медленные операции (запросы к БД, внешние HTTP-вызовы) и добавьте кэширование или асинхронную обработку.

---

### 3.3 `ClassCastException` при получении результата

**Симптомы**

```
java.lang.ClassCastException: class java.util.LinkedHashMap
  cannot be cast to class ru.example.MyResult
```

**Причина**

Тип результата `R` в `HaveResult<R>` не зарегистрирован в `TypeMappingRegistry`, либо на стороне отправителя и получателя используются разные имена типа.

**Решение**

Убедитесь, что тип результата зарегистрирован:

```java
registry.register("ru.example.MyResult", MyResult.class);
```

Проверьте, что generic-параметр `HaveResult<MyResult>` в объявлении команды совпадает с реально возвращаемым типом в хендлере:

```java
// Команда:
public class MyCommand implements HaveResult<MyResult> { ... }

// Хендлер:
public MyResult handle(MyCommand command) { ... }
```

---

## 4. Проблемы сессии

### 4.1 `SessionHolder.get() == null` внутри хендлера

**Симптомы**

```java
Session session = SessionHolder.get(); // null
// NullPointerException при обращении к session.getUserId() и т.д.
```

**Причина**

Отправитель команды не передал данные сессии в `AdditionalData` сообщения. Фреймворк не может восстановить сессию из входящего сообщения.

**Решение**

Проверьте, что отправляющая сторона заполняет `AdditionalData` перед отправкой команды. Для Java-адаптеров:

```java
// Сессия должна быть установлена до отправки:
SessionHolder.set(session);
commandBus.send(command);
```

При отладке вручную через RabbitMQ API передавайте `AdditionalData` в заголовках сообщения (см. раздел 7).

---

### 4.2 Сессия теряется при асинхронном выполнении

**Симптомы**

`SessionHolder.get()` возвращает `null` или устаревшую сессию внутри `CompletableFuture`, `@Async`-метода или лямбды, выполняемой в другом потоке.

**Причина**

`SessionHolder` использует `ThreadLocal`. При переключении потока контекст сессии не передаётся автоматически.

**Решение**

Захватите сессию явно перед переключением потока и восстановите её внутри асинхронного блока:

```java
Session capturedSession = SessionHolder.get(); // захват в текущем потоке

CompletableFuture.runAsync(() -> {
    SessionHolder.set(capturedSession); // восстановление в новом потоке
    try {
        // ваш асинхронный код
    } finally {
        SessionHolder.clear(); // очистка после завершения
    }
});
```

Убедитесь, что используется актуальная версия `CommandConsumer`, поддерживающая паттерн захвата контекста.

---

## 5. Проблемы WatchDog и online/offline

### 5.1 Адаптер не переходит в статус ONLINE

**Симптомы**

```
GET /actuator/health  →  {"status": "OUT_OF_SERVICE", "adapter": "OFFLINE"}
```

Адаптер запущен, RabbitMQ подключён, но WatchDog не переводит его в ONLINE.

**Причина**

Один или несколько зависимых endpoints, указанных в `sal.service.adapter-dependency`, недоступны.

**Решение**

Проверьте список зависимостей в конфигурации:

```yaml
sal:
  service:
    adapter-dependency:
      - name: OtherAdapter
        url: http://other-adapter:8080/ping
```

Убедитесь, что все зависимые сервисы отвечают на healthcheck. Вызовите каждый из них вручную:

```bash
curl http://other-adapter:8080/ping
```

---

### 5.2 Все HTTP-запросы отклоняются с `AdapterIsOffline`

**Симптомы**

```json
{"error": "AdapterIsOffline", "message": "Adapter is currently offline"}
```

Все входящие HTTP-запросы возвращают ошибку, хотя адаптер физически работает.

**Причина**

Параметр `enable-offline-mode: false` при этом адаптер находится в статусе OFFLINE. Фреймворк блокирует все запросы до перехода в ONLINE.

**Решение**

**Вариант 1** — включить offline-режим (разрешить работу в деградированном состоянии):

```yaml
sal:
  service:
    enable-offline-mode: true
```

**Вариант 2** — устранить причину перехода в OFFLINE (см. п. 5.1).

---

### 5.3 Ручное отключение адаптера

**Симптомы**

Адаптер находится в статусе OFFLINE после ручного переключения. `WatchDogService.isManualOnline()` возвращает `false`.

**Причина**

Администратор вызвал endpoint для ручного отключения адаптера.

**Решение**

Вызовите toggle-endpoint для повторного включения:

```bash
curl -X POST http://localhost:8080/actuator/adapter/toggle-online
```

Или используйте соответствующий метод `WatchDogService` программно:

```java
watchDogService.setManualOnline(true);
```

---

## 6. Проблемы C#/Java interop

### 6.1 C# не может прочитать сообщение от Java-адаптера

**Симптомы**

C#-адаптер получает сообщение, но десериализация падает или поля `null`. Ожидается `PascalCase`, приходит `camelCase`.

**Причина**

Сообщение сериализовано стандартным `ObjectMapper` Spring (camelCase), а не `wireObjectMapper` (PascalCase, совместимый с C# конвенциями).

**Решение**

Убедитесь, что при отправке используется `wireObjectMapper`:

```java
@Autowired
@Qualifier("wireObjectMapper")
private ObjectMapper wireObjectMapper;

// При формировании тела сообщения:
String json = wireObjectMapper.writeValueAsString(payload);
```

Фреймворк делает это автоматически для команд, отправляемых через `CommandBus`. Если вы публикуете сообщения вручную — используйте квалификатор явно.

---

### 6.2 Java не может десериализовать команду от C#-адаптера

**Симптомы**

Команда от C# приходит, но `payload` — `LinkedHashMap` или `ClassCastException` при каст.

**Причина**

Тип C#-команды не зарегистрирован в `TypeMappingRegistry` Java-адаптера.

**Решение**

Зарегистрируйте тип, указав полное имя типа в точности так, как C# публикует его в `content_type`:

```java
// C# публикует с content_type = "My.Namespace.MyCommand"
registry.register("My.Namespace.MyCommand", MyCommand.class);
```

Проверьте реальное значение `content_type` в RabbitMQ Management UI → нужная очередь → **Get messages**.

---

### 6.3 Несовместимость формата сессии

**Симптомы**

Сессия, переданная C#-адаптером, не читается Java, или наоборот. Возможны ошибки вида:

```
Invalid session data: unexpected byte sequence
SessionDeserializationException: 7-bit length prefix mismatch
```

**Причина**

C# использует формат сериализации строк с 7-bit encoded length prefix (BinaryWriter). Если Java-сторона читает данные иначе — формат ломается.

**Решение**

Убедитесь, что используется `SessionSerializer`, реализующий совместимый 7-bit length prefix формат. Не заменяйте стандартный сериализатор кастомным без учёта этой особенности.

При отладке: захватите raw-байты сессии из заголовка `Additional-Data` и декодируйте вручную, сверяясь с [wire-protocol.md](wire-protocol.md).

---

## 7. Диагностические инструменты

| Инструмент | Что проверять |
|------------|---------------|
| **RabbitMQ Management UI** `http://localhost:15672` | Exchanges, очереди, bindings, message rates, содержимое сообщений |
| **GET /actuator/health** | Статус адаптера (ONLINE/OFFLINE), соединение с RabbitMQ, состояние зависимых endpoints |
| **GET /ping** | SAL-совместимый healthcheck; используется WatchDog зависимых адаптеров |
| **MDC в логах** | `correlationId` для трассировки запроса между адаптерами |
| **curl + RabbitMQ HTTP API** | Ручная отправка сообщений для отладки конкретного хендлера |

### Пример: ручная отправка `EchoCommand` через RabbitMQ Management API

Полезно для проверки работы хендлера в изоляции от реального источника команд:

```bash
curl -u guest:guest -X POST "http://localhost:15672/api/exchanges/dev/CommandExchange/publish" \
  -H "Content-Type: application/json" \
  -d '{
    "routing_key": "ru.copperside.sal.example.EchoCommand",
    "properties": {
      "content_type": "ru.copperside.sal.example.EchoCommand",
      "content_encoding": "UTF-8",
      "delivery_mode": 2,
      "correlation_id": "test-echo-001",
      "message_id": "1",
      "priority": 0,
      "headers": {
        "Additional-Data": "{\"SourceServiceId\":\"ExampleAdapter.example-adapter\",\"IsCommand\":\"\"}"
      }
    },
    "payload": "{\"Payload\":\"Hello from SAL test!\"}",
    "payload_encoding": "string"
  }'
```

Параметры запроса:

- `dev` в URL — имя vhost (замените при необходимости).
- `routing_key` и `content_type` — полное имя типа команды.
- `correlation_id` — идентификатор для отслеживания в логах через MDC.
- `Additional-Data` — JSON с метаданными сессии; `SourceServiceId` должен быть валидным идентификатором источника.
- `Payload` — тело команды в формате PascalCase (C#/wire-protocol соглашение).

После отправки проверьте логи приложения — должен появиться лог вида:

```
[HANDLER] EchoCommandHandler received: Hello from SAL test! correlationId=test-echo-001
```

---

## 8. См. также

- [architecture.md](architecture.md) — общая архитектура системы, роли компонентов
- [wire-protocol.md](wire-protocol.md) — формат сообщений RabbitMQ, сериализация сессии
- [configuration-reference.md](configuration-reference.md) — полный справочник параметров конфигурации
- [operations-guide.md](operations-guide.md) — руководство по эксплуатации и мониторингу
- [glossary.md](glossary.md) — глоссарий терминов TCB-SAL
