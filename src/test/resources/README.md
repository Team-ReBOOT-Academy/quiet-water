# Ресурсы тестов

Файлы доступны тестам через class loader и не попадают в собранный сервис.

- `messages/market.candle.closed.json` — входящее событие `market.candle.closed` по SBER. Его читает `CandleToRecommendationTest`.
