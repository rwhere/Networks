import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Random;

public class Ipv6Client {
  static int VERSION = 6;
  static int TRAFFIC_CLASS = 0;
  static int FLOW_LABEL = 0;
  static int HEADER_LENGTH = 40;
  static int NEXT_HEADER = 17;
  static int HOP_LIMIT = 20;

  public static void main(String[] args) throws Exception {
    try(Socket socket = new Socket("codebank.xyz", 38004)) {
      //get streams
      InputStream is = socket.getInputStream(); //read
      OutputStream os = socket.getOutputStream(); //write
      byte[] data = new byte[4096];
      initArray(data);
      int dataLength = 2;
      //send header + data + read Response
      for(int i = 0; i < 12; ++i, dataLength *= 2) {
        byte[] header = new byte[HEADER_LENGTH];
        createPartialHeader(header, dataLength);
        addAddresses(header, socket.getInetAddress().getAddress());
        System.out.println("Data length: " + dataLength);
        os.write(header);
        os.write(data, 0, dataLength);
        int response = 0;
        for(int j = 3; j >= 0; --j) {
          response |= is.read() << 8*j;
        }
        System.out.printf("Respone: 0x%02X%n", response);
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
    header[8] = 0;
    header[9] = 0;
    header[10] = 0;
    header[11] = 0;
    header[12] = 0;
    header[13] = 0;
    header[14] = 0;
    header[15] = 0;
    header[16] = 0;
    header[17] = 0;
    header[18] = (byte)0xFF;
    header[19] = (byte)0xFF;
    for(int i = 3, j = 20; i >= 0; --i, ++j) {
      header[j] = (byte)(sourceAddr >> 8*i);
    }
    header[24] = 0;
    header[25] = 0;
    header[26] = 0;
    header[27] = 0;
    header[28] = 0;
    header[29] = 0;
    header[30] = 0;
    header[31] = 0;
    header[32] = 0;
    header[33] = 0;
    header[34] = (byte)0xFF;
    header[35] = (byte)0xFF;
    for(int i = 0, j = 36; i < 4; ++i, ++j) {
      header[j] = serverAddr[i];
    }
  }
  public static void createPartialHeader(byte[] partialHeader, int bytesInData) {
    partialHeader[0] = (byte)(VERSION << 4 | TRAFFIC_CLASS);
    partialHeader[1] = (byte)FLOW_LABEL;
    partialHeader[2] = (byte)FLOW_LABEL;
    partialHeader[3] = (byte)FLOW_LABEL;
    partialHeader[4] = (byte)(bytesInData >> 8);
    partialHeader[5] = (byte)bytesInData;
    partialHeader[6] = (byte)NEXT_HEADER;
    partialHeader[7] = (byte)HOP_LIMIT;
  }
}
