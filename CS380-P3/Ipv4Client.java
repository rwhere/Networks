import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.util.Random;

public class Ipv4Client {
  static int VERSION = 4;
  static int HLEN = 5;
  static int TOS = 0;
  static int IDENT = 0;
  static int FLAGS = 2;
  static int OFFSET = 0;
  static int TTL = 50;
  static int PROTOCOL = 6;
  static int BYTES_IN_WORD = 4;

  public static void main(String[] args) throws Exception {
    try(Socket socket = new Socket("codebank.xyz", 38003)) {
      //get streams
      InputStream is = socket.getInputStream(); //read
      OutputStream os = socket.getOutputStream(); //write
      Scanner sc = new Scanner(is, "UTF-8"); //readline
      byte[] data = new byte[4096];
      initArray(data);
      int dataLength = 2;
      //send header + data + read Response
      for(int i = 0; i < 12; ++i, dataLength *= 2) {
        byte[] header = new byte[20];
        createPartialHeader(header, dataLength);
        addAddresses(header, socket.getInetAddress().getAddress());
        short checkSum = checksum(header);
        //update with checksum
        header[10] = (byte)(checkSum >> 8);
        header[11] = (byte)checkSum;
        System.out.println("Data length: " + dataLength);
        os.write(header);
        os.write(data, 0, dataLength);
        System.out.println(sc.nextLine());
      }
    }
  }
  public static void initArray(byte[] b) {
    for(int i = 0; i < b.length; ++i) {
      b[i] = 0;
    }
  }
  public static void addAddresses(byte[] header, byte[] serverAddr) {
    Random rand = new Random();
    int sourceAddr = rand.nextInt();
    int destAddr = rand.nextInt();
    for(int i = 3, j = 12; i >= 0; --i, ++j) {
      header[j] = (byte)(sourceAddr >> 8*i);
    }
    for(int i = 0, j = 16; i < 4; ++i, ++j) {
      header[j] = serverAddr[i];
    }
  }
  public static void copyPartialHeader(byte[] partialHeader, byte[] header) {
    for(int i = 0; i < partialHeader.length; ++i) {
      header[i] = partialHeader[i];
    }
  }
  public static void createPartialHeader(byte[] partialHeader, int bytesInData) {
    partialHeader[0] = (byte)(VERSION << 4 | HLEN);
    partialHeader[1] = (byte)TOS;
    partialHeader[2] = (byte)((HLEN*BYTES_IN_WORD + bytesInData) >> 8);
    partialHeader[3] = (byte)(HLEN*BYTES_IN_WORD + bytesInData);
    partialHeader[4] = (byte)IDENT;
    partialHeader[5] = (byte)IDENT;
    partialHeader[6] = (byte)(FLAGS << 5);
    partialHeader[7] = (byte)OFFSET;
    partialHeader[8] = (byte)TTL;
    partialHeader[9] = (byte)PROTOCOL;
    partialHeader[10] = 0;
    partialHeader[11] = 0;
  }
  public static short checksum(byte[] b) {
    int sum = 0;
    for(int i = 0; i < b.length; i += 2) {
      //account for even or odd bytes
      if(i == b.length - 1)
        sum += b[i] << 8 & 0xFFFF;
      else
        sum += (b[i] << 8 & 0xFFFF) | (b[i+1] & 0xFF);
      if((sum & 0xFFFF0000) > 0) {
        sum &= 0xFFFF;
        sum++;
      }
    }
    return (short)~sum;
  }
}
