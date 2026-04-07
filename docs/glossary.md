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
| **EndPoint** | Экземпляр удалённого адаптера, за которым следит WatchDog. Содержит URI, статус доступности, версию SAL. |
| **EnvironmentKey** | Общий секрет среды. Проверяется `EnvironmentKeyInterceptor` при каждом HTTP-запросе. Задаётся в `sal.service.environment-key`. |
| **Event** | Доменное событие, публикуемое через EventBus по модели publish/subscribe. Реализует интерфейс `Event`. |
| **EventBus** | Шина событий — публикация событий всем подписчикам через RabbitMQ. Интерфейс `EventBus`, реализация `DefaultEventBus`. |
| **EventHandler** | Обработчик входящего события. Реализует интерфейс `EventHandler<E>`. |
| **Fire-and-Forget** | Паттерн отправки команды без ожидания результата. Используется когда `SourceServiceId` отсутствует. |
| **MDC** | Mapped Diagnostic Context (SLF4J). SAL записывает `correlationId`, `sessionId`, `adapterName` для корреляции логов. Утилита `SalMdc`. |
| **RecordedMessage** | Внутренняя обёртка сообщения SAL. Содержит payload, метаданные (correlationId, priority, timestamps), AdditionalData. Маппится на AMQP properties при отправке/получении. |
| **Request/Reply** | Паттерн отправки команды с ожиданием результата. `CommandBus.executeCommandAsync()` возвращает `CompletableFuture<R>`. |
| **SAL** | Service Adapter Layer — инфраструктурный фреймворк для межадаптерного взаимодействия в экосистеме TCB. |
| **@ServiceMessage** | Аннотация на Event-классе. Меняет routing key на `"service"`, что позволяет создать отдельную очередь для служебных сообщений. |
| **Session** | Контекстные данные (Map<String, Object>), пробрасываемые между адаптерами. Хранятся в `SessionHolder` (ThreadLocal), сериализуются в compressed Base64 для передачи. |
| **SessionHolder** | ThreadLocal-контейнер для session data текущего потока. Аналог C# `CallContext.LogicalSetData("Session")`. |
| **TypeMappingRegistry** | Реестр bidirectional маппинга C#-имён типов на Java-классы. Используется `SalMessageConverter` при десериализации payload по `ContentType` header. |
| **WatchDog** | Подсистема мониторинга здоровья адаптера. Периодически пингует зависимые EndPoint'ы и управляет состоянием online/offline. |
| **Wire-format** | Формат данных на проводе (AMQP + JSON). PascalCase, совместимый с C# Newtonsoft.Json. Определяется `wireObjectMapper`. |
| **@CommandType** | Аннотация, задающая wire-имя команды (обычно C# полное имя типа). Используется для регистрации в `CommandHandlerRegistry` и `TypeMappingRegistry`. |

## См. также

- [architecture.md](architecture.md) — архитектура SAL и взаимодействие компонентов
- [wire-protocol.md](wire-protocol.md) — формат данных на проводе и сериализация сообщений
