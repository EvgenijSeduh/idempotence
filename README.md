# Idempotence Spring Boot Starter

Spring Boot starter для обеспечения идемпотентности REST-операций.

Библиотека позволяет безопасно обрабатывать повторные HTTP-запросы с заголовком `Idempotency-Key`: первый запрос выполняется обычным образом, а повторный запрос с тем же ключом и тем же содержимым получает сохраненный HTTP-ответ без повторного вызова бизнес-логики.

Основной сценарий применения — защита операций создания заказов, платежей, заявок, транзакций и других действий, где повторная отправка запроса не должна приводить к дублированию результата.

---

## Возможности

* Подключение через один Spring Boot starter.
* Включение идемпотентности аннотацией `@Idempotent`.
* Получение ключа идемпотентности из HTTP-заголовка `Idempotency-Key`.
* Расчет fingerprint запроса по HTTP-методу, пути, query-параметрам и телу запроса.
* Сохранение состояния обработки в Redis.
* Повтор сохраненного HTTP-ответа при повторном запросе.
* Обнаружение конфликта при использовании одного ключа для разных запросов.
* Обработка состояния `IN_PROGRESS`, когда первый запрос еще выполняется.
* Настройка TTL, HTTP-статусов, сообщений об ошибках и сохраняемых ответов.
* Возможность заменить Redis на другое хранилище через собственную реализацию `IdempotenceStore`.
* Возможность заменить источник ключа, алгоритм fingerprint и другие точки расширения через Spring beans.

---

## Модули проекта

```text
idempotence-parent
├── idempotence-core
├── idempotence-redis
├── idempotence-spring-boot-autoconfigure
└── idempotence-spring-boot-starter
```

Краткое назначение модулей:

| Модуль                                  | Назначение                                                                 |
| --------------------------------------- | -------------------------------------------------------------------------- |
| `idempotence-core`                      | Базовые модели, сервис идемпотентности, интерфейсы хранилища и fingerprint |
| `idempotence-redis`                     | Redis-реализация хранилища идемпотентности                                 |
| `idempotence-spring-boot-autoconfigure` | Автоконфигурация Spring Boot, interceptor, filter, response advice         |
| `idempotence-spring-boot-starter`       | Starter для подключения библиотеки одной зависимостью                      |

---

## Требования

* Java 21 или выше.
* Spring Boot 3.x.
* Redis для production-сценариев.

Для тестов и локальных single-instance сценариев доступен `InMemoryIdempotenceStore`, но он не предназначен для распределенного production-окружения.

---

## Установка

Добавьте starter в проект:

```xml
<dependency>
    <groupId>com.idempotence</groupId>
    <artifactId>idempotence-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Если библиотека используется локально из исходников, сначала установите ее в локальный Maven-репозиторий:

```bash
mvn clean install
```

---

## Быстрый старт

### 1. Настройте Redis

Пример `application.yml`:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### 2. Включите настройки идемпотентности

```yaml
idempotence:
  enabled: true
  header-name: Idempotency-Key
  require-key: true
  ttl: 24h
```

### 3. Пометьте endpoint аннотацией `@Idempotent`

```java
import com.idempotence.idempotencelib.core.annotation.Idempotent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @PostMapping
    @Idempotent
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        OrderResponse response = orderService.create(request);
        return ResponseEntity.status(201).body(response);
    }
}
```

### 4. Передавайте ключ идемпотентности в запросе

```http
POST /orders
Content-Type: application/json
Idempotency-Key: 8a4c7a5b-1f6b-4e8b-9f0d-2d7c4a7c0001

{
  "amount": 1500,
  "currency": "RUB"
}
```

При первом запросе контроллер выполнится, а ответ будет сохранен. При повторном запросе с тем же ключом и тем же содержимым библиотека вернет сохраненный ответ без повторного вызова контроллера.

---

## Поведение библиотеки

| Ситуация                                                 | Результат                                              |
| -------------------------------------------------------- | ------------------------------------------------------ |
| Первый запрос с новым `Idempotency-Key`                  | Контроллер выполняется, создается запись `IN_PROGRESS` |
| Запрос успешно завершен                                  | HTTP-ответ сохраняется, запись переходит в `COMPLETED` |
| Повтор с тем же ключом и тем же fingerprint              | Возвращается сохраненный HTTP-ответ                    |
| Повтор с тем же ключом, но другим fingerprint            | Возвращается конфликт                                  |
| Повтор, пока первый запрос еще выполняется               | Возвращается статус `in-progress-status`               |
| Отсутствует ключ идемпотентности при `require-key: true` | Возвращается ошибка отсутствующего ключа               |
| Тело `@Idempotent`-запроса превышает `max-body-size`     | Возвращается `payload-too-large-status`                |

---

## Конфигурация

Все настройки задаются с префиксом `idempotence`.

```yaml
idempotence:
  enabled: true
  header-name: Idempotency-Key
  require-key: true
  ttl: 24h
  key-prefix: "idempotence:"

  conflict-status: 409
  in-progress-status: 409
  payload-too-large-status: 413

  missing-key-message: "Idempotency key is required"
  conflict-message: "Idempotency key is already used for another request"
  in-progress-message: "Request is still in progress"
  payload-too-large-message: "Request body is too large for idempotency processing"

  max-body-size: 1048576
  cleanup-on-exception: true
  redis-failure-policy: FAIL_CLOSED

  stored-statuses: []

  stored-response:
    excluded-headers:
      - Authorization
      - Cookie
      - Set-Cookie
```

### Основные настройки

| Свойство                           | Значение по умолчанию | Назначение                                                                    |
| ---------------------------------- | --------------------: | ----------------------------------------------------------------------------- |
| `idempotence.enabled`              |                `true` | Включает или отключает механизм идемпотентности                               |
| `idempotence.header-name`          |     `Idempotency-Key` | Имя HTTP-заголовка с ключом идемпотентности                                   |
| `idempotence.require-key`          |                `true` | Если `true`, запрос к `@Idempotent` endpoint без ключа завершится ошибкой     |
| `idempotence.ttl`                  |                 `24h` | Время жизни записи идемпотентности                                            |
| `idempotence.key-prefix`           |        `idempotence:` | Префикс ключей в Redis                                                        |
| `idempotence.max-body-size`        |             `1048576` | Максимальный размер тела запроса для обработки идемпотентности                |
| `idempotence.cleanup-on-exception` |                `true` | Удаляет `IN_PROGRESS` запись, если при выполнении запроса возникло исключение |
| `idempotence.redis-failure-policy` |         `FAIL_CLOSED` | Поведение при ошибке Redis                                                    |

### HTTP-статусы и сообщения

| Свойство                                |                                  Значение по умолчанию | Назначение                                          |
| --------------------------------------- | -----------------------------------------------------: | --------------------------------------------------- |
| `idempotence.conflict-status`           |                                                  `409` | Статус при конфликте fingerprint                    |
| `idempotence.in-progress-status`        |                                                  `409` | Статус при повторе запроса, который еще выполняется |
| `idempotence.payload-too-large-status`  |                                                  `413` | Статус при превышении `max-body-size`               |
| `idempotence.missing-key-message`       |                          `Idempotency key is required` | Сообщение при отсутствии ключа                      |
| `idempotence.conflict-message`          |  `Idempotency key is already used for another request` | Сообщение при конфликте                             |
| `idempotence.in-progress-message`       |                         `Request is still in progress` | Сообщение для `IN_PROGRESS`                         |
| `idempotence.payload-too-large-message` | `Request body is too large for idempotency processing` | Сообщение при превышении размера тела               |

### Настройка сохраняемых ответов

По умолчанию `stored-statuses: []` означает, что библиотека может сохранять ответы без ограничения по статусам.

Чтобы сохранять только успешные ответы, можно указать:

```yaml
idempotence:
  stored-statuses:
    - 200
    - 201
    - 202
```

Заголовки, которые не должны попадать в сохраненный ответ, настраиваются через `stored-response.excluded-headers`:

```yaml
idempotence:
  stored-response:
    excluded-headers:
      - Authorization
      - Cookie
      - Set-Cookie
```

---

## Настройка Redis

Redis используется как основное production-хранилище записей идемпотентности.

Пример настройки:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: password
```

Настройки подключения к Redis задаются стандартными свойствами Spring Boot `spring.data.redis.*`.

В Redis сохраняются записи с TTL. Для первого запроса используется атомарная попытка создания записи, поэтому конкурентные запросы с одним ключом не должны одновременно пройти в бизнес-логику.

---

## Политика при недоступности Redis

Поведение при ошибке Redis задается свойством:

```yaml
idempotence:
  redis-failure-policy: FAIL_CLOSED
```

Доступные значения:

| Значение      | Поведение                                                                            |
| ------------- | ------------------------------------------------------------------------------------ |
| `FAIL_CLOSED` | Безопасный режим. При ошибке Redis запрос блокируется ошибкой                        |
| `FAIL_OPEN`   | Режим доступности. При ошибке Redis запрос пропускается без гарантии идемпотентности |

Для операций, где повторное выполнение критично, рекомендуется использовать `FAIL_CLOSED`.

---

## Точки расширения

Библиотека рассчитана на расширение через Spring beans. Если приложение объявляет собственный bean нужного типа, автоконфигурация использует его вместо стандартной реализации.

### Замена хранилища

Чтобы использовать PostgreSQL, MongoDB, Cassandra или другое хранилище вместо Redis, реализуйте интерфейс `IdempotenceStore`.

```java
import com.idempotence.idempotencelib.core.model.IdempotenceRecord;
import com.idempotence.idempotencelib.core.store.IdempotenceStore;

public class PostgresIdempotenceStore implements IdempotenceStore {

    @Override
    public IdempotenceRecord find(String key) {
        // Найти запись по ключу.
        return null;
    }

    @Override
    public boolean tryAcquire(IdempotenceRecord record) {
        // Атомарно создать запись IN_PROGRESS, если ключ еще не занят.
        // Для SQL-хранилища обычно используется unique constraint по key.
        return false;
    }

    @Override
    public void save(IdempotenceRecord record) {
        // Сохранить COMPLETED запись с HTTP-ответом.
    }

    @Override
    public void delete(String key) {
        // Удалить запись, например при cleanup-on-exception.
    }
}
```

Зарегистрируйте реализацию как Spring bean:

```java
import com.idempotence.idempotencelib.core.store.IdempotenceStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdempotenceStoreConfiguration {

    @Bean
    public IdempotenceStore idempotenceStore() {
        return new PostgresIdempotenceStore();
    }
}
```

Главное требование к `tryAcquire` — атомарность. Метод должен гарантировать, что только один конкурентный запрос с новым ключом сможет создать запись `IN_PROGRESS`.

Примеры реализации:

| Хранилище  | Как обеспечить атомарность                 |
| ---------- | ------------------------------------------ |
| Redis      | `SET NX EX`                                |
| PostgreSQL | `INSERT` с `UNIQUE` constraint по ключу    |
| MongoDB    | `insert` с unique index по ключу           |
| Cassandra  | Lightweight transaction / условная вставка |

### Свой источник ключа идемпотентности

По умолчанию ключ берется из заголовка `Idempotency-Key`. Чтобы брать ключ из другого места, объявите bean `IdempotencyKeyResolver`.

Пример: ключ из другого заголовка.

```java
import com.idempotence.idempotencelib.spring.web.IdempotencyKeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdempotencyKeyConfiguration {

    @Bean
    public IdempotencyKeyResolver idempotencyKeyResolver() {
        return request -> request.getHeader("X-Request-Id");
    }
}
```

Пример: ключ на основе пользователя и клиентского ключа.

```java
@Bean
public IdempotencyKeyResolver idempotencyKeyResolver() {
    return request -> {
        String userId = request.getUserPrincipal().getName();
        String clientKey = request.getHeader("Idempotency-Key");
        return userId + ":" + clientKey;
    };
}
```

Такой подход полезен, если одинаковые `Idempotency-Key` могут приходить от разных пользователей и должны быть изолированы друг от друга.

### Свой алгоритм fingerprint

Fingerprint используется для проверки, что один и тот же ключ не применяется к разным запросам.

Чтобы заменить алгоритм расчета fingerprint, объявите bean `FingerprintService`.

```java
import com.idempotence.idempotencelib.core.fingerprint.FingerprintService;
import com.idempotence.idempotencelib.core.fingerprint.FingerprintSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FingerprintConfiguration {

    @Bean
    public FingerprintService fingerprintService() {
        return source -> {
            String value = source.httpMethod()
                    + "|" + source.path()
                    + "|" + source.queryParams()
                    + "|" + source.body();

            return customHash(value);
        };
    }

    private String customHash(String value) {
        // Реализация пользовательского алгоритма.
        return value;
    }
}
```

Стандартный сценарий — учитывать метод, путь, query-параметры и тело запроса. Если в проекте нужны другие правила, например игнорирование отдельных query-параметров, это можно вынести в собственную реализацию.

### Свой состав HTTP fingerprint

Если нужно изменить не только алгоритм хеширования, но и набор данных, из которых строится fingerprint, можно объявить собственный bean `HttpRequestFingerprintBuilder`.

Пример сценариев:

| Сценарий                               | Что можно изменить                                            |
| -------------------------------------- | ------------------------------------------------------------- |
| Игнорировать служебные query-параметры | Исключить `utm_*`, `timestamp`, `debug`                       |
| Учитывать пользователя                 | Добавить user id в fingerprint                                |
| Учитывать отдельные заголовки          | Добавить `Content-Type` или tenant header                     |
| Нормализовать JSON                     | Привести тело к каноническому виду перед расчетом fingerprint |

Пример регистрации:

```java
import com.idempotence.idempotencelib.core.fingerprint.FingerprintService;
import com.idempotence.idempotencelib.spring.web.HttpRequestFingerprintBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestFingerprintConfiguration {

    @Bean
    public HttpRequestFingerprintBuilder httpRequestFingerprintBuilder(
            FingerprintService fingerprintService
    ) {
        return new CustomHttpRequestFingerprintBuilder(fingerprintService);
    }
}
```

### Настройка сериализации ответа

Сохраненный HTTP-ответ сериализуется с использованием `ObjectMapper` из Spring context. Если в приложении уже настроен собственный `ObjectMapper`, библиотека будет использовать общую конфигурацию сериализации приложения.

Пример:

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
```

---

## Пример полной настройки

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379

idempotence:
  enabled: true
  header-name: Idempotency-Key
  require-key: true
  ttl: 24h
  key-prefix: "idempotence:"

  conflict-status: 409
  in-progress-status: 409
  payload-too-large-status: 413

  missing-key-message: "Idempotency key is required"
  conflict-message: "Idempotency key is already used for another request"
  in-progress-message: "Request is still in progress"
  payload-too-large-message: "Request body is too large for idempotency processing"

  max-body-size: 1048576
  cleanup-on-exception: true
  redis-failure-policy: FAIL_CLOSED

  stored-statuses:
    - 200
    - 201

  stored-response:
    excluded-headers:
      - Authorization
      - Cookie
      - Set-Cookie
```

---

## Тестирование

Для запуска всех тестов:

```bash
mvn clean test
```

Redis-интеграционные тесты используют Testcontainers. Для их запуска должен быть доступен Docker или другой совместимый container runtime.

Локально поднимать Redis на `localhost:6379` для тестов не требуется.

---

## Ограничения

* Redis является основным production-хранилищем по умолчанию.
* `InMemoryIdempotenceStore` предназначен только для тестов, локальной разработки и single-instance приложений.
* При использовании собственного `IdempotenceStore` метод `tryAcquire` должен быть атомарным.
* Для `@Idempotent` endpoint тело запроса ограничивается настройкой `max-body-size`.
* Если используется `FAIL_OPEN`, при недоступности Redis запрос может пройти без защиты идемпотентности.
* Повтор сохраненного ответа работает только для ответов, которые прошли через web-слой библиотеки и были сохранены после выполнения запроса.

---

## Когда использовать

Библиотека подходит для endpoint-ов, где повторная отправка одного и того же запроса не должна повторно выполнять бизнес-операцию:

* Создание заказа.
* Создание платежа.
* Создание заявки.
* Запуск операции, которую нельзя безопасно выполнить дважды.
* Обработка retry-запросов от клиента или внешней системы.

Не рекомендуется включать идемпотентность на все endpoint-ы подряд. Используйте `@Idempotent` только на операциях, где повторный вызов действительно опасен.
