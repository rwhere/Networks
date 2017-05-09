import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.util.Random;

public class UdpClient {

  static int VERSION = 4;
  static int HLEN = 5;
  static int TOS = 0;
  static int IDENT = 0;
  static int FLAGS = 2;
  static int OFFSET = 0;
  static int TTL = 50;
  static int PROTOCOL = 17;
  static int BYTES_IN_WORD = 4;
  static int sourcePort = 9999;

  public static void main(String[] args) throws Exception {
    try(Socket socket = new Socket("codebank.xyz", 38005)) {
      //get streams
      InputStream is = socket.getInputStream(); //read
      OutputStream os = socket.getOutputStream(); //write
      Scanner sc = new Scanner(is, "UTF-8"); //readline
      byte[] deadBeef = {(byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF};

      //commence the handshake process
      byte[] handshakeHeader = new byte[20];
      createIPV4Header(handshakeHeader, 4);
      addAddresses(handshakeHeader, socket.getInetAddress().getAddress());
      short handshakeCheckSum = checksum(handshakeHeader);
      handshakeHeader[10] = (byte)(handshakeCheckSum >> 8);
      handshakeHeader[11] = (byte)handshakeCheckSum;
      os.write(handshakeHeader);
      os.write(deadBeef);
      int response = 0;
      for(int j = 3; j >= 0; --j) {
        response |= is.read() << 8*j;
      }
      int destPort = (is.read() & 0xFF) << 8;
      destPort |= is.read() & 0xFF;
      System.out.printf("Handshake Respone: 0x%02X%n", response);
      System.out.println("Port number received: " + destPort + "\n");

      //send header + data + read Response
      int dataLength = 2;
      Random rand = new Random();
      for(int i = 0; i < 12; ++i, dataLength *= 2) {
        byte[] data = new byte[dataLength];
        for(int x = 0; x < dataLength; ++x) {
          data[x] = (byte)rand.nextInt();
        }
        byte[] ipv4header = new byte[20];
        createIPV4Header(ipv4header, 8 + dataLength);
        addAddresses(ipv4header, socket.getInetAddress().getAddress());
        short ipv4checkSum = checksum(ipv4header);
        //update with checksum
        ipv4header[10] = (byte)(ipv4checkSum >> 8);
        ipv4header[11] = (byte)ipv4checkSum;
        byte[] udpheader = new byte[8];
        createUDPHeader(udpheader, 8 + dataLength, destPort);
        byte[] pseudoheader = createpseudoheader(ipv4header, udpheader, data, dataLength);
        short udpcheckSum = checksum(pseudoheader);
        udpheader[6] = (byte)(udpcheckSum >> 8);
        udpheader[7] = (byte)udpcheckSum;
        System.out.println("Sending packet with " + dataLength + " bytes of data.");
        long start_time = System.currentTimeMillis();
        os.write(ipv4header);
        os.write(udpheader);
        os.write(data);
        int finalResponse = 0;
        for(int j = 3; j >= 0; --j) {
          finalResponse |= is.read() << 8*j;
        }
        System.out.printf("Respone: 0x%02X%n", finalResponse);
        System.out.println("RTT: " + (System.currentTimeMillis() - start_time) + "\n");
      }
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
  public static byte[] createpseudoheader(byte[] ipv4header, byte[] udpheader,
    byte[] data, int dataLength) {

    byte[] pseudoheader = new byte[20 + dataLength];
    pseudoheader[0] = ipv4header[12];
    pseudoheader[1] = ipv4header[13];
    pseudoheader[2] = ipv4header[14];
    pseudoheader[3] = ipv4header[15];
    pseudoheader[4] = ipv4header[16];
    pseudoheader[5] = ipv4header[17];
    pseudoheader[6] = ipv4header[18];
    pseudoheader[7] = ipv4header[19];
    pseudoheader[8] = 0;
    pseudoheader[9] = 17;
    pseudoheader[10] = udpheader[4];
    pseudoheader[11] = udpheader[5];
    pseudoheader[12] = udpheader[0];
    pseudoheader[13] = udpheader[1];
    pseudoheader[14] = udpheader[2];
    pseudoheader[15] = udpheader[3];
    pseudoheader[16] = udpheader[4];
    pseudoheader[17] = udpheader[5];
    pseudoheader[18] = 0;
    pseudoheader[19] = 0;
    for(int i = 20, j = 0; i < 20 + dataLength; ++i, ++j) {
      pseudoheader[i] = data[j];
    }
    return pseudoheader;
  }
  public static void createUDPHeader(byte[] partialHeader, int bytesInData, int destPort) {

    partialHeader[0] = (byte)(sourcePort >> 8);
    partialHeader[1] = (byte)sourcePort;
    partialHeader[2] = (byte)(destPort >> 8);
    partialHeader[3] = (byte)destPort;
    partialHeader[4] = (byte)(bytesInData >> 8);
    partialHeader[5] = (byte)bytesInData;
    partialHeader[6] = 0;
    partialHeader[7] = 0;
  }
  public static void createIPV4Header(byte[] partialHeader, int bytesInData) {

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
