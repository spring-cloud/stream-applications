FROM debezium/example-mongodb:0.10

## Bundle data source
COPY entrypoint.sh /usr/local/bin/

# Grant permissions for the import-data script to be executable
RUN chmod +x /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/init-inventory.sh

CMD  /usr/local/bin/entrypoint.sh
#CMD ["mongod", "--replSet", "rs0", "--auth"]
#CMD ["/bin/bash", "./entrypoint.sh"]

#CMD ["mongod", "--replSet", "rs0", "--auth", "&", "sleep", "10s", "&", "/usr/local/bin/init-inventory.sh"]
#CMD mongod --replSet rs0 --auth

