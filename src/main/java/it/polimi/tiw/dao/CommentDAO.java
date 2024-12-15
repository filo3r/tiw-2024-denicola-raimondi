package it.polimi.tiw.dao;

import it.polimi.tiw.model.Comment;
import it.polimi.tiw.util.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Data Access Object for performing operations on the Comment entity.
 * This class provides methods to retrieve and manage comments in the database.
 */
public class CommentDAO {

    /**
     * Connection pool to manage database connections efficiently.
     */
    private final DatabaseConnectionPool databaseConnectionPool;

    /**
     * Initializes the CommentDAO by obtaining an instance of the DatabaseConnectionPool.
     * @throws SQLException if there is a database access error.
     */
    public CommentDAO() throws SQLException {
        this.databaseConnectionPool = DatabaseConnectionPool.getInstance();
    }

    /**
     * Retrieves the count of comments made by a specific user.
     * @param username the username of the comment author.
     * @return the count of comments made by the user.
     * @throws SQLException if a database access error occurs.
     */
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

    /**
     * Retrieves the comments associated with a specific image ID.
     * @param imageId the ID of the image.
     * @return a list of comments associated with the image.
     * @throws SQLException if a database access error occurs.
     */
    public ArrayList<Comment> getCommentsByImageId(int imageId) throws SQLException {
        String query = "SELECT * FROM Comment WHERE image_id = ? ORDER BY comment_id DESC";
        ArrayList<Comment> comments = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, imageId);
            result = statement.executeQuery();
            while (result.next()) {
                Comment comment = new Comment(
                        result.getInt("image_id"),
                        result.getString("comment_author"),
                        result.getString("comment_text")
                );
                comment.setCommentId(result.getInt("comment_id"));
                comments.add(comment);
            }
            return comments;
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
     * Adds a new comment to the database.
     * @param comment the Comment object containing the details of the comment to be added.
     * @return true if the comment was added successfully, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean addComment(Comment comment) throws SQLException {
        String query = "INSERT INTO Comment (image_id, comment_author, comment_text) VALUES (?, ?, ?)";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, comment.getImageId());
            statement.setString(2, comment.getCommentAuthor());
            statement.setString(3, comment.getCommentText());
            int rowsInserted = statement.executeUpdate();
            return rowsInserted > 0;
        } finally {
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
    }

}