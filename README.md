# oppijan-tunnistus

## Local Postgres setup

    docker-compose up

## Run locally,

If needed, set logback.access property:

    export JVM_OPTS=-Dlogback.access=does-not-exist.xml

Start server:

    ./lein run

 OPen browser:
 
    open http://localhost:9090/oppijan-tunnistus/swagger/api-docs
    
## Creating executable JAR

    ./lein uberjar

## Run tests with local Postgres setup up and running

    ./lein with-profile test spec
