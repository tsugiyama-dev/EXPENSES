# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.1/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.1/maven-plugin/build-image.html)
* [Spring Boot Testcontainers support](https://docs.spring.io/spring-boot/4.0.1/reference/testing/testcontainers.html#testing.testcontainers)
* [Testcontainers MySQL Module Reference Guide](https://java.testcontainers.org/modules/databases/mysql/)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/4.0.1/reference/using/devtools.html)
* [Flyway Migration](https://docs.spring.io/spring-boot/4.0.1/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
* [MyBatis Framework](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
* [Spring Security](https://docs.spring.io/spring-boot/4.0.1/reference/web/spring-security.html)
* [Testcontainers](https://java.testcontainers.org/)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.1/reference/web/servlet.html)

### Guides
The following guides illustrate how to use some features concretely:

* [MyBatis Quick Start](https://github.com/mybatis/spring-boot-starter/wiki/Quick-Start)
* [Accessing data with MySQL](https://spring.io/guides/gs/accessing-data-mysql/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Testcontainers support

This project uses [Testcontainers at development time](https://docs.spring.io/spring-boot/4.0.1/reference/features/dev-services.html#features.dev-services.testcontainers).

Testcontainers has been configured to use the following Docker images:

* [`mysql:latest`](https://hub.docker.com/_/mysql)

Please review the tags of the used images and set them to the same as you're running in production.

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

