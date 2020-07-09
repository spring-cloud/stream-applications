FROM microsoft/mssql-server-linux:2017-CU9-GDR2

## Create sql directory
RUN mkdir -p /usr/src/data
WORKDIR /usr/src/data

## Bundle data source
COPY . /usr/src/data

# Grant permissions for the import-data script to be executable
RUN chmod +x /usr/src/data/import-data.sh

CMD /bin/bash ./entrypoint.sh
