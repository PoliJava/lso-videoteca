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

            String risposta = input.readLine();
            System.out.println("Server: " + risposta);

            output.println("Ciao server, sono il client!");

            //qui da aggiungere logica login, prestiti ecc.
        } catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
