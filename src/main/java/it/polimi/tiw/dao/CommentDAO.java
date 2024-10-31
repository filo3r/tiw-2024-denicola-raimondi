package it.polimi.tiw.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CommentDAO {

    private Connection connection;

    public CommentDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * create a new comment in comment's table
     * @param commentText
     * @param commentAuthor
     * @param imageId
     * @throws SQLException
     */
    public int createComment(String commentText, String commentAuthor, int imageId) throws SQLException {
        String query = "INSERT into Comment (comment_text, comment_author, image_id) VALUES(?, ?, ?)";
        int row = 0;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, commentText);
            preparedStatement.setString(2, commentAuthor);
            preparedStatement.setInt(3, imageId);
            row = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException(e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (Exception e1) {
                throw new SQLException("Failed to close PreparedStatement", e1);
            }
        }
        return row;
    }

}
