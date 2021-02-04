FROM mongo
# Creiamo uno script ".JS", lanciabile nella mongo-client-shell, che contiene la configurazione
# del replica-set che andremo ad utilizzare per usufruire delle transazioni su MongoDB.
# Tale script necessita di esser lanciato all'interno del container "mongo" con il seguente comando:
# mongo -u root -p 1208 --port 27017 < ./docker-entrypoint-initdb.d/replica-init.js
#   |       |       |            |                        |
#   V       V       V            V                        V
# mongo [-u User] [-p Password] [--port Porta] < [Locazione sul container dello script di configurazione]
RUN echo "config = {_id: 'rs0', members: [{_id: 0, host: 'mongo:27017', priority: 10}]}; rs.initiate(config); rs.status();" > /docker-entrypoint-initdb.d/replica-init.js
# Avvia il servizio "mongod" necessario per utilizzare la mongo-client-shell sfruttando un nuovo
# replica-set di nome "rs0".
CMD [ "--replSet", "rs0" ]
