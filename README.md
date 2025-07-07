# lso-videoteca
***Progetto LSO***
1) Per utilizzare il progetto occorre avere preliminarmente installato VcXsrv. Docker non supporta nativamente GUI come quelle di JavaFX ed è necessario il supporto di quest'applicazione third party (è fornito il collegamento al download);
2) Aprire il file xconfig con VcXsrv;
3) Aprire la cmd ed entrare nella cartella del progetto come working directory;
4) Avviare il progetto con "docker compose up". Nel caso si vogliano avviare più istanze del client si utilizzi "docker compose up --scale videoteca-client=n --build", dove n è il numero dei client da avviare.
