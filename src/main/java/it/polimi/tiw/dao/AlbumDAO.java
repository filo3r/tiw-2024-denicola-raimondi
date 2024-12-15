package it.polimi.tiw.dao;

import it.polimi.tiw.model.Album;
import it.polimi.tiw.model.Image;
import it.polimi.tiw.util.DatabaseConnectionPool;

import java.sql.*;
import java.util.ArrayList;

/**
 * Data Access Object for performing operations on the Album entity.
 * This class provides methods to retrieve, create, and manage albums
 * and their related images in the database.
 */
public class AlbumDAO {

    /**
     * Connection pool to manage database connections efficiently.
     */
    private final DatabaseConnectionPool databaseConnectionPool;

    /**
     * Initializes the AlbumDAO by obtaining an instance of the DatabaseConnectionPool.
     * @throws SQLException if there is a database access error.
     */
    public AlbumDAO() throws SQLException {
        this.databaseConnectionPool = DatabaseConnectionPool.getInstance();
    }

    /**
     * Retrieves the list of albums created by a specific user, ordered by the most recent.
     * @param username the username of the album creator.
     * @return a list of albums created by the user.
     * @throws SQLException if a database access error occurs.
     */
    public ArrayList<Album> getMyAlbums(String username) throws SQLException {
        String query = "SELECT * FROM Album WHERE album_creator = ? ORDER BY album_date DESC";
        ArrayList<Album> myAlbums = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            result = statement.executeQuery();
            while (result.next()) {
                int albumId = result.getInt("album_id");
                String albumCreator = result.getString("album_creator");
                String albumTitle = result.getString("album_title");
                Timestamp albumDate = result.getTimestamp("album_date");
                Album myAlbum = new Album(albumCreator, albumTitle);
                myAlbum.setAlbumId(albumId);
                myAlbum.setAlbumDate(albumDate);
                myAlbums.add(myAlbum);
            }
        } finally {
            if (result != null)
                result.close();
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
        return myAlbums;
    }

    /**
     * Retrieves the list of albums created by other users, ordered by the most recent.
     * @param username the username of the user to exclude from the results.
     * @return a list of albums created by other users.
     * @throws SQLException if a database access error occurs.
     */
    public ArrayList<Album> getOtherAlbums(String username) throws SQLException {
        String query = "SELECT * FROM Album WHERE album_creator != ? ORDER BY album_date DESC";
        ArrayList<Album> otherAlbums = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            result = statement.executeQuery();
            while (result.next()) {
                int albumId = result.getInt("album_id");
                String albumCreator = result.getString("album_creator");
                String albumTitle = result.getString("album_title");
                Timestamp albumDate = result.getTimestamp("album_date");
                Album otherAlbum = new Album(albumCreator, albumTitle);
                otherAlbum.setAlbumId(albumId);
                otherAlbum.setAlbumDate(albumDate);
                otherAlbums.add(otherAlbum);
            }
        } finally {
            if (result != null)
                result.close();
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
        return otherAlbums;
    }

    /**
     * Creates a new album in the database.
     * @param album the Album object containing the details of the album to be created.
     * @return true if the album was created successfully, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean createAlbum(Album album) throws SQLException {
        String query = "INSERT INTO Album (album_creator, album_title, album_date) VALUES (?, ?, ?)";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, album.getAlbumCreator());
            statement.setString(2, album.getAlbumTitle());
            statement.setTimestamp(3, album.getAlbumDate());
            int rowsInserted = statement.executeUpdate();
            return rowsInserted > 0;
        } finally {
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
    }

    /**
     * Retrieves the number of albums created by a specific user.
     * @param username the username of the album creator.
     * @return the count of albums created by the user.
     * @throws SQLException if a database access error occurs.
     */
    public int getAlbumsCountByUser(String username) throws SQLException {
        String query = "SELECT COUNT(*) AS albums_count FROM Album WHERE album_creator = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            result = statement.executeQuery();
            if (result.next())
                return result.getInt("albums_count");
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
     * Retrieves the ID of the personal album of a specific user, identified by a specific naming convention.
     * @param username the username of the album creator.
     * @return the ID of the user's personal album, or -1 if not found.
     * @throws SQLException if a database access error occurs.
     */
    public int getUserPersonalAlbumId(String username) throws SQLException {
        String query = "SELECT album_id FROM Album WHERE album_creator = ? AND album_title = ? ORDER BY album_date ASC";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, "@" + username);
            result = statement.executeQuery();
            if (result.next())
                return result.getInt("album_id");
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
     * Checks if a specific album is owned by a given user.
     * @param albumId  the ID of the album.
     * @param username the username of the potential album owner.
     * @return true if the album is owned by the user, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean isAlbumOwnedByUser(int albumId, String username) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM Album WHERE album_id = ? AND album_creator = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, albumId);
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

    /**
     * Retrieves the list of album IDs created by a specific user.
     * @param username the username of the album creator.
     * @return a list of album IDs created by the user.
     * @throws SQLException if a database access error occurs.
     */
    public ArrayList<Integer> getMyAlbumIds(String username) throws SQLException {
        String query = "SELECT album_id FROM Album WHERE album_creator = ?";
        ArrayList<Integer> albumIds = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            result = statement.executeQuery();
            while (result.next())
                albumIds.add(result.getInt("album_id"));
            return albumIds;
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
     * Retrieves an album by its ID.
     * @param albumId the ID of the album to retrieve.
     * @return the Album object corresponding to the given ID, or null if no album is found.
     * @throws SQLException if a database access error occurs.
     */
    public Album getAlbumById(int albumId) throws SQLException {
        String query = "SELECT * FROM Album WHERE album_id = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, albumId);
            result = statement.executeQuery();
            if (result.next()) {
                String albumCreator = result.getString("album_creator");
                String albumTitle = result.getString("album_title");
                Timestamp albumDate = result.getTimestamp("album_date");
                Album album = new Album(albumCreator, albumTitle);
                album.setAlbumId(albumId);
                album.setAlbumDate(albumDate);
                return album;
            }
            return null;
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
     * Checks whether an album with a given ID exists in the database.
     * @param albumId the ID of the album.
     * @return true if the album exists, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean doesAlbumExist(int albumId) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM Album WHERE album_id = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, albumId);
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

    /**
     * Retrieves the images associated with a specific album ID.
     * @param albumId the ID of the album.
     * @return a list of Image objects associated with the album.
     * @throws SQLException if a database access error occurs.
     */
    public ArrayList<Image> getImagesByAlbumId(int albumId) throws SQLException {
        String query = "SELECT i.* FROM AlbumContainsImage aci JOIN Image i ON aci.image_id = i.image_id WHERE aci.album_id = ? ORDER BY i.image_date DESC";
        ArrayList<Image> images = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, albumId);
            result = statement.executeQuery();
            while (result.next()) {
                int imageId = result.getInt("image_id");
                String imageUploader = result.getString("image_uploader");
                String imageTitle = result.getString("image_title");
                Timestamp imageDate = result.getTimestamp("image_date");
                String imageText = result.getString("image_text");
                String imagePath = result.getString("image_path");
                Image image = new Image(imageUploader, imageTitle, imageText);
                image.setImageId(imageId);
                image.setImageDate(imageDate);
                image.setImagePath(imagePath);
                images.add(image);
            }
            return images;
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
     * Retrieves the number of images associated with a specific album ID.
     * @param albumId the ID of the album.
     * @return the count of images in the album.
     * @throws SQLException if a database access error occurs.
     */
    public int getImagesCountByAlbumId(int albumId) throws SQLException {
        String query = "SELECT COUNT(*) AS image_count FROM AlbumContainsImage WHERE album_id = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, albumId);
            result = statement.executeQuery();
            if (result.next())
                return result.getInt("image_count");
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
     * Retrieves the images associated with a specific album ID with pagination.
     * @param albumId   the ID of the album.
     * @param pageSize  the number of images to retrieve per page.
     * @param startIndex the starting index for pagination.
     * @return a list of Image objects associated with the album.
     * @throws SQLException if a database access error occurs.
     */
    public ArrayList<Image> getImagesByAlbumIdWithPagination(int albumId, int pageSize, int startIndex) throws SQLException {
        String query = "SELECT i.* FROM AlbumContainsImage aci JOIN Image i ON aci.image_id = i.image_id WHERE aci.album_id = ? ORDER BY i.image_date DESC LIMIT ? OFFSET ?";
        ArrayList<Image> images = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, albumId);
            statement.setInt(2, pageSize);
            statement.setInt(3, startIndex);
            result = statement.executeQuery();
            while (result.next()) {
                int imageId = result.getInt("image_id");
                String imageUploader = result.getString("image_uploader");
                String imageTitle = result.getString("image_title");
                Timestamp imageDate = result.getTimestamp("image_date");
                String imageText = result.getString("image_text");
                String imagePath = result.getString("image_path");
                Image image = new Image(imageUploader, imageTitle, imageText);
                image.setImageId(imageId);
                image.setImageDate(imageDate);
                image.setImagePath(imagePath);
                images.add(image);
            }
            return images;
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