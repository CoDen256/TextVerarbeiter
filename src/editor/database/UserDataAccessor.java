package editor.database;

import javafx.scene.control.PasswordField;

import java.awt.*;
import java.sql.*;

import java.util.*;
import java.util.List;

public class UserDataAccessor {
    private Connection connection ;
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/editor?" +
            "useUnicode=true&" +
            "useJDBCCompliantTimezoneShift=true&" +
            "useLegacyDatetimeCode=false&" +
            "serverTimezone=UTC";

    private static final UserDataAccessor instance = new UserDataAccessor(URL, "root", "root");

    public static UserDataAccessor getInstance() {
        return instance;
    }

    public UserDataAccessor(String dbURL, String user, String password)  {
        try {
            connection = DriverManager.getConnection(dbURL, user, password);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void shutdown() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    public ResultSet execQuery(String sqlRequest, String... params) {
        ResultSet rs = null;
        try {
            PreparedStatement stmnt = connection.prepareStatement(sqlRequest);
            for (int i = 1; i <= params.length; i++) {
                stmnt.setString(i, params[i - 1]);
            }
            rs = stmnt.executeQuery();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return rs;
    }

    public List<User> getPersonList() throws SQLException {
        try (
                Statement stmnt = connection.createStatement();
                ResultSet rs = stmnt.executeQuery("select * from user");
        ){
            List<User> personList = new ArrayList<>();
            while (rs.next()) {
                String username = rs.getString("username");
                String password = rs.getString("password");
                User user = new User(username, password);
                personList.add(user);
            }
            return personList ;
        }
    }

    public Integer getUserId(User user) {
        try {
            ResultSet rs = execQuery("select id from user as u where u.username = ? and u.password = ?", user.getUsername(), user.getPassword());
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void createFile(User user, String path)  {
        Integer user_id = getUserId(user);
        if (user_id > 0 && !checkForFile(user, path)) {
            try {
                PreparedStatement stmnt = connection.prepareStatement("INSERT INTO user_file_created VALUES (?, ?)");
                stmnt.setString(1, user_id.toString());
                stmnt.setString(2, path);
                int res = stmnt.executeUpdate();
                if (res > 0) System.out.println("Inserted to DB successfully!");
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

        }
    }

    public User checkUser(String username, String password) {
        try {
            ResultSet rs = execQuery("SELECT * FROM user where user.username = ? and user.password = ?", username, password);
            if (rs.next()) {
                return new User(username, password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean checkForFile(User user, String filePath) {
        Integer user_id = getUserId(user);
        ResultSet rs = execQuery("SELECT * FROM user_file_created as ufc where ufc.user_id = ? and ufc.file_path = ?", user_id.toString(), filePath);
        try {
            if (rs.next()) return true;
        } catch (SQLException throwable) {
            throwable.printStackTrace();
        }
        return false;
    }

    public void saveState(User user, String path, String fileType) throws SQLException {
        Integer user_id = getUserId(user);
        if (user_id > 0) {
            PreparedStatement stmnt = connection.prepareStatement("INSERT INTO user_memento VALUES (?, ?, ?)");
            stmnt.setString(1, user_id.toString());
            stmnt.setString(2, path);
            stmnt.setString(3, fileType);

            int res = stmnt.executeUpdate();
            if (res > 0) System.out.println("Inserted to DB successfully!");
        }
    }

    public Map<String, String> getSavedState(User user) {
        Integer user_id = getUserId(user);
        Map<String, String> pathMap = new HashMap<>();
        if (user_id > 0) {
            try {
                PreparedStatement stmnt = connection.prepareStatement("select * from user_memento as um where um.user_id = ?");
                stmnt.setString(1, user_id.toString());
                ResultSet rs = stmnt.executeQuery();
                while (rs.next()) {
                    String file_path = rs.getString("file_opened");
                    String fileType = rs.getString("file_type");
                    pathMap.put(file_path, fileType);
                }
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        }
        return pathMap;
    }

    public void removeLastState(User user) {
        Integer user_id = getUserId(user);
        if (user_id > 0) {
            try {
                PreparedStatement stmnt = connection.prepareStatement("DELETE FROM user_memento as um WHERE um.user_id = ?");
                stmnt.setString(1, user_id.toString());
                int res = stmnt.executeUpdate();
                if (res > 0) System.out.println("Deleted from DB successfully!");
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        }
    }
}
