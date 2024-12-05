package it.polimi.tiw.dao;

import it.polimi.tiw.util.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CommentDAO {

    private final DatabaseConnectionPool databaseConnectionPool;

    public CommentDAO() throws SQLException {
        this.databaseConnectionPool = DatabaseConnectionPool.getInstance();
    }

    public int getCommentsCountByUser(String username) throws SQLException {
        String query = "SELECT COUNT(*) AS comments_count FROM Comment WHERE comment_author = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            result = statement.executeQuery();
            if (result.next())
                return result.getInt("comments_count");
            else
                return 0;
        } finally {
            if (result != null)
                result.close();
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
    }

}