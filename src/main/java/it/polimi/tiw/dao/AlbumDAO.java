package it.polimi.tiw.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AlbumDAO {

    private Connection connection;

    public AlbumDAO(Connection connection) {
        this.connection = connection;
    }

    public int createAlbum(String albumTitle, String creator) throws SQLException{
        int raw = 0;
        String query = "INSERT INTO Album (album_title, album_creator) VALUES (?, ?)";
        PreparedStatement preparedStatement = null;

        try{
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, albumTitle);
            preparedStatement.setString(2, creator);
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
