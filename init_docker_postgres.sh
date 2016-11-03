#!/usr/bin/env bash
eval "$(docker-machine env dockerVM)"
docker run -d -p 5432:5432 postgres
while ! echo exit | nc oppijantunnistusdb 5432; do sleep 10; done
psql -hoppijantunnistusdb -p5432 -Upostgres postgres -c "CREATE DATABASE oppijantunnistusdb;"
