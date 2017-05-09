import java.io.OutputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public final class EchoServer {

    public static void main(String[] args) throws Exception
    {
        try(ServerSocket serverSocket = new ServerSocket(22222)) {
          while(true)
          {
            Socket socket = serverSocket.accept();
            Runnable echoer = () -> {
              // welcome
              try {
                String address = socket.getInetAddress().getHostAddress();
                System.out.printf("Client connected: %s%n", address);
                // all the streams we need
                OutputStream os = socket.getOutputStream();
                PrintStream out = new PrintStream(os, true, "UTF-8");
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                do
                {
                  // grab the data from the client
                  line = br.readLine();
                  // send the data back to the client
                  if(line!=null)
                    out.println(line);
                  if(line==null)
                    break;
                } while(!line.equals("exit"));
                System.out.printf("Client disconnected: %s%n", address);
                socket.close();
              }
              catch(IOException e) {
                e.printStackTrace();
              }
            };
            Thread echoerThread = new Thread(echoer);
            echoerThread.start();
          }
        }
    }
}
