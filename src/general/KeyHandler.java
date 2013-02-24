package general;

import JCClient.JCClientFrame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

public class KeyHandler implements KeyListener {

	private ArrayList<Integer> pressed;
	private JCClientFrame frame;
	private int[] handledKeys = {KeyEvent.VK_CONTROL, KeyEvent.VK_ENTER};


	public KeyHandler (JCClientFrame frame) {
		this.frame = frame;
		pressed = new ArrayList<>();
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent key) {
		if (!pressed.contains(key.getKeyCode())) {
			pressed.add(key.getKeyCode());
		}
//		if (pressed.size() == 2) {
//			if (pressed.contains(KeyEvent.VK_CONTROL) && pressed.contains(KeyEvent.VK_ENTER)) {
//				// perform action;
//				frame.sendMessage();
//			}
//		}
		if (pressed.size() == handledKeys.length) {
			boolean result = true;
			for (int i = 0; i < handledKeys.length; i++) {
				if (!pressed.contains(handledKeys[i])) {
					result = false;
				}
			}
			if (result == true) {
				// perform action;
				frame.sendMessage();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent key) {
		if (pressed.contains(key.getKeyCode())) {
			pressed.remove((Object)key.getKeyCode());
		}
	}

}
