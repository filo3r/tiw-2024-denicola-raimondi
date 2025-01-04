package it.polimi.tiw.dao;

import it.polimi.tiw.util.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Data Access Object (DAO) class for managing the user's custom image order for albums.
 * This class provides methods to check, retrieve, save, and delete custom image orders in the database.
 */
public class UserImageOrderDAO {

    /**
     * Connection pool to manage database connections efficiently.
     */
    private final DatabaseConnectionPool databaseConnectionPool;

    /**
     * Constructor that initializes the connection pool.
     * @throws SQLException if an error occurs while retrieving the connection pool instance.
     */
    public UserImageOrderDAO() throws SQLException {
        this.databaseConnectionPool = DatabaseConnectionPool.getInstance();
    }

    /**
     * Checks if the user has a custom image order for a specific album.
     * @param username the username of the user.
     * @param albumId  the ID of the album.
     * @return true if the user has a custom order, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
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

    /**
     * Deletes the user's custom image order for a specific album.
     * @param username the username of the user.
     * @param albumId  the ID of the album.
     * @return true if the deletion was successful, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
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

    /**
     * Saves a custom image order for a specific album for the user.
     * @param username the username of the user.
     * @param albumId  the ID of the album.
     * @param imageIds the list of image IDs in the desired order.
     * @return true if the save operation was successful, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
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

    /**
     * Retrieves the user's custom image order for a specific album.
     * @param username the username of the user.
     * @param albumId  the ID of the album.
     * @return a list of image IDs in the order defined by the user.
     * @throws SQLException if a database access error occurs.
     */
    public ArrayList<Integer> getUserImagesOrderForAlbum(String username, int albumId) throws SQLException {
        String query = "SELECT image_id FROM UserImageOrder WHERE username = ? AND album_id = ? ORDER BY order_position ASC";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        ArrayList<Integer> imageIds = new ArrayList<>();
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setInt(2, albumId);
            result = statement.executeQuery();
            while (result.next())
                imageIds.add(result.getInt("image_id"));
            return imageIds;
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