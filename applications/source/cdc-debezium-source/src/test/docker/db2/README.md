The integration tests use a `DB2` database server running in a custom Docker container based upon the ibmcom/db2 Docker image maintained by the DB2 team.

However, the OOTB DB2 is not configured for CDC! This Dockerfile folder helps to build CDC enabled DB2 docker image used for testing. 
Because of license constrain this image can not be pre-build and distributed but has to be build on the fly.

Start the DB2 CDC container manually:

From within the `src/test/docker/db2` folder run:

```
docker build -t db2-cdc2 .
docker run -itd --name mydb2 --privileged=true -p 50000:50000 -e LICENSE=accept -e DB2INST1_PASSWORD=password -e DBNAME=testdb db2-cdc2

docker logs -f mydb2
```

explore: `docker exec -it mydb2 /bin/bash`

and clean: `docker stop mydb2`

All databases used in the integration tests are defined and populated using *.sql files and *.sh scripts in the src/test/docker/db2-cdc-docker directory, which are copied into the Docker image and run by DB2 upon startup. Multiple test methods within a single integration test class can reuse the same database, but generally each integration test class should use its own dedicated database(s).

(TODO) The build will automatically start the DB2 container before the integration tests are run and automatically stop and remove it after all of the integration tests complete (regardless of whether they success or fail).


