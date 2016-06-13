# oppijan-tunnistus

## Run locally, Open url http://localhost:9090/oppijan-tunnistus/swagger

If needed, set logback.access property: `export JVM_OPTS=-Dlogback.access=does-not-exist.xml`

./lein run

## Creating executable JAR

./lein uberjar

## Local Postgres setup

MacOS users install docker with command `brew cask install dockertoolbox`.

1. Create new docker-machine `docker-machine create —-driver virtualbox dockerVM`
2. `docker-machine env dockerVM`
3. Check DOCKER_HOST variable
4. Edit /etc/hosts. Add line `<docker-host-ip-goes-here> oppijantunnistusdb`
2. `eval "$(docker-machine env dockerVM)"`
5. `docker run -p 5432:5432 postgres`
6. `psql -hoppijantunnistusdb -p5432 -Upostgres postgres -c "CREATE DATABASE oppijantunnistusdb;"`

## Run tests with local Postgres setup up and running

./lein with-profile test spec
