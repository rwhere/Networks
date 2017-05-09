import java.io.OutputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.zip.CRC32;

public class Ex2Client
{
  public static void main(String[] args) throws Exception
  {
    try (Socket socket = new Socket("codebank.xyz", 38102))
    {
      String address = socket.getInetAddress().getHostAddress();
      System.out.println("Connected to server: " + address);

      InputStream is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();

      byte[] b = new byte[200];
      byte[] ret = new byte[100];
      for(int i = 0; i < 200; ++i)
        b[i] = (byte)is.read();
      System.out.println("Received bytes: ");
      for(int i = 0; i < 200; ++i)
        System.out.printf("%01X", b[i]);
      System.out.print("\n");
      for(int i = 0, j = 0; i <= ret.length-1; ++i, j +=2)
        ret[i] = (byte)(b[j] << 4 | b[j+1]);
      CRC32 hasher = new CRC32();
      hasher.update(ret);
      long hash = hasher.getValue();
      System.out.print("Generated CRC32: ");
      System.out.printf("%02X%n", hash);
      for(int i = 4; i > 0; --i)
        os.write((int)hash >> 8*(i-1));
      int x;
      System.out.println("Response: " + (x = is.read()));
      if(x==1)
        System.out.println("Response good.");
      else
        System.out.println("Response not good.");
    }
    System.out.println("Disconnected from server.");
  }
}
