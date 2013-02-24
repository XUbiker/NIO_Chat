package general.timer;

import java.util.TimerTask;

public class JCTimerTask extends TimerTask {

	private int index;
	private JCTimerSupport manager;

	public JCTimerTask (JCTimerSupport manager, int idx) {
		this.manager = manager;
		index = idx;
	}

	@Override
	public void run() {
		manager.timerAction(index);
	}

}
