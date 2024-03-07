# oppijan-tunnistus

## Local Postgres setup

    $ docker-compose up

If you use podman, set one of the following first:

    $ export DOCKER_HOST=unix://run/podman/podman.sock  # rootful
    $ export DOCKER_HOST=unix://run/user/$UID/podman/podman.sock  # rootless

## Run locally

If needed, set logback.access property:

    $ export JVM_OPTS=-Dlogback.access=does-not-exist.xml

Start server:

    $ lein with-profile dev run

You can also use a REPL and start from there (e.g. for precise reloads
or NREPL editor integrations):

    $ lein with-profiles +dev repl
    $ fi.vm.sade.oppijantunnistus.main=> (-main)

Open browser:
 
    $ open http://localhost:9090/oppijan-tunnistus/swagger/api-docs
    
## Creating executable JAR

    $ lein uberjar

## Run tests

Set up local postgres first (see above).

    $ lein with-profile test spec
