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

#define BACKLOG 10



void *gestione_client(void *arg)
{
    int client_fd = *((int *)arg);
    memset(arg, 0, sizeof(arg));

    char buffer[1024];
    bzero(buffer, sizeof(buffer));

    char *msg = "Benvenuto alla videoteca!\n";
    write(client_fd, msg, strlen(msg));

    //ricezione del messaggio dal client
    read(client_fd, buffer, sizeof(buffer));
    printf("Messaggio ricevuto dal client: %s\n", buffer);

    close(client_fd);
    pthread_exit(NULL);
}

int main()
{
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
