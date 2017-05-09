import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;

public class Ex3Client
{
  public static void main(String[] args) throws Exception
  {
    try(Socket socket = new Socket("codebank.xyz", 38103))
    {
      System.out.println("Connected to server.");
      InputStream is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      int x, numBytes = is.read();
      numBytes &= 0xFF;
      System.out.println("Reading " + numBytes + " bytes.\nData Received:");
      byte[] array = new byte[numBytes];
      for(int i = 0; i < numBytes; ++i)
      {
        System.out.printf("%02X", array[i] = (byte)is.read());
        if((i + 1)%8==0)
          System.out.print("\n");
      }
      short checkSum = checksum(array);
      System.out.printf("\nChecksum calculated: 0x%02X%n", checkSum);
      for(int i = 1; i >= 0; --i)
        os.write((int)checkSum >> i*8);
      System.out.println("Response: " + (x = is.read()));
      if(x==1)
        System.out.println("Response good.");
      else
        System.out.println("Response not good.");
    }
  }

  public static short checksum(byte[] b)
  {
    int sum = 0;
    for(int i = 0; i < b.length; i += 2)
    {
      //account for even or odd bytes
      if(i == b.length - 1)
        sum += b[i] << 8 & 0xFFFF;
      else
        sum += (b[i] << 8 & 0xFFFF) | (b[i+1] & 0xFF);
      if((sum & 0xFFFF0000) > 0)
      {
        sum &= 0xFFFF;
        sum++;
      }
    }
    return (short)~sum;
  }
}
