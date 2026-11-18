# Сценарии

Прикладной слой. Он вызывает порты и не знает, Kafka это или память.

## Закрытая свеча

- `candle/ClosedCandleCommand.kt` — команда «закрылась свеча»: исходное событие, correlation id и сама свеча.
- `candle/BaselineHold.kt` — фиксированная базовая рекомендация, которую сервис отдаёт, пока торговый сигнал не публикуется.
- `candle/ProcessClosedCandle.kt` — кладёт свечу в окно инструмента, собирает рекомендацию из базового HOLD и отдаёт её издателю.

## Регистрация

- `registration/RegistrationSession.kt` — потокобезопасное состояние регистрации: ожидание, принятие с учётными данными или отказ.

## Здоровье процесса

- `health/GetHealthStatus.kt` — опрашивает зонды и сводит общий статус в UP, если все компоненты живы, иначе в DEGRADED.

## Порты

Границы слоя. Реализации лежат в `adapter`.

- `port/inbound/CandleListener.kt` — запуск и остановка прослушивания свечей.
- `port/outbound/OutboundSender.kt` — отправка строки в топик по ключу.
- `port/outbound/InstrumentWindowStore.kt` — запись свечи и чтение окна по инструменту.
- `port/outbound/RecommendationPublisher.kt` — публикация готовой рекомендации.
- `port/outbound/HealthProbe.kt` — проверка одного компонента и статусы UP, DOWN и DEGRADED.
