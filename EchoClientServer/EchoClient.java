import java.io.OutputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;

public final class EchoClient {

    public static void main(String[] args) throws Exception
    {
        try (Socket socket = new Socket("localhost", 22222))
        {
          // all the streams we need
          OutputStream os = socket.getOutputStream();
          PrintStream out = new PrintStream(os, true, "UTF-8");
          InputStream is = socket.getInputStream();
          InputStreamReader isr = new InputStreamReader(is, "UTF-8");
          BufferedReader br = new BufferedReader(isr);
          BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
          String line = null;
          do
          {
            System.out.print("Client> ");
            line = console.readLine();
            out.println(line);
            String echo = br.readLine();
            System.out.println("Server> " + echo);
          } while(!line.equals("exit"))
          System.exit(0);
        }
    }
}
