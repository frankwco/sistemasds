package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

public class ByteToBase64Image {
    
    public static String converter(byte[] imageData) {
        // Your byte array of image data
       
        
        // Convert byte array to Base64 encoded string
        String encodedImage = null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = bais.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            byte[] imageInByteArray = baos.toByteArray();
            baos.close();
            bais.close();
            encodedImage = Base64.getEncoder().encodeToString(imageInByteArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
       return encodedImage;
    }
}
