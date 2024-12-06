package it.polimi.tiw.dao;

import it.polimi.tiw.model.Album;
import it.polimi.tiw.util.DatabaseConnectionPool;

import java.sql.*;
import java.util.ArrayList;

/**
 * Data Access Object for performing operations on the Album entity.
 * This class provides methods to retrieve albums created by the user, albums created by others,
 * and to create a new album.
 */
public class AlbumDAO {

    /**
     * Connection pool to manage database connections efficiently
     */
    private final DatabaseConnectionPool databaseConnectionPool;

    /**
     * Initializes the AlbumDAO by obtaining an instance of the DatabaseConnectionPool.
     * @throws SQLException if there is a database access error
     */
    public AlbumDAO() throws SQLException {
        this.databaseConnectionPool = DatabaseConnectionPool.getInstance();
    }

    /**
     * Retrieves the list of albums created by a specific user, ordered by the most recent.
     * @param username the username of the album creator
     * @return a list of albums created by the user
     * @throws SQLException if a database access error occurs
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
     * @param username the username of the user to exclude from the results
     * @return a list of albums created by other users
     * @throws SQLException if a database access error occurs
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
     * @param album the Album object containing the details of the album to be created
     * @return true if the album was created successfully, false otherwise
     * @throws SQLException if a database access error occurs
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

    public int getUserPersonalAlbumId(String username) throws SQLException {
        String query = "SELECT album_id FROM Album WHERE album_creator = ? AND album_title = ?";
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

}