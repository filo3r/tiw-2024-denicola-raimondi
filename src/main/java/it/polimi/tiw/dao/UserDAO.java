package it.polimi.tiw.dao;

import it.polimi.tiw.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserDAO {

    private Connection connection;

    public UserDAO(Connection connection) {
        this.connection = connection;
    }


    /**
     * Create a new user in User's table
     * @param username
     * @param email
     * @param password
     * @return
     * @throws SQLException
     */
    public int createUser(String username, String email, String password) throws SQLException {
        int raw = 0;
        String query = "INSERT INTO User (username, email, password) VALUES (?, ?, ?)";
        PreparedStatement preparedStatement = null;

        try{
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, email);
            preparedStatement.setString(3, password);
            raw = preparedStatement.executeUpdate();
        }catch (SQLException e){
            throw new SQLException(e);
        }finally{
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (Exception e1) {
                throw new SQLException("Failed to close PreparedStatement", e1);
            }
        }

        //raw = 1 --> success
        return raw;
    }


}
