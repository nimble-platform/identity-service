#!/usr/bin/env bash

set -e    # Exit immediately if a command exits with a non-zero status.

function build(){
    mvn clean install -DskipTests
}

function docker_build(){
    mvn -f identity-service/pom.xml docker:build
}

if [ "$1" == "build" ]; then
    # build projects
    build
elif [ "$1" == "docker-build" ]; then
    # build projects
    build
    # build docker
    docker_build

elif [ "$1" == "docker-push" ]; then

    mvn -f identity-service/pom.xml docker:push

elif [ "$1" == "dev-startdb" ]; then
#        -v /tmp/postgres:/var/lib/postgresql/data \
    docker run --rm \
        --name identityDB \
        -p 5433:5432 \
        -e POSTGRES_PASSWORD=changeme \
        -e POSTGRES_USER=root \
        -e POSTGRES_DB=identityDB postgres
fi
