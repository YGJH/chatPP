package chat;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    // 設定伺服器 IP 與 port
    private static final String SERVER_IP = "127.0.0.1"; // 若在本機測試使用 127.0.0.1
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try {
            // 與伺服器建立連線
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            System.out.println("已連接到聊天伺服器");

            // 建立輸出流與輸入流
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 開啟一個線程，不斷讀取來自伺服器的訊息
            new Thread(new Runnable() {
                public void run() {
                    String message;
                    try {
                        while ((message = in.readLine()) != null) {
                            System.out.println(message);
                        }
                    } catch (IOException e) {
                        System.out.println("伺服器連線中斷: " + e.getMessage());
                    }
                }
            }).start();

            // 主線程讀取使用者輸入，並傳送至伺服器
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                out.println(input);
            }
            scanner.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("無法連接到伺服器: " + e.getMessage());
        }
    }
}
