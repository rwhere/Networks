import java.net.ServerSocket;
import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

public class WebServer {

    public static void main(String[] args) throws Exception {

      try(ServerSocket serverSocket = new ServerSocket(8080)) {
        System.out.println("Listening on port 8080...");
        while(true) {
          try(Socket socket = serverSocket.accept()) {
            //get streams
            OutputStream os = socket.getOutputStream();
            PrintStream out = new PrintStream(os, true, "UTF-8");
            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            //grab the http request & print it to console
            String firstLine = br.readLine();
            System.out.println(firstLine);
            while(br.ready()) {
              System.out.println(br.readLine());
            }
            //grab the path whats requested
            String[] split = firstLine.split(" ");
            String path = split[1];
            //check if the path exist
            File file = new File("." + path);
            if(file.exists() && file.isFile()) {
              //send the success response
              BufferedReader forFile = new BufferedReader(new FileReader(file));
              out.println("HTTP/1.1 200 OK");
              out.println("Content-type: text/html");
              out.println("Content-length: " + file.length() + "\n");
              while(forFile.ready()) {
                out.println(forFile.readLine());
              }
            } else {
              //send the error response
              File notFoundFile = new File("notFound.html");
              BufferedReader notFound = new BufferedReader(new FileReader(notFoundFile));
              out.println("HTTP/1.1 404 Not Found");
              out.println("Content-type: text/html");
              out.println("Content-length: " + notFoundFile.length() + "\n");
              while(notFound.ready()) {
                out.println(notFound.readLine());
              }
            }
          }
        }
      }
    }
}
