import java.io.*;
import java.net.*;

public class NATProxy {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java NATProxy <listenPort> <internalHost> <internalPort>");
            return;
        }
        int listenPort = Integer.parseInt(args[0]);
        String internalHost = args[1];
        int internalPort = Integer.parseInt(args[2]);

        try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
            System.out.println("NAT Proxy 正在監聽埠 " + listenPort + "，轉發到內部 " + internalHost + ":" + internalPort);
            while (true) {
                Socket externalSocket = serverSocket.accept();
                System.out.println("接受外部連線：" + externalSocket.getInetAddress() + ":" + externalSocket.getPort());
                // 為每個連線建立一個連線處理器
                new Thread(new ConnectionHandler(externalSocket, internalHost, internalPort)).start();
            }
        } catch (IOException e) {
            System.err.println("啟動 NAT Proxy 時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 處理每個外部連線
    private static class ConnectionHandler implements Runnable {
        private Socket externalSocket;
        private String internalHost;
        private int internalPort;

        public ConnectionHandler(Socket externalSocket, String internalHost, int internalPort) {
            this.externalSocket = externalSocket;
            this.internalHost = internalHost;
            this.internalPort = internalPort;
        }

        @Override
        public void run() {
            try (Socket internalSocket = new Socket(internalHost, internalPort)) {
                System.out.println("連線到內部主機 " + internalHost + ":" + internalPort + " 成功");
                // 分別開兩個線程轉發雙向資料
                Thread t1 = new Thread(new DataForwarder(externalSocket.getInputStream(), internalSocket.getOutputStream()));
                Thread t2 = new Thread(new DataForwarder(internalSocket.getInputStream(), externalSocket.getOutputStream()));
                t1.start();
                t2.start();
                // 等待兩個轉發線程結束
                t1.join();
                t2.join();
            } catch (Exception e) {
                System.err.println("連線處理錯誤: " + e.getMessage());
            } finally {
                try {
                    if (externalSocket != null && !externalSocket.isClosed()) {
                        externalSocket.close();
                    }
                } catch (IOException ex) {
                    // 略過
                }
                System.out.println("連線關閉");
            }
        }
    }

    // 資料轉發線程：將資料從輸入流讀取後寫入輸出流
    private static class DataForwarder implements Runnable {
        private InputStream in;
        private OutputStream out;

        public DataForwarder(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[4096];
            int bytesRead;
            try {
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    out.flush();
                }
            } catch (IOException e) {
                // 通常表示連線已關閉或發生錯誤
            }
        }
    }
}
