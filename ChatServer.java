package chat;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    // 用來存放所有客戶端的輸出串流，用於廣播訊息
    private static Set<PrintWriter> clientWriters = new HashSet<>();

    public static void main(String[] args) {
        // 檢查命令列參數是否正確
        if (args.length < 1) {
            System.out.println("使用方式:");
            System.out.println("java ChatServer <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        // 檢查端口號是否有效
        if (port < 1024 || port > 65535) {
            System.out.println("請輸入有效的端口號 (1024-65535)");
            return;
        }
        System.out.println("聊天伺服器啟動中，等待客戶端連線...");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                // 接受新的客戶端連線
                Socket socket = serverSocket.accept();
                System.out.println("新客戶端連線: " + socket);
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private String name;
        public ClientHandler(Socket socket) {
            this.socket = socket;
            String ip = socket.getInetAddress().getHostAddress();
            this.name = " (" + ip + ")";
            System.out.println("新客戶端連線: " + this.name);
        }
        
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);                
                // 將此客戶端的輸出流加入集合
                clientWriters.add(out);
                
                // 通知其他用戶有新用戶加入
                broadcast("新用戶已連線！", out);
                
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("收到訊息: " + message);
                    // 廣播訊息給所有客戶端
                    // broadcast(message, out); // 傳給所有人
                    broadcast(message, null); // 傳給所有人
                    // 回應發送者
                    // out.println("伺服器收到: " + message);
                }
            } catch (IOException e) {
                System.out.println("連線錯誤: " + e.getMessage());
            } finally {
                // 從集合中移除此客戶端
                if (out != null) {
                    clientWriters.remove(out);
                    broadcast("有用戶離開了聊天室", null);
                }
                try {
                    socket.close();
                } catch (IOException e) {}
            }
        }
        
        // 廣播訊息給所有客戶端，except可以排除特定客戶端
        private void broadcast(String message, PrintWriter except) {
            for (PrintWriter writer : clientWriters) {
                if (writer != except) { // 如果except為null，則發給所有人
                    writer.println(message);
                }
            }
        }
    }

}
