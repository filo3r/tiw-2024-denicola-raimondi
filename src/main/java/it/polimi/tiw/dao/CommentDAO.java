package it.polimi.tiw.dao;

import it.polimi.tiw.model.Comment;
import it.polimi.tiw.util.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

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

    public ArrayList<Comment> getCommentsByImageId(int imageId) throws SQLException {
        String query = "SELECT * FROM Comment WHERE image_id = ?";
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