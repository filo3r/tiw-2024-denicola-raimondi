package it.polimi.tiw.util;

/**
 * Utility class for detecting the MIME type of image data based on byte signatures.
 */
public class MimeDetector {

    /**
     * Checks if the given byte array represents a JPEG image.
     * @param data the byte array to check
     * @return true if the byte array represents a JPEG image, false otherwise
     */
    public static boolean isJPEG(byte[] data) {
        if (data.length < 3) {
            return false;
        }
        return (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF);
    }

    /**
     * Checks if the given byte array represents a PNG image.
     * @param data the byte array to check
     * @return true if the byte array represents a PNG image, false otherwise
     */
    public static boolean isPNG(byte[] data) {
        if (data.length < 8) {
            return false;
        }
        return (data[0] == (byte) 0x89 && data[1] == (byte) 0x50 && data[2] == (byte) 0x4E && data[3] == (byte) 0x47 &&
                data[4] == (byte) 0x0D && data[5] == (byte) 0x0A && data[6] == (byte) 0x1A && data[7] == (byte) 0x0A);
    }

    /**
     * Checks if the given byte array represents a WEBP image.
     * @param data the byte array to check
     * @return true if the byte array represents a WEBP image, false otherwise
     */
    public static boolean isWEBP(byte[] data) {
        if (data.length < 12) {
            return false;
        }
        return (data[0] == (byte) 0x52 && data[1] == (byte) 0x49 && data[2] == (byte) 0x46 && data[3] == (byte) 0x46 &&
                data[8] == (byte) 0x57 && data[9] == (byte) 0x45 && data[10] == (byte) 0x42 && data[11] == (byte) 0x50);
    }

    /**
     * Detects the MIME type of the given byte array.
     * @param data the byte array to analyze
     * @return the MIME type as a string (e.g., "image/jpeg", "image/png", "image/webp"), or null if the type is unknown
     */
    public static String detectMimeType(byte[] data) {
        if (isJPEG(data))
            return "image/jpeg";
        if (isPNG(data))
            return "image/png";
        if (isWEBP(data))
            return "image/webp";
        return null;
    }

    /**
     * Returns the file extension associated with the given MIME type.
     * @param mimeType the MIME type as a string
     * @return the corresponding file extension (e.g., ".jpeg", ".png", ".webp"), or null if the MIME type is unknown
     */
    public static String getExtension(String mimeType) {
        if (mimeType.equals("image/jpeg"))
            return ".jpeg";
        if (mimeType.equals("image/png"))
            return ".png";
        if (mimeType.equals("image/webp"))
            return ".webp";
        return null;
    }

}