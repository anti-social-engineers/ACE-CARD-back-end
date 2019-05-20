ACE-CARD-back-end

[![Build Status](https://travis-ci.com/anti-social-engineers/AceCardAPI.svg?branch=master)](https://travis-ci.com/anti-social-engineers/AceCardAPI)

This application was generated using http://start.vertx.io

== Building

To launch your tests:
```
./mvnw clean test
```

To package your application:
```
./mvnw clean package
```

To run your application:
```
./mvnw clean compile exec:java
```

== Help

* https://vertx.io/docs/[Vert.x Documentation]
* https://stackoverflow.com/questions/tagged/vert.x?sort=newest&pageSize=15[Vert.x Stack Overflow]
* https://groups.google.com/forum/?fromgroups#!forum/vertx[Vert.x User Group]
* https://gitter.im/eclipse-vertx/vertx-users[Vert.x Gitter]


## Launching from IDE
* First create a config.json, see src/main/conf/how-to-config.txt
* Create an application launcher
* main class: io.vertx.core.Launcher
* run acecardapi.MainVerticle -conf src/main/conf/config.json
