package eu.nimble.core.infrastructure.identity.utils;

import eu.nimble.utility.persistence.binary.ImageScaler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Johannes Innerbichler on 2019-01-08.
 */
public class ImageUtils {
    public static byte[] scaleImage(byte[] value, boolean isThumbnail, String mimeCode) throws IOException {
        // get correct format of the image (after image/... part)
        String format = mimeCode.substring(6);
        BufferedImage image = new ImageScaler().scale(new ByteArrayInputStream(value), isThumbnail);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        byte[] imageBytes = baos.toByteArray();
        baos.close();
        return imageBytes;
    }
}
