# Start SQL Server, start the script to create the DB and import the data.
# Use the tail to keep the container running
/opt/mssql/bin/sqlservr & \
/usr/src/data/import-data.sh & \
echo "Data Loaded" & \
tail -f /dev/null
