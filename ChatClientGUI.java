package chat;
import javax.swing.*; 
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;

public class ChatClientGUI extends JFrame {
    // GUI 元件
    private JTextArea chatArea;      // 顯示聊天訊息
    private JTextField inputField;   // 輸入訊息
    private JButton sendButton;      // 發送按鈕
    private String name = ""; // 使用者名稱，預設為 "使用者"
    private static final String AES_KEY = genAESKey(); // AES 加密金鑰，16 字元長度

    private static final String genAESKey() {
        SecureRandom rand = new SecureRandom();
        // 產生隨機的 AES 金鑰，長度為 16 字元
        byte[] keyBytes = new byte[16];
        rand.nextBytes(keyBytes);
        StringBuilder aesKeyBuilder = new StringBuilder();
        for (byte b : keyBytes) {
            aesKeyBuilder.append(String.format("%02X", b));
        }
        return aesKeyBuilder.toString(); // 生成 AES 金鑰
    }
    // 網路連線相關變數
    private Socket socket;
    private DataOutputStream out;
    private BufferedReader in;


    public ChatClientGUI(String serverIP, int serverPort) {
        
        
        super("聊天平台客戶端");


        // 設定視窗圖示與標題
        this.setIconImage(Toolkit.getDefaultToolkit().getImage("icon.jpg")); // 設定視窗圖示
        Scanner scanner = new Scanner(System.in);
        System.out.print("請輸入您的名稱: ");
        String name = scanner.nextLine(); // 讀取使用者名稱
        if (name.isEmpty()) {
            this.name = "使用者"; // 若使用者名稱為空，則預設為 "使用者"
        } else {
            this.name = name; // 設定使用者名稱
        }


        this.setTitle("聊天平台客戶端 - " + this.name); // 設定視窗標題
        try {
            // inputField.putClientProperty("JTextField.placeholderText", "輸入訊息..."); // 可選：設置佔位符文字
            // 設定全局外觀風格
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            
            // 客製化特定元件外觀
            UIManager.put("Panel.background", new Color(240, 240, 240));
            UIManager.put("Button.arc", 10); // Nimbus 特有屬性，設定按鈕圓角
            UIManager.put("nimbusBase", new Color(51, 98, 140)); // Nimbus 基礎色調
            
            UIManager.put("TextArea.font", new Font("SansSerif", Font.PLAIN, 14)); // 設定字型
            UIManager.put("TextField.font", new Font("SansSerif", Font.PLAIN, 14)); // 設定字型
            UIManager.put("Button.font", new Font("SansSerif", Font.PLAIN, 14)); // 設定字型

            // 更新所有已開啟的視窗
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("伺服器 IP: " + serverIP + ", Port: " + serverPort);
        if(connectToServer(serverIP, serverPort)) {  // 連線到聊天伺服器
            System.out.println("成功連線到伺服器 " + serverIP + ":" + serverPort);
        } else {
            System.exit(1); // 連線失敗則關閉程式
            return ;
        }
        System.out.println("連線成功，開始聊天...");

        startMessageReader();              // 開啟背景線程讀取訊息
        initializeGUI();                   // 建立 GUI  
    }


    // 初始化 GUI 元件並設定視窗外觀
    private void initializeGUI() {

        inputField = new JTextField();
        inputField.enableInputMethods(true); // 啟用輸入法

        // 建立用來顯示聊天訊息的文字區域，並禁止編輯
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true); // 自動換行
        JScrollPane scrollPane = new JScrollPane(chatArea); // 加入捲軸

        // 建立文字欄位供使用者輸入訊息
        inputField = new JTextField();
        // 建立發送按鈕
        sendButton = new JButton("發送");

        // 建立輸入面板，包含輸入欄與發送按鈕，使用 BorderLayout 進行區域配置
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // 設定主要視窗的版面配置為 BorderLayout
        this.setLayout(new BorderLayout());
        // 將捲軸包含的文字區域放在中央
        this.add(scrollPane, BorderLayout.CENTER);
        // 將輸入面板放在視窗下方
        this.add(inputPanel, BorderLayout.SOUTH);

        // 當按下「發送」按鈕時呼叫 sendMessage() 方法
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        // 當在文字輸入欄中按下 Enter 鍵時呼叫 sendMessage() 方法
        inputField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // 設定視窗大小、關閉操作與置中顯示
        this.setSize(1000, 600); // 設定視窗大小
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null); // 視窗置中顯示
        this.setVisible(true); // 顯示視窗
    }

    // 連線到聊天伺服器
    private boolean connectToServer(String serverIP, int serverPort) {
        try {
            socket = new Socket(serverIP, serverPort);
            // 指定使用 UTF-8 編碼
            out = new DataOutputStream(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            if(in == null) {
                System.out.println("無法取得伺服器輸入流");
                return false;
            }
            return true;
        } catch (IOException ex) {
                System.out.println("無法連線到伺服器: " + ex.getMessage());
        }
        return false;
    }

    // AES 加密方法
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

    // 開啟線程持續讀取伺服器訊息
    private void startMessageReader() {

        new Thread(new Runnable() {
            public void run() {
                String message;
                try {
                    while ((message = in.readLine()) != null) {
                        chatArea.append(message + "\n");
                    }
                } catch (IOException ex) {
                    chatArea.append("連線中斷: " + ex.getMessage() + "\n");
                }
            }
        }).start();
    }

    // 發送訊息給伺服器
    private void sendMessage() {
        // 取得使用者輸入的訊息，並加上使用者名稱
        String message = inputField.getText().trim();
        if(message.isEmpty()) {
            return;
        }
        message = this.name + " : "+ message;
        // 將訊息加密
        try {
            byte[] encryptedMsg = encrypt(message);
            System.out.println("加密後的訊息: " + new String(encryptedMsg, "UTF-8"));
            out.writeInt(encryptedMsg.length); // 先傳送訊息長度
            out.write(encryptedMsg); // 再傳送加密後的訊息
            out.flush(); // 確保訊息已發送
        } catch (Exception e) {
            chatArea.append("發送失敗: " + e.getMessage() + "\n");
        }
        inputField.setText(""); // 清空輸入欄位
    }

    public static void main(String[] args) {
        // 這裡設定伺服器的 IP 與 Port，根據實際情況修改
        String serverIP = args[0]; 
        int serverPort = Integer.parseInt(args[1]);
        // 檢查命令列參數是否正確

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ChatClientGUI(serverIP, serverPort);
            }
        });
    }
}


