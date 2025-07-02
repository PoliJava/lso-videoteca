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
#include <time.h>
#include "models.h"

#define BACKLOG 10

sqlite3 *db;

// funzione per il rental return

void get_expiration_date(char *buffer, size_t size)
{
    time_t t = time(NULL);
    struct tm tm = *localtime(&t);

    tm.tm_mon += 1;

    // Normalizza il tempo (aumenta l'anno se il mese è dicembre)
    mktime(&tm);

    // Format: YYYY-MM-DD
    strftime(buffer, size, "%Y-%m-%d", &tm);
}

// funzioni registrazione e autenticazione utente
void registerUser(sqlite3 *db, const char *username, const char *password)
{
    const char *sql = "INSERT INTO users (username, password) VALUES (?, ?)";
    sqlite3_stmt *stmt;

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 2, password, -1, SQLITE_STATIC);

        if (sqlite3_step(stmt) != SQLITE_DONE)
        {
            fprintf(stderr, "Error inserting user: %s\n", sqlite3_errmsg(db));
        }
        else
        {
            printf("User registered successfully.\n");
        }
    }
    else
    {
        fprintf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

int authenticateUser(sqlite3 *db, const char *username, const char *password)
{
    const char *sql = "SELECT 1 FROM users WHERE username = ? AND password = ?";
    sqlite3_stmt *stmt;
    int result = 0;

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        printf("stmt: %s\n", sqlite3_sql(stmt));
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        printf("username: %s\n", username);
        sqlite3_bind_text(stmt, 2, password, -1, SQLITE_STATIC);
        printf("password: %s\n", password);

        printf("stmt: %s\n", sqlite3_sql(stmt));
        // print SQLITE_ROW
        int stepResult = sqlite3_step(stmt);

        if (stepResult == SQLITE_ROW)
        {
            printf("DEBUG - Login OK\n");
            result = 1;
        }
        else
        {
            printf("DEBUG - Login FALLITO, stepResult = %d\n", stepResult);
            result = 0;
        }
    }
    else
    {
        fprintf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return result;
}

int authenticateAdmin(sqlite3 *db, const char *username, const char *password)
{
    const char *sql = "SELECT 1 FROM admins WHERE username = ? AND password = ?";
    sqlite3_stmt *stmt;
    int result = 0;

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        printf("stmt: %s\n", sqlite3_sql(stmt));
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        printf("username: %s\n", username);
        sqlite3_bind_text(stmt, 2, password, -1, SQLITE_STATIC);
        printf("password: %s\n", password);

        printf("stmt: %s\n", sqlite3_sql(stmt));
        // print SQLITE_ROW
        int stepResult = sqlite3_step(stmt);

        if (stepResult == SQLITE_ROW)
        {
            printf("DEBUG - Login OK\n");
            result = 1;
        }
        else
        {
            printf("DEBUG - Login FALLITO, stepResult = %d\n", stepResult);
            result = 0;
        }
    }
    else
    {
        fprintf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return result;
}

void trimNewline(char *str)
{
    size_t len = strlen(str);
    if (len > 0 && (str[len - 1] == '\n' || str[len - 1] == '\r'))
        str[len - 1] = '\0';
}

int read_line(int fd, char *buffer, size_t max_len)
{
    size_t i = 0;
    char c;
    while (i < max_len - 1)
    {
        int n = read(fd, &c, 1);
        if (n > 0)
        {
            if (c == '\n')
            {
                break;
            }
            buffer[i++] = c;
        }
        else if (n == 0)
        {
            // EOF
            break;
        }
        else
        {
            // Error
            return -1;
        }
    }
    // Remove trailing carriage return if present
    if (i > 0 && buffer[i - 1] == '\r')
    {
        i--;
    }
    buffer[i] = '\0';
    return i;
}

// funzioni per database
void setupDatabase()
{
    const char *dbName = "videoteca.db";
    int flags = SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_FULLMUTEX;
    if (sqlite3_open_v2(dbName, &db, flags, NULL) != SQLITE_OK)
    {
        fprintf(stderr, "Cannot open database: %s\n", sqlite3_errmsg(db));
        exit(1);
    }

    const char *sqlMovies = "CREATE TABLE IF NOT EXISTS movies ( id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, genre TEXT NOT NULL, duration INTEGER NOT NULL, availableCopies INTEGER NOT NULL CHECK (availableCopies >= 0), totalCopies INTEGER NOT NULL);";
    const char *sqlUsers = "CREATE TABLE IF NOT EXISTS users ( username TEXT PRIMARY KEY NOT NULL, password TEXT NOT NULL);";
    const char *sqlRentals = "CREATE TABLE IF NOT EXISTS rentals ( movieId INTEGER, username TEXT NOT NULL, rentaldate TEXT NOT NULL, returndate TEXT NOT NULL, FOREIGN KEY (movieId) REFERENCES movies(id), FOREIGN KEY (username) REFERENCES users(username), UNIQUE(username,movieId));";
    const char *sqlCart = "CREATE TABLE IF NOT EXISTS cart ( id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL, movieid INTEGER NOT NULL, FOREIGN KEY (username) REFERENCES users(username), FOREIGN KEY (movieid) REFERENCES movies(id), UNIQUE(username,movieid));";
    const char *sqlDec = "DROP TRIGGER IF EXISTS decrease_available_copies; "
                         "CREATE TRIGGER decrease_available_copies "
                         "AFTER INSERT ON rentals "
                         "FOR EACH ROW "
                         "BEGIN "
                         "UPDATE movies SET availableCopies = availableCopies - 1 WHERE id = NEW.movieId; "
                         "END;";
    const char *sqlInc = "DROP TRIGGER IF EXISTS increase_available_copies; "
                         "CREATE TRIGGER increase_available_copies "
                         "AFTER DELETE ON rentals "
                         "FOR EACH ROW "
                         "BEGIN "
                         "UPDATE movies SET availableCopies = availableCopies + 1 WHERE id = OLD.movieId; "
                         "END;";
    const char *sqlAdmin = "CREATE TABLE IF NOT EXISTS admins ( id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL)";
    // const char *sqlUpd = "UPDATE movies SET availableCopies = CASE WHEN availableCopies > 0 THEN availableCopies - 1 ELSE 0 END WHERE id = NEW.movieId;";
    const char *sqlMessages = "CREATE TABLE IF NOT EXISTS messages ( id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL, sender TEXT NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL, movieId INTEGER NOT NULL, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (movieid) REFERENCES movies(id), FOREIGN KEY (username) REFERENCES users(username));";
    char *errMsg = 0;

    if (sqlite3_exec(db, sqlMovies, 0, 0, &errMsg) != SQLITE_OK ||
        sqlite3_exec(db, sqlUsers, 0, 0, &errMsg) != SQLITE_OK ||
        sqlite3_exec(db, sqlRentals, 0, 0, &errMsg) != SQLITE_OK ||
        sqlite3_exec(db, sqlCart, 0, 0, &errMsg) != SQLITE_OK ||
        sqlite3_exec(db, sqlDec, 0, 0, &errMsg) != SQLITE_OK ||
        sqlite3_exec(db, sqlInc, 0, 0, &errMsg) != SQLITE_OK ||
        sqlite3_exec(db, sqlAdmin, 0, 0, &errMsg) != SQLITE_OK ||
        sqlite3_exec(db, sqlMessages, 0, 0, &errMsg) != SQLITE_OK)
        {
            fprintf(stderr, "SQL Error: %s\n", errMsg);
            sqlite3_free(errMsg);
        }
}

void loadMovies(struct Movie **movies, int *num_film)
{
    const char *sql = "SELECT id, title, genre, duration, totalCopies, availableCopies FROM movies"; // ordine parametri ? Si puo' mettere * o vuole tutte le colonne?
    sqlite3_stmt *stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        while (sqlite3_step(stmt) == SQLITE_ROW)
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

// funzioni per noleggio film e restituzione film
void rentMovie(sqlite3 *db, int movieId, const char *username, const char *rentalDate, const char *returnDate)
{
    const char *sql = "INSERT INTO rentals (movieId, username, rentalDate, returnDate) VALUES (?, ?, ?, ?)";
    sqlite3_stmt *stmt;

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        sqlite3_bind_int(stmt, 1, movieId);
        sqlite3_bind_text(stmt, 2, username, -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 3, rentalDate, -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 4, returnDate, -1, SQLITE_STATIC);

        if (sqlite3_step(stmt) != SQLITE_DONE)
        {
            fprintf(stderr, "Error renting movie: %s\n", sqlite3_errmsg(db));
        }
        else
        {
            const char *updateSql = "UPDATE movies SET availableCopies = availableCopies - 1 WHERE id = ?";
            sqlite3_stmt *updateStmt;
            if (sqlite3_prepare_v2(db, updateSql, -1, &updateStmt, 0) == SQLITE_OK)
            {
                sqlite3_bind_int(updateStmt, 1, movieId);
                sqlite3_step(updateStmt);
            }
            sqlite3_finalize(updateStmt);
            printf("Movie rented successfully.\n");
        }
    }
    else
    {
        fprintf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

void returnMovie(sqlite3 *db, int movieId, const char *username)
{
    const char *sql = "DELETE FROM rentals WHERE username = ? AND movieId = ?";
    sqlite3_stmt *stmt;
    int rc;
    int rows_affected = 0;
    // printf("Entered DelFromC\n");
    printf("Deleting id %d for user %s\n", movieId, username);
    // Inizia una transazione esplicita
    rc = sqlite3_exec(db, "BEGIN TRANSACTION", 0, 0, 0);
    if (rc != SQLITE_OK)
    {
        fprintf(stderr, "Error beginning transaction: %s\n", sqlite3_errmsg(db));
        printf("-1\n");
        return -1;
    }

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        sqlite3_bind_int(stmt, 2, movieId);

        rc = sqlite3_step(stmt);
        if (rc == SQLITE_DONE)
        {
            rows_affected = sqlite3_changes(db);
            printf("Deleted %d rows from cart\n", rows_affected);

            // Commit della transazione
            rc = sqlite3_exec(db, "COMMIT", 0, 0, 0);
            if (rc != SQLITE_OK)
            {
                fprintf(stderr, "Error committing transaction: %s\n", sqlite3_errmsg(db));
                sqlite3_finalize(stmt);
                printf("-1\n");
                return -1;
            }
        }
        else
        {
            fprintf(stderr, "Error deleting from cart: %s\n", sqlite3_errmsg(db));
            // Rollback in caso di errore
            sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
            sqlite3_finalize(stmt);
            printf("-1");
            return -1;
        }
        sqlite3_finalize(stmt);
        printf("%d\n", (rows_affected > 0) ? 0 : 1);
        return (rows_affected > 0) ? 0 : 1; // 0=success, 1=no rows deleted
    }
    else
    {
        fprintf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
        sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
        printf("-1");
        return -1;
    }

    sqlite3_finalize(stmt);
}

// funzioni per carrello
void getCart(sqlite3 *db, const char *username)
{
    const char *sql = "SELECT movieId FROM cart WHERE username = ?";
    sqlite3_stmt *stmt;

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);

        printf("Movies in cart for user: %s\n", username);
        while (sqlite3_step(stmt) == SQLITE_ROW)
        {
            int movieId = sqlite3_column_int(stmt, 0);
            printf("Movie ID: %d\n", movieId);
        }
    }
    else
    {
        fprintf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
    }
}

int addToCart(sqlite3 *db, const char *username, int movieId)
{
    const char *sql = "INSERT INTO cart (username, movieId) VALUES (?, ?)";
    sqlite3_stmt *stmt;

    printf("Chiamata addToCart\n");

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        sqlite3_bind_int(stmt, 2, movieId);

        if (sqlite3_step(stmt) != SQLITE_DONE)
        {
            fprintf(stderr, "Error adding to cart: %s\n", sqlite3_errmsg(db));
            sqlite3_finalize(stmt);
            return -1; // errore
        }
        else
        {
            printf("Movie added to cart successfully.\n");
            sqlite3_finalize(stmt);
            return 0; // successo
        }
    }
    else
    {
        fprintf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
        return 1; // errore
    }
}

int deleteFromCart(sqlite3 *db, const char *username, int movieid)
{
    const char *sql = "DELETE FROM cart WHERE username = ? AND movieid = ?";
    sqlite3_stmt *stmt;
    int rc;
    int rows_affected = 0;
    // printf("Entered DelFromC\n");
    printf("Deleting id %d for user %s\n", movieid, username);
    // Inizia una transazione esplicita
    rc = sqlite3_exec(db, "BEGIN TRANSACTION", 0, 0, 0);
    if (rc != SQLITE_OK)
    {
        fprintf(stderr, "Error beginning transaction: %s\n", sqlite3_errmsg(db));
        printf("-1\n");
        return -1;
    }

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        sqlite3_bind_int(stmt, 2, movieid);

        rc = sqlite3_step(stmt);
        if (rc == SQLITE_DONE)
        {
            rows_affected = sqlite3_changes(db);
            printf("Deleted %d rows from cart\n", rows_affected);

            // Commit della transazione
            rc = sqlite3_exec(db, "COMMIT", 0, 0, 0);
            if (rc != SQLITE_OK)
            {
                fprintf(stderr, "Error committing transaction: %s\n", sqlite3_errmsg(db));
                sqlite3_finalize(stmt);
                printf("-1\n");
                return -1;
            }
        }
        else
        {
            fprintf(stderr, "Error deleting from cart: %s\n", sqlite3_errmsg(db));
            // Rollback in caso di errore
            sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
            sqlite3_finalize(stmt);
            printf("-1");
            return -1;
        }
        sqlite3_finalize(stmt);
        printf("%d\n", (rows_affected > 0) ? 0 : 1);
        return (rows_affected > 0) ? 0 : 1; // 0=success, 1=no rows deleted
    }
    else
    {
        fprintf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
        sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
        printf("-1");
        return -1;
    }
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
    // ricezione del messaggio dal client
    read_line(client_fd, buffer, sizeof(buffer));
    // read(client_fd, buffer, sizeof(buffer));
    printf("Messaggio ricevuto dal client: %s\n", buffer);

    int scelta = atoi(buffer);
    printf("Scelta: %d\n", scelta);

    // TODO: Questo deve diventare uno switch ma ho paura che se lo tocco si scassa tutto

    if (scelta == 1)
    {
        // Registrazione utente
        char username[50], password[50]; // Assolutamente non sicuro. C'è un'alternativa migliore?
        memset(username, 0, sizeof(username));
        memset(password, 0, sizeof(password));
        read_line(client_fd, username, sizeof(username));
        read_line(client_fd, password, sizeof(password));
        registerUser(db, username, password);
    }
    else if (scelta == 2)
    {
        // Autenticazione utente
        char username[50], password[50];
        memset(username, 0, sizeof(username));
        memset(password, 0, sizeof(password));
        read_line(client_fd, username, sizeof(username));
        read_line(client_fd, password, sizeof(password));

        if (authenticateUser(db, username, password) == 1)
        {
            write(client_fd, "Login riuscito!\n", strlen("Login riuscito!\n"));
        }
        else
        {
            write(client_fd, "Login fallito.\n", strlen("Login fallito.\n"));
        }
    }
    else if (scelta == 3) // aggiunta al carrello
    {
        char username[100];
        char id_film_str[10];
        int id_film;

        memset(username, 0, sizeof(username));
        memset(id_film_str, 0, sizeof(id_film_str));

        // Leggi l'username dal client
        if (read_line(client_fd, username, sizeof(username)) <= 0)
        {
            printf("Errore durante la lettura dell'username.\n");
            return;
        }

        // Leggi l'ID del film
        if (read_line(client_fd, id_film_str, sizeof(id_film_str)) <= 0)
        {
            printf("Errore durante la lettura dell'id_film.\n");
            return;
        }

        // Converti l'ID del film in intero
        id_film = atoi(id_film_str);

        // Debug
        printf("Aggiunta al carrello richiesta da: %s per il film ID: %d\n", username, id_film);

        // Aggiungi al carrello nel database
        if (addToCart(db, username, id_film) == 0)
        {
            write(client_fd, "Film aggiunto al carrello con successo.\n", 40);
        }
        else
        {
            write(client_fd, "Errore nell'aggiunta al carrello.\n", 35);
        }
    }

    else if (scelta == 4) // cancellazione carrello
    {
        char username[100];
        char id_film_str[10];
        int id_film;

        memset(username, 0, sizeof(username));
        memset(id_film_str, 0, sizeof(id_film_str));

        // Leggi l'username dal client
        if (read_line(client_fd, username, sizeof(username)) <= 0)
        {
            printf("Errore durante la lettura dell'username.\n");
            return;
        }

        // Leggi l'ID del film
        if (read_line(client_fd, id_film_str, sizeof(id_film_str)) <= 0)
        {
            printf("Errore durante la lettura dell'id_film.\n");
            return;
        }

        // Converti l'ID del film in intero
        id_film = atoi(id_film_str);

        // Debug
        printf("\nCancellazione dal carrello richiesta da: %s\n per il film ID: %d\n", username, id_film);

        // Aggiungi al carrello nel database
        if (deleteFromCart(db, username, id_film) == 0)
        {
            printf("%s %d", db, username, id_film);
            write(client_fd, "Film rimosso dal carrello con successo.\n", strlen("Film rimosso dal carrello con successo.\n"));
        }
        else
        {
            write(client_fd, "Errore nella rimozione dal carrello.\n", strlen("Errore nella rimozione dal carrello.\n"));
        }
    }

    else if (scelta == 5) // visualizzazione carrello
    {
        char username[100];
        memset(username, 0, sizeof(username));

        if (read_line(client_fd, username, sizeof(username)) <= 0)
        {
            printf("Errore durante la lettura dell'username.\n");
            return;
        }

        printf("Richiesta visualizzazione carrello per: %s\n", username);

        sqlite3_stmt *stmt;
        const char *sql = "SELECT m.id, m.title, m.genre, m.duration, m.availableCopies "
                          "FROM movies AS m "
                          "JOIN cart AS c ON c.movieId = m.id "
                          "WHERE c.username = ?";

        if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) != SQLITE_OK)
        {
            fprintf(stderr, "Errore nella preparazione della query: %s\n", sqlite3_errmsg(db));
            write(client_fd, "ERROR\n", 6);
            return;
        }

        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);

        while (sqlite3_step(stmt) == SQLITE_ROW)
        {
            int id = sqlite3_column_int(stmt, 0);
            const unsigned char *title = sqlite3_column_text(stmt, 1);
            const unsigned char *genre = sqlite3_column_text(stmt, 2);
            int duration = sqlite3_column_int(stmt, 3);
            int copies = sqlite3_column_int(stmt, 4);

            char row[512];
            snprintf(row, sizeof(row), "%d|%s|%s|%d|%d\n", id, title, genre, duration, copies);
            write(client_fd, row, strlen(row));
        }

        sqlite3_finalize(stmt);

        // Fine dati
        write(client_fd, "END_OF_CART\n", strlen("END_OF_CART\n"));
    }
    else if (scelta == 6) // aggiunta ai noleggi
    {                     // bisogna modulizzare in una funzione, è un casino da leggere

        char username[100];
        int nrows = 0;
        char buffer[32];
        char movie_id[4];
        char date[20];
        char exp_date[20];

        memset(username, 0, sizeof(username));
        memset(buffer, 0, sizeof(buffer));
        memset(movie_id, 0, sizeof(movie_id));
        memset(date, 0, sizeof(date));
        memset(exp_date, 0, sizeof(exp_date));

        time_t t = time(NULL);
        struct tm tm = *localtime(&t);
        strftime(date, sizeof(date), "%Y-%m-%d", &tm); // Format: "YYYY-MM-DD"
        get_expiration_date(exp_date, sizeof(exp_date));

        if (read_line(client_fd, username, sizeof(username)) <= 0)
        {
            printf("Errore durante la lettura dell'username.\n");
            return;
        }

        printf("DEBUG → Username ricevuto: '%s'\n", username);

        // Leggi l'ID del film
        if (read_line(client_fd, buffer, sizeof(buffer)) <= 0)
        {
            printf("Errore durante la lettura dei numeri di righe.\n");
            return;
        }

        nrows = atoi(buffer);
        printf("%d\n", nrows);

        for (int i = 0; i < nrows; i++)
        {
            if (read_line(client_fd, movie_id, sizeof(movie_id)) <= 0)
            {
                printf("Errore durante la lettura degli id.\n");
                return;
            }

            int id = atoi(movie_id);

            sqlite3_stmt *stmt;
            printf("DEBUG → Inserisco noleggio: user='%s', movieId=%d, date=%s, return=%s\n", username, id, date, exp_date);

            const char *sql = "INSERT INTO rentals (username, movieId, rentaldate, returndate) VALUES (?, ?, ?, ?)";

            if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) != SQLITE_OK)
            {
                fprintf(stderr, "Failed to prepare statement: %s\n", sqlite3_errmsg(db));
                return;
            }

            sqlite3_bind_text(stmt, 1, username, -1, SQLITE_TRANSIENT); // Bind username al primo ?
            sqlite3_bind_int(stmt, 2, id);                              // Bind id al secondo ?
            sqlite3_bind_text(stmt, 3, date, -1, SQLITE_TRANSIENT);     // Terzo ?
            sqlite3_bind_text(stmt, 4, exp_date, -1, SQLITE_TRANSIENT); // Quarto ?

            if (sqlite3_step(stmt) != SQLITE_DONE)
            {
                fprintf(stderr, "Execution failed: %s\n", sqlite3_errmsg(db));
                return;
            }

            // printf("Aggiunto %d ai rentals con successo.\n", id);
            write(client_fd, "Film aggiunto al carrello con successo.\n", 40);
            sqlite3_finalize(stmt);
        }

        sqlite3_stmt *stmt_clear;
        const char *sql_clear = "DELETE FROM cart WHERE USERNAME = ?";

        if (sqlite3_prepare_v2(db, sql_clear, -1, &stmt_clear, NULL) != SQLITE_OK)
        {
            fprintf(stderr, "Failed to prepare statement: %s\n", sqlite3_errmsg(db));
            return;
        }

        sqlite3_bind_text(stmt_clear, 1, username, -1, SQLITE_TRANSIENT);

        if (sqlite3_step(stmt_clear) != SQLITE_DONE)
        {
            fprintf(stderr, "Execution failed: %s\n", sqlite3_errmsg(db));
        }
    }

    else if (scelta == 7) // visualizzazione noleggi
    {
        char username[100];

        memset(username, 0, sizeof(username));

        if (read_line(client_fd, username, sizeof(username)) <= 0)
        {
            printf("Errore durante la lettura dell'username.\n");
            return;
        }

        printf("Richiesta visualizzazione noleggi per: %s\n", username);

        sqlite3_stmt *stmt;
        const char *sql = "SELECT m.id, m.title, m.genre, m.duration, r.rentaldate, r.returndate "
                          "FROM movies AS m "
                          "JOIN rentals AS r ON r.movieId = m.id "
                          "WHERE r.username = ?";

        if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) != SQLITE_OK)
        {
            fprintf(stderr, "Errore nella preparazione della query: %s\n", sqlite3_errmsg(db));
            write(client_fd, "ERROR\n", 6);
            return;
        }

        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);

        while (sqlite3_step(stmt) == SQLITE_ROW)
        {
            int id = sqlite3_column_int(stmt, 0);
            const unsigned char *title = sqlite3_column_text(stmt, 1);
            const unsigned char *genre = sqlite3_column_text(stmt, 2);
            int duration = sqlite3_column_int(stmt, 3);
            const unsigned char *rentalDate = sqlite3_column_text(stmt, 4);
            const unsigned char *expirationDate = sqlite3_column_text(stmt, 5);
            // paura: la data è formattata nello stesso modo fra come noi la segniamo e come Java la mette in LocalDate?

            printf("%s data - %s scadenza", rentalDate, expirationDate);
            char row[1024];
            memset(row, 0, sizeof(row));
            snprintf(row, sizeof(row), "%d|%s|%s|%d|%s|%s\n", id, title, genre, duration, rentalDate, expirationDate);
            write(client_fd, row, strlen(row));
        }

        sqlite3_finalize(stmt);

        // Fine dati
        write(client_fd, "END_OF_CART\n", strlen("END_OF_CART\n"));
    }

    else if (scelta == 8)
    {
        char username[100];
        char movieIdStr[20];
        int movieId;

        // Read username
        if (read_line(client_fd, username, sizeof(username)) <= 0)
        {
            write(client_fd, "ERROR: Missing username\n", 23);
            return;
        }

        // Read movie ID
        if (read_line(client_fd, movieIdStr, sizeof(movieIdStr)) <= 0)
        {
            write(client_fd, "ERROR: Missing movie ID\n", 24);
            return;
        }

        movieId = atoi(movieIdStr);

        // Start transaction
        sqlite3_exec(db, "BEGIN TRANSACTION", 0, 0, 0);

        // Delete from rentals
        const char *deleteSql = "DELETE FROM rentals WHERE username = ? AND movieId = ?";
        sqlite3_stmt *deleteStmt;

        if (sqlite3_prepare_v2(db, deleteSql, -1, &deleteStmt, 0) == SQLITE_OK)
        {
            sqlite3_bind_text(deleteStmt, 1, username, -1, SQLITE_STATIC);
            sqlite3_bind_int(deleteStmt, 2, movieId);

            if (sqlite3_step(deleteStmt) != SQLITE_DONE)
            {
                fprintf(stderr, "Delete failed: %s\n", sqlite3_errmsg(db));
                write(client_fd, "ERROR: Delete failed\n", 20);
                sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
            }
            else
            {
                // Update available copies
                const char *updateSql = "UPDATE movies SET availableCopies = availableCopies + 1 WHERE id = ?";
                sqlite3_stmt *updateStmt;

                if (sqlite3_prepare_v2(db, updateSql, -1, &updateStmt, 0) == SQLITE_OK)
                {
                    sqlite3_bind_int(updateStmt, 1, movieId);

                    if (sqlite3_step(updateStmt) == SQLITE_DONE)
                    {
                        sqlite3_exec(db, "COMMIT", 0, 0, 0);
                        write(client_fd, "SUCCESS\n", 8);
                    }
                    else
                    {
                        fprintf(stderr, "Update failed: %s\n", sqlite3_errmsg(db));
                        write(client_fd, "ERROR: Update failed\n", 21);
                        sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
                    }
                    sqlite3_finalize(updateStmt);
                }
            }
            sqlite3_finalize(deleteStmt);
        }
        else
        {
            fprintf(stderr, "Prepare failed: %s\n", sqlite3_errmsg(db));
            write(client_fd, "ERROR: Prepare failed\n", 22);
            sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
        }
    }
    else if (scelta == 9) // login admin
    {
        printf("Handling movies request\n");
        char username[50], password[50];
        memset(username, 0, sizeof(username));
        memset(password, 0, sizeof(password));
        read_line(client_fd, username, sizeof(username));
        read_line(client_fd, password, sizeof(password));

        if (authenticateAdmin(db, username, password) == 1)
        {
            write(client_fd, "Login riuscito!\n", strlen("Login riuscito!\n"));
        }
        else
        {
            write(client_fd, "Login fallito.\n", strlen("Login fallito.\n"));
        }
    }
    else if (scelta == 10)
    { // film
        sqlite3_stmt *stmt;
        const char *sql = "SELECT id, title, genre, duration, availableCopies, totalCopies FROM movies";

        if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK)
        {
            while (sqlite3_step(stmt) == SQLITE_ROW)
            {
                char row[512];
                snprintf(row, sizeof(row), "%d|%s|%s|%d|%d|%d\n",
                         sqlite3_column_int(stmt, 0),
                         sqlite3_column_text(stmt, 1),
                         sqlite3_column_text(stmt, 2),
                         sqlite3_column_int(stmt, 3),
                         sqlite3_column_int(stmt, 4),
                         sqlite3_column_int(stmt, 5));
                write(client_fd, row, strlen(row));
            }

            write(client_fd, "END_OF_DATA\n", 12);
        }
        sqlite3_finalize(stmt);
    }
    else if (scelta == 11)
    {
        sqlite3_stmt *stmt;
        const char *sql = "SELECT r.movieId, m.title, r.username, r.rentaldate, r.returndate FROM rentals r JOIN movies m ON r.movieId = m.id";

        if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK)
        {
            // First send count
            int count = 0;
            while (sqlite3_step(stmt) == SQLITE_ROW)
                count++;
            dprintf(client_fd, "COUNT:%d\n", count);
            sqlite3_reset(stmt);

            // Send each column separately
            while (sqlite3_step(stmt) == SQLITE_ROW)
            {
                dprintf(client_fd, "MOVIEID:%d\n", sqlite3_column_int(stmt, 0));
                dprintf(client_fd, "TITLE:%s\n", sqlite3_column_text(stmt, 1)); // Send the movie title
                printf("DEBUG - Sending title: %s\n", sqlite3_column_text(stmt, 1));
                dprintf(client_fd, "USERNAME:%s\n", sqlite3_column_text(stmt, 2));
                dprintf(client_fd, "RENTALDATE:%s\n", sqlite3_column_text(stmt, 3));
                dprintf(client_fd, "RETURNDATE:%s\n", sqlite3_column_text(stmt, 4));
                dprintf(client_fd, "----\n"); // Record separator
            }
            write(client_fd, "END\n", 4);
        }
        sqlite3_finalize(stmt);
    }

    else if (scelta == 12)
    { // Add new movie (admin)
        char title[100], genre[50];
        char durationStr[10], totalCopiesStr[10];
        int duration, totalCopies;

        printf("DEBUG: Received request to add new movie\n");

        // Read movie data from client
        if (read_line(client_fd, title, sizeof(title)) <= 0)
        {
            printf("ERROR: Failed to read title\n");
            write(client_fd, "ERROR: Failed to read title\n", 28);
            return;
        }
        if (read_line(client_fd, genre, sizeof(genre)) <= 0)
        {
            printf("ERROR: Failed to read genre\n");
            write(client_fd, "ERROR: Failed to read genre\n", 28);
            return;
        }
        if (read_line(client_fd, durationStr, sizeof(durationStr)) <= 0)
        {
            printf("ERROR: Failed to read duration\n");
            write(client_fd, "ERROR: Failed to read duration\n", 30);
            return;
        }
        if (read_line(client_fd, totalCopiesStr, sizeof(totalCopiesStr)) <= 0)
        {
            printf("ERROR: Failed to read total copies\n");
            write(client_fd, "ERROR: Failed to read total copies\n", 34);
            return;
        }

        printf("DEBUG: Received data - Title: %s, Genre: %s, Duration: %s, Copies: %s\n",
               title, genre, durationStr, totalCopiesStr);

        duration = atoi(durationStr);
        totalCopies = atoi(totalCopiesStr);

        // Prepare SQL statement
        const char *sql = "INSERT INTO movies (title, genre, duration, totalCopies, availableCopies) VALUES (?, ?, ?, ?, ?)";
        sqlite3_stmt *stmt;

        printf("DEBUG: Preparing SQL statement\n");
        if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
        {
            printf("DEBUG: Binding parameters\n");
            sqlite3_bind_text(stmt, 1, title, -1, SQLITE_STATIC);
            sqlite3_bind_text(stmt, 2, genre, -1, SQLITE_STATIC);
            sqlite3_bind_int(stmt, 3, duration);
            sqlite3_bind_int(stmt, 4, totalCopies);
            sqlite3_bind_int(stmt, 5, totalCopies);

            printf("DEBUG: Executing statement\n");
            int rc = sqlite3_step(stmt);
            if (rc == SQLITE_DONE)
            {
                printf("DEBUG: Movie added, committing transaction\n");
                sqlite3_exec(db, "COMMIT", 0, 0, 0); // Commit transaction
                write(client_fd, "SUCCESS\n", 8);
            }
            else
            {
                printf("ERROR: Failed to execute statement: %s\n", sqlite3_errmsg(db));
                sqlite3_exec(db, "ROLLBACK", 0, 0, 0); // Rollback on error
                write(client_fd, "ERROR: Failed to add movie\n", 26);
            }
            sqlite3_finalize(stmt);
        }
        else
        {
            sqlite3_exec(db, "ROLLBACK", 0, 0, 0); // Rollback on error
            printf("ERROR: Failed to prepare statement: %s\n", sqlite3_errmsg(db));
            write(client_fd, "ERROR: Database error\n", 22);
        }
    }

    sleep(1);
    close(client_fd);
    printf("Client fd chiuso\n");
    pthread_exit(NULL);
}

int main()
{
    setupDatabase();
    int fd1;
    struct sockaddr_in server_address, client_address;
    socklen_t client_len = sizeof(client_address);

    // creazione della socket
    if ((fd1 = socket(AF_INET, SOCK_STREAM, 0)) < 0)
    {
        perror("Failed to create socket...\n");
        exit(1);
    }

    // inizializzazione della struttura sockaddr_in
    // bzero(&server_address, sizeof(server_address));
    server_address.sin_family = AF_INET;                // IPv4
    server_address.sin_addr.s_addr = htons(INADDR_ANY); // accetta fg da qualsiasi indirizzo
    server_address.sin_port = htons(8080);              // porta 80

    // binding socket
    if ((bind(fd1, &server_address, sizeof(server_address))) < 0)
    {
        perror("Failed to bind socket...\n");
        exit(1);
    }

    // listening
    if ((listen(fd1, BACKLOG)) != 0)
    {
        perror("Failed to start listening...\n");
        exit(1);
    }

    printf("====SERVER READY====\n");

    while (1)
    {
        printf("====WAITING FOR CONNECTION====\n");
        int *fd2 = malloc(sizeof(int));
        if ((*fd2 = accept(fd1, (struct sockaddr *)&client_address, &client_len)) < 0)
        {
            perror("Failed to accept connection...\n");
            free(fd2);
            continue;
        }

        pthread_t tid;
        if (pthread_create(&tid, NULL, gestione_client, fd2) != 0)
        {
            perror("Failed to create thread...\n");
            close(*fd2);
            free(fd2);
        }
        pthread_detach(tid);
    }
    close(fd1);
    sqlite3_close(db);
    return 0;
}
