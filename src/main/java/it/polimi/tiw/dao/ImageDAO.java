package it.polimi.tiw.dao;

import it.polimi.tiw.model.Image;
import it.polimi.tiw.util.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Data Access Object for performing operations on the Image entity.
 * This class provides methods to retrieve, create, update, and manage images in the database.
 */
public class ImageDAO {

    /**
     * Connection pool to manage database connections efficiently.
     */
    private final DatabaseConnectionPool databaseConnectionPool;

    /**
     * Initializes the ImageDAO by obtaining an instance of the DatabaseConnectionPool.
     * @throws SQLException if there is a database access error
     */
    public ImageDAO() throws SQLException {
        this.databaseConnectionPool = DatabaseConnectionPool.getInstance();
    }

    /**
     * Retrieves the count of images uploaded by a specific user.
     * @param username the username of the image uploader
     * @return the count of images uploaded by the user
     * @throws SQLException if a database access error occurs
     */
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

    /**
     * Adds a new image to the database.
     * @param image the Image object containing the details of the image to be added
     * @return the ID of the newly inserted image, or -1 if the insertion fails
     * @throws SQLException if a database access error occurs
     */
    public int addImage(Image image) throws SQLException {
        String query = "INSERT INTO Image (image_uploader, image_title, image_date, image_text, image_path) VALUES (?, ?, ?, ?, ?)";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setString(1, image.getImageUploader());
            statement.setString(2, image.getImageTitle());
            statement.setTimestamp(3, image.getImageDate());
            statement.setString(4, image.getImageText());
            statement.setString(5, image.getImagePath());
            statement.executeUpdate();
            result = statement.getGeneratedKeys();
            if (result.next())
                return result.getInt(1);
            else
                return -1;
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
     * Updates the file path of an existing image in the database.
     * @param imageId the ID of the image to be updated
     * @param imagePath the new file path for the image
     * @return true if the update was successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean updateImagePath(int imageId, String imagePath) throws SQLException {
        String query = "UPDATE Image SET image_path = ? WHERE image_id = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, imagePath);
            statement.setInt(2, imageId);
            int rowsUpdated = statement.executeUpdate();
            return rowsUpdated > 0;
        } finally {
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
    }

    /**
     * Adds an image to multiple albums in the database.
     * @param imageId the ID of the image to be added
     * @param albumIds a list of album IDs to which the image should be added
     * @return true if the image was successfully added to all specified albums, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean addImageToAlbums(int imageId, ArrayList<Integer> albumIds) throws SQLException {
        String query = "INSERT INTO AlbumContainsImage (album_id, image_id) VALUES (?, ?)";
        Connection connection = null;
        PreparedStatement statement = null;
        if (albumIds == null || albumIds.isEmpty())
            return false;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            for (int albumId : albumIds) {
                statement.setInt(1, albumId);
                statement.setInt(2, imageId);
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

    public boolean doesImageExist(int imageId) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM Image WHERE image_id = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, imageId);
            result = statement.executeQuery();
            if (result.next())
                return result.getInt("count") > 0;
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

    public boolean doesImageBelongToAlbum(int imageId, int albumId) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM AlbumContainsImage WHERE album_id = ? AND image_id = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, albumId);
            statement.setInt(2, imageId);
            result = statement.executeQuery();
            if (result.next())
                return result.getInt("count") > 0;
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

    public Image getImageById(int imageId) throws SQLException {
        String query = "SELECT * FROM Image WHERE image_id = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, imageId);
            result = statement.executeQuery();
            if (result.next()) {
                Image image = new Image(result.getString("image_uploader"),
                        result.getString("image_title"),
                        result.getString("image_text")
                );
                image.setImageId(result.getInt("image_id"));
                image.setImageDate(result.getTimestamp("image_date"));
                image.setImagePath(result.getString("image_path"));
                return image;
            } else {
                return null;
            }
        } finally {
            if (result != null)
                result.close();
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
    }

    public boolean doesImageBelongToUser(int imageId, String username) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM Image WHERE image_id = ? AND image_uploader = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, imageId);
            statement.setString(2, username);
            result = statement.executeQuery();
            if (result.next())
                return result.getInt("count") > 0;
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

    public boolean deleteImageById(int imageId) throws SQLException {
        String query = "DELETE FROM Image WHERE image_id = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, imageId);
            int rowsDeleted = statement.executeUpdate();
            return rowsDeleted > 0;
        } finally {
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
    }

}