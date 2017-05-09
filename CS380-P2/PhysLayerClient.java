import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.lang.StringBuilder;

public class PhysLayerClient
{
  public static HashMap<String, Integer> map;
  final static int PREAMBLE_SIZE = 64;
  final static int PAYLOAD_SIZE = 32;
  final static int BITS_IN_BYTE = 8;
  final static double MULTIPLIER_4B5B = 1.25;
  public static double baseline;

  public static void main(String[] args) throws Exception
  {
    try(Socket socket = new Socket("codebank.xyz", 38002))
    {
      init();
      System.out.println("Connected to server.");
      InputStream is = socket.getInputStream();

      int sum = 0;
      for(int i = 0; i < PREAMBLE_SIZE; ++i)
        sum += is.read() & 0xFF;

      baseline = (double)sum/PREAMBLE_SIZE;
      System.out.format("Baseline established from preamble: %.2f%n", baseline);

      StringBuilder sb = new StringBuilder();
      int current, prev = signal(is.read() & 0xFF);
      if(prev == 1)
        sb.append("1");
      else
        sb.append("0");
      for(int i = 0; i < PAYLOAD_SIZE*BITS_IN_BYTE*MULTIPLIER_4B5B - 1; ++i)
      {
        current = signal(is.read() & 0xFF);
        if(current == prev)
          sb.append("0");
        else
          sb.append("1");
        prev = current;
      }
      byte[] array = new byte[PAYLOAD_SIZE];
      int one, two;
      for(int i = 0; i < PAYLOAD_SIZE*BITS_IN_BYTE*MULTIPLIER_4B5B; i+=10)
      {
        one = map.get(sb.substring(i, i+5));
        two = map.get(sb.substring(i+5, i+10));
        array[i/10] = (byte)(one << 4 | two);
      }
      System.out.print("Received 32 bytes: ");
      for(int i = 0; i < array.length; ++i)
        System.out.printf("%X", array[i]);
      System.out.print("\n");

      OutputStream os = socket.getOutputStream();
      os.write(array);

      if(is.read()==1)
        System.out.println("Response good.");
      else
        System.out.println("Response not good.");

    }
  }
  public static int signal(int val)
  {
    return val > baseline ? 1 : 0;
  }
  public static void init()
  {
    map = new HashMap<String, Integer>();
    map.put("11110", 0);
    map.put("01001", 1);
    map.put("10100", 2);
    map.put("10101", 3);
    map.put("01010", 4);
    map.put("01011", 5);
    map.put("01110", 6);
    map.put("01111", 7);
    map.put("10010", 8);
    map.put("10011", 9);
    map.put("10110", 10);
    map.put("10111", 11);
    map.put("11010", 12);
    map.put("11011", 13);
    map.put("11100", 14);
    map.put("11101", 15);
  }
}
