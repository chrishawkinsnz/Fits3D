import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Created by chrishawkins on 20/07/15.
 */
public class KeyboardSelectionController implements KeyListener{

    public static float speedMove = 0.05f;
    public static float speedScale = 0.25f;

    private static final char MOVE_LEFT_KEY = 'a';
    private static final char MOVE_RIGHT_KEY = 'd';
    private static final char MOVE_IN_KEY = 'w';
    private static final char MOVE_OUT_KEY = 's';
    private static final char MOVE_UP_KEY = 'q';
    private static final char MOVE_DOWN_KEY = 'e';

    private static final char SCALE_LEFT_KEY = 'j';
    private static final char SCALE_RIGHT_KEY = 'l';
    private static final char SCALE_IN_KEY = 'i';
    private static final char SCALE_OUT_KEY = 'k';
    private static final char SCALE_UP_KEY = 'u';
    private static final char SCALE_DOWN_KEY = 'o';








    private Selection selection;

    public KeyboardSelectionController(Selection selection) {
        this.selection = selection;
    }


    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyChar()) {
            case MOVE_LEFT_KEY:
                selection.moveX(-speedMove);
                break;
            case MOVE_RIGHT_KEY:
                selection.moveX(speedMove);
                break;
            case MOVE_IN_KEY:
                selection.moveZ(speedMove);
                break;
            case MOVE_OUT_KEY:
                selection.moveZ(-speedMove);
                break;
            case MOVE_UP_KEY:
                selection.moveY(speedMove);
                break;
            case MOVE_DOWN_KEY:
                selection.moveY(-speedMove);
                break;
            case SCALE_LEFT_KEY:
                selection.scaleAddX(-speedMove);
                break;
            case SCALE_RIGHT_KEY:
                selection.scaleAddX(speedMove);
                break;
            case SCALE_IN_KEY:
                selection.scaleAddZ(speedMove);
                break;
            case SCALE_OUT_KEY:
                selection.scaleAddZ(-speedMove);
                break;
            case SCALE_UP_KEY:
                selection.scaleAddY(speedMove);
                break;
            case SCALE_DOWN_KEY:
                selection.scaleAddY(-speedMove);
                break;
        }
        FrameMaster.setNeedsDisplay();
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
