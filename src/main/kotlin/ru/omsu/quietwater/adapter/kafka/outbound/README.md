# Исходящий поток

- `RequestRegistration.kt` — пока регистрация в ожидании, публикует заявку `account.registration.request`.
- `KafkaRecommendationPublisher.kt` — публикует рекомендацию в исходящий топик только после принятой регистрации. Иначе пишет в лог, что она удержана.
