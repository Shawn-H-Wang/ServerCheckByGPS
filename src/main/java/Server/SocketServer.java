package Server;

import DB.DBUtil;
import com.alibaba.fastjson.JSONObject;
import henu.wh.checkbygps.role.User.User;
import henu.wh.checkbygps.role.User.UserOperation;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SocketServer {

    static int count = 0;
    static ServerSocket serverSocket;
    public static volatile List<ServerThread> stl = new ArrayList<ServerThread>();
    public static volatile List<Socket> skl = new ArrayList<Socket>();
    public static volatile LinkedList<JSONObject> message_list = new LinkedList<JSONObject>();   // 消息队列
    public static volatile boolean isPrint = false;
    public static volatile int mid = 0;

    public static void main(String[] args) {
        new SocketServer();
    }

    public SocketServer() {
        new ListeneSocket();
        new PrintOutThread();
    }

    class ListeneSocket extends Thread {

        public ListeneSocket() {
            start();
        }

        @Override
        public void run() {
            super.run();
            try {
                serverSocket = new ServerSocket(5001);
                System.out.println("***服务器即将启动，等待客户端的链接***");
                //循环监听等待客户端的链接
                while (true) {
                    //调用accept()方法开始监听，等待客户端的链接
                    Socket socket = serverSocket.accept();
                    //创建一个新的线程
                    User user = new User();
                    ServerThread serverThread = new ServerThread(socket, user);
                    //启动线程
                    serverThread.start();
                    stl.add(serverThread);
                    System.out.println(stl.size());
                    skl.add(socket);
                    System.out.println(skl.size());
                    count++;
                    //统计客户端的数量
                    System.out.println("客户端数量: " + count);
                    InetAddress address = socket.getInetAddress();
                    System.out.println("当前客户端的IP ： " + address.getHostAddress());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    class PrintOutThread extends Thread {

        public PrintOutThread() {
            start();
        }

        @Override
        public synchronized void run() {
            super.run();
            while (true) {
                if (isPrint) {
                    mid = UserOperation.getUserDao().countMessage() + 1;
                    JSONObject messege = message_list.getFirst();
                    List<String> uid_list = (List<String>) messege.get("uid");
                    System.out.println(uid_list);
                    System.out.println(message_list);
                    for (ServerThread serverThread : stl) {
                        if (uid_list.contains(serverThread.getUser().getPhone())) {
                            serverThread.sendMessage(messege);
                        }
                    }
                    for (String s : uid_list) {
                        UserOperation.getUserDao().insertMessage(messege, s);
                    }
                    message_list.removeFirst();
                    isPrint = false;
                }
            }
        }
    }


}