quiet-water/
├── docs/
│   └── project-description.md
├── README.md
├── .env.example
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/wrapper/
├── src/main/kotlin/ru/omsu/quietwater/
│   ├── QuietWaterApplication.kt          ← main, composition root
│   ├── api/                              ← входящий HTTP
│   │   ├── health/
│   │   │   └── HealthRoutes.kt
│   │   └── dto/
│   │       └── HealthResponse.kt
│   ├── application/                      ← сценарии (use cases)
│   │   ├── health/
│   │   │   └── GetHealthStatus.kt
│   │   ├── candle/
│   │   │   └── ProcessClosedCandle.kt
│   │   └── port/                         ← контракты, без реализаций
│   │       ├── inbound/
│   │       │   └── CandleListener.kt
│   │       └── outbound/
│   │           ├── RecommendationPublisher.kt
│   │           ├── InstrumentWindowStore.kt
│   │           └── HealthProbe.kt
│   ├── domain/                           ← ядро, без фреймворков
│   │   ├── candle/
│   │   │   ├── Candle.kt
│   │   │   └── Instrument.kt
│   │   ├── signal/
│   │   │   └── Signal.kt                 ← BUY / SELL / HOLD
│   │   ├── recommendation/
│   │   │   └── Recommendation.kt
│   │   ├── walkforward/
│   │   │   ├── Phase.kt                  ← LEARN / EXAM / TRADE / HALT
│   │   │   └── WalkForwardPolicy.kt
│   │   ├── setup/
│   │   │   └── RsiReversalSetup.kt
│   │   └── score/
│   │       └── SuccessRate.kt
│   ├── adapter/                          ← реализации портов
│   │   ├── kafka/
│   │   │   ├── inbound/
│   │   │   │   └── KafkaCandleListener.kt
│   │   │   ├── outbound/
│   │   │   │   └── KafkaRecommendationPublisher.kt
│   │   │   └── dto/                      ← JSON-обёртка стенда
│   │       │   ├── EventEnvelopeDto.kt
│   │       │   ├── CandlePayloadDto.kt
│   │       │   └── RecommendationPayloadDto.kt
│   │   ├── persistence/
│   │   │   └── InMemoryInstrumentWindowStore.kt
│   │   ├── health/
│   │   │   ├── KafkaHealthProbe.kt
│   │   │   └── CompositeHealthProbe.kt
│   │   └── config/
│   │       └── EnvConfig.kt
│   └── data/                             ← появится на persistence
│       └── (пусто до задания БД)
├── src/main/resources/
│   ├── application.conf
│   └── logback.xml
└── src/test/kotlin/ru/omsu/quietwater/
    ├── domain/                           ← фазы, RSI, score без Ktor
    ├── application/
    └── api/                              ← Ktor test host для /health