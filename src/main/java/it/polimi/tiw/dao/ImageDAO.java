package it.polimi.tiw.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ImageDAO {

    private Connection connection;

    public ImageDAO(Connection connection) {
        this.connection = connection;
    }

    public int createImage(String imageUploader, String imageTitle, String imageText, String imagePath) throws SQLException {
        int raw = 0;
        String query = "INSERT INTO Album (image_uploader, image_title, image_text, image_path) VALUES (?, ?, ?, ?)";
        PreparedStatement preparedStatement = null;

        try{
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, imageUploader);
            preparedStatement.setString(2, imageTitle);
            preparedStatement.setString(3, imageText);
            preparedStatement.setString(4, imagePath);
            raw = preparedStatement.executeUpdate();
        }catch(SQLException e){
            throw new SQLException(e);
        }finally{
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
