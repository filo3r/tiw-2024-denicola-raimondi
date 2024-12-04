package it.polimi.tiw.dao;

import it.polimi.tiw.util.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ImageDAO {

    private final DatabaseConnectionPool databaseConnectionPool;

    public ImageDAO() throws SQLException {
        this.databaseConnectionPool = DatabaseConnectionPool.getInstance();
    }

    public int getImagesCountByUser(String username) throws SQLException {
        String query = "SELECT COUNT(*) AS images_count FROM Image WHERE image_uploader = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            result = statement.executeQuery();
            if (result.next())
                return result.getInt("images_count");
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