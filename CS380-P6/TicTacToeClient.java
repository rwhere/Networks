import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

public class TicTacToeClient {

  public static void main(String[] args) throws Exception {
    try(Socket socket = new Socket("codebank.xyz", 38006)) {
      //get streams
      InputStream is = socket.getInputStream(); //read
      OutputStream os = socket.getOutputStream(); //write
      ObjectOutputStream oos = new ObjectOutputStream(os);
      ObjectInputStream ois = new ObjectInputStream(is);
      //send Connect Message
      ConnectMessage cm = new ConnectMessage("rnwaer");
      oos.writeObject(cm);
      CommandMessage commandMessage = new CommandMessage(CommandMessage.Command.NEW_GAME);
      oos.writeObject(commandMessage);
      BoardMessage boardMessage;
      while(true) {
        //receive board
        boardMessage = (BoardMessage)ois.readObject();
        if(boardMessage.getStatus() != BoardMessage.Status.IN_PROGRESS) {
          break;
        }
        System.out.println("Game status: " + boardMessage.getStatus());
        System.out.println("Game turn: " + boardMessage.getTurn());
        printBoard(boardMessage.getBoard());
        MoveMessage mm = makeMove(boardMessage.getBoard());
        oos.writeObject(mm);
      }
      printBoard(boardMessage.getBoard());
      System.out.println(boardMessage.getStatus());
    }
  }
  public static void printBoard(byte[][] board) {
    System.out.println(" ABC");
    for(int i = 0; i < board.length; ++i) {
      System.out.print((char)(65+i));
      for(int j = 0; j < board[0].length; ++j) {
        if(i+1%3==0) {
          System.out.print("\n");
        }
        System.out.print(board[i][j]);
      }
      System.out.print("\n");
    }
  }
  public static MoveMessage makeMove(byte[][] board) {
    Scanner sc = new Scanner(System.in);
    System.out.print("\nPlease enter move(e.g. AB where [A=row][B=col]): ");
    String move = sc.nextLine();
    move = move.toUpperCase();
    while(!validMove(board, move)) {
      System.out.println("\nInvalid move.");
      printBoard(board);
      System.out.print("\nPlease enter move(e.g. AB where [A=row][B=col]): ");
      move = sc.nextLine();
      move = move.toUpperCase();
    }
    return new MoveMessage((byte)(move.charAt(0) - 65), (byte)(move.charAt(1) - 65));
  }
  public static boolean validMove(byte[][] board, String move) {
    return board[move.charAt(0) - 65][move.charAt(1) - 65] == 0 ? true : false;
  }
}
