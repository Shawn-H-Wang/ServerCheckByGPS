package Server;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientTest2 {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("172.0.0.1", 5001);
            Scanner scanner = new Scanner(System.in);
            String r = "";
            while (!r.equals("quit")) {
                System.out.print("请输入需要输入的数值：");
                r = scanner.next();
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("连接失败");
        }
    }

}
