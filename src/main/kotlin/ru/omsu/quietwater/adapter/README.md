# Адаптеры

Инфраструктура вокруг прикладного слоя.

## Конфигурация

- `config/LocalEnvFile.kt` — разбирает локальный `.env`: пропускает комментарии и пустые строки, снимает кавычки со значений.
- `config/EnvConfig.kt` — собирает настройки из `.env`, переменных окружения и `application.conf`. Переменные окружения перекрывают файл.

## Kafka

Клиент брокера и перевод платформенных событий в команды приложения.

- `kafka/KafkaClientProperties.kt` — собирает свойства consumer и producer. SASL добавляется, только если протокол не PLAINTEXT.
- `kafka/KafkaProducerHandle.kt` — держит подключённый producer и синхронно отправляет запись с таймаутом.
- `kafka/PlatformEventCodec.kt` — кодирует и декодирует конверт события и его payload.
- `kafka/CandleMessageMapper.kt` — переводит payload закрытой свечи в команду. Принимает и `instrument`, и `ticker`.
- `kafka/CandleEventValidator.kt` — проверяет конверт и обязательные поля свечи и возвращает список недостающих.
- `kafka/ProcessedEventIds.kt` — помнит последние event id в пределах ёмкости, чтобы не обрабатывать дубли.
- `kafka/dto/EventEnvelopeDto.kt` — конверт: идентификаторы, тип, версия схемы, время, producer и произвольный payload.
- `kafka/dto/CandlePayloadDto.kt` — тело свечи: инструмент, прогон, номер и цены.
- `kafka/dto/RegistrationRequestPayload.kt` — заявка на регистрацию счёта: имя, группа, ключ и идентификаторы студента и стратегии.
- `kafka/dto/RegistrationResultPayload.kt` — ответ платформы: принятый счёт и сессия либо причина отказа.
- `kafka/dto/RecommendationPayloadDto.kt` — исходящая рекомендация со счётом, сигналом и ссылкой на исходную свечу.
- `kafka/inbound/KafkaCandleListener.kt` — в отдельном потоке читает топики, переподключается с нарастающей паузой и коммитит offset после обработки.
- `kafka/inbound/IncomingEventHandler.kt` — разбирает событие: регистрацию принимает или отклоняет, свечу проверяет и либо обрабатывает сразу, либо держит до принятия регистрации.
- `kafka/outbound/RequestRegistration.kt` — пока регистрация в ожидании, публикует заявку `account.registration.request`.
- `kafka/outbound/KafkaRecommendationPublisher.kt` — публикует рекомендацию в исходящий топик только после принятой регистрации. Иначе пишет в лог, что она удержана.

## Зонды

- `health/KafkaLink.kt` — потокобезопасный снимок связи с брокером: не настроена, недоступна или поднята.
- `health/KafkaHealthProbe.kt` — зонд компонента `kafka`, который читает этот снимок.
- `health/CompositeHealthProbe.kt` — сводит несколько зондов в один: UP, только если все в порядке.

## Лог

- `logging/StructuredLog.kt` — пишет событие одной JSON-строкой и маскирует секрет в тексте ошибки.

## Память

Постоянной базы нет: окно живёт, пока жив процесс.

- `persistence/InMemoryInstrumentWindowStore.kt` — хранит свечи по инструменту, отбрасывает повтор того же номера и вытесняет старые сверх лимита.
