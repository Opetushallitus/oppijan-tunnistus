# oppijan-tunnistus

## Local Postgres setup

MacOS users install docker with command `brew cask install dockertoolbox`.

1. Create new docker-machine `docker-machine create —-driver virtualbox dockerVM` or
    Start old one `docker-machine start dockerVM`
2. `docker-machine env dockerVM`
3. Check DOCKER_HOST variable
4. Edit /etc/hosts. Add line `<docker-host-ip-goes-here> oppijantunnistusdb`
5. init docker db `. init_docker_postgres.sh`

## Run locally,

If needed, set logback.access property:

    export JVM_OPTS=-Dlogback.access=does-not-exist.xml

Start server:

    ./lein run

 OPen browser:
 
    open http://localhost:9090/oppijan-tunnistus/swagger
    
## Creating executable JAR

    ./lein uberjar

## Run tests with local Postgres setup up and running

    ./lein with-profile test spec
