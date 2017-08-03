# Identity Service
Service for managing identities on the platform.

## Run locally

In order to run the project locally without integrating it into the Micro-serivce infrastructure.

```
mvn clean install
cd identity-service
mvn spring-boot:run -Drun.jvmArguments="-Dspring.profiles.active=local_dev"
```