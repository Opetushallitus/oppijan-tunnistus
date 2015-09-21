# oppijan-tunnistus

## Run locally

./lein run

Open url http://localhost:8080/oppijan-tunnistus/swagger

## Creating executable JAR

./lein uberjar

## Run tests

./lein spec

## Local Postgres setup

MacOS users install docker with command `brew cask install dockertoolbox`.

1. Create new docker-machine `docker-machine create —-driver virtualbox dockerVM`
2. eval (docker-machine env dockerv)
3. Check DOCKER_HOST variable
4. Edit /etc/hosts. Add line `<docker-host-ip-goes-here> oppijantunnistusdb`
5. `docker run -p 5432:5432 postgres`
6. `psql -hoppijantunnistusdb -p5432 -Upostgres postgres -c "CREATE DATABASE oppijantunnistusdb;"`

## Run tests with local Postgres setup up and running

./lein with-profile test spec
