package Server;

import com.alibaba.fastjson.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientTest {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("172.0.0.1", 5001);
            Scanner scanner = new Scanner(System.in);
            String r = "";
            DataOutputStream write = new DataOutputStream(socket.getOutputStream());
            while (!r.equals("quit")) {
                System.out.print("请输入需要输入的数值：");
                r = scanner.next();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("发送的数据",r);
                String message = jsonObject.toString();
                write.writeUTF(message);
            }
            write.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("连接失败");
        }
    }

}
