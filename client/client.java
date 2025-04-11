package client;

import java.io.*;
import java.net.*;

public class client
{
    public static void main(String[] args) throws IOException
    {
        final String SERVER_IP = "127.0.0.1"; //da aggiornare
        final int SERVER_PORT = 8080;

        try(Socket socket = new Socket(SERVER_IP, SERVER_PORT))
        {
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in)); //per prendere l'input dell'utente


            //1. Legge il messaggio di benvenuto
            String risposta = input.readLine();
            System.out.println("Server: " + risposta);
            //2. Manda un messaggio iniziale
            output.println("Ciao server, sono il client!");

            //3. Riceve il menu
            String menuLine;
            while((menuLine = input.readLine()) != null && !menuLine.isEmpty())
            {
                System.out.println(menuLine);
                if(menuLine.contains("Scelta")) break;
            }

            //4. Invia la scelta dell'utente
            String scelta = userInput.readLine();
            output.println(scelta);

            if (scelta.equals("1")) {
                // Registrazione
                System.out.print("Inserisci username: ");
                String username = userInput.readLine();
                System.out.print("Inserisci password: ");
                String password = userInput.readLine();
                output.println(username);
                output.println(password);
                System.out.println("==Risposta del server==");
                String responseLine;
                while ((responseLine = input.readLine()) != null) {
                    System.out.println(responseLine);
                }
            } else if (scelta.equals("2")) {
                // Login
                System.out.print("Inserisci username: ");
                String username = userInput.readLine();
                System.out.print("Inserisci password: ");
                String password = userInput.readLine();
                output.println(username);
                output.println(password);
                System.out.println("==Risposta del server==");
                String responseLine;
                while ((responseLine = input.readLine()) != null) {
                    System.out.println(responseLine);
                }
            }

           //da implementare scelta noleggio film, restituzione film, ancora da implementare visualizza film (da server)

           
        } catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
