import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.security.Key;

public class VPNServer {
    private static final int PORT = 5555;
    // 為了示範，這裡使用固定的 16 字節 AES 密鑰，實際應用中應採用更安全的協商機制
    private static final String AES_KEY = "1234567890123456";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("VPN Server 正在執行，監聽埠 " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("有新連線：" + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        public void run() {
            try {
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                // 持續讀取加密後的訊息
                while (true) {
                    int len = dis.readInt();
                    byte[] encryptedData = new byte[len];
                    dis.readFully(encryptedData);
                    String decryptedMessage = decrypt(encryptedData);
                    System.out.println("收到訊息: " + decryptedMessage);
                    
                    // Echo 回去，先組裝訊息，再加密回傳
                    String response = "Echo: " + decryptedMessage;
                    byte[] encryptedResponse = encrypt(response);
                    dos.writeInt(encryptedResponse.length);
                    dos.write(encryptedResponse);
                    dos.flush();
                }
            } catch (Exception e) {
                System.out.println("連線中斷: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch(IOException e) {}
            }
        }
    }

    // 使用 AES 加密文字
    private static byte[] encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        Key key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(plainText.getBytes("UTF-8"));
    }

    // 使用 AES 解密資料
    private static String decrypt(byte[] cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        Key key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(cipherText);
        return new String(decrypted, "UTF-8");
    }
}
