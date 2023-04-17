#wait for the SQL Server to come up
sleep 10s

#populate the db
/usr/local/bin/init-inventory.sh
