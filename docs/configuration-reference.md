# Configuration Reference

Полный справочник по конфигурации SAL Spring Boot Starter.

---

## Содержание

1. [SAL Properties](#1-sal-properties)
2. [Spring RabbitMQ Properties](#2-spring-rabbitmq-properties)
3. [Spring Boot Properties](#3-spring-boot-properties)
4. [Переменные окружения](#4-переменные-окружения)
5. [Примеры конфигурации](#5-примеры-конфигурации)
6. [См. также](#6-см-также)

---

## 1. SAL Properties

Все свойства задаются под префиксом `sal.*` в `application.yml` (или через переменные окружения).
Источник значений по умолчанию: `SalProperties.java`.

### 1.1 `sal.adapter.*` — идентификация адаптера

| Свойство | Тип | Default | Описание | C# эквивалент |
|---|---|---|---|---|
| `sal.adapter.name` | `String` | `"unnamed-adapter"` | Уникальное имя экземпляра адаптера. Используется в именовании очередей RabbitMQ и в `spring.application.name`. | `Adapter.Name` |
| `sal.adapter.type` | `String` | `"GenericAdapter"` | Тип адаптера. Используется при формировании имён очередей RabbitMQ. | `Adapter.Type` |

### 1.2 `sal.service.*` — параметры сервиса

| Свойство | Тип | Default | Описание | C# эквивалент |
|---|---|---|---|---|
| `sal.service.environment-key` | `String` | `""` | Ключ среды для HTTP-валидации входящих запросов. Пустая строка — валидация отключена. | `Service.EnvironmentKey` |
| `sal.service.enable-offline-mode` | `boolean` | `false` | Разрешить приём HTTP-запросов, когда адаптер находится в offline-состоянии. | `Service.EnableOfflineMode` |
| `sal.service.mb-mode` | `boolean` | `false` | Режим message bus (зарезервирован для будущего использования). | `Service.MBMode` |
| `sal.service.sal-version` | `int` | `1` | Версия SAL-протокола данного адаптера. | `Service.SalVersion` |
| `sal.service.min-ep-sal-version` | `int` | `0` | Минимальная допустимая версия SAL endpoint'а при подключении. | `Service.MinEpSalVersion` |
| `sal.service.max-ep-sal-version` | `int` | `2147483647` | Максимальная допустимая версия SAL endpoint'а при подключении (`Integer.MAX_VALUE`). | `Service.MaxEpSalVersion` |
| `sal.service.adapter-dependency` | `String[]` | `[]` | Список имён адаптеров, от которых зависит данный. Адаптер не перейдёт в online до появления всех зависимостей. | `Service.AdapterDependency` |
| `sal.service.data-path` | `String` | `null` | Путь к директории данных адаптера. | `Service.DataPath` |
| `sal.service.disk-store-path` | `String` | `null` | Путь к директории disk store (постоянное хранилище состояния). | `Service.DiskStorePath` |

### 1.3 `sal.client.*` — HTTP-клиент

| Свойство | Тип | Default | Описание | C# эквивалент |
|---|---|---|---|---|
| `sal.client.request-timeout` | `int` | `120` | Таймаут HTTP-запросов к другим адаптерам (в секундах). | `SalClient.RequestTimeout` |
| `sal.client.service-request-timeout` | `int` | `10` | Таймаут служебных HTTP-запросов (ping, health и т.п.), в секундах. | `SalClient.ServiceRequestTimeout` |

### 1.4 `sal.command.*` — обработчик команд

| Свойство | Тип | Default | Описание | C# эквивалент |
|---|---|---|---|---|
| `sal.command.threads` | `int` | `10` | Количество consumer-потоков для обработки входящих команд из RabbitMQ. | `CommandProcessor.ThreadCount` |
| `sal.command.result-threads` | `int` | `10` | Количество consumer-потоков для обработки результатов команд. | `CommandProcessor.ResultThreadCount` |

### 1.5 `sal.event.*` — обработчик событий

| Свойство | Тип | Default | Описание | C# эквивалент |
|---|---|---|---|---|
| `sal.event.threads` | `int` | `1` | Количество consumer-потоков для обработки входящих событий из RabbitMQ. | `EventProcessor.ThreadCount` |

### 1.6 `sal.watchdog.*` — планировщик WatchDog

| Свойство | Тип | Default | Описание | C# эквивалент |
|---|---|---|---|---|
| `sal.watchdog.ping-interval-ms` | `long` | `10000` | Интервал периодической проверки (ping) зарегистрированных endpoint'ов в миллисекундах. | Custom |
| `sal.watchdog.remover-interval-ms` | `long` | `60000` | Интервал очистки устаревших/недоступных endpoint'ов в миллисекундах. | Custom |

---

## 2. Spring RabbitMQ Properties

SAL использует RabbitMQ для обмена командами и событиями. Стандартные свойства Spring Boot:

| Свойство | Тип | Default | Описание |
|---|---|---|---|
| `spring.rabbitmq.host` | `String` | `localhost` | Хост брокера RabbitMQ. |
| `spring.rabbitmq.port` | `int` | `5672` | Порт брокера RabbitMQ. |
| `spring.rabbitmq.virtual-host` | `String` | `/` | Virtual host в RabbitMQ. В SAL-инсталляциях обычно соответствует имени среды (`dev`, `prod` и т.п.). |
| `spring.rabbitmq.username` | `String` | `guest` | Имя пользователя для подключения к RabbitMQ. |
| `spring.rabbitmq.password` | `String` | `guest` | Пароль пользователя RabbitMQ. В продакшене задавать через переменную окружения. |

> **Важно:** `spring.application.name` рекомендуется привязывать к имени адаптера:
> ```yaml
> spring:
>   application:
>     name: ${sal.adapter.name}
> ```

---

## 3. Spring Boot Properties

Стандартные свойства Spring Boot, критичные для корректной работы адаптера:

| Свойство | Тип | Default | Описание |
|---|---|---|---|
| `server.port` | `int` | `8080` | HTTP-порт, на котором адаптер принимает входящие запросы. |
| `server.shutdown` | `String` | `immediate` | Стратегия остановки сервера. Рекомендуемое значение: `graceful` — дожидается завершения текущих запросов. |
| `spring.lifecycle.timeout-per-shutdown-phase` | `Duration` | `30s` | Максимальное время ожидания завершения каждой фазы graceful shutdown. Пример: `30s`. Требует `server.shutdown: graceful`. |
| `management.endpoints.web.exposure.include` | `String` | `health,info` | Список Actuator endpoints, доступных через HTTP. Рекомендуемое значение: `health,info,prometheus,metrics`. |

---

## 4. Переменные окружения

Spring Boot поддерживает [relaxed binding](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding): свойства `application.yml` можно переопределять через переменные окружения.

Правило преобразования: имя свойства переводится в верхний регистр, точки и дефисы заменяются на `_`.

### SAL-свойства

| YAML-свойство | Переменная окружения |
|---|---|
| `sal.adapter.name` | `SAL_ADAPTER_NAME` |
| `sal.adapter.type` | `SAL_ADAPTER_TYPE` |
| `sal.service.environment-key` | `SAL_SERVICE_ENVIRONMENT_KEY` |
| `sal.service.enable-offline-mode` | `SAL_SERVICE_ENABLE_OFFLINE_MODE` |
| `sal.service.mb-mode` | `SAL_SERVICE_MB_MODE` |
| `sal.service.sal-version` | `SAL_SERVICE_SAL_VERSION` |
| `sal.service.min-ep-sal-version` | `SAL_SERVICE_MIN_EP_SAL_VERSION` |
| `sal.service.max-ep-sal-version` | `SAL_SERVICE_MAX_EP_SAL_VERSION` |
| `sal.service.adapter-dependency` | `SAL_SERVICE_ADAPTER_DEPENDENCY` |
| `sal.service.data-path` | `SAL_SERVICE_DATA_PATH` |
| `sal.service.disk-store-path` | `SAL_SERVICE_DISK_STORE_PATH` |
| `sal.client.request-timeout` | `SAL_CLIENT_REQUEST_TIMEOUT` |
| `sal.client.service-request-timeout` | `SAL_CLIENT_SERVICE_REQUEST_TIMEOUT` |
| `sal.command.threads` | `SAL_COMMAND_THREADS` |
| `sal.command.result-threads` | `SAL_COMMAND_RESULT_THREADS` |
| `sal.event.threads` | `SAL_EVENT_THREADS` |
| `sal.watchdog.ping-interval-ms` | `SAL_WATCHDOG_PING_INTERVAL_MS` |
| `sal.watchdog.remover-interval-ms` | `SAL_WATCHDOG_REMOVER_INTERVAL_MS` |

### RabbitMQ и инфраструктура

| YAML-свойство | Переменная окружения |
|---|---|
| `spring.rabbitmq.host` | `SPRING_RABBITMQ_HOST` |
| `spring.rabbitmq.port` | `SPRING_RABBITMQ_PORT` |
| `spring.rabbitmq.virtual-host` | `SPRING_RABBITMQ_VIRTUAL_HOST` |
| `spring.rabbitmq.username` | `SPRING_RABBITMQ_USERNAME` |
| `spring.rabbitmq.password` | `SPRING_RABBITMQ_PASSWORD` |
| `server.port` | `SERVER_PORT` |

---

## 5. Примеры конфигурации

### Минимальная конфигурация

Минимальный `application.yml` для запуска адаптера:

```yaml
sal:
  adapter:
    name: my-adapter
    type: MyAdapter

spring:
  rabbitmq:
    host: localhost
    port: 5672
    virtual-host: dev
    username: guest
    password: guest
```

Все остальные свойства принимают значения по умолчанию.

---

### Продакшен-конфигурация

Полный `application.yml` с комментариями для каждой секции:

```yaml
# =============================================================================
# Идентификация адаптера
# =============================================================================
sal:
  adapter:
    # Уникальное имя экземпляра — используется в именах очередей RabbitMQ
    name: my-production-adapter
    # Тип адаптера — используется при маршрутизации сообщений
    type: MyProductionAdapter

  # ---------------------------------------------------------------------------
  # Параметры сервиса
  # ---------------------------------------------------------------------------
  service:
    # Ключ среды для HTTP-валидации входящих запросов; пусто = отключена
    environment-key: "prod-secret-key"
    # false: адаптер не принимает HTTP-запросы в offline-состоянии
    enable-offline-mode: false
    # Зарезервировано; не изменять без необходимости
    mb-mode: false
    # Версия SAL-протокола данного адаптера
    sal-version: 1
    # Диапазон допустимых версий SAL endpoint'ов при подключении
    min-ep-sal-version: 1
    max-ep-sal-version: 2147483647
    # Зависимости: адаптер ждёт появления перечисленных адаптеров перед переходом в online
    adapter-dependency:
      - core-adapter
      - auth-adapter
    # Пути к файловым ресурсам (опционально)
    data-path: /var/data/my-adapter
    disk-store-path: /var/data/my-adapter/store

  # ---------------------------------------------------------------------------
  # HTTP-клиент для обращений к другим адаптерам
  # ---------------------------------------------------------------------------
  client:
    # Таймаут бизнес-запросов (секунды)
    request-timeout: 120
    # Таймаут служебных запросов — ping, health (секунды)
    service-request-timeout: 10

  # ---------------------------------------------------------------------------
  # Обработчик команд из RabbitMQ
  # ---------------------------------------------------------------------------
  command:
    # Consumer-потоки для входящих команд
    threads: 20
    # Consumer-потоки для результатов команд
    result-threads: 20

  # ---------------------------------------------------------------------------
  # Обработчик событий из RabbitMQ
  # ---------------------------------------------------------------------------
  event:
    # Consumer-потоки для входящих событий
    threads: 4

  # ---------------------------------------------------------------------------
  # WatchDog — мониторинг endpoint'ов
  # ---------------------------------------------------------------------------
  watchdog:
    # Интервал ping-проверки зарегистрированных endpoint'ов (мс)
    ping-interval-ms: 15000
    # Интервал очистки устаревших endpoint'ов (мс)
    remover-interval-ms: 120000

# =============================================================================
# Spring Boot
# =============================================================================
spring:
  application:
    # Имя приложения берётся из имени адаптера
    name: ${sal.adapter.name}

  # ---------------------------------------------------------------------------
  # RabbitMQ — пароль задавать через переменную окружения SPRING_RABBITMQ_PASSWORD
  # ---------------------------------------------------------------------------
  rabbitmq:
    host: rabbitmq.internal
    port: 5672
    virtual-host: production
    username: sal-user
    password: ${RABBITMQ_PASSWORD}

  # ---------------------------------------------------------------------------
  # Graceful shutdown: дожидаться завершения текущих запросов
  # ---------------------------------------------------------------------------
  lifecycle:
    timeout-per-shutdown-phase: 30s

# =============================================================================
# HTTP-сервер
# =============================================================================
server:
  port: 8080
  # graceful: сервер перестаёт принимать новые запросы и ждёт завершения текущих
  shutdown: graceful

# =============================================================================
# Actuator — метрики и health checks
# =============================================================================
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
      probes:
        # Liveness и Readiness probes для Kubernetes
        enabled: true
```

---

## 6. См. также

- [operations-guide.md](operations-guide.md) — развёртывание, мониторинг, graceful shutdown
- [adapter-development-guide.md](adapter-development-guide.md) — разработка адаптера на Spring Boot
- [glossary.md](glossary.md) — термины SAL: endpoint, adapter, command, event, WatchDog
