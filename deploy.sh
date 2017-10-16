#!/usr/bin/env bash

set -e    # Exits immediately if a command exits with a non-zero status.sadfaksjdflakjsdf

if [ "$1" == "build" ]; then

    mvn clean install -DskipTests

elif [ "$1" == "docker-build" ]; then

    mvn -f identity-service/pom.xml docker:build

elif [ "$1" == "docker-push" ]; then

    mvn -f identity-service/pom.xml docker:push

elif [ "$1" == "dev-startdb" ]; then
#        -v /tmp/postgres:/var/lib/postgresql/data \
    docker run \
        --name identityDB \
        -p 5433:5432 \
        -e POSTGRES_PASSWORD=changeme \
        -e POSTGRES_USER=root \
        -e POSTGRES_DB=identityDB postgres
fi
