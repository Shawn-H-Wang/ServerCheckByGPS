package Server;

import DB.DBUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import henu.wh.checkbygps.role.User.*;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class ServerThread extends Thread {

    Socket socket = null;

    static String ip = "";
    static User user = new User();

    public User getUser() {
        return user;
    }

    public Socket getSocket() {
        return socket;
    }

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        DataInputStream read = null;
        DataOutputStream write = null;
        user.setIp(socket.getInetAddress().getHostAddress());
        ip = socket.getInetAddress().getHostAddress();
        try {
            while (true) {
                read = new DataInputStream(socket.getInputStream());
                String s = read.readUTF();
                JSONObject jsonObject = JSONObject.parseObject(s);
                if (jsonObject.containsKey("operation")) {
                    String operation = jsonObject.getString("operation");
                    if (operation.equals("login")) {
                        user.setPhone(jsonObject.getString("phone"));
                        user.setPassword(jsonObject.getString("passwd"));
                        boolean login = UserOperation.getUserDao().login(user);
                        System.out.println(login);
                        write = new DataOutputStream(socket.getOutputStream());
                        write.writeBoolean(login);
                    } else if (operation.equals("register")) {
                        user.set(jsonObject.getString("phone"),
                                jsonObject.getString("name"),
                                jsonObject.getString("passwd"),
                                jsonObject.getBoolean("sex"),
                                jsonObject.getBoolean("iden"));
                        boolean register = UserOperation.getUserDao().register(user);
                        write = new DataOutputStream(socket.getOutputStream());
                        write.writeBoolean(register);
                    } else if (operation.equals("quit")) {
                        System.out.println("客户端" + socket.getInetAddress().getHostAddress() + "已退出");
                        UserOperation.getUserDao().logoff(user);
                    } else if (operation.equals("updateInfo")) {
                        user.setName(jsonObject.getString("newname"));
                        user.setPassword(jsonObject.getString("newpwd"));
                        boolean updateInfo = UserOperation.getUserDao().updateInfo(user);
                        write = new DataOutputStream(socket.getOutputStream());
                        write.writeBoolean(updateInfo);
                    } else if (operation.equals("createG")) {
                        Group group = new Group();
                        group.setGname(jsonObject.getString("gname"));
                        group.setGroupowner(jsonObject.getString("manager"));
                        UserOperation.getUserDao().createGroup(group);
                    } else if (operation.equals("addG")) {
                        String groupid = jsonObject.getString("groupid");
                        String groupowner = jsonObject.getString("groupowner");
                        String userid = jsonObject.getString("userid");
                        if (UserOperation.getUserDao().isInGroup(userid, groupid)) {
                            // 成员已经在群中，不需要再次申请
                            System.out.println("成员已经在群中，不需要再次申请");
                            write = new DataOutputStream(socket.getOutputStream());
                            write.writeUTF("0");
                        } else {
                            // 成员不在群中
                            // 需要发送申请给管理员
                            // 这里插入语句到message中去
                            // 客户端创建消息队列，不断接收服务器发送的信息
                            System.out.println("用户不在群内");
                            JSONObject addGroupProposal = new JSONObject();
                            addGroupProposal.put("code", "addG");
                            JSONObject info = new JSONObject();
                            info.put("gid", groupid);
                            info.put("memid", userid);
                            info.put("gname", jsonObject.getString("groupname"));
                            addGroupProposal.put("info", info);
                            addGroupProposal.put("isSend", 0);
                            List<String> list = new ArrayList<String>();
                            list.add(groupowner);
                            addGroupProposal.put("uid", list);
                            System.out.println(addGroupProposal.toJSONString());
                            SocketServer.message_list.push(addGroupProposal);
                            SocketServer.isPrint = true;
                            write = new DataOutputStream(socket.getOutputStream());
                            write.writeUTF("1");
                        }
                    } else if (operation.equals("agreeInG")) {
                        UserOperation.getUserDao().updateMsgRead(String.valueOf(jsonObject.getInteger("mid")));
                        System.out.println(jsonObject);
                        JSONObject info = JSONObject.parseObject(jsonObject.getString("info"));
                        String gid = info.getString("gid");
                        String memid = info.getString("memid");
                        UserOperation.getUserDao().insertMemInGroup(gid, memid);
                        JSONObject back = new JSONObject();
                        back.put("add_G_result", true);
                        List<String> list = new ArrayList<String>();
                        list.add(memid);
                        back.put("uid", list);
                        back.put("info", "同意加入群聊：" + gid);
                        back.put("isSend", 0);
                        back.put("code", "mesg");
                        System.out.println(back.toJSONString());
                        SocketServer.message_list.push(back);
                        SocketServer.isPrint = true;
                    } else if (operation.equals("denyInG")) {
                        UserOperation.getUserDao().updateMsgRead(String.valueOf(jsonObject.getInteger("mid")));
                        System.out.println(jsonObject);
                        JSONObject info = JSONObject.parseObject(jsonObject.getString("info"));
                        String gid = info.getString("gid");
                        String memid = info.getString("memid");
                        JSONObject back = new JSONObject();
                        back.put("add_G_result", false);
                        List<String> list = new ArrayList<String>();
                        list.add(memid);
                        back.put("uid", list);
                        back.put("info", "拒绝加入群聊：" + gid);
                        back.put("isSend", 0);
                        back.put("code", "mesg");
                        System.out.println(back.toJSONString());
                        SocketServer.message_list.push(back);
                        SocketServer.isPrint = true;
                    } else if (operation.equals("deleteG")) {
                        String gid = jsonObject.getString("gid");
                        JSONObject back = new JSONObject();
                        List<String> users = new ArrayList<String>();
                        UserOperation.getUserDao().selectGroupMember(gid, users);
                        UserOperation.getUserDao().deleteGroup(gid);
                        back.put("uid", users);
                        back.put("info", "管理员删除群聊：" + gid);
                        back.put("isSend", 0);
                        back.put("code", "mesg");
                        SocketServer.message_list.push(back);
                        SocketServer.isPrint = true;
                    } else if (operation.equals("quitG")) {
                        String gid = jsonObject.getString("gid");
                        String memid = jsonObject.getString("uid");
                        JSONObject back = new JSONObject();
                        List<String> USER = new ArrayList<String>();
                        UserOperation.getUserDao().selectGroupManager(gid, USER);
                        UserOperation.getUserDao().deleteGroup(gid, memid);
                        back.put("uid", USER);
                        back.put("info", "成员" + memid + "退出群聊：" + gid);
                        back.put("isSend", 0);
                        back.put("code", "mesg");
                        SocketServer.message_list.push(back);
                        SocketServer.isPrint = true;
                    } else if (operation.equals("sendsignG")) {
                        String gid = jsonObject.getString("gid");
                        SignInfo signInfo = new SignInfo();
                        signInfo.setGid(gid);
                        signInfo.setLocatiojn1(jsonObject.getString("Locatiojn1"));
                        signInfo.setLocatiojn2(jsonObject.getString("Locatiojn2"));
                        signInfo.setSignID(jsonObject.getString("SignID"));
                        List<String> groupmembersid = new ArrayList<String>();
                        UserOperation.getUserDao().selectGroupMember(gid, groupmembersid);
                        signInfo.setMemnum(groupmembersid.size());
                        boolean b = UserOperation.getUserDao().selectSignInfo(signInfo);
                        if (!b) {
                            write = new DataOutputStream(socket.getOutputStream());
                            write.writeBoolean(false);
                        } else {
                            boolean b1 = UserOperation.getUserDao().insertSignInfo(signInfo);
                            if (b1) {
                                write = new DataOutputStream(socket.getOutputStream());
                                write.writeBoolean(true);
                                JSONObject back = new JSONObject();
                                back.put("uid", groupmembersid);
                                JSONObject info = new JSONObject();
                                info.put("signid", signInfo.getSignID());
                                info.put("gid", signInfo.getGid());
                                info.put("Locatiojn1", signInfo.getLocatiojn1());
                                info.put("Locatiojn2", signInfo.getLocatiojn2());
                                back.put("info", info.toJSONString());
                                back.put("isSend", 0);
                                back.put("code", "sign");
                                SocketServer.message_list.push(back);
                                SocketServer.isPrint = true;
                            } else {
                                write = new DataOutputStream(socket.getOutputStream());
                                write.writeBoolean(false);
                            }
                        }
                    } else if (operation.equals("user_sign")) {
                        String uid = jsonObject.getString("uid");
                        String gid = jsonObject.getString("gid");
                        String signid = jsonObject.getString("signid");
                        String weidu = jsonObject.getString("weidu");
                        String jingdu = jsonObject.getString("jingdu");
                        String signdate = jsonObject.getString("signdate");
                        String type = jsonObject.getString("type");
                        System.out.println(signid);
                        int n = UserOperation.getUserDao().selectSignNum(signid);
                        int n1 = UserOperation.getUserDao().selectSignMemNum(signid);
                        if (n < 0 || n >= n1) {
                            write = new DataOutputStream(socket.getOutputStream());
                            write.writeBoolean(false);
                        } else {
                            MemSignInfo memSignInfo = new MemSignInfo(signid, uid, gid, jingdu, weidu, signdate, type);
                            memSignInfo.setSign(true);
                            boolean b = UserOperation.getUserDao().insertMemSignInfo(memSignInfo);
                            if (b) {
                                System.out.println(jsonObject.getString("messid"));
                                UserOperation.getUserDao().updateMsgRead(jsonObject.getString("messid"));
                                System.out.println(n);
                                b = UserOperation.getUserDao().updateSignNum(signid, n);
                            }
                            write = new DataOutputStream(socket.getOutputStream());
                            write.writeBoolean(b);
                        }
                    }
                } else if (jsonObject.containsKey("want")) {
                    String want = jsonObject.getString("want");
                    if (want.equals("info")) {
                        UserOperation.getUserDao().getInfo(user);
                        JSONObject info = new JSONObject();
                        info.put("phone", user.getPhone());
                        info.put("name", user.getName());
                        info.put("sex", user.isSex());
                        info.put("iden", user.isIdentify());
                        System.out.println(info.toJSONString());
                        write = new DataOutputStream(socket.getOutputStream());
                        write.writeUTF(info.toJSONString());
                    } else if (want.equals("groupInfo")) {
                        JSONObject groupInfo = new JSONObject();
                        UserOperation.getUserDao().selectInGroup(user);
                        UserOperation.getUserDao().selectManageGroup(user);
                        groupInfo.put("manager", user.getManagegroup());
                        groupInfo.put("member", user.getIngroup());
                        groupInfo.put("memberG", user.getGroupmem());
                        System.out.println(groupInfo.toJSONString());
                        write = new DataOutputStream(socket.getOutputStream());
                        write.writeUTF(groupInfo.toJSONString());
                    } else if (want.equals("searchG")) {
                        List<Group> Glist = new ArrayList<Group>();
                        String keyword = jsonObject.getString("keyword");
                        UserOperation.getUserDao().selectGroup(keyword, Glist);
                        JSONObject searchG = new JSONObject();
                        searchG.put("grouplist", Glist);
                        write = new DataOutputStream(socket.getOutputStream());
                        write.writeUTF(searchG.toJSONString());
                    } else if (want.equals("msgs")) {
                        List<Message> Mlist = new ArrayList<Message>();
                        UserOperation.getUserDao().selectMessage(user.getPhone(), Mlist);
                        write = new DataOutputStream(socket.getOutputStream());
                        JSONObject getMsg = new JSONObject();
                        getMsg.put("messagelist", Mlist);
                        write.writeUTF(getMsg.toJSONString());
                        UserOperation.getUserDao().updateMessageSend(user.getPhone());
                        for (Message message : Mlist) {
                            if (message.getMtype().equals("mesg")) {
                                message.setRead(true);
                                UserOperation.getUserDao().updateMsgRead(String.valueOf(message.getMid()));
                            }
                        }
                    } else if (want.equals("meminfo")) {
                        String gid = jsonObject.getString("gid");
                        List<User> list = new ArrayList<User>();
                        UserOperation.getUserDao().selectMemberInfo(gid, list);
                        JSONObject memInfo = new JSONObject();
                        memInfo.put("memInfo", list);
                        write.writeUTF(memInfo.toJSONString());
                    } else if (want.equals("signinfo")) {
                        String gid = jsonObject.getString("gid");
                        JSONObject o1 = new JSONObject();
                        if (!jsonObject.getBoolean("tag1")) {
                            // 如果不是管理员
                            List<MemSignInfo> memSignInfoList = new ArrayList<MemSignInfo>();
                            UserOperation.getUserDao().selectSignInfoI(user.getPhone(), gid, memSignInfoList);
                            o1.put("signInfo", memSignInfoList);
                            o1.put("tag", "member");
                        } else {
                            // 如果是管理员
                            List<SignInfo> signInfoList = new ArrayList<SignInfo>();
                            UserOperation.getUserDao().selectSignInfoM(gid, signInfoList);
                            o1.put("signInfo", signInfoList);
                            o1.put("tag", "master");
                        }
                        write = new DataOutputStream(socket.getOutputStream());
                        write.writeUTF(o1.toJSONString());
                    } else if (want.equals("memsign")) {
                        String gid = jsonObject.getString("gid");
                        String signid = jsonObject.getString("signid");
                        List<MemSignInfo> memSignInfoList = new ArrayList<MemSignInfo>();
                        List<String> userid = new ArrayList<String>();
                        UserOperation.getUserDao().selectGroupMember(gid, userid);
                        System.out.println("修改之前：" + userid);
                        UserOperation.getUserDao().selectUserSignInfo(signid, memSignInfoList);
                        System.out.println("修改之前：" + memSignInfoList);
                        for (MemSignInfo memSignInfo : memSignInfoList) {
                            userid.remove(memSignInfo.getUid());
                        }
                        System.out.println("修改之后：" + userid);
                        for (String s1 : userid) {
                            MemSignInfo memSignInfo = new MemSignInfo();
                            memSignInfo.setUid(s1);
                            memSignInfo.setSignid(signid);
                            memSignInfo.setGid(gid);
                            memSignInfo.setSign(false);
                            memSignInfo.setSignType("未签到");
                            memSignInfoList.add(memSignInfo);
                        }
                        JSONObject o = new JSONObject();
                        o.put("memlist", memSignInfoList);
                        System.out.println(o);
                        write = new DataOutputStream(socket.getOutputStream());
                        write.writeUTF(o.toJSONString());
                    } else if (want.equals("signJWD")) {
                        String signID = jsonObject.getString("signID");
                        JSONObject back = new JSONObject();
                        UserOperation.getUserDao().selectJWD(back, signID);
                        write = new DataOutputStream(socket.getOutputStream());
                        write.writeUTF(back.toJSONString());
                    } else if (want.equals("users_info")) {
                        JSONArray o = (JSONArray) jsonObject.get("uids");
                        List<String> uids = o.toJavaList(String.class);
                        List<User> users = new ArrayList<User>();
                        for (String uid : uids) {
                            Connection conn = DBUtil.getDBUtil().getConnection();
                            UserOperation.getUserDao().selectUsersInfo(uid, conn, users);
                            DBUtil.getDBUtil().closeConnection(conn);
                        }
                        JSONObject back = new JSONObject();
                        back.put("users", users);
                        write = new DataOutputStream(socket.getOutputStream());
                        write.writeUTF(back.toJSONString());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("客户端 " + socket.getInetAddress().getHostAddress() + "已退出");
            UserOperation.getUserDao().logoff(user);
            --SocketServer.count;
            SocketServer.stl.remove(this);
        } finally {
            try {
                //关闭资源
                if (read != null) {
                    read.close();
                }
                if (write != null) {
                    write.close();
                }
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(JSONObject jsonObject) {
        JSONObject copy = jsonObject;
        String phone = this.getUser().getPhone();
        copy.put("uid", phone);
        try {
            DataOutputStream write = new DataOutputStream(this.socket.getOutputStream());
            write.writeUTF(copy.toJSONString());
            jsonObject.put("isSend", 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
