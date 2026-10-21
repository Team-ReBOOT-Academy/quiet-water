# Схема компонентов

Рядом с [проектным документом](project-description.md).
Показаны только основные части процесса, их ответственность и потоки данных.

```mermaid
flowchart LR
  subgraph outside [Стенд]
    broker[Kafka]
  end

  subgraph adapters [Адаптеры]
    env[Конфигурация окружения]
    client["Клиент Kafka: чтение и запись"]
    memory[Окно свечей в памяти]
  end

  subgraph scenarios [Сценарии]
    registration[Регистрация сервиса]
    candle[Решение по закрытой свече]
    health[Сводка health]
  end

  subgraph core [Домен]
    policy["Walk-forward и RSI"]
  end

  subgraph http [HTTP]
    route["GET /health"]
  end

  broker -->|"свечи и ответ регистрации"| client
  client -->|"запрос регистрации и рекомендация"| broker
  env -->|"адрес, топик, group.id, пороги"| client
  env -->|"имя и версия сервиса"| health
  client --> registration
  registration -->|"можно ли публиковать"| candle
  client --> candle
  candle --> memory
  candle --> policy
  policy -->|"BUY / SELL / HOLD"| candle
  health --> route
```

| Часть | Ответственность |
|---|---|
| Конфигурация окружения | Адрес брокера, топик, свой `group.id`, пороги стратегии. Секретов в репозитории нет |
| Клиент Kafka | Подписка на `market.candle.closed`, запрос регистрации, публикация рекомендации. Обрыв брокера не останавливает процесс |
| Регистрация | Ждёт `account.registration.accepted`. При отказе фиксирует причину и запрещает публикацию |
| Окно в памяти | Короткая история свечей по тикеру. Отдельной базы в этом задании нет |
| Walk-forward и RSI | Фаза и сигнал. Не знает ни про HTTP, ни про Kafka |
| GET /health | Отдаёт `status`, `service`, `version`, `components` без авторизации |
