#!/usr/bin/env bash

if ! command -v mvn &> /dev/null
then
    echo "Maven is not installed. Please install Maven to continue."
    exit 1
fi


if [ ! -d "./target" ]; then
    mkdir ./target
fi

mvn clean install -f ./Common

mvn clean package -f ./server
mv ./server/target/server-1.0-SNAPSHOT.jar ./target
rm -rf ./server/target

mvn clean package -f ./client
mv ./client/target/client-1.0-SNAPSHOT.jar ./target
rm -rf ./server/target