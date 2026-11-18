# `ru.omsu.quietwater`

Корень сервиса. Слои не зависят внутрь наружу: домен не знает про Kafka и HTTP.

- `QuietWaterApplication.kt` — собирает сервисы, поднимает HTTP на Netty и Kafka-слушатель, а при остановке корректно их гасит.
- [domain](domain/README.md) — свечи, сигнал, RSI и walk-forward.
- [application](application/README.md) — сценарии и порты, без брокера и фреймворка.
- [adapter](adapter/README.md) — Kafka, конфиг, логи, health и память.
- [api](api/README.md) — HTTP-маршруты и ответы.
- [data](data/README.md) — зарезервированный пустой пакет. Постоянного хранилища пока нет.
