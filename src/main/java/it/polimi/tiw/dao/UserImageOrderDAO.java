package it.polimi.tiw.dao;

import it.polimi.tiw.util.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class UserImageOrderDAO {

    private final DatabaseConnectionPool databaseConnectionPool;

    public UserImageOrderDAO() throws SQLException {
        this.databaseConnectionPool = DatabaseConnectionPool.getInstance();
    }

    public boolean userHasImagesOrderForAlbum(String username, int albumId) throws SQLException {
        String query = "SELECT EXISTS (SELECT 1 FROM UserImageOrder WHERE username = ? AND album_id = ?)";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setInt(2, albumId);
            result = statement.executeQuery();
            if (result.next())
                return result.getBoolean(1);
            return false;
        } finally {
            if (result != null)
                result.close();
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
    }

    public boolean deleteUserImagesOrderForAlbum(String username, int albumId) throws SQLException {
        String query = "DELETE FROM UserImageOrder WHERE username = ? AND album_id = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setInt(2, albumId);
            int rowsDeleted = statement.executeUpdate();
            return rowsDeleted > 0;
        } finally {
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
    }

    public boolean saveUserImagesOrderForAlbum(String username, int albumId, ArrayList<Integer> imageIds) throws SQLException {
        String query = "INSERT INTO UserImageOrder (username, album_id, image_id, order_position) VALUES (?, ?, ?, ?)";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            for (int i = 0; i < imageIds.size(); i++) {
                statement.setString(1, username);
                statement.setInt(2, albumId);
                statement.setInt(3, imageIds.get(i));
                statement.setInt(4, i);
                statement.addBatch();
            }
            int[] rowsInserted = statement.executeBatch();
            for (int rows : rowsInserted) {
                if (rows == 0)
                    return false;
            }
            return true;
        } finally {
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
    }



}
