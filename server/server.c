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
pthread_mutex_t db_mutex = PTHREAD_MUTEX_INITIALIZER;

void get_expiration_date(char *buffer, size_t size)
{
    time_t t = time(NULL);
    struct tm tm = *localtime(&t);

    tm.tm_mon += 1;
    mktime(&tm);
    strftime(buffer, size, "%Y-%m-%d", &tm);
}

void registerUser(sqlite3 *db, const char *username, const char *password)
{
    pthread_mutex_lock(&db_mutex);
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
    pthread_mutex_unlock(&db_mutex);
}

int authenticateUser(sqlite3 *db, const char *username, const char *password)
{
    pthread_mutex_lock(&db_mutex);
    const char *sql = "SELECT 1 FROM users WHERE username = ? AND password = ?";
    sqlite3_stmt *stmt;
    int result = 0;

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 2, password, -1, SQLITE_STATIC);

        int stepResult = sqlite3_step(stmt);
        if (stepResult == SQLITE_ROW)
        {
            result = 1;
        }
    }
    else
    {
        fprintf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    pthread_mutex_unlock(&db_mutex);
    return result;
}

int authenticateAdmin(sqlite3 *db, const char *username, const char *password)
{
    pthread_mutex_lock(&db_mutex);
    const char *sql = "SELECT 1 FROM admins WHERE username = ? AND password = ?";
    sqlite3_stmt *stmt;
    int result = 0;

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 2, password, -1, SQLITE_STATIC);

        int stepResult = sqlite3_step(stmt);
        if (stepResult == SQLITE_ROW)
        {
            result = 1;
        }
    }
    else
    {
        fprintf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    pthread_mutex_unlock(&db_mutex);
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
            break;
        }
        else
        {
            return -1;
        }
    }
    if (i > 0 && buffer[i - 1] == '\r')
    {
        i--;
    }
    buffer[i] = '\0';
    return i;
}

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
    pthread_mutex_lock(&db_mutex);
    const char *sql = "SELECT id, title, genre, duration, totalCopies, availableCopies FROM movies";
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
    pthread_mutex_unlock(&db_mutex);
}

void rentMovie(sqlite3 *db, int movieId, const char *username, const char *rentalDate, const char *returnDate)
{
    pthread_mutex_lock(&db_mutex);
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
    pthread_mutex_unlock(&db_mutex);
}

void returnMovie(sqlite3 *db, int movieId, const char *username)
{
    pthread_mutex_lock(&db_mutex);
    const char *sql = "DELETE FROM rentals WHERE username = ? AND movieId = ?";
    sqlite3_stmt *stmt;
    int rc;
    int rows_affected = 0;
    rc = sqlite3_exec(db, "BEGIN TRANSACTION", 0, 0, 0);
    if (rc != SQLITE_OK)
    {
        fprintf(stderr, "Error beginning transaction: %s\n", sqlite3_errmsg(db));
        pthread_mutex_unlock(&db_mutex);
        return;
    }

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        sqlite3_bind_int(stmt, 2, movieId);

        rc = sqlite3_step(stmt);
        if (rc == SQLITE_DONE)
        {
            rows_affected = sqlite3_changes(db);

            rc = sqlite3_exec(db, "COMMIT", 0, 0, 0);
            if (rc != SQLITE_OK)
            {
                fprintf(stderr, "Error committing transaction: %s\n", sqlite3_errmsg(db));
                sqlite3_finalize(stmt);
                pthread_mutex_unlock(&db_mutex);
                return;
            }
        }
        else
        {
            fprintf(stderr, "Error deleting from cart: %s\n", sqlite3_errmsg(db));
            sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
            sqlite3_finalize(stmt);
            pthread_mutex_unlock(&db_mutex);
            return;
        }
        sqlite3_finalize(stmt);
        pthread_mutex_unlock(&db_mutex);
        return;
    }
    else
    {
        fprintf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
        sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
        pthread_mutex_unlock(&db_mutex);
        return;
    }
}

void getCart(sqlite3 *db, const char *username)
{
    pthread_mutex_lock(&db_mutex);
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
    sqlite3_finalize(stmt);
    pthread_mutex_unlock(&db_mutex);
}

int addToCart(sqlite3 *db, const char *username, int movieId)
{
    pthread_mutex_lock(&db_mutex);
    const char *sql = "INSERT INTO cart (username, movieId) VALUES (?, ?)";
    sqlite3_stmt *stmt;
    int result = -1;

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK)
    {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        sqlite3_bind_int(stmt, 2, movieId);

        if (sqlite3_step(stmt) != SQLITE_DONE)
        {
            fprintf(stderr, "Error adding to cart: %s\n", sqlite3_errmsg(db));
            result = -1;
        }
        else
        {
            printf("Movie added to cart successfully.\n");
            result = 0;
        }
    }
    else
    {
        fprintf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
        result = 1;
    }
    sqlite3_finalize(stmt);
    pthread_mutex_unlock(&db_mutex);
    return result;
}

int deleteFromCart(sqlite3 *db, const char *username, int movieid)
{
    pthread_mutex_lock(&db_mutex);
    const char *sql = "DELETE FROM cart WHERE username = ? AND movieid = ?";
    sqlite3_stmt *stmt;
    int rc;
    int rows_affected = 0;
    int result = -1;

    rc = sqlite3_exec(db, "BEGIN TRANSACTION", 0, 0, 0);
    if (rc != SQLITE_OK)
    {
        fprintf(stderr, "Error beginning transaction: %s\n", sqlite3_errmsg(db));
        pthread_mutex_unlock(&db_mutex);
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
            rc = sqlite3_exec(db, "COMMIT", 0, 0, 0);
            if (rc != SQLITE_OK)
            {
                fprintf(stderr, "Error committing transaction: %s\n", sqlite3_errmsg(db));
                result = -1;
            }
            else
            {
                result = (rows_affected > 0) ? 0 : 1;
            }
        }
        else
        {
            fprintf(stderr, "Error deleting from cart: %s\n", sqlite3_errmsg(db));
            sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
            result = -1;
        }
        sqlite3_finalize(stmt);
    }
    else
    {
        fprintf(stderr, "Error preparing statement: %s\n", sqlite3_errmsg(db));
        sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
        result = -1;
    }
    pthread_mutex_unlock(&db_mutex);
    return result;
}

static void viewCart(int client_fd, const char *username) {
    sqlite3_stmt *stmt;
    const char *sql = "SELECT m.id, m.title, m.genre, m.duration, m.availableCopies "
                      "FROM movies AS m "
                      "JOIN cart AS c ON c.movieId = m.id "
                      "WHERE c.username = ?";

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) != SQLITE_OK) {
        fprintf(stderr, "Errore nella preparazione della query: %s\n", sqlite3_errmsg(db));
        write(client_fd, "ERROR\n", 6);
        return;
    }

    sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);

    while (sqlite3_step(stmt) == SQLITE_ROW) {
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
    write(client_fd, "END_OF_CART\n", strlen("END_OF_CART\n"));
}

static void rentMovies(int client_fd, const char *username, int nrows) {
    char movie_id[4];
    char date[20];
    char exp_date[20];

    memset(movie_id, 0, sizeof(movie_id));
    memset(date, 0, sizeof(date));
    memset(exp_date, 0, sizeof(exp_date));

    time_t t = time(NULL);
    struct tm tm = *localtime(&t);
    strftime(date, sizeof(date), "%Y-%m-%d", &tm);
    get_expiration_date(exp_date, sizeof(exp_date));

    for (int i = 0; i < nrows; i++) {
        if (read_line(client_fd, movie_id, sizeof(movie_id)) <= 0) {
            printf("Errore durante la lettura degli id.\n");
            return;
        }

        int id = atoi(movie_id);
        sqlite3_stmt *stmt;
        const char *sql = "INSERT INTO rentals (username, movieId, rentaldate, returndate) VALUES (?, ?, ?, ?)";

        if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) != SQLITE_OK) {
            fprintf(stderr, "Failed to prepare statement: %s\n", sqlite3_errmsg(db));
            return;
        }

        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_TRANSIENT);
        sqlite3_bind_int(stmt, 2, id);
        sqlite3_bind_text(stmt, 3, date, -1, SQLITE_TRANSIENT);
        sqlite3_bind_text(stmt, 4, exp_date, -1, SQLITE_TRANSIENT);

        if (sqlite3_step(stmt) != SQLITE_DONE) {
            fprintf(stderr, "Execution failed: %s\n", sqlite3_errmsg(db));
            sqlite3_finalize(stmt);
            return;
        }
        sqlite3_finalize(stmt);
    }

    sqlite3_stmt *stmt_clear;
    const char *sql_clear = "DELETE FROM cart WHERE USERNAME = ?";
    if (sqlite3_prepare_v2(db, sql_clear, -1, &stmt_clear, NULL) != SQLITE_OK) {
        fprintf(stderr, "Failed to prepare statement: %s\n", sqlite3_errmsg(db));
        return;
    }

    sqlite3_bind_text(stmt_clear, 1, username, -1, SQLITE_TRANSIENT);
    if (sqlite3_step(stmt_clear) != SQLITE_DONE) {
        fprintf(stderr, "Execution failed: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt_clear);
    write(client_fd, "SUCCESS\n", 8);
}

static void viewRentedMovies(int client_fd, const char *username) {
    sqlite3_stmt *stmt;
    const char *sql = "SELECT m.id, m.title, m.genre, m.duration, r.rentaldate, r.returndate "
                      "FROM movies AS m "
                      "JOIN rentals AS r ON r.movieId = m.id "
                      "WHERE r.username = ?";

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) != SQLITE_OK) {
        fprintf(stderr, "Errore nella preparazione della query: %s\n", sqlite3_errmsg(db));
        write(client_fd, "ERROR\n", 6);
        return;
    }

    sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
    while (sqlite3_step(stmt) == SQLITE_ROW) {
        int id = sqlite3_column_int(stmt, 0);
        const unsigned char *title = sqlite3_column_text(stmt, 1);
        const unsigned char *genre = sqlite3_column_text(stmt, 2);
        int duration = sqlite3_column_int(stmt, 3);
        const unsigned char *rentalDate = sqlite3_column_text(stmt, 4);
        const unsigned char *expirationDate = sqlite3_column_text(stmt, 5);

        char row[1024];
        memset(row, 0, sizeof(row));
        snprintf(row, sizeof(row), "%d|%s|%s|%d|%s|%s\n", id, title, genre, duration, rentalDate, expirationDate);
        write(client_fd, row, strlen(row));
    }

    sqlite3_finalize(stmt);
    write(client_fd, "END_OF_CART\n", strlen("END_OF_CART\n"));
}

static void returnMovieHandler(int client_fd, const char *username, int movieId) {
    sqlite3_exec(db, "BEGIN TRANSACTION", 0, 0, 0);

    const char *deleteSql = "DELETE FROM rentals WHERE username = ? AND movieId = ?";
    sqlite3_stmt *deleteStmt;

    if (sqlite3_prepare_v2(db, deleteSql, -1, &deleteStmt, 0) == SQLITE_OK) {
        sqlite3_bind_text(deleteStmt, 1, username, -1, SQLITE_STATIC);
        sqlite3_bind_int(deleteStmt, 2, movieId);

        if (sqlite3_step(deleteStmt) != SQLITE_DONE) {
            fprintf(stderr, "Delete failed: %s\n", sqlite3_errmsg(db));
            write(client_fd, "ERROR: Delete failed\n", 21);
            sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
        } else {
            const char *updateSql = "UPDATE movies SET availableCopies = availableCopies + 1 WHERE id = ?";
            sqlite3_stmt *updateStmt;

            if (sqlite3_prepare_v2(db, updateSql, -1, &updateStmt, 0) == SQLITE_OK) {
                sqlite3_bind_int(updateStmt, 1, movieId);

                if (sqlite3_step(updateStmt) == SQLITE_DONE) {
                    sqlite3_exec(db, "COMMIT", 0, 0, 0);
                    write(client_fd, "SUCCESS\n", 8);
                } else {
                    fprintf(stderr, "Update failed: %s\n", sqlite3_errmsg(db));
                    write(client_fd, "ERROR: Update failed\n", 21);
                    sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
                }
                sqlite3_finalize(updateStmt);
            } else {
                fprintf(stderr, "Prepare update failed: %s\n", sqlite3_errmsg(db));
                write(client_fd, "ERROR: Prepare update failed\n", 28);
                sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
            }
        }
        sqlite3_finalize(deleteStmt);
    } else {
        fprintf(stderr, "Prepare delete failed: %s\n", sqlite3_errmsg(db));
        write(client_fd, "ERROR: Prepare delete failed\n", 28);
        sqlite3_exec(db, "ROLLBACK", 0, 0, 0);
    }
}

static void listMovies(int client_fd) {
    sqlite3_stmt *stmt;
    const char *sql = "SELECT id, title, genre, duration, availableCopies, totalCopies FROM movies";

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK) {
        while (sqlite3_step(stmt) == SQLITE_ROW) {
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

static void listRentals(int client_fd) {
    sqlite3_stmt *stmt;
    const char *sql = "SELECT r.movieId, m.title, r.username, r.rentaldate, r.returndate FROM rentals r JOIN movies m ON r.movieId = m.id";

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK) {
        int count = 0;
        while (sqlite3_step(stmt) == SQLITE_ROW)
            count++;
        dprintf(client_fd, "COUNT:%d\n", count);
        sqlite3_reset(stmt);

        while (sqlite3_step(stmt) == SQLITE_ROW) {
            dprintf(client_fd, "MOVIEID:%d\n", sqlite3_column_int(stmt, 0));
            dprintf(client_fd, "TITLE:%s\n", sqlite3_column_text(stmt, 1));
            dprintf(client_fd, "USERNAME:%s\n", sqlite3_column_text(stmt, 2));
            dprintf(client_fd, "RENTALDATE:%s\n", sqlite3_column_text(stmt, 3));
            dprintf(client_fd, "RETURNDATE:%s\n", sqlite3_column_text(stmt, 4));
            dprintf(client_fd, "----\n");
        }
        write(client_fd, "END\n", 4);
    }
    sqlite3_finalize(stmt);
}

static void addMovie(int client_fd, const char *title, const char *genre, int duration, int totalCopies) {
    const char *sql = "INSERT INTO movies (title, genre, duration, totalCopies, availableCopies) VALUES (?, ?, ?, ?, ?)";
    sqlite3_stmt *stmt;

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK) {
        sqlite3_bind_text(stmt, 1, title, -1, SQLITE_STATIC);
        sqlite3_bind_text(stmt, 2, genre, -1, SQLITE_STATIC);
        sqlite3_bind_int(stmt, 3, duration);
        sqlite3_bind_int(stmt, 4, totalCopies);
        sqlite3_bind_int(stmt, 5, totalCopies);

        int rc = sqlite3_step(stmt);
        if (rc == SQLITE_DONE) {
            write(client_fd, "SUCCESS\n", 8);
        } else {
            write(client_fd, "ERROR: Failed to add movie\n", 26);
        }
        sqlite3_finalize(stmt);
    } else {
        write(client_fd, "ERROR: Database error\n", 22);
    }
}

static void sendMessage(int client_fd, const char *admin_username, const char *username, const char *movie_title, int movie_id, const char *message_content) {
    sqlite3_stmt *stmt;
    const char *sql = "INSERT INTO messages (username, sender, title, movieId, message, timestamp) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK) {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_TRANSIENT);
        sqlite3_bind_text(stmt, 2, admin_username, -1, SQLITE_TRANSIENT);
        sqlite3_bind_text(stmt, 3, movie_title, -1, SQLITE_TRANSIENT);
        sqlite3_bind_int(stmt, 4, movie_id);
        sqlite3_bind_text(stmt, 5, message_content, -1, SQLITE_TRANSIENT);

        if (sqlite3_step(stmt) == SQLITE_DONE) {
            send(client_fd, "SUCCESS\n", 8, 0);
        } else {
            send(client_fd, "ERROR: Failed to insert message\n", 31, 0);
        }
        sqlite3_finalize(stmt);
    } else {
        send(client_fd, "ERROR: Database preparation failed\n", 34, 0);
    }
}

static void viewMessagesAdmin(int client_fd/*, char* adminname*/) {
    sqlite3_stmt *stmt;
    const char *sql = "SELECT title, username, message FROM messages"; //WHERE sender = ?

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK) {
        //sqlite3_bind_text(stmt, 1, adminname, -1, SQLITE_STATIC);
        while (sqlite3_step(stmt) == SQLITE_ROW) {
            const char *title = (const char*)sqlite3_column_text(stmt, 0);
            const char *username = (const char*)sqlite3_column_text(stmt, 1);
            const char *message = (const char*)sqlite3_column_text(stmt, 2);
            dprintf(client_fd, "TITLE:%s\n", title);
            dprintf(client_fd, "USER:%s\n", username);
            dprintf(client_fd, "TEXT:%s\n", message);
        }
        write(client_fd, "END\n", 4);
        sqlite3_finalize(stmt);
    }
}

static void viewMessagesUser(int client_fd, const char *username){
    sqlite3_stmt *stmt;
    const char *sql = "SELECT m.title, m.sender, m.username, m.message, r.returndate FROM messages as m JOIN rentals as r ON m.movieId = r.movieId AND m.username = r.username WHERE m.username = ?";

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK) {
        sqlite3_bind_text(stmt, 1, username, -1, SQLITE_STATIC);
        while (sqlite3_step(stmt) == SQLITE_ROW) {
            const char *title = (const char*)sqlite3_column_text(stmt, 0);
            const char *sender = (const char*)sqlite3_column_text(stmt, 1);
            const char *username = (const char*)sqlite3_column_text(stmt, 2);
            const char *message = (const char*)sqlite3_column_text(stmt, 3);
            const char *returndate = (const char*)sqlite3_column_text(stmt, 4);
            dprintf(client_fd, "TITLE:%s\n", title);
            dprintf(client_fd, "SENDER:%s\n", sender);
            dprintf(client_fd, "USER:%s\n", username);
            dprintf(client_fd, "TEXT:%s\n", message);
            dprintf(client_fd, "EXPIRE:%s\n", returndate);
        }
        write(client_fd, "END\n", 4);
        sqlite3_finalize(stmt);
    }
}

void *gestione_client(void *arg)
{
    int client_fd = *((int *)arg);
    free(arg);
    struct Movie *movies = malloc(100 * sizeof(struct Movie));
    int num_film = 0;
    loadMovies(&movies, &num_film);

    char buffer[1024];
    bzero(buffer, sizeof(buffer));
    if (read_line(client_fd, buffer, sizeof(buffer)) <= 0) {
        perror("Failed to read choice\n");
        goto cleanup;
    }
    int scelta = atoi(buffer);

    switch (scelta) {
        case 1: {
            char username[50], password[50];
            memset(username, 0, sizeof(username));
            memset(password, 0, sizeof(password));
            if (read_line(client_fd, username, sizeof(username)) <= 0 ||
                read_line(client_fd, password, sizeof(password)) <= 0) {
                perror("Failed to read user registration data\n");
                goto cleanup;
            }
            registerUser(db, username, password);
            break;
        }
        case 2: {
            char username[50], password[50];
            memset(username, 0, sizeof(username));
            memset(password, 0, sizeof(password));
            if (read_line(client_fd, username, sizeof(username)) <= 0 ||
                read_line(client_fd, password, sizeof(password)) <= 0) {
                perror("Failed to read user login data\n");
                goto cleanup;
            }

            if (authenticateUser(db, username, password) == 1) {
                write(client_fd, "Login riuscito!\n", strlen("Login riuscito!\n"));
            } else {
                write(client_fd, "Login fallito.\n", strlen("Login fallito.\n"));
            }
            break;
        }
        case 3: {
            char username[100];
            char id_film_str[10];
            int id_film;

            memset(username, 0, sizeof(username));
            memset(id_film_str, 0, sizeof(id_film_str));

            if (read_line(client_fd, username, sizeof(username)) <= 0) {
                printf("Errore durante la lettura dell'username.\n");
                goto cleanup;
            }

            if (read_line(client_fd, id_film_str, sizeof(id_film_str)) <= 0) {
                printf("Errore durante la lettura dell'id_film.\n");
                goto cleanup;
            }

            id_film = atoi(id_film_str);
            int result = addToCart(db, username, id_film);
            if (result == 0) {
                write(client_fd, "Film aggiunto al carrello con successo.\n", 40);
            } else {
                write(client_fd, "Errore nell'aggiunta al carrello.\n", 35);
            }
            break;
        }
        case 4: {
            char username[100];
            char id_film_str[10];
            int id_film;

            memset(username, 0, sizeof(username));
            memset(id_film_str, 0, sizeof(id_film_str));

            if (read_line(client_fd, username, sizeof(username)) <= 0) {
                printf("Errore durante la lettura dell'username.\n");
                goto cleanup;
            }

            if (read_line(client_fd, id_film_str, sizeof(id_film_str)) <= 0) {
                printf("Errore durante la lettura dell'id_film.\n");
                goto cleanup;
            }

            id_film = atoi(id_film_str);
            int result = deleteFromCart(db, username, id_film);
            if (result == 0) {
                write(client_fd, "Film rimosso dal carrello con successo.\n", strlen("Film rimosso dal carrello con successo.\n"));
            } else if (result == 1) {
                write(client_fd, "Film non trovato nel carrello.\n", 31);
            } else {
                write(client_fd, "Errore nella rimozione dal carrello.\n", strlen("Errore nella rimozione dal carrello.\n"));
            }
            break;
        }
        case 5: {
            pthread_mutex_lock(&db_mutex);
            char username[100];
            memset(username, 0, sizeof(username));

            if (read_line(client_fd, username, sizeof(username)) <= 0) {
                printf("Errore durante la lettura dell'username.\n");
                pthread_mutex_unlock(&db_mutex);
                goto cleanup;
            }
            viewCart(client_fd, username);
            pthread_mutex_unlock(&db_mutex);
            break;
        }
        case 6: {
            pthread_mutex_lock(&db_mutex);
            char username[100];
            int nrows = 0;
            char buffer[32];

            memset(username, 0, sizeof(username));
            memset(buffer, 0, sizeof(buffer));

            if (read_line(client_fd, username, sizeof(username)) <= 0) {
                printf("Errore durante la lettura dell'username.\n");
                pthread_mutex_unlock(&db_mutex);
                goto cleanup;
            }

            if (read_line(client_fd, buffer, sizeof(buffer)) <= 0) {
                printf("Errore durante la lettura dei numeri di righe.\n");
                pthread_mutex_unlock(&db_mutex);
                goto cleanup;
            }

            nrows = atoi(buffer);
            rentMovies(client_fd, username, nrows);
            pthread_mutex_unlock(&db_mutex);
            break;
        }
        case 7: {
            pthread_mutex_lock(&db_mutex);
            char username[100];
            memset(username, 0, sizeof(username));

            if (read_line(client_fd, username, sizeof(username)) <= 0) {
                printf("Errore durante la lettura dell'username.\n");
                pthread_mutex_unlock(&db_mutex);
                goto cleanup;
            }
            viewRentedMovies(client_fd, username);
            pthread_mutex_unlock(&db_mutex);
            break;
        }
        case 8: {
            pthread_mutex_lock(&db_mutex);
            char username[100];
            char movieIdStr[20];
            int movieId;

            if (read_line(client_fd, username, sizeof(username)) <= 0) {
                write(client_fd, "ERROR: Missing username\n", 24);
                pthread_mutex_unlock(&db_mutex);
                goto cleanup;
            }

            if (read_line(client_fd, movieIdStr, sizeof(movieIdStr)) <= 0) {
                write(client_fd, "ERROR: Missing movie ID\n", 24);
                pthread_mutex_unlock(&db_mutex);
                goto cleanup;
            }

            movieId = atoi(movieIdStr);
            returnMovieHandler(client_fd, username, movieId);
            pthread_mutex_unlock(&db_mutex);
            break;
        }
        case 9: {
            char username[50], password[50];
            memset(username, 0, sizeof(username));
            memset(password, 0, sizeof(password));
            if (read_line(client_fd, username, sizeof(username)) <= 0 ||
                read_line(client_fd, password, sizeof(password)) <= 0) {
                perror("Failed to read admin login data\n");
                goto cleanup;
            }

            if (authenticateAdmin(db, username, password) == 1) {
                write(client_fd, "Login riuscito!\n", strlen("Login riuscito!\n"));
            } else {
                write(client_fd, "Login fallito.\n", strlen("Login fallito.\n"));
            }
            break;
        }
        case 10: {
            pthread_mutex_lock(&db_mutex);
            listMovies(client_fd);
            pthread_mutex_unlock(&db_mutex);
            break;
        }
        case 11: {
            pthread_mutex_lock(&db_mutex);
            listRentals(client_fd);
            pthread_mutex_unlock(&db_mutex);
            break;
        }
        case 12: {
            pthread_mutex_lock(&db_mutex);
            char title[100], genre[50];
            char durationStr[10], totalCopiesStr[10];
            int duration, totalCopies;

            if (read_line(client_fd, title, sizeof(title)) <= 0) {
                write(client_fd, "ERROR: Failed to read title\n", 28);
                pthread_mutex_unlock(&db_mutex);
                goto cleanup;
            }
            if (read_line(client_fd, genre, sizeof(genre)) <= 0) {
                write(client_fd, "ERROR: Failed to read genre\n", 28);
                pthread_mutex_unlock(&db_mutex);
                goto cleanup;
            }
            if (read_line(client_fd, durationStr, sizeof(durationStr)) <= 0) {
                write(client_fd, "ERROR: Failed to read duration\n", 30);
                pthread_mutex_unlock(&db_mutex);
                goto cleanup;
            }
            if (read_line(client_fd, totalCopiesStr, sizeof(totalCopiesStr)) <= 0) {
                write(client_fd, "ERROR: Failed to read total copies\n", 34);
                pthread_mutex_unlock(&db_mutex);
                goto cleanup;
            }

            duration = atoi(durationStr);
            totalCopies = atoi(totalCopiesStr);
            addMovie(client_fd, title, genre, duration, totalCopies);
            pthread_mutex_unlock(&db_mutex);
            break;
        }
        case 13: {
            pthread_mutex_lock(&db_mutex);
            char admin_username[512];
            char username[512];
            char movie_title[512];
            char movie_id_str[4];
            char message_content[4096];

            if (read_line(client_fd, admin_username, sizeof(admin_username)) <= 0 ||
                read_line(client_fd, username, sizeof(username)) <= 0 ||
                read_line(client_fd, movie_title, sizeof(movie_title)) <= 0 ||
                read_line(client_fd, movie_id_str, sizeof(movie_id_str)) <= 0 ||
                read_line(client_fd, message_content, sizeof(message_content)) <= 0) {
                pthread_mutex_unlock(&db_mutex);
                goto cleanup;
            }

            int movie_id = atoi(movie_id_str);
            sendMessage(client_fd, admin_username, username, movie_title, movie_id, message_content);
            pthread_mutex_unlock(&db_mutex);
            break;
        }
        case 14: {
            pthread_mutex_lock(&db_mutex);
            viewMessagesAdmin(client_fd);
            pthread_mutex_unlock(&db_mutex);
            break;
        }
        case 15: {
            pthread_mutex_lock(&db_mutex);
            char username[100];
            memset(username, 0, sizeof(username));

            if (read_line(client_fd, username, sizeof(username)) <= 0) {
                pthread_mutex_unlock(&db_mutex);
                goto cleanup;
            }
            
            viewMessagesUser(client_fd, username);
            pthread_mutex_unlock(&db_mutex);
            break;
            }
        default:
            perror("Invalid input\n");
            break;
    }

cleanup:
    free(movies);
    close(client_fd);
    pthread_exit(NULL);
}

int main()
{
    setupDatabase();
    int fd1;
    struct sockaddr_in server_address, client_address;
    socklen_t client_len = sizeof(client_address);

    if ((fd1 = socket(AF_INET, SOCK_STREAM, 0)) < 0)
    {
        perror("Failed to create socket...\n");
        exit(1);
    }

    server_address.sin_family = AF_INET;
    server_address.sin_addr.s_addr = htons(INADDR_ANY);
    server_address.sin_port = htons(8080);

    if ((bind(fd1, (struct sockaddr*)&server_address, sizeof(server_address))) < 0)
    {
        perror("Failed to bind socket...\n");
        exit(1);
    }

    if ((listen(fd1, BACKLOG)) != 0)
    {
        perror("Failed to start listening...\n");
        exit(1);
    }

    printf("====SERVER READY====\n");

    while (1)
    {
        int *fd2 = malloc(sizeof(int));
        if ((*fd2 = accept(fd1, (struct sockaddr*)&client_address, &client_len)) < 0)
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
    pthread_mutex_destroy(&db_mutex);
    sqlite3_close(db);
    return 0;
}