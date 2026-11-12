# SCA con OSV · 92 dependencias resueltas (compile, runtime y test)

| Dependencia | Alcance | ID | CVE | Severidad | Corregida en | Resumen |
|---|---|---|---|---|---|---|
| com.fasterxml.jackson.core:jackson-core:2.21.4 | compile | GHSA-7hhh-6rmp-j9qf | CVE-2026-89425 | HIGH | 2.18.11, 2.21.7, 2.22.3 | jackson-core: UTF8DataInputJsonParser._reportInvalidToken() missing maxErrorTokenLength li |
| com.fasterxml.jackson.core:jackson-core:2.21.4 | compile | GHSA-p6pp-m3f8-5c89 | CVE-2026-89407 | HIGH | 2.18.11, 2.21.7, 2.22.3 |  jackson-core: ReDoS: quadratic backtracking in NumberInput.PATTERN_FLOAT via looksLikeVal |
| com.fasterxml.jackson.core:jackson-databind:2.21.4 | compile | GHSA-5gvw-p9qm-jgwh | CVE-2026-59889 | MODERATE | 2.18.9, 2.21.5, 2.22.1 | jackson-databind: @JsonView bypassed for @JsonUnwrapped container properties on deserializ |
| com.fasterxml.jackson.core:jackson-databind:2.21.4 | compile | GHSA-5jmj-h7xm-6q6v | CVE-2026-54515 | MODERATE | 2.18.9, 2.21.5, 2.22.1 | jackson-databind has case-insensitive deserialization bypasses per-property @JsonIgnorePro |
| com.fasterxml.jackson.core:jackson-databind:2.21.4 | compile | GHSA-cxp5-3px4-pw24 | CVE-2026-91777 | HIGH | 2.18.11, 2.21.7, 2.22.3 | jackson-databind quadratic forward-reference completion  |
| com.fasterxml.jackson.core:jackson-databind:2.21.4 | compile | GHSA-gx83-3vf8-gh7j | CVE-2026-83557 | MODERATE | 2.18.10, 2.21.6, 2.22.2 | jackson-databind: Comparable missing from DefaultBaseTypeLimitingValidator's unsafe base t |
| com.fasterxml.jackson.core:jackson-databind:2.21.4 | compile | GHSA-mhm7-754m-9p8w |  | MODERATE | 2.18.9, 2.21.5 | jackson-databind: `@JsonView` bypass for creator properties with `@JsonTypeInfo(include=As |
| com.fasterxml.jackson.core:jackson-databind:2.21.4 | compile | GHSA-q4xh-88c3-wmh7 | CVE-2026-68497 | HIGH | 2.18.10, 2.21.6, 2.22.2 | jackson-databind: Duration XMLGregorianCalendar Unbounded Number Parse DoS |
| com.fasterxml.jackson.core:jackson-databind:2.21.4 | compile | GHSA-vvgp-rfg2-7rr6 | CVE-2026-77310 | MODERATE | 2.18.9, 2.21.5, 2.22.1 | jackson-databind: Incomplete fix for CVE-2026-54514: eager DNS resolution (SSRF) still pre |
| com.fasterxml.jackson.core:jackson-databind:2.21.4 | compile | GHSA-wjgm-6hv5-3cvf | CVE-2026-19032 | MODERATE | 2.18.10, 2.21.6, 2.22.2 | jackson-databind: Path Deserialization Missing Scheme Allowlist for FileSystemProvider Res |
| com.fasterxml.jackson.core:jackson-databind:2.21.4 | compile | GHSA-wv8q-qhhj-9h54 | CVE-2026-91776 | HIGH | 2.18.11, 2.21.7, 2.22.3 | jackson-databind retains every unknown raw type ID  |
| com.google.guava:guava:33.4.8-jre | compile | GHSA-xxph-c9ww-hj94 | CVE-2026-102554 | HIGH | 33.7.2 | Google Guava: CompactHashMap/CompactHashSet/MapMakerInternalMap deserialization triggers u |
| org.apache.logging.log4j:log4j-api:2.24.3 | compile | GHSA-qv9r-c865-cp47 | CVE-2026-49844 | MODERATE | 2.25.5, 2.26.1 | Apache Log4j API: Improper encoding of non-finite floating-point values during MapMessage  |
| org.apache.tomcat.embed:tomcat-embed-core:10.1.55 | compile | GHSA-5v3h-fjv2-54fg | CVE-2026-55956 | MODERATE | 10.1.56, 11.0.23, 9.0.119 | Apache Tomcat has an Improper Authorization vulnerability |
| org.apache.tomcat.embed:tomcat-embed-core:10.1.55 | compile | GHSA-9xv2-5v5q-p794 | CVE-2026-65905 | CRITICAL | 10.1.58, 11.0.25, 9.0.121 | Apache Tomcat's DIGEST authenticator has an Authentication Bypass by Capture-replay vulner |
| org.apache.tomcat.embed:tomcat-embed-core:10.1.55 | compile | GHSA-f525-44xv-f2qj | CVE-2026-65637 | CRITICAL | 10.1.59, 11.0.25, 9.0.121 | Apache Tomcat - HTTP/2 no-authority bypass of strict SNI validation |
| org.apache.tomcat.embed:tomcat-embed-core:10.1.55 | compile | GHSA-gcx9-497g-6cp6 | CVE-2026-65182 | CRITICAL | 10.1.58, 11.0.25, 9.0.121 | Apache Tomcat has an Improper Access Control, Incorrect Authorization vulnerability |
| org.apache.tomcat.embed:tomcat-embed-core:10.1.55 | compile | GHSA-h3x4-894j-xpx5 | CVE-2026-68525 | CRITICAL | 10.1.58, 11.0.25, 9.0.121 | Apache Tomcat's FORM authentication process has an Incorrect Authorization vulnerability |
| org.apache.tomcat.embed:tomcat-embed-core:10.1.55 | compile | GHSA-h792-v28v-ppgr | CVE-2026-53404 | HIGH | 10.1.56, 11.0.23, 9.0.119 | Apache Tomcat's rewrite valve has an Always-Incorrect Control Flow Implementation vulnerab |
| org.apache.tomcat.embed:tomcat-embed-core:10.1.55 | compile | GHSA-mqg3-r7h5-24x4 | CVE-2026-55276 | CRITICAL | 10.1.56, 11.0.23, 9.0.119 | Apache Tomcat has an Always-Incorrect Control Flow Implementation vulnerability |
| org.springframework:spring-webmvc:6.2.19 | compile | GHSA-j9f9-w8pj-32f8 | CVE-2026-47890 | CRITICAL | 7.0.9 | Spring Framework Server Sent Event stream corruption while rendering fragments |
| org.springframework:spring-webmvc:6.2.19 | compile | GHSA-pc63-qcmh-9cmg | CVE-2026-47884 | CRITICAL | 7.0.9 | Spring Framework Improper Path Limitation in XsltView |
| org.postgresql:postgresql:42.7.11 | runtime | GHSA-j92g-9f8w-j867 | CVE-2026-54291 | HIGH | 42.7.12 | PostgreSQL JDBC Driver: Silent channel-binding authentication downgrade via unsupported ce |

## Dependencias analizadas

- ch.qos.logback:logback-classic:1.5.34 (compile)
- ch.qos.logback:logback-core:1.5.34 (compile)
- com.fasterxml.jackson.core:jackson-annotations:2.21 (compile)
- com.fasterxml.jackson.core:jackson-core:2.21.4 (compile)
- com.fasterxml.jackson.core:jackson-databind:2.21.4 (compile)
- com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.21.4 (compile)
- com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.4 (compile)
- com.fasterxml.jackson.module:jackson-module-parameter-names:2.21.4 (compile)
- com.github.virtuald:curvesapi:1.08 (compile)
- com.google.errorprone:error_prone_annotations:2.36.0 (compile)
- com.google.guava:failureaccess:1.0.3 (compile)
- com.google.guava:guava:33.4.8-jre (compile)
- com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava (compile)
- com.google.j2objc:j2objc-annotations:3.0.0 (compile)
- com.zaxxer:HikariCP:6.3.3 (compile)
- com.zaxxer:SparseBitSet:1.3 (compile)
- commons-codec:commons-codec:1.18.0 (compile)
- commons-io:commons-io:2.18.0 (compile)
- io.micrometer:micrometer-commons:1.15.12 (compile)
- io.micrometer:micrometer-observation:1.15.12 (compile)
- jakarta.annotation:jakarta.annotation-api:2.1.1 (compile)
- org.apache.commons:commons-collections4:4.4 (compile)
- org.apache.commons:commons-compress:1.27.1 (compile)
- org.apache.commons:commons-lang3:3.20.0 (compile)
- org.apache.commons:commons-math3:3.6.1 (compile)
- org.apache.logging.log4j:log4j-api:2.24.3 (compile)
- org.apache.logging.log4j:log4j-to-slf4j:2.24.3 (compile)
- org.apache.poi:poi:5.4.1 (compile)
- org.apache.poi:poi-ooxml:5.4.1 (compile)
- org.apache.poi:poi-ooxml-lite:5.4.1 (compile)
- org.apache.tomcat.embed:tomcat-embed-core:10.1.55 (compile)
- org.apache.tomcat.embed:tomcat-embed-el:10.1.55 (compile)
- org.apache.tomcat.embed:tomcat-embed-websocket:10.1.55 (compile)
- org.apache.xmlbeans:xmlbeans:5.3.0 (compile)
- org.attoparser:attoparser:2.0.7.RELEASE (compile)
- org.jspecify:jspecify:1.0.0 (compile)
- org.slf4j:jul-to-slf4j:2.0.18 (compile)
- org.slf4j:slf4j-api:2.0.18 (compile)
- org.springframework:spring-aop:6.2.19 (compile)
- org.springframework:spring-beans:6.2.19 (compile)
- org.springframework:spring-context:6.2.19 (compile)
- org.springframework:spring-core:6.2.19 (compile)
- org.springframework:spring-expression:6.2.19 (compile)
- org.springframework:spring-jcl:6.2.19 (compile)
- org.springframework:spring-jdbc:6.2.19 (compile)
- org.springframework:spring-tx:6.2.19 (compile)
- org.springframework:spring-web:6.2.19 (compile)
- org.springframework:spring-webmvc:6.2.19 (compile)
- org.springframework.boot:spring-boot:3.5.15 (compile)
- org.springframework.boot:spring-boot-autoconfigure:3.5.15 (compile)
- org.springframework.boot:spring-boot-starter:3.5.15 (compile)
- org.springframework.boot:spring-boot-starter-jdbc:3.5.15 (compile)
- org.springframework.boot:spring-boot-starter-json:3.5.15 (compile)
- org.springframework.boot:spring-boot-starter-logging:3.5.15 (compile)
- org.springframework.boot:spring-boot-starter-thymeleaf:3.5.15 (compile)
- org.springframework.boot:spring-boot-starter-tomcat:3.5.15 (compile)
- org.springframework.boot:spring-boot-starter-web:3.5.15 (compile)
- org.springframework.security:spring-security-crypto:6.5.11 (compile)
- org.thymeleaf:thymeleaf:3.1.5.RELEASE (compile)
- org.thymeleaf:thymeleaf-spring6:3.1.5.RELEASE (compile)
- org.unbescape:unbescape:1.1.6.RELEASE (compile)
- org.yaml:snakeyaml:2.4 (compile)
- org.postgresql:postgresql:42.7.11 (runtime)
- com.jayway.jsonpath:json-path:2.9.0 (test)
- com.vaadin.external.google:android-json:0.0.20131108.vaadin1 (test)
- jakarta.activation:jakarta.activation-api:2.1.4 (test)
- jakarta.xml.bind:jakarta.xml.bind-api:4.0.5 (test)
- net.bytebuddy:byte-buddy:1.17.8 (test)
- net.bytebuddy:byte-buddy-agent:1.17.8 (test)
- net.minidev:accessors-smart:2.5.2 (test)
- net.minidev:json-smart:2.5.2 (test)
- org.apiguardian:apiguardian-api:1.1.2 (test)
- org.assertj:assertj-core:3.27.7 (test)
- org.awaitility:awaitility:4.2.2 (test)
- org.hamcrest:hamcrest:3.0 (test)
- org.junit.jupiter:junit-jupiter:5.12.2 (test)
- org.junit.jupiter:junit-jupiter-api:5.12.2 (test)
- org.junit.jupiter:junit-jupiter-engine:5.12.2 (test)
- org.junit.jupiter:junit-jupiter-params:5.12.2 (test)
- org.junit.platform:junit-platform-commons:1.12.2 (test)
- org.junit.platform:junit-platform-engine:1.12.2 (test)
- org.mockito:mockito-core:5.17.0 (test)
- org.mockito:mockito-junit-jupiter:5.17.0 (test)
- org.objenesis:objenesis:3.3 (test)
- org.opentest4j:opentest4j:1.3.0 (test)
- org.ow2.asm:asm:9.7.1 (test)
- org.skyscreamer:jsonassert:1.5.3 (test)
- org.springframework:spring-test:6.2.19 (test)
- org.springframework.boot:spring-boot-starter-test:3.5.15 (test)
- org.springframework.boot:spring-boot-test:3.5.15 (test)
- org.springframework.boot:spring-boot-test-autoconfigure:3.5.15 (test)
- org.xmlunit:xmlunit-core:2.10.4 (test)
