#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <sys/types.h>
#include <pthread.h>
#include <sqlite3.h>

#include "models.h"

#define BACKLOG 10

sqlite3 *db;

//funzioni registrazione e autenticazione utente
void registerUser(sqlite3 *db, const char *username, const char *password)
{
    const char *sql = "INSERT INTO users (username, password) VALUES (?, ?)";
    sqlite3_stmt *stmt;

    if(sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 2, password, -1, SQLITE_STATIC);

        if(sqlite3_step(stmt) != SQLITE_DONE)
        {
            fprintf(stderr, "Error inserting user: %s\n", sqlite3_errmsg(db));
        } else {
            printf("User registered successfully.\n");
        }
    } else {
        printf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

int authenticateUser(sqlite3 *d, const char *username, const char *password)
{
    const char *sql = "SELECT 1 FROM users WHERE username = ? AND password = ?";
    sqlite3_stmt *stmt;
    int result = 0;

    if(sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 2, password, -1, SQLITE_STATIC);

        if(sqlite3_step(stmt) == SQLITE_ROW)
        {
            result = 1;
        } else {
            result = 0;
        }
    } else {
        printf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

void setupDatabase()
{
    const char *dbName = "videoteca.db";
    if(sqlite3_open(dbName, &db) != SQLITE_OK)
    {
        fprintf(stderr, "Cannot open database: %s\n", sqlite3_errmsg(db));
        exit(1);
    }

    const char *sqlMovies = "CREATE TABLE IF NOT EXISTS movies ( id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, genre NOT NULL, duration INTEGER NOT NULL, availableCopies INTEGER NOT NULL, totalCopies INTEGER NOT NULL);";
    const char *sqlUsers = "CREATE TABLE IF NOT EXISTS users ( username TEXT PRIMARY KEY NOT NULL, password TEXT NOT NULL);";
    const char *sqlRentals = "CREATE TABLE IF NOT EXISTS rentals ( id INTEGER, username TEXT NOT NULL, rentaldate TEXT NOT NULL, returndate TEXT NOT NULL, FOREIGN KEY (id) REFERENCES movies(id), FOREIGN KEY (username) REFERENCES users(username));";
    char *errMsg = 0;

    if (sqlite3_exec(db, sqlMovies, 0, 0, &errMsg) != SQLITE_OK ||
        sqlite3_exec(db, sqlUsers, 0, 0, &errMsg) != SQLITE_OK ||
        sqlite3_exec(db, sqlRentals, 0, 0, &errMsg) != SQLITE_OK)
        {
            fprintf(stderr, "SQL Error: %s\n", errMsg);
            sqlite3_free(errMsg);
        }
}

void loadMovies(struct Movie **movies, int *num_film)
{
    const char *sql = "SELECT id, title, genre, duration, totalCopies, availableCopies FROM movies"; //ordine parametri ? Si puo' mettere * o vuole tutte le colonne?
    sqlite3_stmt *stmt;
    if(sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        while(sqlite3_step(stmt) == SQLITE_ROW)
        {
            struct Movie movie;
            movie.id = sqlite3_column_int(stmt, 0);
            strcpy(movie.title, (char *)sqlite3_column_text(stmt, 1));
            strcpy(movie.genre, (char *)sqlite3_column_text(stmt, 2));
            movie.duration = sqlite3_column_int(stmt, 3);
            movie.totalCopies = sqlite3_column_int(stmt, 4);
            movie.availableCopies = sqlite3_column_int(stmt, 5);
            (*movies)[(*num_film)++] = movie;
        }
    }
    sqlite3_finalize(stmt);
}



void *gestione_client(void *arg)
{
    int client_fd = *((int *)arg);
    memset(arg, 0, sizeof(arg));

    struct Movie *movies = malloc(100 * sizeof(struct Movie));
    int num_film = 0;
    loadMovies(&movies, &num_film);

    char buffer[1024];
    bzero(buffer, sizeof(buffer));

    char *msg = "Benvenuto alla videoteca! \n1) Registrati\n2) Login\nScelta: \n";
    write(client_fd, msg, strlen(msg));

    //ricezione del messaggio dal client
    read(client_fd, buffer, sizeof(buffer));
    printf("Messaggio ricevuto dal client: %s\n", buffer);


    bzero(buffer, sizeof(buffer));
    read(client_fd, buffer, sizeof(buffer));
    int scelta = atoi(buffer);

    if (scelta == 1) {
        // Registrazione utente
        char username[50], password[50];
        write(client_fd, "Inserisci username: ", strlen("Inserisci username: "));
        read(client_fd, username, sizeof(username));
        write(client_fd, "Inserisci password: ", strlen("Inserisci password: "));
        read(client_fd, password, sizeof(password));
        registerUser(db, username, password);
    } else if (scelta == 2) {
        // Autenticazione utente
        char username[50], password[50];
        write(client_fd, "Inserisci username: ", strlen("Inserisci username: "));
        read(client_fd, username, sizeof(username));
        write(client_fd, "Inserisci password: ", strlen("Inserisci password: "));
        read(client_fd, password, sizeof(password));

        if (authenticateUser(db, username, password)) {
            write(client_fd, "Login riuscito!\n", strlen("Login riuscito!\n"));
            // Procedi con altre operazioni per utenti autenticati
        } else {
            write(client_fd, "Login fallito.\n", strlen("Login fallito.\n"));
        }
    }

    close(client_fd);
    pthread_exit(NULL);
}

int main()
{
    setupDatabase();
    int fd1;
    struct sockaddr_in server_address, client_address;
    socklen_t client_len = sizeof(client_address);

    //creazione della socket
    if((fd1 = socket(AF_INET, SOCK_STREAM, 0)) < 0)
    {
        perror("Failed to create socket...\n");
        exit(1);
    }

    //inizializzazione della struttura sockaddr_in
    //bzero(&server_address, sizeof(server_address));
    server_address.sin_family = AF_INET; //IPv4
    server_address.sin_addr.s_addr = htons(INADDR_ANY); //accetta connessioni da qualsiasi indirizzo
    server_address.sin_port = htons(8080); //porta 80
    
    //binding socket
    if((bind(fd1, &server_address , sizeof(server_address))) < 0)
    {
        perror("Failed to bind socket...\n");
        exit(1);
    }

    //listening
    if((listen(fd1, BACKLOG)) != 0)
    {
        perror("Failed to start listening...\n");
        exit(1);
    }

    printf("====SERVER READY====\n");

    while(1)
    {
        printf("====WAITING FOR CONNECTION====\n");
        int *fd2 = malloc(sizeof(int));
        if((*fd2 = accept(fd1, (struct sockaddr *)&client_address, &client_len)) < 0)
        {
            perror("Failed to accept connection...\n");
            free(fd2);
            continue;
        }

        pthread_t tid;
        if(pthread_create(&tid, NULL, gestione_client, fd2) != 0)
        {
            perror("Failed to create thread...\n");
            close(*fd2);
            free(fd2);
        }
        pthread_detach(tid);
    }
    close(fd1);
    return 0;
}
