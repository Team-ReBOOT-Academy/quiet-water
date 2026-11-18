# Kafka

Клиент брокера и перевод платформенных событий в команды приложения.

- `KafkaClientProperties.kt` — собирает свойства consumer и producer. SASL добавляется, только если протокол не PLAINTEXT.
- `KafkaProducerHandle.kt` — держит подключённый producer и синхронно отправляет запись с таймаутом.
- `PlatformEventCodec.kt` — кодирует и декодирует конверт события и его payload.
- `CandleMessageMapper.kt` — переводит payload закрытой свечи в команду. Принимает и `instrument`, и `ticker`.
- `CandleEventValidator.kt` — проверяет конверт и обязательные поля свечи и возвращает список недостающих.
- `ProcessedEventIds.kt` — помнит последние event id в пределах ёмкости, чтобы не обрабатывать дубли.
- [dto](dto/README.md) — сериализуемые тела сообщений.
- [inbound](inbound/README.md) — чтение топиков.
- [outbound](outbound/README.md) — заявка на регистрацию и публикация рекомендации.
