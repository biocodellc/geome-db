package biocode.fims.photos;

import org.junit.Test;

import java.awt.image.BufferedImage;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ImageScalerTest {

    @Test
    public void given_square_image_scaled_img_keeps_aspect_ratio() {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        ImageScaler imageScaler = new ImageScaler(img);

        BufferedImage scaledImg = imageScaler.scale(10);

        assertEquals(10, scaledImg.getWidth());
        assertEquals(10, scaledImg.getHeight());
    }

    @Test
    public void given_landscape_image_scaled_img_keeps_aspect_ration() {
        BufferedImage img = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        ImageScaler imageScaler = new ImageScaler(img);

        BufferedImage scaledImg = imageScaler.scale(10);

        assertEquals(10, scaledImg.getWidth());
        assertEquals(5, scaledImg.getHeight());
    }

    @Test
    public void given_portrait_image_scaled_img_keeps_aspect_ration() {
        BufferedImage img = new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB);
        ImageScaler imageScaler = new ImageScaler(img);

        BufferedImage scaledImg = imageScaler.scale(10);

        assertEquals(5, scaledImg.getWidth());
        assertEquals(10, scaledImg.getHeight());
    }
    
}