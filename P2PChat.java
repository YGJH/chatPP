package chat;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class P2PChat {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // 建構子初始化 Socket 的輸入與輸出流
    public P2PChat(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    // 啟動聊天：啟動一個線程持續接收訊息，同時主線程讀取使用者輸入並送出
    public void startChat() {
        // 開啟背景線程讀取對方訊息
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("對方: " + message);
                }
            } catch (IOException e) {
                System.out.println("連線中斷: " + e.getMessage());
            }
        }).start();

        // 主線程讀取鍵盤輸入並傳送
        Scanner scanner = new Scanner(System.in);
        System.out.println("請輸入訊息 (輸入 'exit' 結束)：");
        while (true) {
            String message = scanner.nextLine();
            if ("exit".equalsIgnoreCase(message)) {
                break;
            }
            out.println(message);
        }
        scanner.close();
        closeConnection();
    }

    // 關閉連線
    public void closeConnection() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("關閉連線時錯誤: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("使用方式:");
            System.out.println("作為伺服器: java P2PChat server <port>");
            System.out.println("作為客戶端: java P2PChat client <server_ip> <port>");
            return;
        }

        String mode = args[0];
        if ("server".equalsIgnoreCase(mode)) {
            int port = Integer.parseInt(args[1]);
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("伺服器啟動，等待連線在 port " + port + "...");
                Socket socket = serverSocket.accept();
                System.out.println("連線建立，對方 IP：" + socket.getInetAddress());
                P2PChat chat = new P2PChat(socket);
                chat.startChat();
            } catch (IOException e) {
                System.out.println("伺服器發生錯誤: " + e.getMessage());
                e.printStackTrace();
            }
        } else if ("client".equalsIgnoreCase(mode)) {
            if (args.length < 3) {
                System.out.println("客戶端模式需要提供 server_ip 與 port");
                return;
            }
            String serverIP = args[1];
            int port = Integer.parseInt(args[2]);
            try {
                Socket socket = new Socket(serverIP, port);
                System.out.println("連線成功到 " + serverIP + ":" + port);
                P2PChat chat = new P2PChat(socket);
                chat.startChat();
            } catch (IOException e) {
                System.out.println("客戶端連線發生錯誤: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("未知模式: " + mode);
        }
    }
}
