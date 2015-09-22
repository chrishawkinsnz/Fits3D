import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Created by chrishawkins on 22/09/15.
 */
public class TreeDeletable extends JTree {

    @Override
    public Point getPopupLocation(MouseEvent e) {
//        if (e != null) {
//            // here do your custom config, like f.i add/remove menu items based on context
//            // this example simply changes the action name
//            TreePath path = getClosestPathForLocation(e.getX(), e.getY());
//            e.putValue(Action.NAME, String.valueOf(path.getLastPathComponent()));
//            return e.getPoint();
//        }
//        action.putValue(Action.NAME, "no mouse");
        return null;
    }
}
