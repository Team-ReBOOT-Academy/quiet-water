# Тест HTTP

- `HealthRoutesTest.kt` — без брокера `/` и `/health` отвечают 200, общий статус DEGRADED, а Kafka — DOWN с текстом «broker is not configured».
