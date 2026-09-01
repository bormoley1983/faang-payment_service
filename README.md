# Payment Service

Service responsible for processing payment requests from other services and managing currency exchange operations.

## Quick start

Prerequisites:
- Java 25+ (JDK)
- Docker (for container runs)
- Redis (for caching)
- [faang-infra services](https://github.com/bormoley1983/faang-infra) running locally or accessible

Run locally:
```sh
SPRING_PROFILES_ACTIVE=payment-mock ./gradlew bootRun
```

The payment endpoint is an educational mock and is deliberately disabled unless
the explicit `payment-mock` Spring profile is active. Do not enable this profile
in production: it does not contact a payment processor or persist a ledger entry.

Run tests:
```sh
./gradlew test --info
```

Build and run in Docker:
```sh
./gradlew build
docker build -t payment-service .
docker run -p 8082:8082 payment-service
```

## Configuration

Main config: [src/main/resources/application.yaml](src/main/resources/application.yaml)

Key configuration properties:
- **Server Port**: 8082
- **Redis**: localhost:6379
- **Currency Exchange API**: https://api.exchangeratesapi.io/v1
- **Currency Exchange API key**: required through `CURRENCY_EXCHANGE_ACCESS_KEY`
- **Currency Update Schedule**: Every 12 hours (cron: `0 0 */12 * * ?`)
- **Mock payment endpoint**: disabled by default; enable only with the
  `payment-mock` profile for local development

## External Integrations

WebClient:
- Currency Exchange API — integration with exchangeratesapi.io for real-time currency rates
- WebClient configuration: [CurrencyExchangeClientConfig](src/main/java/faang/school/paymentservice/config/currency/CurrencyExchangeClientConfig.java)

Currency services:
- [CurrencyService](src/main/java/faang/school/paymentservice/service/currency/CurrencyService.java) — manages currency exchange rates and conversions
- [CurrencyRateFetcher](src/main/java/faang/school/paymentservice/service/currency/CurrencyRateFetcher.java) — fetches currency rates from external API

## Architecture

Controllers:
- [PaymentController](src/main/java/faang/school/paymentservice/controller/PaymentController.java) — payment operation endpoints

Configuration:
- [RedisConfig](src/main/java/faang/school/paymentservice/config/redis/RedisConfig.java) — Redis caching configuration
- [CurrencyExchangeConfig](src/main/java/faang/school/paymentservice/config/currency/CurrencyExchangeConfig.java) — currency exchange settings

**Note:** Base code structure and architecture patterns are based on [FAANG School](https://github.com/faang-school) educational project.
