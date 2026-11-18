# HTTP

Тонкий слой Ktor. Бизнес-решение здесь не считается.

- `health/HealthRoutes.kt` — маршруты проверки живости: `GET /` и `GET /health`.
- `dto/HealthResponse.kt` — JSON-ответ этих маршрутов: `status`, `service`, `version` и `components`.
