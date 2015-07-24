import com.apple.eawt.Application;
import com.sun.istack.internal.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

/**
 * Created by chrishawkins on 24/07/15.
 */
public class ImageLoader {

    @Nullable
    public static Image loadImageNamed(String name) {
        Image img = null;
        try {
            URL url = ImageLoader.class.getClassLoader().getResource("resources/"+name);
            img  = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return img;
    }
}
