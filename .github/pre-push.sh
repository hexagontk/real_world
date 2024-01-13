#!/bin/sh

set -e

docker compose up -d --build --force-recreate

./gradlew build

npm install newman

export TEST_RES="backend/src/test/resources/postman"
node_modules/.bin/newman run --verbose $TEST_RES/postman.json -e $TEST_RES/docker.json

export REPO="https://raw.githubusercontent.com/gothinkster/realworld"
mkdir -p build
curl $REPO/main/api/Conduit.postman_collection.json -o build/postman.json
node_modules/.bin/newman run --verbose build/postman.json -e $TEST_RES/docker.json
