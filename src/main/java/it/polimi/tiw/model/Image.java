package it.polimi.tiw.model;

import java.sql.Timestamp;

/**
 * Represents an image with associated metadata such as uploader, title, date, and text description.
 */
public class Image {

    /**
     * The unique identifier for the image.
     */
    private int imageId;

    /**
     * The username of the individual who uploaded the image.
     */
    private String imageUploader;

    /**
     * The title of the image.
     */
    private String imageTitle;

    /**
     * The date and time when the image was uploaded.
     */
    private Timestamp imageDate;

    /**
     * A text description of the image.
     */
    private String imageText;

    /**
     * The file path where the image is stored.
     */
    private String imagePath;

    /**
     * Constructs a new Image with the specified attributes.
     * @param imageUploader the username of the uploader
     * @param imageTitle   the title of the image
     * @param imageText    the description of the image
     * @param imagePath    the path to the image file
     */
    public Image(String imageUploader, String imageTitle, String imageText, String imagePath) {
        this.imageUploader = imageUploader;
        this.imageTitle = imageTitle;
        this.imageDate = new Timestamp(System.currentTimeMillis());
        this.imageText = imageText;
        this.imagePath = imagePath;
    }

    /**
     * Retrieves the unique identifier of the image.
     * @return the unique image ID
     */
    public int getImageId() {
        return imageId;
    }

    /**
     * Updates the unique identifier of the image.
     * @param imageId the new unique identifier for the image
     */
    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    /**
     * Retrieves the username of the image uploader.
     * @return the username of the uploader
     */
    public String getImageUploader() {
        return imageUploader;
    }

    /**
     * Updates the username of the image uploader.
     * @param imageUploader the new username of the uploader
     */
    public void setImageUploader(String imageUploader) {
        this.imageUploader = imageUploader;
    }

    /**
     * Retrieves the title of the image.
     * @return the title of the image
     */
    public String getImageTitle() {
        return imageTitle;
    }

    /**
     * Updates the title of the image.
     * @param imageTitle the new title of the image
     */
    public void setImageTitle(String imageTitle) {
        this.imageTitle = imageTitle;
    }

    /**
     * Retrieves the date and time the image was uploaded.
     * @return the upload date and time
     */
    public Timestamp getImageDate() {
        return imageDate;
    }

    /**
     * Updates the date and time the image was uploaded.
     * @param imageDate the new upload date and time
     */
    public void setImageDate(Timestamp imageDate) {
        this.imageDate = imageDate;
    }

    /**
     * Retrieves the description of the image.
     * @return the text description of the image
     */
    public String getImageText() {
        return imageText;
    }

    /**
     * Updates the description of the image.
     * @param imageText the new text description of the image
     */
    public void setImageText(String imageText) {
        this.imageText = imageText;
    }

    /**
     * Retrieves the file path where the image is stored.
     * @return the file path of the image
     */
    public String getImagePath() {
        return imagePath;
    }

    /**
     * Updates the file path of the image.
     * @param imagePath the new file path of the image
     */
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

}