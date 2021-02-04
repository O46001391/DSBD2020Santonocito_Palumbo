# DSBD2020_SANTONOCITO_PALUMBO

>>> DSBD2020 SANTONOCITO PALUMBO PROJECT.

# TEST MICROSERVIZIO #

>>> 1) Importare l'intero progetto;
>>> 2) Recarsi nella directory: 
>>>    DSBD2020_Project/docker/docker-prod;
>>> 3) Avviare il docker-compose building;
>>> 4) Avviare il docker-compose up;
>>> 5) Aprire /bin/sh del container Mongo;
>>> 6) Avviare da linea di comando lo script di setting del Replica-Set all'interno della mongo-client-shell integrata:
>>>    mongo -u root -p 1208 --port 27017 < ./docker-entrypoint-initdb.d/replica-init.js;

# DOCUMENTAZIONE #

>>> * /doc/UML: file ".asta" contenente:
>>>   1) Diagramma della classi di progetto;
>>>   2) Diagramma dei package;
>>>   3) Diagrammi di sequenza.
>>> * /doc/use-case: file ".pdf" contenente:
>>>   1) Descrizione dettagliata dei casi d'uso implementati.
>>> * /doc/environment: file ".pdf" contenente:
>>>   1) Descrizione grafica dell'ambiente di sviluppo e test del microservizio.
>>> * /doc/overview: file ".pdf" contenente:
>>>   1) Panoramica degli aspetti fondamentali del microservizio.