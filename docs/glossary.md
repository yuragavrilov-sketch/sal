# Глоссарий TCB-SAL

Глоссарий основных терминов и компонентов Service Adapter Layer (SAL) в экосистеме TCB. Используется для унификации терминологии при описании архитектуры и взаимодействия адаптеров.

| Термин | Определение |
|--------|-----------|
| **Adapter** | Самостоятельный сервис в экосистеме TCB, взаимодействующий с другими адаптерами через SAL. Каждый адаптер имеет уникальное имя (`sal.adapter.name`) и тип (`sal.adapter.type`). |
| **Command** | Сообщение-запрос, отправляемое одним адаптером другому через CommandBus. Реализует интерфейс `Command`. |
| **CommandResult** | Ответное сообщение на команду. Реализует интерфейс `CommandResult`. |
| **CommandBus** | Шина команд — отправка и получение команд между адаптерами через RabbitMQ. Интерфейс `CommandBus`, реализация `DefaultCommandBus`. |
| **CommandHandler** | Обработчик входящей команды. Может быть синхронным (`CommandHandler`), асинхронным (`CommandHandlerAsync`) или двухфазным (`ConfirmatoryCommandHandler`). |
| **Confirmatory Command** | Двухфазная команда: сначала выполняется предварительная проверка (confirmation), затем — основная обработка. |
| **CorrelationId** | UUID, связывающий command и его result. Используется для корреляции в логах и в механизме request/reply. |
| **EnvironmentKey** | Общий секрет среды, задаётся в `sal.service.environment-key`. После удаления web-слоя фреймворк это значение нигде не проверяет (HTTP-интерцепторов в стартере нет) — свойство сохранено для совместимости конфигов. |
| **Event** | Доменное событие, публикуемое через EventBus по модели publish/subscribe. Реализует интерфейс `Event`. |
| **EventBus** | Шина событий — публикация событий всем подписчикам через RabbitMQ. Интерфейс `EventBus`, реализация `DefaultEventBus`. |
| **EventHandler** | Обработчик входящего события. Реализует интерфейс `EventHandler<E>`. |
| **Fire-and-Forget** | Паттерн отправки команды без ожидания результата. Используется когда `SourceServiceId` отсутствует. |
| **MDC** | Mapped Diagnostic Context (SLF4J). SAL записывает единственный ключ `correlationId` (константа `SalContext.MDC_CORRELATION_ID`) через `SalContext.setCorrelationId(String)`. Прежних ключей `sessionId`/`adapterName` в MDC больше нет. |
| **RecordedMessage** | Внутренняя обёртка сообщения SAL. Содержит payload, метаданные (correlationId, priority, timestamps), AdditionalData. Маппится на AMQP properties при отправке/получении. |
| **Request/Reply** | Паттерн отправки команды с ожиданием результата. `CommandBus.executeCommandAsync()` возвращает `CompletableFuture<R>`. |
| **SAL** | Service Adapter Layer — инфраструктурный фреймворк для межадаптерного взаимодействия в экосистеме TCB. |
| **@ServiceMessage** | Аннотация на Event-классе. Меняет routing key на `"service"`, что позволяет создать отдельную очередь для служебных сообщений. |
| **Session** | Контекстные данные (`Map<String, Object>`), пробрасываемые между адаптерами. Хранятся в `SalContext` (ThreadLocal, доступ через `SalContext.session()` / `SalContext.setSession(Map)`), сериализуются в compressed Base64 для передачи. |
| **SalContext** | Единый ThreadLocal-фасад в `sal-spring-boot-starter`, объединяющий session, `CommandContext` и MDC `correlationId`. API: `session()` / `setSession(Map)`, `commandContext()` / `setCommandContext(CommandContext)`, `setCorrelationId(String)`, `clear()`. Аналог C# `CallContext.LogicalSetData("Session")` + `CurrentCommand`. |
| **TypeMappingRegistry** | Реестр bidirectional маппинга C#-имён типов на Java-классы. Используется `SalMessageConverter` при десериализации payload по `ContentType` header. |
| **Wire-format** | Формат данных на проводе (AMQP + JSON). PascalCase, совместимый с C# Newtonsoft.Json. Определяется `wireObjectMapper`. |
| **@CommandType** | Аннотация, задающая wire-имя команды (обычно C# полное имя типа). Используется для регистрации в `CommandHandlerRegistry` и `TypeMappingRegistry`. |

## См. также

- [architecture.md](architecture.md) — архитектура SAL и взаимодействие компонентов
- [wire-protocol.md](wire-protocol.md) — формат данных на проводе и сериализация сообщений
