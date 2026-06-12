# idempotence-lib

Spring Boot стартер для HTTP-идемпотентности REST API.

Библиотека перехватывает повторные запросы по заголовку `Idempotency-Key` и возвращает
кэшированный ответ первого выполнения — без повторного вызова контроллера.

---

## Проблема

Клиент отправляет POST-запрос и не получает ответ (сетевой таймаут, retry-логика, двойной клик).
Операция на сервере уже выполнилась, но клиент не знает об этом и повторяет запрос.
Результат — дублирование заказов, платежей, транзакций.

**idempotence-lib** гарантирует, что операция выполнится ровно один раз.

---

## Быстрый старт

### 1. Добавить зависимость

```xml
<dependency>
    <groupId>com.idempotence</groupId>
    <artifactId>idempotence-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. Пометить контроллер аннотацией

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @PostMapping
    @Idempotent  // ← достаточно одной аннотации
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        // ваша бизнес-логика
    }
}
```

### 3. Клиент передаёт заголовок

```http
POST /orders
Idempotency-Key: unique-client-generated-uuid
Content-Type: application/json

{"amount": 1500, "currency": "RUB"}
```

Готово. Библиотека автоматически:
- При первом запросе — выполняет контроллер и сохраняет ответ в Redis
- При повторном запросе с тем же ключом — возвращает сохранённый ответ

---

## Как это работает

```
Клиент → [Idempotency-Key: abc123]
              ↓
    IdempotenceInterceptor
              ↓
    Ключ уже есть в Redis?
       ↙           ↘
     НЕТ            ДА
      ↓              ↓
  PROCEED         Fingerprint
  Контроллер      совпадает?
  выполняется      ↙     ↘
      ↓          ДА      НЕТ
  Ответ          ↓        ↓
  сохраняется  REPLAY  CONFLICT
  в Redis      Кэш     409
```

### Четыре решения

| Решение | Условие | HTTP результат |
|---|---|---|
| **PROCEED** | Ключ встречается впервые | Контроллер выполняется |
| **REPLAY** | Тот же ключ + то же тело | Кэшированный ответ, контроллер не вызван |
| **CONFLICT** | Тот же ключ, другое тело | 409 Conflict |
| **IN_PROGRESS** | Первый запрос ещё выполняется | 409 (настраивается) |

---

## Структура проекта

```
idempotence-parent/
├── idempotence-core/                       # Доменная логика (без Spring, без Redis)
│   ├── annotation/  @Idempotent
│   └── core/        IdempotenceService, IdempotenceStore, FingerprintService, ...
│
├── idempotence-redis/                      # Redis реализация хранилища
│   └── redis/       RedisIdempotenceStore (атомарный SET NX EX)
│
├── idempotence-spring-boot-autoconfigure/  # Spring Boot авто-конфигурация
│   ├── autoconfigure/  IdempotenceAutoConfiguration, ...
│   └── spring/web/     Interceptor, ResponseBodyAdvice, Filter, ...
│
└── idempotence-spring-boot-starter/        # Стартер — одна зависимость для подключения


```

---

## Конфигурация

Все свойства задаются через `application.yml` с префиксом `idempotence`:

```yaml
idempotence:
  enabled: true                    # Включить/выключить (default: true)
  header-name: Idempotency-Key     # Имя заголовка (default: Idempotency-Key)
  require-key: true                # 400 если заголовок отсутствует (default: true)
  ttl: 24h                         # Время жизни записи в Redis (default: 24h)
  key-prefix: "idempotence:"       # Префикс ключей в Redis (default: idempotence:)
  conflict-status: 409             # HTTP статус при конфликте fingerprint (default: 409)
  in-progress-status: 409          # HTTP статус при повторе in-progress (default: 409)
  max-body-size: 1048576           # Макс. размер тела для кэширования, байт (default: 1MB)
  cleanup-on-exception: true       # Удалять IN_PROGRESS при исключении (default: true)
  redis-failure-policy: FAIL_CLOSED  # FAIL_CLOSED (блокировать) | FAIL_OPEN (пропустить)

  stored-statuses: []              # Статусы для сохранения, [] = все (default: [])
  stored-response:
    excluded-headers:              # Заголовки, не сохраняемые в кэше
      - Authorization
      - Cookie
      - Set-Cookie
```

---

## Точки расширения

Все бины помечены `@ConditionalOnMissingBean` — любой из них можно заменить своей реализацией.

### Своя стратегия извлечения ключа

```java
@Bean
public IdempotencyKeyResolver myKeyResolver() {
    // Например, брать ключ из JWT токена
    return request -> extractFromJwt(request.getHeader("Authorization"));
}
```

### Своё хранилище (без Redis)

```java
@Bean
public IdempotenceStore myStore() {
    return new PostgresIdempotenceStore(dataSource);
}
```

### Свой алгоритм fingerprint

```java
@Bean
public FingerprintService myFingerprintService() {
    return source -> md5(source.httpMethod() + source.path() + source.body());
}
```

---

## Политика при недоступности Redis

```yaml
idempotence:
  redis-failure-policy: FAIL_CLOSED  # (default) — блокировать запрос, вернуть 500
  # redis-failure-policy: FAIL_OPEN  # — пропустить без защиты идемпотентности
```

- **FAIL_CLOSED** — безопасный режим. Если Redis недоступен, запрос блокируется.
- **FAIL_OPEN** — режим высокой доступности. Запрос проходит без идемпотентности.

---

## Требования

- Java 21+
- Spring Boot 3.x
- Redis (для production; `InMemoryIdempotenceStore` доступен для тестов)

---

## Сборка

```bash
# Собрать все модули
cd idempotence-parent
mvn install -DskipTests

# Запустить все тесты (требуется Redis на localhost:6379)
mvn test

# Запустить только unit-тесты (без Redis)
mvn test -Dtest="IdempotenceServiceTest,DefaultFingerprintServiceTest,\
IdempotenceAutoConfigurationTest,IdempotenceRedisAutoConfigurationTest,\
IdempotenceWebAutoConfigurationTest,IdempotencePropertiesBindingTest,\
RedisIdempotenceStoreFailurePolicyTest"
```

---

## Тестирование в своём проекте

Для unit-тестов используйте `InMemoryIdempotenceStore`:

```java
@Test
void myServiceShouldBeIdempotent() {
    IdempotenceStore store = new InMemoryIdempotenceStore();
    IdempotenceService service = new IdempotenceService(store, Clock.systemUTC(), Duration.ofMinutes(10));

    // Первый вызов — PROCEED
    assertEquals(DecisionType.PROCEED, service.beforeExecution("key-1", "fp-1").getType());

    // Сохраняем ответ
    service.afterExecution("key-1", "fp-1", new StoredResponse(200, Map.of(), "{\"id\":1}"));

    // Повторный вызов — REPLAY
    IdempotenceDecision decision = service.beforeExecution("key-1", "fp-1");
    assertEquals(DecisionType.REPLAY, decision.getType());
    assertEquals("{\"id\":1}", decision.getStoredResponse().getBody());
}
```
