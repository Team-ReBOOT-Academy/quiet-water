# `ru.omsu.quietwater` (тесты)

Проверки повторяют слои рабочего кода и не поднимают настоящий брокер.

## Сценарии

- `application/ProcessClosedCandleTest.kt` — одна свеча даёт ровно одну рекомендацию HOLD с заполненными полями и временем обработки.
- `application/GetHealthStatusTest.kt` — упавший компонент делает общий статус процесса DEGRADED.

## Домен

- `domain/RsiReversalSetupTest.kt` — BUY после распродажи, SELL после ралли и HOLD на слишком коротком ряде.
- `domain/SuccessRateTest.kt` — штраф за неполное окно и расчёт по последним W исходам.
- `domain/WalkForwardPolicyTest.kt` — на фазах LEARN и EXAM наружу всегда уходит HOLD.

## Адаптеры

Брокер не нужен: отправка записывается в память.

- `adapter/config/LocalEnvFileTest.kt` — читает присваивания, пропускает комментарии и битые строки и на отсутствующем файле возвращает пустую карту.
- `adapter/kafka/KafkaClientPropertiesTest.kt` — свойства SASL-клиента и отсутствие учётных данных при PLAINTEXT.
- `adapter/kafka/RegistrationFlowTest.kt` — заявка на регистрацию, публикация отложенной свечи после принятия, игнор чужого аккаунта и отказ.
- `adapter/kafka/CandleToRecommendationTest.kt` — пример свечи становится одним событием рекомендации HOLD, а неполный payload отклоняется до публикации.
- `adapter/persistence/InMemoryInstrumentWindowStoreTest.kt` — повтор той же свечи не увеличивает окно.

## HTTP

- `api/HealthRoutesTest.kt` — без брокера `/` и `/health` отвечают 200, общий статус DEGRADED, а Kafka — DOWN с текстом «broker is not configured».
