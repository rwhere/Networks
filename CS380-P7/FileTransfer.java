import java.security.*;
import javax.crypto.*;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.lang.Math;
import java.nio.file.Files;
import java.util.Scanner;
import java.io.EOFException;

/*
* Adrian Hy
* Ryan Waer
*
*
*/


public class FileTransfer {

  public static void main(String[] args) throws Exception {

    if(args.length < 1) {
      printUsageStatement();
      return;
    }

    String argOne = args[0].toLowerCase();

    if(argOne.equals("makekeys")) {
      makeKeys(args);
    } else if(argOne.equals("server")) {
      server(args);
    } else if(argOne.equals("client")) {
      client(args);
    } else {
      printUsageStatement();
      return;
    }
  }
  public static void makeKeys(String[] args) {

    if(args.length != 1) {
      printUsageStatement();
      return;
    }
    try {
      System.out.println("Making keys...");
      KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
      gen.initialize(2048); // you can use 2048 for faster key generation
      KeyPair keyPair = gen.genKeyPair();
      PrivateKey privateKey = keyPair.getPrivate();
      PublicKey publicKey = keyPair.getPublic();
      try(ObjectOutputStream oos = new ObjectOutputStream(
        new FileOutputStream(new File("public.bin")))) {
        oos.writeObject(publicKey);
      }
      try(ObjectOutputStream oos = new ObjectOutputStream(
        new FileOutputStream(new File("private.bin")))) {
        oos.writeObject(privateKey);
      }
      System.out.println("Operation completed");
    } catch(NoSuchAlgorithmException | IOException e) {
      e.printStackTrace(System.err);
    }
  }
  public static void server(String[] args) throws Exception {

    if(args.length < 3) {
      printUsageStatement();
      return;
    }
    String port = args[2];
    String privateKeyFileName = args[1];
    try(ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port))) {
      System.out.println("Listening on port " + port);
      while(true) {
        try(Socket socket = serverSocket.accept()) {

          //get streams
          InputStream is = socket.getInputStream(); //read
          OutputStream os = socket.getOutputStream(); //write
          ObjectOutputStream oos = new ObjectOutputStream(os);
          ObjectInputStream ois = new ObjectInputStream(is);
          //stuff
          int expectedSeqNum = 0;
          int totalChunks = 0;
          int chunkSize = 0;
          int totalTransferSize = 0;
          String fileName = null;
          //a temp buffer where i can store the file being transferred
          byte[] buffer = null;
          boolean transferStarted = false;
          Key sessionKey = null;
          Cipher cipher = null;

          while(true) {
            Message message = null;
            try {
              message = (Message)ois.readObject();
            } catch(EOFException e) {
              System.out.println("ERROR: Client disconnected.\n");
              break;
            }
            MessageType messageType = message.getType();

            if(messageType == MessageType.DISCONNECT) {
              socket.close();
              break;
            } else if(messageType == MessageType.START) {
              StartMessage startMessage = (StartMessage)message;
              //prep work
              chunkSize = startMessage.getChunkSize();
              totalTransferSize = (int)startMessage.getSize();
              totalChunks = (int)Math.ceil((double)totalTransferSize/chunkSize);
              String getFileString = startMessage.getFile();
              if(!getFileString.contains(".")) {
                fileName = getFileString.concat("2");
              } else {
                fileName = getFileString.replace(".", "2.");
              }
              buffer = new byte[totalTransferSize];
              transferStarted = true;
              //get the private key then get the encrpyed session key and decrypt it
              ObjectInputStream keyois = new ObjectInputStream(new FileInputStream(privateKeyFileName));
              PrivateKey privateKey = (PrivateKey)keyois.readObject();
              Cipher c = Cipher.getInstance("RSA");
              c.init(Cipher.UNWRAP_MODE, privateKey);
              sessionKey = c.unwrap(startMessage.getEncryptedKey(), "AES", Cipher.SECRET_KEY);
              cipher = Cipher.getInstance("AES");
              cipher.init(Cipher.DECRYPT_MODE, sessionKey);
              //send the ackmessage && set the expected sequence number to 0
              oos.writeObject(new AckMessage(expectedSeqNum = 0));
            } else if(messageType == MessageType.STOP) {
              //discard the file transfer
              chunkSize = 0;
              totalTransferSize = 0;
              fileName = null;
              expectedSeqNum = 0;
              buffer = null;
              transferStarted = false;
              sessionKey = null;
              totalChunks = 0;
              //send the ackmessage with -1
              oos.writeObject(new AckMessage(-1));
            } else if(messageType == MessageType.CHUNK  && transferStarted) {
              Chunk chunk = (Chunk)message;
              //check if its in order
              if(chunk.getSeq() == expectedSeqNum) {
                //decrypt the data in the chunk using sessionKey
                byte[] chunkPlainText = cipher.doFinal(chunk.getData());
                //calculate the CRC32 and compare with chunk CRC32
                if(checksum(chunkPlainText) != (short)chunk.getCrc()) {
                  //discard the file transfer
                  chunkSize = 0;
                  totalTransferSize = 0;
                  fileName = null;
                  expectedSeqNum = 0;
                  buffer = null;
                  transferStarted = false;
                  sessionKey = null;
                  //send the ackmessage with -1
                  oos.writeObject(new AckMessage(-1));
                  System.out.println("ERROR: CRC32 does not match.");
                } else {
                  System.out.println("Chunk received [" + (expectedSeqNum + 1)
                  + "/" + totalChunks + "].");
                  //if CRC32 is ok -> store data
                  storeData(buffer, chunkPlainText, expectedSeqNum, chunkSize);
                  //if this is the last chunk to be sent
                  if(expectedSeqNum == totalChunks - 1) {
                    File f = new File(fileName);
                    if(!f.exists())
                      f.createNewFile();
                    FileOutputStream fs = new FileOutputStream(f, false);
                    fs.write(buffer);
                    fs.close();
                    oos.writeObject(new AckMessage(++expectedSeqNum));
                    System.out.println("Transfer complete.");
                    System.out.println("Output path: " + fileName + "\n");
                    break;
                  }
                  //increment seq and send ack
                  oos.writeObject(new AckMessage(++expectedSeqNum));
                }
              } else {
                oos.writeObject(new AckMessage(expectedSeqNum));
              }
            } else {
              System.out.println("ERROR: Invalid message sent.");
            }
          }
        }
      }
    }
  }
  public static void client(String[] args) throws Exception {

    if(args.length < 4) {
      printUsageStatement();
      return;
    }

    String publicKeyFileName = args[1];
    String hostName = args[2];
    String port = args[3];
    Scanner sc = new Scanner(System.in);

    try(Socket socket = new Socket(hostName, Integer.parseInt(port))) {

      //get streams
      InputStream is = socket.getInputStream(); //read
      OutputStream os = socket.getOutputStream(); //write
      ObjectOutputStream oos = new ObjectOutputStream(os);
      ObjectInputStream ois = new ObjectInputStream(is);

      System.out.println("Connected to server: " + hostName);
      System.out.print("Enter path: ");
      String fileName = sc.nextLine();
      File f = new File(fileName);
      if(!f.exists()) {
        System.out.println("ERROR: File does not exist.");
        return;
      }
      System.out.print("Enter chunk size in bytes (for default enter 1024): ");
      int chunkSize = sc.nextInt();
      int totalTransferSize = (int)f.length();
      int totalChunks = (int)Math.ceil((double)totalTransferSize/chunkSize);
      System.out.println("Sending " + fileName + " File size in bytes: "
        + totalTransferSize + ".");
      System.out.println("Sending " + totalChunks + " chunks.");

      //generate an AES session key
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(128);
      Key key = keyGen.generateKey();
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.ENCRYPT_MODE, key);
      //encrypt the session key using the server's public key
      ObjectInputStream keyois = new ObjectInputStream(new FileInputStream(publicKeyFileName));
      PublicKey publicKey = (PublicKey)keyois.readObject();
      Cipher c = Cipher.getInstance("RSA");
      c.init(Cipher.WRAP_MODE, publicKey);
      //send the start message
      StartMessage sm = new StartMessage(fileName, c.wrap(key), chunkSize);
      oos.writeObject(sm);
      //read the file to byte array
      byte[] theFile = Files.readAllBytes(f.toPath());
      //receive messages
      while(true) {
        Message message = (Message)ois.readObject();
        MessageType messageType = message.getType();
        if(messageType == MessageType.ACK) {
          AckMessage ack = (AckMessage)message;
          int seqNum = ack.getSeq();
          if(seqNum == totalChunks) {
            break;
          }
          byte[] plainTextChunkData = readData(theFile, seqNum, chunkSize, totalChunks);
          int crcVal = (int)checksum(plainTextChunkData) & 0xFFFF;
          byte[] cipherText = cipher.doFinal(plainTextChunkData);
          Chunk chunk = new Chunk(seqNum, cipherText, crcVal);
          oos.writeObject(chunk);
          System.out.println("Chunks completed [" + (seqNum + 1)
          + "/" + totalChunks + "].");
        } else {
          System.out.println("ERROR: Invalid message sent.");
        }
      }
    }
  }
  public static byte[] readData(byte[] buff, int seqNum, int chunkSize, int totalChunks) {

    int amountToRead = chunkSize;
    if(seqNum == totalChunks - 1) {
      amountToRead = buff.length - seqNum * chunkSize;
    }
    byte[] retBuff = new byte[amountToRead];
    for(int i = 0, j = seqNum * chunkSize; i < amountToRead; ++i, ++j) {
      retBuff[i] = buff[j];
    }
    return retBuff;
  }
  public static void printUsageStatement() {
    System.out.println("Usage: FileTransfer makekeys" +
    "\n  FileTransfer server private_key_in port_number" +
    "\n  FileTransfer client public_key_in host_name port_number");
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
  public static void storeData(byte[] buff, byte[] chunkBuff, int seqNum, int chunkSize) {
    for(int i = 0, j = seqNum * chunkSize; i < chunkBuff.length; ++i, ++j) {
      buff[j] = chunkBuff[i];
    }
  }
}
