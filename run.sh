#!/usr/bin/env bash

# take care of UAA Java Client
git submodule update --init --recursive
mvn -f libs/uaa-java-client/pom.xml clean install -DskipTests

mvn clean spring-boot:run