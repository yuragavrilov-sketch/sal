# Operations Guide — TCB-SAL

Руководство для DevOps/SRE-команд по деплою, мониторингу и обслуживанию адаптеров на базе TCB-SAL.

---

## Содержание

1. [Требования к окружению](#1-требования-к-окружению)
2. [Конфигурация RabbitMQ](#2-конфигурация-rabbitmq)
3. [Деплой адаптера](#3-деплой-адаптера)
4. [Health и мониторинг](#4-health-и-мониторинг)
5. [Логирование](#5-логирование)
6. [RabbitMQ топология](#6-rabbitmq-топология)
7. [Graceful shutdown](#7-graceful-shutdown)
8. [Масштабирование](#8-масштабирование)
9. [Ручное управление](#9-ручное-управление)
10. [См. также](#10-см-также)

---

## 1. Требования к окружению

### Минимальные требования

| Компонент     | Версия / значение          | Примечание                                      |
|---------------|----------------------------|-------------------------------------------------|
| Java          | 21+                        | LTS, виртуальные потоки (Project Loom)          |
| RabbitMQ      | 3.x                        | Требуется плагин `rabbitmq_management`          |
| Spring Boot   | 3.4.5                      | Встроен в starter, не меняется вручную          |

### Сетевые порты

| Порт  | Протокол | Назначение                                  |
|-------|----------|---------------------------------------------|
| 5672  | AMQP     | Соединение адаптера с RabbitMQ              |
| 15672 | HTTP     | Management UI и HTTP API RabbitMQ           |
| 8080  | HTTP     | REST-эндпоинты адаптера и Actuator          |

### Shared virtual host

Все адаптеры одного окружения работают в рамках **одного виртуального хоста** (vhost) RabbitMQ. Имя vhost задаётся через `spring.rabbitmq.virtual-host`. По умолчанию для dev-окружения используется `dev`.

---

## 2. Конфигурация RabbitMQ

### Dev-окружение: docker-compose

Для локальной разработки и тестирования используйте `docker-compose.yml` из корня репозитория:

```yaml
services:
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
      RABBITMQ_DEFAULT_VHOST: dev
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
```

Запуск:

```bash
docker compose up -d
```

Management UI доступен по адресу: `http://localhost:15672` (логин: `guest` / `guest`).

### Production: рекомендации по кластеру

**Кластеризация и HA:**

- Используйте кластер из нечётного числа узлов (3 или 5) для обеспечения кворума.
- Для критичных очередей включите **quorum queues** (`x-queue-type: quorum`) — они обеспечивают репликацию и устойчивость к потере узла.
- Классические зеркальные очереди (mirrored queues) считаются устаревшими начиная с RabbitMQ 3.9+.
- Настройте `ha-sync-mode: automatic` для автоматической синхронизации реплик при восстановлении узла.

**Права пользователя на vhost:**

Создайте отдельного пользователя для каждого окружения (не используйте `guest` в production):

```bash
rabbitmqctl add_user sal-prod <strong-password>
rabbitmqctl add_vhost prod
rabbitmqctl set_permissions -p prod sal-prod ".*" ".*" ".*"
```

Минимально необходимые права:

| Параметр  | Паттерн | Назначение                                               |
|-----------|---------|----------------------------------------------------------|
| configure | `.*`    | Создание exchange и queue при старте адаптера            |
| write     | `.*`    | Публикация сообщений в exchange                          |
| read      | `.*`    | Чтение сообщений из очередей (consume)                   |

**Дополнительные настройки для production:**

```bash
# Включить management plugin
rabbitmq-plugins enable rabbitmq_management

# Настроить политику TTL для dead-letter-queue (опционально)
rabbitmqctl set_policy dlq-ttl "^dead-letter-queue$" \
  '{"message-ttl": 604800000}' --apply-to queues
```

**Мониторинг кластера:**

- `rabbitmq-diagnostics cluster_status` — состояние узлов.
- `rabbitmq-diagnostics memory_breakdown` — использование памяти.
- Настройте алерты через Management HTTP API или Prometheus (плагин `rabbitmq_prometheus`).

---

## 3. Деплой адаптера

### Сборка

```bash
# Сборка без запуска тестов
mvn clean package -DskipTests

# Артефакт будет в
target/<adapter-name>-<version>.jar
```

### Запуск

```bash
java -jar target/my-adapter-1.0.0.jar
```

С указанием Spring-профиля:

```bash
java -Dspring.profiles.active=prod -jar target/my-adapter-1.0.0.jar
```

### Вынесенная конфигурация

**Через переменные окружения** (рекомендуется для production):

```bash
export SPRING_RABBITMQ_HOST=rabbitmq.internal
export SPRING_RABBITMQ_PORT=5672
export SPRING_RABBITMQ_VIRTUAL_HOST=prod
export SPRING_RABBITMQ_USERNAME=sal-prod
export SPRING_RABBITMQ_PASSWORD=<secret>
export SAL_ADAPTER_NAME=my-adapter
export SAL_ADAPTER_TYPE=MyAdapter
export SAL_COMMAND_THREADS=20
export SAL_EVENT_THREADS=4
export SERVER_PORT=8080

java -jar target/my-adapter-1.0.0.jar
```

**Через внешний application.yml:**

```bash
java -jar target/my-adapter-1.0.0.jar \
     --spring.config.location=/etc/sal/application.yml
```

**Через Spring-профили:**

Создайте файл `application-prod.yml` рядом с jar-файлом или в директории `config/`. Spring Boot подхватывает его автоматически при активации профиля `prod`.

### Запуск как systemd-сервис (Linux)

```ini
[Unit]
Description=SAL Adapter: my-adapter
After=network.target

[Service]
Type=simple
User=sal
WorkingDirectory=/opt/sal/my-adapter
ExecStart=/usr/bin/java -Dspring.profiles.active=prod \
          -jar /opt/sal/my-adapter/my-adapter.jar
Restart=on-failure
RestartSec=10s
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

---

## 4. Health и мониторинг

### Actuator эндпоинты

| Эндпоинт                   | Описание                                                          |
|----------------------------|-------------------------------------------------------------------|
| `GET /actuator/health`     | Общий health-статус: disk, rabbit, adapterState                  |
| `GET /actuator/health/adapterState` | Детальный статус адаптера                              |
| `GET /ping`                | Простая проверка доступности (возвращает `200 OK`)               |
| `GET /actuator/metrics`    | Метрики (Micrometer)                                              |
| `GET /actuator/prometheus` | Метрики в формате Prometheus (если включено)                      |

Конфигурация в `application.yml`:

```yaml
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

### Пример ответа `/actuator/health`

```json
{
  "status": "UP",
  "components": {
    "adapterState": {
      "status": "UP",
      "details": {
        "adapter": "my-adapter",
        "online": true,
        "shutdown": false,
        "endpoints.total": 5,
        "endpoints.available": 5
      }
    },
    "rabbit": {
      "status": "UP",
      "details": {
        "version": "3.12.0"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

Когда адаптер находится в offline-состоянии:

```json
{
  "status": "DOWN",
  "components": {
    "adapterState": {
      "status": "DOWN",
      "details": {
        "adapter": "my-adapter",
        "online": false,
        "shutdown": false,
        "endpoints.total": 5,
        "endpoints.available": 2
      }
    }
  }
}
```

### Рекомендуемые алерты

| Условие                                         | Критичность | Действие                                                    |
|-------------------------------------------------|-------------|-------------------------------------------------------------|
| `adapterState.status == DOWN`                   | WARNING     | Проверить WatchDog-мониторы, доступность зависимых систем   |
| `rabbit.status == DOWN`                         | CRITICAL    | Проверить доступность RabbitMQ-кластера                     |
| Глубина `dead-letter-queue` > 100               | WARNING     | Проверить логи на ошибки обработки, анализировать DLQ       |
| Глубина `dead-letter-queue` > 1000              | CRITICAL    | Немедленное расследование, потенциальная потеря сообщений   |
| `endpoints.available < endpoints.total`         | WARNING     | Часть эндпоинтов недоступна, деградированный режим          |
| HTTP 503 на `/ping`                             | CRITICAL    | Адаптер недоступен, перезапуск или escalation               |

---

## 5. Логирование

### MDC-поля

Все запросы и обработка сообщений сопровождаются следующими MDC-ключами, устанавливаемыми через `SalMdc`:

| MDC-ключ        | Описание                                                             |
|-----------------|----------------------------------------------------------------------|
| `correlationId` | Идентификатор запроса/команды для сквозной трассировки               |
| `sessionId`     | Идентификатор сессии клиента                                         |
| `adapterName`   | Имя адаптера (`sal.adapter.name`), фиксируется из конфигурации       |

### Пример строки лога (dev-профиль)

```
2026-03-28 14:23:11.042 [sess-001:corr-abc123] INFO  CommandBus - Received command: MyCommand, queue=Command_MyCommand
```

Формат паттерна:

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{sessionId:-no-sid}:%X{correlationId:-no-cid}] %-5level %logger{36} - %msg%n
```

### Конфигурация logback-spring.xml

Файл используется из `sal-example-adapter` как эталонный:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <springProperty scope="context" name="adapterName"
                    source="sal.adapter.name" defaultValue="unknown"/>

    <!-- Dev profile: human-readable console -->
    <springProfile name="default,dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{sessionId:-no-sid}:%X{correlationId:-no-cid}] %-5level %logger{36} - %msg%n</pattern>
                <charset>UTF-8</charset>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <!-- Prod profile: structured JSON (Logstash-compatible) -->
    <springProfile name="prod">
        <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <customFields>{"adapterName":"${adapterName}"}</customFields>
                <fieldNames>
                    <timestamp>timestamp</timestamp>
                    <thread>thread</thread>
                    <logger>logger</logger>
                    <level>level</level>
                    <message>message</message>
                    <stackTrace>stackTrace</stackTrace>
                </fieldNames>
                <includeMdcKeyName>correlationId</includeMdcKeyName>
                <includeMdcKeyName>sessionId</includeMdcKeyName>
                <includeMdcKeyName>adapterName</includeMdcKeyName>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="JSON"/>
        </root>
    </springProfile>

    <!-- Named loggers matching C# convention -->
    <logger name="CommandBus" level="INFO"/>
    <logger name="EventBus" level="INFO"/>
    <logger name="WatchDog" level="INFO"/>
    <logger name="RabbitMQ" level="INFO"/>

</configuration>
```

### Рекомендации по ELK / Logstash

- Активируйте профиль `prod` (`-Dspring.profiles.active=prod`) — адаптер будет выводить логи в JSON-формате (Logstash-compatible).
- Настройте Filebeat или Fluentd для сбора stdout/stderr контейнера.
- В Kibana создайте фильтры по полям `adapterName`, `correlationId`, `sessionId` для фильтрации по конкретному адаптеру или запросу.
- Индексируйте по полю `timestamp` (ISO-8601).
- Рекомендуемый паттерн Logstash-фильтра: поле `correlationId` для JOIN между логами разных адаптеров в одной цепочке обработки.

---

## 6. RabbitMQ топология

### Exchanges и очереди, создаваемые автоматически при старте

Следующая топология создаётся бином `SalTopologyConfigurer` при запуске адаптера:

#### Exchanges

| Exchange                                           | Тип    | Durable | Назначение                                        |
|----------------------------------------------------|--------|---------|---------------------------------------------------|
| `CommandExchange`                                  | Direct | Yes     | Маршрутизация команд к адаптерам                  |
| `TCB.Infrastructure.Command.CommandCompletedEvent` | Direct | Yes     | Результаты успешно выполненных команд             |
| `TCB.Infrastructure.Command.CommandFailedEvent`    | Direct | Yes     | Результаты команд, завершившихся с ошибкой        |
| `dead-letter-exchange`                             | Fanout | Yes     | Приём "мёртвых" сообщений                         |
| `rejected-message-exchange`                        | Fanout | Yes     | Приём отклонённых (rejected) сообщений            |

#### Служебные очереди (инфраструктурные)

| Очередь                  | Привязка к exchange        | Назначение                                    |
|--------------------------|----------------------------|-----------------------------------------------|
| `dead-letter-queue`      | `dead-letter-exchange`     | Хранение сообщений с исчерпанными retry        |
| `rejected-message-queue` | `rejected-message-exchange`| Хранение отклонённых (nack) сообщений         |

#### Динамические очереди (создаются адаптерами при старте)

| Шаблон имени                          | Пример                                    | Назначение                                   |
|---------------------------------------|-------------------------------------------|----------------------------------------------|
| `Command_{FullTypeName}`              | `Command_MyNamespace.MyCommand`           | Очередь команд конкретного типа              |
| `{ServiceName}_Event`                 | `MyAdapter_Event`                         | Очередь событий для адаптера                 |
| `{ServiceName}_service`               | `MyAdapter_service`                       | Служебная очередь событий                    |
| `{ServiceName}_CommandResult`         | `MyAdapter_CommandResult`                 | Результаты команд, инициированных адаптером  |

Все command-очереди имеют приоритизацию: `x-max-priority: 11`.

### Что нужно проверить вручную (до деплоя)

1. **Vhost существует** — Management UI → Virtual Hosts → убедитесь, что нужный vhost создан.
2. **Пользователь имеет права** — проверьте разрешения (configure/write/read) на vhost.
3. **Dead-letter-queue пуста** — если в ней накопились сообщения, разберитесь с причиной до деплоя новых версий.

### Мониторинг через Management UI

Перейдите на `http://<host>:15672`:

- **Queues** → наблюдайте за:
  - **Message rates** (publish rate / deliver rate): должны быть сбалансированы.
  - **Queue depth** (`Messages Ready`): рост очереди сигнализирует о проблеме с потребителями.
  - **Consumer count**: при нуле потребителей очередь накапливает сообщения.
- **Connections** → каждый адаптер создаёт несколько соединений (по одному на пул потоков).
- **Exchanges** → убедитесь, что все exchange из таблицы выше существуют и имеют корректные привязки.

---

## 7. Graceful shutdown

### Порядок шагов при получении SIGTERM

Адаптер поддерживает корректное завершение работы (`server.shutdown: graceful`). Последовательность:

```
kill -TERM <pid>
       |
       v
1. AdapterLifecycle.stop()
       |
       +--> adapterState.setShutDown(true)
       |    adapterState.setOnline(false)
       |
       v
2. OfflineCheckInterceptor отклоняет новые HTTP-запросы
   (возвращает 503 Service Unavailable)
       |
       v
3. Spring AMQP завершает обработку in-flight сообщений
   (drain period, по умолчанию 30s)
       |
       v
4. RabbitMQ listener containers останавливаются
       |
       v
5. Tomcat останавливается (graceful HTTP drain)
       |
       v
6. JVM завершает работу
```

Таймаут фазы shutdown управляется параметром:

```yaml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### Команда остановки

```bash
# Корректное завершение — рекомендуется всегда
kill -TERM <pid>

# или через systemd
systemctl stop my-adapter
```

**Никогда не используйте `kill -9` (`SIGKILL`)** в production — это прервёт обработку in-flight сообщений, приведёт к их переpostановке в очередь и потенциальным дублирующим обработкам.

### Проверка статуса shutdown через health

После отправки SIGTERM и до полной остановки адаптер вернёт:

```json
{
  "status": "DOWN",
  "components": {
    "adapterState": {
      "status": "DOWN",
      "details": {
        "online": false,
        "shutdown": true
      }
    }
  }
}
```

Поле `"shutdown": true` указывает, что идёт плановое завершение (а не аварийный offline).

---

## 8. Масштабирование

### Вертикальное масштабирование (один экземпляр)

Настройте количество потоков обработки в `application.yml` или через переменные окружения:

| Параметр                     | Env-переменная                  | По умолчанию | Назначение                                       |
|------------------------------|---------------------------------|--------------|--------------------------------------------------|
| `sal.command.threads`        | `SAL_COMMAND_THREADS`           | 10           | Потоки обработки входящих команд                 |
| `sal.command.result-threads` | `SAL_COMMAND_RESULT__THREADS`   | 10           | Потоки обработки результатов команд              |
| `sal.event.threads`          | `SAL_EVENT_THREADS`             | 1            | Потоки обработки событий                         |

Пример для нагруженного адаптера:

```yaml
sal:
  command:
    threads: 30
    result-threads: 20
  event:
    threads: 4
```

### Горизонтальное масштабирование (несколько экземпляров)

Несколько экземпляров одного адаптера работают как **конкурирующие потребители** (competing consumers) на одной RabbitMQ-очереди:

- Очереди типа `Command_{TypeName}` и `{ServiceName}_Event` автоматически поддерживают competing consumers — RabbitMQ сам балансирует нагрузку.
- **Важное ограничение:** очередь результатов `{ServiceName}_CommandResult` — **общая для всех экземпляров**. Результат команды может прийти в любой из экземпляров. Убедитесь, что логика обработки результатов не зависит от состояния конкретного экземпляра (stateless).

**Рекомендации при горизонтальном масштабировании:**

1. Используйте один и тот же `sal.adapter.name` для всех экземпляров.
2. Не храните локальное состояние, критичное для завершения цепочки обработки.
3. При использовании `sal.service.enable-offline-mode: true` каждый экземпляр независимо управляет своим offline-состоянием.
4. Мониторьте consumer count в RabbitMQ — он должен равняться числу запущенных экземпляров умноженному на `sal.command.threads`.

---

## 9. Ручное управление

### Переключение online/offline через WatchDog

`WatchDogService` поддерживает ручной переключатель (`manualOnlineSwitch`), позволяющий принудительно перевести адаптер в offline без его остановки.

Если в адаптере реализован `SwitchController` (опциональный контроллер), доступен HTTP API:

```http
POST /switch/toggle
```

Ответ:

```json
{
  "manualOnline": false
}
```

При `manualOnline: false`:
- Метод `WatchDogService.signalHealthy()` игнорируется — адаптер не перейдёт в online, даже если все WatchDog-мониторы пройдут.
- Адаптер принудительно переходит в offline (`signalUnhealthy("manual switch")`).
- HTTP-запросы к бизнес-эндпоинтам будут отклонены с кодом `503`.

Для возврата в online повторите вызов — переключатель инвертируется:

```http
POST /switch/toggle
# -> { "manualOnline": true }
```

После этого WatchDog-мониторы при следующем успешном проходе автоматически переведут адаптер обратно в online.

### Проверка состояния

```bash
# Текущий статус адаптера
curl -s http://localhost:8080/actuator/health/adapterState | jq .

# Простая проверка доступности
curl -s http://localhost:8080/ping
```

---

## 10. См. также

- [configuration-reference.md](./configuration-reference.md) — полный справочник по `sal.*` параметрам конфигурации.
- [troubleshooting.md](./troubleshooting.md) — диагностика частых проблем: очереди не создаются, адаптер не переходит в online, потеря сообщений.
- [architecture.md](./architecture.md) — архитектурный обзор: модули, диаграммы, принятые решения (ADR).
- [glossary.md](./glossary.md) — терминология: адаптер, эндпоинт, WatchDog, командная шина, DLQ и др.
- [wire-protocol.md](./wire-protocol.md) — полная спецификация протокола обмена сообщениями через RabbitMQ.
