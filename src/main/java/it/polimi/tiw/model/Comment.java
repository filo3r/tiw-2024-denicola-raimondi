package it.polimi.tiw.model;

/**
 * Represents a comment on an image, including the author, text, and associated image ID.
 */
public class Comment {

    /**
     * The unique identifier for the comment.
     */
    private int commentId;

    /**
     * The unique identifier of the image associated with the comment.
     */
    private int imageId;

    /**
     * The username of the author of the comment.
     */
    private String commentAuthor;

    /**
     * The text content of the comment.
     */
    private String commentText;

    /**
     * Constructs a new Comment with the specified attributes.
     * @param imageId       the unique identifier of the associated image
     * @param commentAuthor the username of the author of the comment
     * @param commentText   the text content of the comment
     */
    public Comment(int imageId, String commentAuthor, String commentText) {
        this.imageId = imageId;
        this.commentAuthor = commentAuthor;
        this.commentText = commentText;
    }

    /**
     * Retrieves the unique identifier of the comment.
     * @return the unique comment ID
     */
    public int getCommentId() {
        return commentId;
    }

    /**
     * Updates the unique identifier of the comment.
     * @param commentId the new unique identifier for the comment
     */
    public void setCommentId(int commentId) {
        this.commentId = commentId;
    }

    /**
     * Retrieves the unique identifier of the image associated with the comment.
     * @return the unique image ID
     */
    public int getImageId() {
        return imageId;
    }

    /**
     * Updates the unique identifier of the image associated with the comment.
     * @param imageId the new unique identifier of the associated image
     */
    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    /**
     * Retrieves the username of the author of the comment.
     * @return the username of the comment author
     */
    public String getCommentAuthor() {
        return commentAuthor;
    }

    /**
     * Updates the username of the author of the comment.
     * @param commentAuthor the new username of the comment author
     */
    public void setCommentAuthor(String commentAuthor) {
        this.commentAuthor = commentAuthor;
    }

    /**
     * Retrieves the text content of the comment.
     * @return the text content of the comment
     */
    public String getCommentText() {
        return commentText;
    }

    /**
     * Updates the text content of the comment.
     * @param commentText the new text content of the comment
     */
    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

}