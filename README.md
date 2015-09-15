# oppijan-tunnistus

## Creating executable JAR

./lein uberjar

## Local Postgres setup

MacOS users install docker with command `brew cask install dockertoolbox`.

1. Create new docker-machine `docker-machine create —-driver virtualbox dockerVM`
2. Eval "$(docker-machine env dockerVM)"`
3. Check DOCKER_HOST variable
4. Edit /etc/hosts. Add line `<docker-host-ip-goes-here> oppijantunnistusdb`
5. `docker run -p 5432:5432 postgres`
6. `psql -hoppijantunnistusdb -p5432 -Upostgres postgres -c "CREATE DATABASE oppijantunnistusdb;"`
