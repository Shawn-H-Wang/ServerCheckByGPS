package henu.wh.checkbygps.role.User;

import DB.DBUtil;
import Server.SocketServer;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserOperation {

    private static UserOperation userOperation;

    public synchronized static UserOperation getUserDao() {
        if (userOperation == null) {
            userOperation = new UserOperation();
        }
        return userOperation;
    }

    public void getInfo(User user) {
        String phone = user.getPhone();
        try {
            String sql = "select * from USER where phone=?";
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs != null && rs.next()) {
                user.setName(rs.getString("user"));
                user.setIp(rs.getString("ip"));
                user.setSex(rs.getBoolean("sex"));
                user.setIdentify(rs.getBoolean("identify"));
            }
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean updateInfo(User user) {
        try {
            String sql = "update USER set user=?, passwd=? where phone=?";
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user.getName());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getPhone());
            int r = ps.executeUpdate();
            if (r > 0) {
                return true;
            }
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean login(User user) {
        String phone = user.getPhone();
        String password = user.getPassword();
        try {
            String sql = "select * from USER where phone=? and passwd=?";
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, phone);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs != null && rs.next()) {
                DBUtil.getDBUtil().closeConnection(conn);
                changeStateOnline(user);
                return true;
            }
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean register(User u) {
        try {
            String sql = "insert into `USER` (phone,user,passwd,sex,identify,isonline) values(?,?,?,?,?,?)";
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, u.getPhone());
            ps.setString(2, u.getName());
            ps.setString(3, u.getPassword());
            ps.setBoolean(4, u.isSex());
            ps.setBoolean(5, u.isIdentify());
            ps.setInt(6, 0);
            int r = ps.executeUpdate();
            System.out.println("用户注册手机号：" + u.getPhone());
            if (r > 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean selectSignInfo(SignInfo signInfo) {
        try {
            String gid = signInfo.getGid();
            String sql = "select * from signInfo where gid=" + gid + " order by sdate desc";
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (null != rs && rs.next()) {
                if (rs.getInt("memnum") != rs.getInt("num")) {
                    return false;
                }
            }
            statement.close();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean insertSignInfo(SignInfo signInfo) {
        try {
            String sql = "insert into signInfo(signid, gid, sdate, memnum, num, jingdu, weidu) values(?,?,?,?,?,?,?)";
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, signInfo.getSignID());
            ps.setString(2, signInfo.getGid());
            ps.setString(3, new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            ps.setInt(4, signInfo.getMemnum());
            ps.setInt(5, 0);
            ps.setString(6, signInfo.getLocatiojn1());
            ps.setString(7, signInfo.getLocatiojn2());
            int r = ps.executeUpdate();
            if (r > 0) {
                return true;
            }
            ps.close();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean changeStateOnline(User user) {
        try {
            String sql = "update USER set isonline=?, ip=? where phone=?";
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, 1);
            ps.setString(2, user.getIp());
            ps.setString(3, user.getPhone());
            int r = ps.executeUpdate();
            if (r > 0) {
                return true;
            }
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void logoff(User user) {
        try {
            String sql = "update `USER` set isonline=? where phone=?";
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, 0);
            ps.setString(2, user.getPhone());
            int r = ps.executeUpdate();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void selectManageGroup(User user) {
        try {
            String sql = "SELECT * FROM `group` WHERE groupowner=?";
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user.getPhone());
            ResultSet rs = ps.executeQuery();
            List<String> list = new ArrayList<String>();
            while (rs != null && rs.next()) {
                String s1 = rs.getString("groupname");
                String s2 = rs.getString("groupid");
                list.add(s2 + ":" + s1);
            }
            user.setManagegroup(list);
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void deleteGroup(String gid) {
        try {
            String sql = "delete from `group` where groupid = " + gid;
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement statement = conn.createStatement();
            int r = statement.executeUpdate(sql);
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteGroup(String gid, String memid) {
        try {
            String sql = "delete from `groupmember` where groupid = " + gid + " and memberid = " + memid;
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement statement = conn.createStatement();
            int r = statement.executeUpdate(sql);
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void selectGroupMember(String gid, List<String> users) {
        try {
            String sql = "select memberid from groupmember where groupid = " + gid;
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (rs != null && rs.next()) {
                String userid = rs.getString("memberid");
                users.add(userid);
            }
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void selectMemberInfo(String gid, List<User> list) {
        try {
            String sql = "select * from `USER` where `USER`.phone in (select memberid from groupmember where groupid = " + gid + ")";
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (rs != null && rs.next()) {
                String phone = rs.getString("phone");
                String user = rs.getString("user");
                Boolean sex = rs.getBoolean("sex");
                User u = new User();
                u.setPhone(phone);
                u.setName(user);
                u.setSex(sex);
                list.add(u);
            }
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void selectUsersInfo(String uid, Connection conn, List<User> users) {
        try {
            String sql = "select * from `USER` where phone = " + uid;
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while (null != resultSet && resultSet.next()) {
                User user = new User();
                user.setPhone(resultSet.getString("phone"));
                user.setName(resultSet.getString("user"));
                user.setSex(resultSet.getBoolean("sex"));
                users.add(user);
            }
            statement.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void selectGroupManager(String gid, List<String> user) {
        user.clear();
        try {
            String sql = "select groupowner from `group` where groupid = " + gid;
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            if (rs != null && rs.next()) {
                String userid = rs.getString("groupowner");
                user.add(userid);
            }
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void selectInGroup(User user) {
        try {
            String sql = "select * from `group` where groupid in (select groupid from groupmember where groupmember.memberid=?);";
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user.getPhone());
            ResultSet rs = ps.executeQuery();
            List<Group> glist = new ArrayList<Group>();
            List<String> list = new ArrayList<String>();
            while (rs != null && rs.next()) {
                String s1 = rs.getString("groupname");
                String s2 = rs.getString("groupid");
                glist.add(new Group(rs.getString("groupid"), rs.getString("groupname"), rs.getString("groupowner")));
                list.add(s2 + ":" + s1);
            }
            user.setIngroup(list);
            user.setGroupmem(glist);
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public boolean createGroup(Group group) {
        try {
            String sql = "INSERT INTO `group` VALUES(?,?,?);";
            group.setGid("");
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");//设置日期格式
            String format = df.format(new Date());
            group.setGid(format);
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, group.getGid());
            ps.setString(2, group.getGname());
            ps.setString(3, group.getGroupowner());
            int r = ps.executeUpdate();
            System.out.println("用户注册群聊：" + group.toString());
            if (r > 0) {
                String sql2 = "INSERT INTO groupmember VALUES(?,?)";
                Connection conn2 = DBUtil.getDBUtil().getConnection();
                PreparedStatement ps2 = conn.prepareStatement(sql2);
                ps2.setString(1, group.getGid());
                ps2.setString(2, group.getGroupowner());
                int r2 = ps2.executeUpdate();
                if (r2 > 0) {
                    DBUtil.getDBUtil().closeConnection(conn2);
                    DBUtil.getDBUtil().closeConnection(conn);
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void selectGroup(String message, List<Group> Glist) {
        Glist.clear();
        try {
            String sql = "select * from `group` where groupid = '" + message + "' or groupname like '%" + message + "%'";
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs != null && rs.next()) {
                Glist.add(new Group(rs.getString("groupid"), rs.getString("groupname"), rs.getString("groupowner")));
            }
            stmt.close();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isInGroup(String userid, String gid) {
        try {
            String sql = "select * from `groupmember` where `groupid` =" + gid + " and `memberid` = " + userid + ";";
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs != null && rs.next()) {
                stmt.close();
                DBUtil.getDBUtil().closeConnection(conn);
                return true;
            }
            stmt.close();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*public boolean selectMessage(JSONObject message, String uid) {
        try {
            String code = message.getString("code");
            String msg = message.get("info").toString();
            String sql = "select * from messge where `message` in " +
                    "(select `message` from messge where memberid=" + uid + " and mtype = " + code + "isRead = 0)";
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs != null && rs.next()) {
                stmt.close();
                DBUtil.getDBUtil().closeConnection(conn);
                return true;
            }
            stmt.close();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }*/

    public void updateMsgRead(String mid) {
        try {
            String sql = "update `messge` set isRead = 1 where messageid = ?";
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, mid);
            int r = ps.executeUpdate();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void selectMessage(String uid, List<Message> Mlist) {
        try {
            String sql = "select * from `messge` where memberid = " + uid;
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs != null && rs.next()) {
                Mlist.add(new Message(
                        rs.getInt("messageid"),
                        rs.getString("memberid"),
                        rs.getString("message"),
                        rs.getDate("mdate"),
                        rs.getString("mtype"),
                        true,
                        rs.getBoolean("isRead")
                ));
            }
            stmt.close();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertMemInGroup(String gid, String mid) {
        try {
            String sql = "insert into groupmember values(?,?)";
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, gid);
            ps.setString(2, mid);
            int r = ps.executeUpdate();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateMessageSend(String uid) {
        try {
            String sql = "update `messge` set isSend = 1 where memberid = ? order by mdate desc";
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, uid);
            int r = ps.executeUpdate();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertMessage(JSONObject message, String uid) {
        try {
            String code = message.getString("code");
            insertMsg(message, uid, code);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int countMessage() {
        int i = -1;
        try {
            String sql = "select count(`messageid`) as `c` from `messge`";
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs != null && rs.next())
                i = rs.getInt("c");
            stmt.close();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return i;
    }

    private void insertMsg(JSONObject message, String uid, String code) throws SQLException {
        String sql = "insert messge values(?,?,?,?,?,?,?)";
        String msg = message.get("info").toString();
        String date = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int isSend = message.getInteger("isSend");
        Connection conn = DBUtil.getDBUtil().getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, String.valueOf(SocketServer.mid++));
        ps.setString(2, uid);
        ps.setString(3, msg);
        ps.setString(4, date);
        ps.setInt(5, isSend);
        ps.setInt(6, 0);
        ps.setString(7, code);
        ps.executeUpdate();
        DBUtil.getDBUtil().closeConnection(conn);
    }

    public void selectSignInfoI(String uid, String gid, List<MemSignInfo> mlist) {
        try {
            String sql = "select * from memsigninfo where gid = " + gid + " and uid = " + uid;
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (null != rs && rs.next()) {
                String signid = rs.getString("signid");
                String jingdu = rs.getString("jingdu");
                String weidu = rs.getString("weidu");
                String signdate = rs.getString("signdate");
                String signType = rs.getString("signType");
                mlist.add(new MemSignInfo(signid, uid, gid, jingdu, weidu, signdate, signType));
            }
            stmt.close();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void selectSignInfoM(String gid, List<SignInfo> signInfoList) {
        try {
            String sql = "select * from `signInfo` where gid = " + gid;
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (null != rs && rs.next()) {
                String signid = rs.getString("signid");
                String jingdu = rs.getString("jingdu");
                String weidu = rs.getString("weidu");
                String sdate = rs.getString("sdate");
                int memnum = rs.getInt("memnum");
                int num = rs.getInt("num");
                signInfoList.add(new SignInfo(signid, gid, jingdu, weidu, sdate, memnum, num));
            }
            stmt.close();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void selectUserSignInfo(String signid, List<MemSignInfo> memid) {
        try {
            String sql = "select * from `memsigninfo` where signid = " + signid;
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs != null && rs.next()) {
                String uid = rs.getString("uid");
                String gid = rs.getString("gid");
                String jingdu = rs.getString("jingdu");
                String weidu = rs.getString("weidu");
                String signdate = rs.getString("signdate");
                String signType = rs.getString("signType");
                MemSignInfo memSignInfo = new MemSignInfo(signid, uid, gid, jingdu, weidu, signdate, signType);
                memSignInfo.setSign(true);
                memid.add(memSignInfo);
            }
            stmt.close();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean insertMemSignInfo(MemSignInfo memSignInfo) {
        try {
            String sql = "insert into `memsigninfo`(signid, uid, gid, jingdu, weidu, signdate, signType) values(?,?,?,?,?,?,?)";
            Connection conn = DBUtil.getDBUtil().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, memSignInfo.getSignid());
            ps.setString(2, memSignInfo.getUid());
            ps.setString(3, memSignInfo.getGid());
            ps.setString(4, memSignInfo.getJingdu());
            ps.setString(5, memSignInfo.getWeidu());
            ps.setString(6, memSignInfo.getSigndate());
            ps.setString(7, memSignInfo.getSignType());
            int r = ps.executeUpdate();
            if (r > 0) {
                ps.close();
                DBUtil.getDBUtil().closeConnection(conn);
                return true;
            }
            ps.close();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int selectSignNum(String signid) {
        int i = -1;
        try {
            String sql = "select num from signInfo where signid = " + signid;
            Connection connection = DBUtil.getDBUtil().getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            if (null != resultSet && resultSet.next()) {
                i = resultSet.getInt("num");
                statement.close();
                DBUtil.getDBUtil().closeConnection(connection);
                return i;
            }
            statement.close();
            DBUtil.getDBUtil().closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return i;
    }

    public int selectSignMemNum(String signid) {
        int i = -1;
        try {
            String sql = "select memnum from signInfo where signid = " + signid;
            Connection connection = DBUtil.getDBUtil().getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            if (null != resultSet && resultSet.next()) {
                i = resultSet.getInt("memnum");
                statement.close();
                DBUtil.getDBUtil().closeConnection(connection);
                return i;
            }
            statement.close();
            DBUtil.getDBUtil().closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return i;
    }

    public boolean updateSignNum(String signid, int n) {
        try {
            String sql = "update signInfo set num=? where signid=?";
            Connection connection = DBUtil.getDBUtil().getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, n + 1);
            preparedStatement.setString(2, signid);
            int r = preparedStatement.executeUpdate();
            if (r > 0) {
                preparedStatement.close();
                DBUtil.getDBUtil().closeConnection(connection);
                return true;
            }
            preparedStatement.close();
            DBUtil.getDBUtil().closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void selectJWD(JSONObject back, String signID) {
        try {
            String sql = "select jingdu,weidu from signInfo where signid = " + signID;
            Connection conn = DBUtil.getDBUtil().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs != null && rs.next()) {
                String jingdu = rs.getString("jingdu");
                String weidu = rs.getString("weidu");
                back.put("jingdu", jingdu);
                back.put("weidu", weidu);
            }
            stmt.close();
            DBUtil.getDBUtil().closeConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
