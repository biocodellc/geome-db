package biocode.fims.photos;

import org.imgscalr.Scalr;

import java.awt.image.BufferedImage;

/**
 * @author rjewing
 */
public class ImageScaler {

    private final BufferedImage srcImg;

    public ImageScaler(BufferedImage img) {
        srcImg = img;
    }

    public BufferedImage scale(int size) {
        return Scalr.resize(srcImg, size);
    }
}
