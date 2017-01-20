import java.io.OutputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.io.IOException;

public class ChatClient
{
  public static void main(String[] args) throws Exception
  {
      try (Socket socket = new Socket("codebank.xyz", 38001))
      {

        // all the streams we need
        OutputStream os = socket.getOutputStream();
        PrintStream out = new PrintStream(os, true, "UTF-8");
        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        //send username
        System.out.print("username: ");
        String name = console.readLine();
        out.println(name);
        //read messages
        Runnable receiver = () -> {
          String incomingText;
          try
          {
            while(true)
            {
              incomingText = br.readLine();
              if(incomingText!=null)
                System.out.println(incomingText);
            }
          }
          catch(IOException e)
          {
            e.printStackTrace();
          }
        };
        Thread thread = new Thread(receiver);
        thread.start();
        //send messages
        while(true)
        {
          String outgoingText;
          outgoingText = console.readLine();
          out.println(outgoingText);
        }
      }
  }
}
