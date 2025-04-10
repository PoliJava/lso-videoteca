
#ifndef MODELS_H
#define MODELS_H
#include <stdio.h>
#include <stdlib.h>


struct Movie
{
    u_int32_t id;
    char title[100];
    char genre[50];
    int duration; //in minutes

    int availableCopies;
    int totalCopies;

    void (*movieConstructor)(char *title, char *director, int year, char *genre, int duration, int availableCopies, int totalCopies);
};

struct User
{
    char username[50];
    char password[50];

    void (*userConstructor)(char *username, char *password);
};

struct Rental
{
    u_int32_t movieId;
    char username[50];
    char rentalDate[11]; // YYYY-MM-DD
    char returnDate[11]; // YYYY-MM-DD

    void (*rentalConstructor)(u_int32_t movieId, char *username, char *rentalDate, char *returnDate);
};

#endif