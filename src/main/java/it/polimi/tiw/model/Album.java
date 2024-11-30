package it.polimi.tiw.model;

import java.sql.Timestamp;

/**
 * Represents an album containing images, with metadata including creator, title, and creation date.
 */
public class Album {

    /**
     * The unique identifier for the album.
     */
    private int albumId;

    /**
     * The username of the creator of the album.
     */
    private String albumCreator;

    /**
     * The title of the album.
     */
    private String albumTitle;

    /**
     * The date and time when the album was created.
     */
    private Timestamp albumDate;

    /**
     * Constructs a new Album with the specified attributes.
     * @param albumCreator  the username of the album creator
     * @param albumTitle    the title of the album
     */
    public Album(String albumCreator, String albumTitle) {
        this.albumCreator = albumCreator;
        this.albumTitle = albumTitle;
        this.albumDate = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Retrieves the unique identifier of the album.
     * @return the unique album ID
     */
    public int getAlbumId() {
        return albumId;
    }

    /**
     * Updates the unique identifier of the album.
     * @param albumId the new unique identifier for the album
     */
    public void setAlbumId(int albumId) {
        this.albumId = albumId;
    }

    /**
     * Retrieves the username of the creator of the album.
     * @return the username of the album creator
     */
    public String getAlbumCreator() {
        return albumCreator;
    }

    /**
     * Updates the username of the creator of the album.
     * @param albumCreator the new username of the album creator
     */
    public void setAlbumCreator(String albumCreator) {
        this.albumCreator = albumCreator;
    }

    /**
     * Retrieves the title of the album.
     * @return the title of the album
     */
    public String getAlbumTitle() {
        return albumTitle;
    }

    /**
     * Updates the title of the album.
     * @param albumTitle the new title of the album
     */
    public void setAlbumTitle(String albumTitle) {
        this.albumTitle = albumTitle;
    }

    /**
     * Retrieves the date and time the album was created.
     * @return the creation date and time of the album
     */
    public Timestamp getAlbumDate() {
        return albumDate;
    }

    /**
     * Updates the date and time the album was created.
     * @param albumDate the new creation date and time of the album
     */
    public void setAlbumDate(Timestamp albumDate) {
        this.albumDate = albumDate;
    }

}