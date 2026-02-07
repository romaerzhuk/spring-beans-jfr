# Зачем Java Flight Recorder для Spring Boot

Java Flight Recorder (JFR) — это механизм легковесного прфилирования Java-приложения.
Появился в коммерческой версии JDK 1.8, а в OpenJDK — с Java11.
Он позволяет записывать и в последствии анализировать огромное количество метрик и событий, происходящих внутри JVM,
с последующим анализом через [Java Mission Control (JMC)](https://github.com/openjdk/jmc),
что значительно облегчает анализ проблем.
Более того, при определённых настройках его накладные расходы настолько малы, что многие, включая Oracle,
рекомендуют держать его постоянно включённым везде, в том числе прод, чтобы в случае возникновения проблем сразу иметь
полную картину происходившего с приложением.

В анонсе [Что нового в Java 25. Часть 3](https://axiomjdk.ru/announcements/2025/09/15/cho-novogo-v-java25-3/)
сказано, что JDK 25 JFR будет собирать более точную информацию о профилировании CPU-времени на Linux, см.
профилирование:

* [JFR Cooperative Sampling (JEP 518)](https://openjdk.org/jeps/518),
* [JFR CPU-Time Profiling (JEP 509)](https://openjdk.org/jeps/509)
* [JEP 520: JFR Method Timing & Tracing](https://openjdk.org/jeps/520)

Возможно, тем, кто уже перешёл на Java25, это решение не будет полезно.

На Java 11 и Java17 по штатной информации JFR помогает профилировать и находить узкие места в реализации.
Но это не тривиально.
Начиная с Java17, Spring Boot 3 может помочь это решение.

`Spring-beans-jfr` подключает запись информации о времени выполнения бизнес-методов в журнал JFR и в лог Slf4j.
Есть возможность доработать и использовать это на Jav11, Spring Boot 2. Не понадобилось.

Реализованы компоненты SpringBoot-а, см. `JfrConfiguration`:

* `JfrLoggingServiceImpl`, основной компонент, собирает статистику выполнения методов,
  пишет в JFR и лог Slf4j.
* `JfrJobFactory`, опционально собирает статистику выполнения задач [Quartz Scheduler](https://www.quartz-scheduler.org/)
* `JfrFeignRequestInterceptor`, опционально собирает статистику выполнения запросов
  [Spring Cloud OpenFeign](https://spring.io/projects/spring-cloud-openfeign).

### Собственный регистратор событий JFR

* [JfrLoggingServiceImpl.java](../src/main/java/jfr/logging/JfrLoggingServiceImpl.java), собирает статистику вызовов методов компонентов Spring.

Группирует «чистую» длительность выполнения методов, т.е. исключает длительность вложенных вызовов.
Это помогает быстрее локализовать медленные участки кода.
Параметр `jfr.thresholdNanos: 10000000` устанавливает пороговую длительность, для записи в JFR, нс.

Проще всего продемонстрировать возможности с логом.
Обычно для `JfrLoggingServiceImpl` требуется аспект, который в каждом проекте следует настраивать под свои нужды.

Функциональность показана на примере SpringBootTest-а и тестового LoggingAspect-а.

При запуске теста в логе будет примерно следующее:

```
DEBUG - [main] jfr.config.HelloWorldService             : HelloWorldService public java.lang.String jfr.config.HelloWorldService.hello(java.lang.String,int) throws java.lang.InterruptedException [мир, 70] statistics: 00:00:00.903
	class=MultiplyService, method=public java.math.BigDecimal jfr.config.MultiplyService.multiply(java.math.BigDecimal,int) throws java.lang.InterruptedException, count=69, sum=00:00:00.733, min=00:00:00.010, avg=00:00:00.010, max=00:00:00.011
	class=FactorialService, method=public java.math.BigDecimal jfr.config.FactorialService.factorial(int) throws java.lang.InterruptedException, count=1, sum=00:00:00.133, min=00:00:00.133, avg=00:00:00.133, max=00:00:00.133
	class=HelloWorldService, method=public java.lang.String jfr.config.HelloWorldService.hello(java.lang.String,int) throws java.lang.InterruptedException, count=1, sum=00:00:00.036, min=00:00:00.036, avg=00:00:00.036, max=00:00:00.036
```

Из лога видно самое узкое мето в коде. Метод MultiplyService.multiply:

* count=69, количество вызовов при обращении к HelloWorldService;
* sum=00:00:00.733, общее время выполнения;
* min=00:00:00.010, avg=00:00:00.010, max=00:00:00.011, минимальное, среднее и максимальное время выполнения.

В продуктиве держать постоянно debug и собарать всё время такую статистику не целесообразно и даже вредно: слишком высокй оверхед I/O.

Чтобы выключить сбор и вывод в лог статистики, просто повысте уровень логгирования до уровня INFO, или выше: `jfr.logging: INFO`.

Чтобы включить запись этой статистики в JFR, достаточно влкючить JFR, об этом см. ниже.

### Дополнительные полезные модули

* [flight-recorder-starter](https://github.com/mirkosertic/flight-recorder-starter),
  позволяет включать, выключать, настраивать JFR без перезагрузки через http-ресурс http://my-host/actuator/flight-recorder;
* [hibernate-jfr](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#appendix-monitoring-with-JFR),
  регистрирует события Hibernate, выполнение SQL-запросов;
* [jfr-servlet](https://github.com/marschall/jfr-servlet), регистрирует события внешних HTTP-запросов.
* [micrometer-jfr](https://github.com/marschall/micrometer-jfr), регистрирует метрики.

Именно для профилирования очень полезными оказались hibernate-jfr и jfr-servlet.

## Полезные ресурсы

*
* [Управление Java Flight Recorder](https://habr.com/ru/companies/krista/articles/532632)
* [Собственные метрики JFR и их анализ](https://habr.com/ru/companies/krista/articles/552538)
* [Java Mission Control (JMC)](https://github.com/openjdk/jmc)
* [Eclipse Adoptium JMC](https://adoptium.net/jmc), JMC 9.0.0 для Java17;
* [Обзор инструмента Java Filght Recorder](https://m.vk.com/video-145052891_456246667)
* https://youtu.be/Gx_JGVborJ0?si=cgbWMzzX2zkGps9U;
* Сравнение [OpenTelemetry (OTel) vs. Java Flight Recorder (JFR)](https://digma.ai/opentelemetry-otel-vs-java-flight-recorder-jfr);
* [flight-recorder-starter](https://github.com/mirkosertic/flight-recorder-starter),
  позволяет включать, выключать, настраивать JFR без перезагрузки через http-ресурс http://my-host/actuator/flight-recorder;
* [Удалённое включение JFR по JMX](https://foojay.io/today/remotely-recording-a-jfr-log-from-a-container-without-using-the-command-line),
  без использования командной строки.

## Как включить JFR

Чтобы включить JFR, достаточно добавить в JAVA_OPTIONS настройку, например:

```
-XX:StartFlightRecording=settings=profile,maxage=24h -XX:FlightRecorderOptions=stackdepth=512
```

где:

* `-XX:StartFlightRecording` включает JFR;
* `settings=profile`, шаблон настройки. В составе OpenJDK идут пара шаблонов: default и profile.
  По утверждению Oracle, `default` увеличивает нагрузку только на 1%, а `profile` — около 2%, но при этом сохраняет значительно больше показателей.
* `maxage=24h`, длительность хранения данных журнала, в часах;
* `-XX:FlightRecorderOptions`, содержит настройки JFR;
* `stackdepth=512`, глубина стека. По умолчанию 64, обычно мало. Максимальное значение 2048.

По умолчанию записи журналов хранятся во временном системном каталоге, в K8s это `/tmp`.
Посмотреть содержимое каталога и загрузить файлы на локальный компьютер можно с помощью Web-консоли _kubernetes_
либо `kubectl`.

Просмотр каталога в терминале Web-консоли Pod-ы:

```shell
$ ls /tmp/
2025_02_07_15_01_01_1  hsperfdata_1000210000  hsperfdata_root  tomcat-docbase.8080.2680875980470110199  tomcat.8080.3850254012180578796
```

То же в терминале локальной машины с помощью `kubectl`:

```shell
$ kubectl exec имя-поды -- ls /tmp
2025_02_07_15_01_01_1  hsperfdata_1000210000  hsperfdata_root  tomcat-docbase.8080.2680875980470110199  tomcat.8080.3850254012180578796
```

Просмотр каталога с файлами JFR в терминале Web-консоли Pod-ы:

```shell
$ ls -al /tmp/2025_02_07_15_01_01_1
2025_02_07_15_01_01_1  hsperfdata_1000210000  hsperfdata_root  tomcat-docbase.8080.2680875980470110199  tomcat.8080.3850254012180578796
```

Просмотр каталога с файлами JFR с помощью `kubectl`:

```shell
$ kubectl exec имя-поды -- ls -al /tmp/2025_02_07_15_01_01_1
otal 42500
drwxr-xr-jfr 2 1000210000 root      130 Feb  7 12:04 .
drwxrwxrwt 1 root       root      151 Feb  7 12:02 ..
-rw-r--r-- 1 1000210000 root 14084094 Feb  7 12:02 2025_02_07_15_01_01.jfr
-rw-r--r-- 1 1000210000 root 13470449 Feb  7 12:03 2025_02_07_15_02_50.jfr
-rw-r--r-- 1 1000210000 root 14057644 Feb  7 12:04 2025_02_07_15_03_41.jfr
-rw-r--r-- 1 1000210000 root  1193854 Feb  7 12:04 2025_02_07_15_04_27.jfr
```

Загрузить на локальную машину файл в текущий каталог с помощью `kubectl`:

```shell
$ kubectl cp имя-поды:tmp/2025_02_07_15_01_01_1/2025_02_07_15_01_01.jfr 2025_02_07_15_01_01.jfr
```

Загрузить на локальную машину весь каталог:

```shell
$ kubectl cp имя-поды:tmp/2025_02_07_15_01_01_1 2025_02_07_15_01_01_1
```

Имя каталога и файлов содержи локальную дату и время записи журнала.
Может быть полезно загрузить выборочно файлы журнала за нужный интервал времени, а не весь каталог целиком.

### Flight Recorder Starter

Это модуль Spring Boot 2 или 3, предоставляет стартер и создаёт HTTP-endpoint для Java Flight Recorder.

Обычно JDK Flight Recorder доступен локально или через удаленный JMX.
В облаке JMX может быть недоступен. Вот тут-то и помогает этот стартер.

Стартер добавляет новый endpoint Spring Boot Actuator для удалённого управления Java Flight Recorder.
Этот RESTful endpoint позволяет запускать, останавливать JFR и загружать файлы `.jfr` для анализа.

## Конфигурирование в application.yml

Пример конфигурации:

```yml
flightrecorder:
  enabled: true  # Активирует стартер JFR
  recording-cleanup-type: TTL # тип очистки журналов JFR, по времени жизни
  old-recordings-TTL: 24 # Длительность хранения старых записей
  old-recordings-TTL-time-unit: Hours  # Единицы длительности хранения, в часах, см. ChronoUnit
  recording-cleanup-interval: 900000 # Интервал удаления старых записей, раз в 15 минут
  jfr-custom-config: profile # Имя конфигурации JFR
``` 

## Запуск JFR

Следующая команда `cURL` выполненная в терминале Pod-ы K8s запускает новый Flight Recording и возвращает ID созданного журнала:

```shell
curl  -i -X POST -H "Content-Type: application/json" \
  -d '{"duration":"3","timeUnit":"DAYS","maxAgeDuration":"24","maxAgeUnit":"HOURS","maxSize":"20000000"}' \
  http://localhost:8080/actuator/flightrecorder

HTTP/1.1 201 
Location: http://localhost:8080/actuator/flightrecorder/1
Content-Length: 0
Date: Fri, 05 Feb 2021 12:37:07 GMT
```

Запись JFR запускается на указанный период, в данном случае на 3 дня, хранит на диске окно данных длительностью 24 часа, затем останавливается.

Каждая сессия JFR получает собственный уникальный ID. Endpoint возвращает этот ID в текстовом виде, в этом случае ID `1`.
Этот идентификатор используется для загрузки сохранённых данных.

Запускать команду можно и удалённо через браузер, с помощью RESTful плагина.

### Продвинутые команды

Список доступных параметров:

| Наименование   | Тип значения                | Обязательный | Описание                                                                           |
|----------------|-----------------------------|:------------:|------------------------------------------------------------------------------------|
| description    | String                      |     Нет      | Описание журнала JFR                                                               |
| duration       | Number                      |      Да      | Длительность записи                                                                |
| timeUnit       | Serialized ChronoUnit value |      Да      | Единица времени длительности                                                       |
| maxAgeDuration | Number                      |     Нет      | Максимальный срок хранения записей                                                 |
| maxAgeUnit     | Serialized ChronoUnit value |     Нет      | Единица времени срока хранения                                                     |
| delayDuration  | Number                      |     Нет      | Откладывает запись старт записи на время                                           |
| delayUnit      | Serialized ChronoUnit value |     Нет      | Единица времени откладывания старта                                                |
| maxSize        | Number                      |     Нет      | Максимальный размер, в байтах                                                      |
| customSettings | JSON Object with N fields   |     Нет      | Объект JSON с собственными свойствами, которые будут перекрывать базовые настройки |

См. [JFR JavaDoc](https://docs.oracle.com/en/java/javase/11/docs/api/jdk.jfr/jdk/jfr/Recording.html).

Пример JSON-параметров:

```json
{
  "description": "MyFirstRecording",
  "duration": "60",
  "timeUnit": "SECONDS",
  "maxAgeDuration": "10",
  "maxAgeUnit": "SECONDS",
  "delayDuration": "5",
  "delayUnit": "SECONDS",
  "maxSize": "100000",
  "customSettings": {
    "myCustomProperty1": "myCustomValue1",
    "myCustomProperty2": "myCustomValue2"
  }
}
```

### Просмотр списка созданных журналов JFR

Список всех созданных журналов JFR можно посмотреть в виде JSON:

```
https://имя-хоста/actuator/flightrecorder
```

Следующая команда `cURL` делает то же:

```shell
curl http://localhost:8080/actuator/flightrecorder
```

### Загрузка результатов

Ниже пример команды `cURL` останавливает JFR с ID `1` загружает файл `.jfr`:

```shell
curl --output recording.jfr http://localhost:8080/actuator/flightrecorder/1
```

Вручную удобнее выполнить загрузку прямо в браузере.
Загруженный файл `.jfr` может быть открыт в JDK Mission Control (JMC) для анализа.

Загрузка выполняется целиком.
Если журнал слишком большой, могут быть трудности с его пересылкой и открытием в JMC.
В таком случае удобнее загрузить отдельные файлы с помощью kubectl, см. выше.

### Интерактивный Flamegraph

Этот стартер может генерировать интерактивный Flamegraph из записей JFR.
Удобен для быстрого обзора через браузер.
Для журнала с ID `1` откройте следующий URL:

```
https://имя-хоста/actuator/flightrecorder/ui/1/flamegraph.html
```

и увидите:

![Flamegraph](flamegraph.png)

Стартер автоматически пытается визуализировать только классы выполняемого Spring Boot-приложения, согласно аннотации `@SpringBootApplication`.

Вы можете получить и полный Flamegraph:

```
http://имя-хоста:8080/actuator/flightrecorder/ui/1/rawflamegraph.html
```

### Остановка Flight Recording

Запись JFR останавливается сразу после загрузки файла, но можно остановить и вручную.
Следующая команда `cURL` останавливает Flight Recording с ID `1`.

```shell
curl -i -X PUT http://localhost:8080/actuator/flightrecorder/1

HTTP/1.1 200
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 05 Feb 2021 12:39:43 GMT

{"id":1,"startedAt":"2021-02-05 13:37:08","status":"CLOSED","finishedAt":"2021-02-05 13:39:43","description":null}
```

### Удаление журнала Flight Recording

Следующая команда `cURL` останавливает и удаляет файлы Flight Recording с ID `1`.

```shell
curl -i -X DELETE http://localhost:8080/actuator/flightrecorder/1

HTTP/1.1 204 
Date: Fri, 05 Feb 2021 12:40:13 GMT
```

Это делать необязательно. Файлы и память будут очищены фоновым процессом через некоторое время.

### Процесс автоматического удаления

**Важно**: Для автоматического удаления убедитесь, что в приложении фоновые задачи включены аннотацией `@EnableScheduling`.

Процесс периодически удаляет записанные файлы журнала JFR, статус которых `STOPPED` или `CLOSED`.
Интервал очистки регулируется свойством:

```properties
flightrecorder.recording-cleanup-interval=5000
```

По умолчанию интервал 5000мс. Учтите, что журналы удаляются физически, восстановить не получится.

Стратегия удаления может базироваться на времени жизни, TTL, или на основе количества, COUNT.

По умолчанию очищается по времени, `TTL` и может быть изменено настройкой:

```properties
flightrecorder.recording-cleanup-type=COUNT
```

#### Удаление по времени, TTL

Если стратегия удаления по времени, `TTL` (time to live), время начала записи является точкой отсчёта для времени удаления.
Срок хранения может быть настроен как показано ниже, по умолчанию 1 час:

```properties
flightrecorder.old-recordings-TTL=1
flightrecorder.old-recordings-TTL-time-unit=Hours # См. доступные значения java.time.temporal.ChronoUnit
```

Файл может быть удалён в статусах `STOPPED` или `CLOSED`, когда время начала записи меньше, чем _now_ минус _threshold_.

#### Удаление по количеству, COUNT

Если стратегия очистки `COUNT`, самые старые записи будут удалены, когда
общее количество существующих записей превысит настроенный порог записей для сохранения.
Порог можно настроить с помощью настройки, как показано ниже, по умолчанию 10 записей:

```properties
flightrecorder.old-recordings-max=10
```

Файлы в статусах `STOPPED` или `CLOSED` будут удалены в порядке очереди, FIFO.

### Триггеры Flight Recording на основе метрик

Стартер допускает автоматический запуск JFR на основе метрик Micrometer.
С помощью настроек приложения можно сконфигурировать триггеры на выражениях SpEL (Spring Expression Language),
который будет запускаться на регулярной основе.
Когда посчитанное выражение вернёт `true`, запустится JFR с заданной длительностью и конфигурацией.
Наиболее распространенная настройка запускает профилирование JFR, как только загрузка ЦП превысит заданное значение.

По умолчанию эта возможность включена.
В случае если требуется выключить, достаточно установить настройку в `false`:

```properties
flightrecorder.trigger-enabled=false
```

Триггеры проверяются каждые 10 секунд. Конфигурация по умолчанию может быть изменена настройкой:

```properties
flightrecorder.trigger-check-interval=10000
```

**Важно**: Для срабатывания триггеров убедитесь, что в приложении фоновые задачи включены аннотацией `@EnableScheduling`.

Пример простой Yaml-конфигурации:

```yml
flightrecorder:
  enabled: true  Активирует стартер JFR
  recording-cleanup-interval: 5000 # интервал автоматической  5 seconds
  trigger-check-interval: 10000 # Вычисляет выражение триггера каждые 10 секунд
  trigger:
    - expression: meter('jvm.memory.used').tag('area','nonheap').tag('id','Metaspace').measurement('value') > 100
      startRecordingCommand:
        duration: 60
        timeUnit: SECONDS
``` 

### Продвинутая конфигурация

#### Место хранения журнала

По умолчанию записи журналов хранятся во временном системном каталоге, в K8s это `/tmp`.
Базовый каталог может быть изменён настройкой:

```yml
flightrecorder:
  jfr-base-path: /my-path
```

### Собственная конфигурация шаблона журнала

По умолчанию используется конфигурация "_<<JAVA_HOME>>/lib/jfr/profile.jfc_".
Собственная конфигурация может быть изменена свойством:

```yml
flightrecorder:
  jfr-custom-config: mycustomjfc
```

**Замечание**: Требуется только одно имя файла без расширения, не полный путь.