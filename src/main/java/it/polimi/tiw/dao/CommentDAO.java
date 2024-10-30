package it.polimi.tiw.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CommentDAO {

    private Connection connection;

    public CommentDAO(Connection connection) {
        this.connection = connection;
    }

    public int createComment(String commentText, String commentAuthor, int imageId) throws SQLException {
        //INSERT INTO album (title, IDUser) VALUES ('Le città italiane', 'andreariboni');
        String query = "INSERT into comment (comment_text, comment_author, image_id) VALUES(?, ?, ?)";
        int raw = 0;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, commentText);
            preparedStatement.setString(2, commentAuthor);
            preparedStatement.setInt(3, imageId);
            raw = preparedStatement.executeUpdate();
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
        return raw;
    }

}
