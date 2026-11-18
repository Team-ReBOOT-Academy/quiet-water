# Готовность quiet-water

Ссылка открывает файл целиком. Предпросмотр Cursor не переходит по якорю после `#` и показывает ошибку поиска.

## Этап 1. Каркас сервиса

- [ ] **да.** Проектный документ хранится в Git и содержит назначение, цели, задачи, границы, внешние системы, состав компонентов, роли, инструменты, допущения и открытые вопросы. [Документ](docs/project-description.md)
- [ ] **да.** Границы отделяют сервис торговой стратегии от преподавательской платформы и других внешних систем. Раздел «2. Границы проекта». [Границы](docs/project-description.md)
- [ ] **да.** Для взаимодействий, ролей и инструментов есть обоснование, а не только перечень названий. Разделы «5. Роли и ответственность» и «6. Выбор инструментов». [Роли и инструменты](docs/project-description.md)
- [ ] **да.** Проект собирается одной командой без среды разработки. Раздел «Команды сборки и запуска без среды разработки». [Команды](README.md)
- [ ] **да.** Контейнер стартует без запросов в консоли, `GET /health` отвечает HTTP 200, `docker stop` завершает процесс по SIGTERM примерно за секунду, без таймаута и SIGKILL. [Dockerfile](Dockerfile), [раздел Docker в README](README.md)
- [ ] **да.** `GET /health` доступен без авторизации и возвращает HTTP 200 с полями `status`, `service`, `version` и `components`. [Маршрут](src/main/kotlin/ru/omsu/quietwater/api/health/HealthRoutes.kt), [поля ответа](src/main/kotlin/ru/omsu/quietwater/api/dto/HealthResponse.kt), [тест](src/test/kotlin/ru/omsu/quietwater/api/HealthRoutesTest.kt)
- [ ] **да.** Конфигурация задаётся переменными окружения, секреты не попадают в Git. [Образец окружения](.env.example), [игнор](.gitignore) `.env`, [чтение окружения](src/main/kotlin/ru/omsu/quietwater/adapter/config/EnvConfig.kt)
- [ ] **да.** По README другой человек понимает назначение сервиса, собирает и запускает его. [README](README.md)

## Этап 2. Регистрация и брокер

- [ ] **да.** Контекстная и компонентная схемы лежат в Git, читаются без специального окружения и не противоречат проектному документу. [Контекст](docs/context-diagram.md), [компоненты](docs/component-diagram.md)
- [ ] **да.** В системе учёта задач есть план работ, ссылка на него стоит в README. [Задачи](docs/tasks.md), [ссылка в README](README.md)
- [ ] **да.** После регистрации платформа показывает студента, учебную группу, стратегию и сервис: Щербак Руслан, МПБ-401, quiet-water, quiet-water. Эти поля уходят в `account.registration.request`. [Запрос регистрации](src/main/kotlin/ru/omsu/quietwater/adapter/kafka/outbound/RequestRegistration.kt), [поля запроса](src/main/kotlin/ru/omsu/quietwater/adapter/kafka/dto/RegistrationRequestPayload.kt)
- [ ] **да.** Повторный запуск берёт те же идентификаторы студента, стратегии, имени, группы и ключа. Новый студент из других значений не собирается. [Идентификаторы](src/main/resources/application.conf), [повтор запроса только пока регистрация не завершена](src/main/kotlin/ru/omsu/quietwater/adapter/kafka/outbound/RequestRegistration.kt)
- [ ] **нет.** Сервис умеет читать последовательность `market.candle.closed`, но имя темы свечей ещё не задано, живая последовательность не приходила. [Подписка](src/main/kotlin/ru/omsu/quietwater/adapter/kafka/inbound/KafkaCandleListener.kt), [проверка события](src/main/kotlin/ru/omsu/quietwater/adapter/kafka/CandleEventValidator.kt)
- [ ] **да.** Адрес брокера и `secretKey` не зашиты в исходный код: адрес берётся из окружения, ключ — из `SECRET_KEY` или логина Kafka. [Окружение](src/main/kotlin/ru/omsu/quietwater/adapter/config/EnvConfig.kt), [свойства клиента](src/main/kotlin/ru/omsu/quietwater/adapter/kafka/KafkaClientProperties.kt)
- [ ] **да.** После обрыва брокера цикл соединения поднимает клиента снова и не завершает процесс. [Цикл переподключения](src/main/kotlin/ru/omsu/quietwater/adapter/kafka/inbound/KafkaCandleListener.kt)

## Этап 3. Свеча и рекомендация HOLD

- [ ] **да.** Сценарии описывают нормальный ход, значимые альтернативы и наблюдаемый результат. [Сценарии](docs/use-cases.md)
- [ ] **да.** Требования и задачи согласованы со сценариями. [Требования](docs/requirements.md), [задачи](docs/tasks.md)
- [ ] **нет.** На каждую корректную свечу сервис готовит ровно одну рекомендацию `HOLD`. Платформа её ещё не принимала: нет темы свечей и подтверждённой регистрации. [Сборка рекомендации](src/main/kotlin/ru/omsu/quietwater/application/candle/ProcessClosedCandle.kt), [значения HOLD](src/main/resources/application.conf)
- [ ] **да.** Рекомендация совпадает с контрактом и связана с исходной свечой через `sourceEventId`, `correlationId`, `causationId`, `runId` и `sequenceNumber`. [Публикация](src/main/kotlin/ru/omsu/quietwater/adapter/kafka/outbound/KafkaRecommendationPublisher.kt), [поля](src/main/kotlin/ru/omsu/quietwater/adapter/kafka/dto/RecommendationPayloadDto.kt)
- [ ] **да.** Некорректное сообщение не останавливает получателя: ошибка пропускается, смещение фиксируется, чтение продолжается. [Отказ до обработки](src/main/kotlin/ru/omsu/quietwater/adapter/kafka/inbound/IncomingEventHandler.kt), [пропуск записи](src/main/kotlin/ru/omsu/quietwater/adapter/kafka/inbound/KafkaCandleListener.kt)
- [ ] **нет.** Рекомендация должна успеть до закрытия окна ожидания платформы. Сервис отправляет её в том же цикле обработки, доставка на стенде не подтверждалась. [Отправка](src/main/kotlin/ru/omsu/quietwater/adapter/kafka/outbound/KafkaRecommendationPublisher.kt)
- [ ] **да.** Преобразование входного события проверено автоматическим тестом на отдельном примере сообщения. [Тест](src/test/kotlin/ru/omsu/quietwater/adapter/kafka/CandleToRecommendationTest.kt), [пример](src/test/resources/messages/market.candle.closed.json)
