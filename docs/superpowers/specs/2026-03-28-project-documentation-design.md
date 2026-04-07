# Спецификация: Проектная документация TCB-SAL

**Дата:** 2026-03-28
**Статус:** Утверждён
**Аудитория документации:** разработчики адаптеров, разработчики ядра SAL, DevOps/SRE

## Контекст

TCB-SAL — Java/Spring Boot миграция инфраструктуры сервис-адаптеров из C#. Проект обеспечивает асинхронное взаимодействие адаптеров через RabbitMQ с полной wire-совместимостью с оригинальной C# системой. Планируется параллельная эксплуатация Java- и C#-адаптеров в смешанной среде.

Необходима проектная документация для внедрения, сопровождения и развития модуля.

## Принципы

- **Язык:** русский текст, английские термины и примеры кода
- **Диаграммы:** Mermaid-формат (рендерится в GitHub/GitLab)
- **Модульность:** каждый документ самодостаточен, с перекрёстными ссылками
- **Акцент:** смешанная C#/Java среда и wire-совместимость
- **Примеры:** на базе sal-example-adapter

## Структура пакета документации

```
docs/
├── architecture.md
├── adapter-development-guide.md
├── core-development-guide.md
├── operations-guide.md
├── configuration-reference.md
├── wire-protocol.md
├── troubleshooting.md
└── glossary.md
```

8 документов, суммарный объём ~90-130 страниц.

---

## 1. architecture.md (~15-20 страниц)

**Аудитория:** все роли — фундамент, на который ссылаются остальные документы.

### Содержание

1. **Обзор SAL** — назначение, место в экосистеме TCB, модель взаимодействия адаптеров
2. **Модульная структура** — sal-api, sal-spring-boot-starter, sal-test, sal-example-adapter; зависимости между модулями
3. **Ключевые абстракции** — Command/CommandResult, Event, CommandBus, EventBus, CommandHandler (sync/async/confirmatory), EventHandler
4. **Потоки данных** — 4 Mermaid-диаграммы:
   - Command fire-and-forget flow
   - Command request/reply flow (с timeout)
   - Confirmatory command flow (двухфазный)
   - Event publish/subscribe flow
5. **Фазы инициализации** — порядок автоконфигурации: Serialization → RabbitMQ → CommandBus/EventBus → Web → WatchDog → Listeners
6. **Контекст и сессия** — ThreadLocal-модель: SessionHolder, CommandContextHolder, SalMdc; жизненный цикл контекста для HTTP и RabbitMQ entry points
7. **Смешанная среда C#/Java** — как Java-адаптеры сосуществуют с C#-адаптерами: общий RabbitMQ, wire-совместимость, TypeMappingRegistry

---

## 2. adapter-development-guide.md (~25-30 страниц)

**Аудитория:** разработчики, создающие новые адаптеры поверх sal-spring-boot-starter.

### Содержание

1. **Quick Start** — минимальный адаптер за 5 шагов: pom.xml зависимость, application.yml, Application класс, первый CommandHandler, запуск с docker-compose
2. **Структура проекта адаптера** — рекомендуемая организация пакетов, naming conventions
3. **Работа с командами:**
   - Определение Command + CommandResult классов, аннотация @CommandType
   - Синхронный CommandHandler — простой случай (на примере EchoCommand)
   - Асинхронный CommandHandlerAsync — когда использовать, работа с CompletableFuture
   - ConfirmatoryCommandHandler — двухфазное подтверждение
   - Отправка команд другим адаптерам через CommandBus (fire-and-forget и request/reply)
   - Таймауты и приоритеты
4. **Работа с событиями:**
   - Определение Event классов, @ServiceMessage аннотация
   - EventHandler — подписка на события
   - Публикация событий через EventBus
5. **Сессия** — чтение/запись данных в SessionHolder, автоматическое пробрасывание между адаптерами
6. **HTTP-контроллеры** — PingController (обязательный), кастомные контроллеры, работа с EnvironmentKey
7. **Регистрация типов для C# interop** — TypeMappingRegistry, маппинг C#-имён на Java-классы
8. **Обработка ошибок** — ErrorException, ValidationException, FatalException; SalErrorCodes
9. **Тестирование** — unit-тесты хендлеров, интеграционные тесты с Testcontainers (RabbitMQ)
10. **Чеклист перед деплоем** — обязательные проверки нового адаптера

---

## 3. core-development-guide.md (~15-20 страниц)

**Аудитория:** разработчики, модифицирующие sal-api и sal-spring-boot-starter.

### Содержание

1. **Архитектурные принципы** — разделение sal-api (нет Spring-зависимостей) и sal-spring-boot-starter; wire-совместимость с C# как инвариант; backward compatibility
2. **Сборка и локальная разработка** — `mvn install`, запуск example-adapter, docker-compose, отладка через RabbitMQ Management UI
3. **Гид по модулям:**
   - sal-api: добавление новых Command/Event/DTO типов, правила именования, Jackson-аннотации
   - sal-spring-boot-starter: конвенции AutoConfiguration, порядок фаз, @ConditionalOnMissingBean для расширяемости
4. **Ключевые компоненты и их контракты:**
   - SalMessageConverter — AMQP property mapping, как добавить новые headers
   - TypeMappingRegistry — добавление маппингов, fallback-цепочка
   - SessionSerializer — формат 7-bit length prefix + Deflate, совместимость с C# BinaryWriter/DeflateStream
   - CommandConsumer — жизненный цикл обработки, async context capture pattern
   - WatchDogService — synchronized-контракт, signalHealthy/signalUnhealthy
5. **Wire-протокол как контракт** — что нельзя менять без координации с C#-стороной (PascalCase, session compression, AMQP property mapping, exchange/queue naming)
6. **Намеренные совместимости** — сохранённые опечатки из C# (`"Exeption"`, `"Confirmtation"`), причины и где они используются
7. **Тестирование ядра** — WireCompatibilityTest, SalMessageConverterTest, unit vs integration; что покрывать при изменениях
8. **Известные технические долги** — пустой sal-test модуль, дублирование ObjectMapper в тестах, exchange-имена событий (Java vs C# names)

---

## 4. operations-guide.md (~15-20 страниц)

**Аудитория:** DevOps/SRE, ответственные за деплой, мониторинг и сопровождение.

### Содержание

1. **Требования к окружению** — Java 21, RabbitMQ 3.x с management plugin, virtual host, учётные данные; сетевые порты (5672, 15672, 8080)
2. **Конфигурация RabbitMQ** — virtual host, пользователи, permissions; docker-compose для dev-среды; рекомендации для production (кластер, HA queues)
3. **Деплой адаптера** — сборка JAR (`mvn package`), запуск, профили Spring, externalized configuration (env vars, config server)
4. **Health и мониторинг:**
   - Actuator endpoints: `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus`
   - AdapterStateHealthIndicator — что означают статусы online/offline/shutdown
   - `/ping` endpoint — проверка доступности адаптера
   - Ключевые метрики для alerting
5. **Логирование** — структура MDC (correlationId, sessionId, adapterName), logback-spring.xml, корреляция запросов между адаптерами, Logstash-интеграция
6. **RabbitMQ топология** — полная карта exchanges и queues, naming conventions; что создаётся автоматически, что нужно pre-provision; dead-letter и rejected-message queues
7. **Graceful shutdown** — порядок остановки (SmartLifecycle phases), `spring.lifecycle.timeout-per-shutdown-phase`, drain in-flight сообщений
8. **Масштабирование** — `sal.command.threads`, `sal.command.result-threads`, `sal.event.threads`; горизонтальное масштабирование (несколько инстансов одного адаптера, конкурирующие consumers)
9. **Ручное управление** — toggle online/offline через SwitchController, WatchDog manual override

---

## 5. configuration-reference.md (~10 страниц)

**Аудитория:** все роли.

### Содержание

1. **SAL Properties** — полная таблица всех `sal.*` свойств. Для каждого параметра: свойство, тип, значение по умолчанию, описание, влияние на поведение, C#-эквивалент (имя конфига)
2. **Spring RabbitMQ Properties** — критичные для SAL `spring.rabbitmq.*` свойства: host, port, virtual-host, credentials, connection timeout, channel cache size
3. **Spring Boot Properties** — `server.port`, `server.shutdown`, `spring.lifecycle.timeout-per-shutdown-phase`, actuator exposure
4. **Переменные окружения** — маппинг YAML → env vars (пример: `sal.adapter.name` → `SAL_ADAPTER_NAME`)
5. **Примеры конфигурации** — минимальный application.yml, production application.yml с комментариями

---

## 6. wire-protocol.md (~15 страниц)

**Аудитория:** все роли, особенно важен при отладке межадаптерного взаимодействия в смешанной C#/Java среде.

### Содержание

1. **Общие принципы** — JSON over AMQP, PascalCase naming, совместимость с C# Newtonsoft.Json
2. **AMQP Message Format** — полная таблица маппинга RecordedMessage ↔ AMQP properties (ContentType, CorrelationId, Priority, Timestamp, Expiration, Additional-Data header)
3. **JSON Serialization Rules:**
   - PascalCase property names
   - Null-поля опускаются
   - Enums как строки
   - Даты в ISO 8601
   - Примеры: как выглядит EchoCommand в JSON на проводе
4. **Session Wire Format** — пошаговая схема компрессии: Map → JSON → UTF-8 → 7-bit length prefix → Deflate → Base64; hex-дамп примера для верификации
5. **HTTP Session Propagation** — заголовок `TCB.Header-Session`, формат body: `[payload bytes][session bytes]`, offset/length encoding
6. **RabbitMQ Topology Contract:**
   - Exchange names (фиксированные, нельзя менять)
   - Queue naming conventions
   - Routing key semantics для commands vs events
7. **Type Mapping** — как ContentType header связывает C# type names с Java-классами; формат `"Namespace.Class, Assembly"` → strip assembly → resolve
8. **Намеренные совместимости** — опечатки сохранённые из C# (`Exeption`, `Confirmtation`), их расположение в протоколе
9. **Примеры сообщений** — полные примеры AMQP-сообщений для: command, command result (completed), command result (failed), event; с hex-дампами headers

---

## 7. troubleshooting.md (~10 страниц)

**Аудитория:** все роли.

### Формат

Каждая проблема: симптомы → причина → решение.

### Содержание

1. **Адаптер не стартует** — connection refused к RabbitMQ, неверный virtual host, отсутствие Java 21
2. **Команда не доставляется** — неправильный @CommandType, exchange не объявлен, routing key mismatch
3. **Handler not found** — несовпадение PayloadType/ContentType с зарегистрированным именем, C# assembly suffix
4. **Результат команды не возвращается** — нет SourceServiceId, result queue не создана, таймаут в DefaultCommandBus
5. **Сессия теряется** — ThreadLocal очищен до async callback, SessionHolder.clear() в неправильном месте
6. **Адаптер не переходит в ONLINE** — WatchDog endpoint недоступен, manualOnlineSwitch выключен, version mismatch в EndPointRegistry
7. **Ошибки сериализации** — PascalCase vs camelCase, неизвестный тип в TypeMappingRegistry, несовместимый session format
8. **Проблемы C#/Java interop** — несовпадение exchange names, type mapping не зарегистрирован, опечатки в ключах
9. **Диагностические инструменты** — RabbitMQ Management UI (queues, bindings, message rates), `/actuator/health`, MDC-корреляция в логах, curl-примеры для ручной отправки сообщений

---

## 8. glossary.md (~2 страницы)

**Аудитория:** все роли.

Краткий словарь терминов: Adapter, Command, CommandResult, CommandBus, EventBus, EndPoint, EnvironmentKey, RecordedMessage, SAL, WatchDog, wire-format, TypeMapping, SessionHolder, MDC, fire-and-forget, request/reply, confirmatory command.
