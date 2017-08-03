# Identity Service
Service for managing identities on the platform.

 ---
The project leading to this application has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 723810.

## Run locally

In order to run the project locally without integrating it into the Micro-serivce infrastructure.

```
mvn clean install
cd identity-service
mvn spring-boot:run -Drun.jvmArguments="-Dspring.profiles.active=local_dev"
```
