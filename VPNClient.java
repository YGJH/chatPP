import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.security.Key;
import java.util.Scanner;

public class VPNClient {
    private static final String SERVER_IP = "127.0.0.1"; // 請根據實際情況修改
    private static final int SERVER_PORT = 5555;
    private static final String AES_KEY = "1234567890123456";

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            System.out.println("已連線到 VPN Server: " + SERVER_IP + ":" + SERVER_PORT);
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            Scanner scanner = new Scanner(System.in);
            
            // 背景線程接收來自伺服器的訊息
            new Thread(() -> {
                try {
                    while (true) {
                        int len = dis.readInt();
                        byte[] encryptedResponse = new byte[len];
                        dis.readFully(encryptedResponse);
                        String response = decrypt(encryptedResponse);
                        System.out.println("Server: " + response);
                    }
                } catch (Exception e) {
                    System.out.println("連線中斷: " + e.getMessage());
                }
            }).start();
            
            // 主線程讀取使用者輸入，傳送加密後的訊息
            while (true) {
                System.out.print("Enter message (exit to quit): ");
                String msg = scanner.nextLine();
                if ("exit".equalsIgnoreCase(msg)) break;
                byte[] encryptedMsg = encrypt(msg);
                dos.writeInt(encryptedMsg.length);
                dos.write(encryptedMsg);
                dos.flush();
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static byte[] encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        Key key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(plainText.getBytes("UTF-8"));
    }

    private static String decrypt(byte[] cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        Key key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(cipherText);
        return new String(decrypted, "UTF-8");
    }
}
