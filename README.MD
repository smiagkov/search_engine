# Проект search_engine.

#### Локальный поисковый движок по сайтам.
## Описание
Поисковый движок представляет из себя Spring-приложение
, работающее с локально установленной базой данных MySQL, имеющее простой
веб-интерфейс и API, через который им можно управлять и получать
результаты поисковой выдачи по запросу.

Принципы работы поискового движка
1. В конфигурационном файле перед запуском приложения задаются
   адреса сайтов, по которым движок должен осуществлять поиск.
2. Поисковый движок должен самостоятельно обходить все страницы
   заданных сайтов и индексировать их (создавать так называемый индекс)
   так, чтобы потом находить наиболее релевантные страницы по любому
   поисковому запросу.
3. Пользователь присылает запрос через API движка. Запрос — это набор
   слов, по которым нужно найти страницы сайта.
4. Запрос определённым образом трансформируется в список слов,
   переведённых в базовую форму. Например, для существительных —
   именительный падеж, единственное число.
5. В индексе ищутся страницы, на которых встречаются все эти слова.
6. Результаты поиска ранжируются, сортируются и отдаются пользователю.
## Настройка
Файл конфигурации (application.yaml):
```
server:
   port: 8080

spring:
   datasource:
      username: root
      password:
      url: jdbc:mysql://localhost:3306/search_engine?useSSL=false&requireSSL=false&allowPublicKeyRetrieval=true
   jpa:
      properties:
         hibernate:
            dialect: org.hibernate.dialect.MariaDBDialect

      show-sql: false
      hibernate:
         ddl-auto: update

parsing-settings:
   jsoup_referer: http://www.google.com
   jsoup_user-agent: Mozilla/5.0 (X11; Linux x86_64; rv:102.0) Gecko/20100101 Firefox/102.0
   redirect: true
   delay: 2000

indexing-settings:
   sites:
      - url: https://www.lenta.ru
        name: Лента.ру 
```
**server.port** - номер порта web-сервера \
**spring.datasource** - параметры подключения к серверу MySQL: логин/пароль, url \
**spring.jpa.properties.hibernate.dialect** - диалект MySQL \
**jpa.show-sql** - если указать true, в консоли будут отображаться запросы к БД /
**jpa.hibernate.ddl-auto** - действие с БД при запуске приложения:
- none - с БД не производятся никакие действия (рекомендутся при использовании)
- update - при необходимости меняется структура и настройки таблиц БД
- create - при запуске приложения заново создаются таблицы БД
- create-drop - при запуске приложения создаются новые таблицы, при остановке - таблицы удаляются 

**indexing-settings** - список сайтов, которые будут проиндексированы, и по которым в дальшейшем будет производиться поиск
## Запуск
Приложение можно запустить в IDE. Используется фреймворк MAVEN.

Также можно собрать приложение в .jar-файл и запускать: \
`java -jar <имя .jar-файла>`
## Использование
Запустив приложение, откройте в браузере ссылку: *localhost:<номер порта>*. \
Номер порта указан в конфигурационном файле: *server.port*. \

В приложении есть 3 вкладки: 
1. **DASHBOARD** - Отображается общая и детальная статистика 
проиндексированных сайтов (статус индексации, количество страниц и лемм, ошибки индексации)
2. **MANAGEMENT** - Здесь можно запустить индексацию/переиндексацию сайтов,
перечисленных в конфигурационном файле. Также можно проиндексировать/переиндексировать
одну страницу какого-либо сайта из перечисленных в конфигурационном файле. 
3. **SEARCH** - На этой вкладке можно произвести поиск по одному или нескольким словам, фразе.
В выпадающем списке можно выбрать один из сайтов, перечисленных в конфигурационном файле, по которому будет
производиться поиск. Либо все сайты.

