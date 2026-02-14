# Payment Service

Service responsible for processing payment requests from other services and managing currency exchange operations.

## Quick start

Prerequisites:
- Java 21+ (JDK)
- Docker (for container runs)
- Redis (for caching)
- [faang-infra services](https://github.com/bormoley1983/faang-infra) running locally or accessible

Run locally:
```sh
./gradlew bootRun
```

Run tests:
```sh
./gradlew test --info
```

Build and run in Docker:
```sh
./gradlew build
docker build -t payment-service .
docker run -p 9080:9080 payment-service
```

## Configuration

Main config: [src/main/resources/application.yaml](src/main/resources/application.yaml)

Key configuration properties:
- **Server Port**: 9080
- **Redis**: localhost:6379
- **Currency Exchange API**: https://api.exchangeratesapi.io/v1
- **Currency Update Schedule**: Every 12 hours (cron: `0 0 */12 * * ?`)

## External Integrations

WebClient:
- Currency Exchange API — integration with exchangeratesapi.io for real-time currency rates
- WebClient configuration: [CurrencyExchangeClientConfig](src/main/java/faang/school/paymentservice/config/currency/CurrencyExchangeClientConfig.java)

Currency services:
- [CurrencyService](src/main/java/faang/school/paymentservice/service/currecny/CurrencyService.java) — manages currency exchange rates and conversions
- [CurrencyRateFetcher](src/main/java/faang/school/paymentservice/service/currecny/CurrencyRateFetcher.java) — fetches currency rates from external API

## Architecture

Controllers:
- [PaymentController](src/main/java/faang/school/paymentservice/controller/PaymentController.java) — payment operation endpoints

Configuration:
- [RedisConfig](src/main/java/faang/school/paymentservice/config/redis/RedisConfig.java) — Redis caching configuration
- [CurrencyExchangeConfig](src/main/java/faang/school/paymentservice/config/currency/CurrencyExchangeConfig.java) — currency exchange settings

**Note:** Base code structure and architecture patterns are based on [FAANG School](https://github.com/faang-school) educational project.