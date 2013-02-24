package general.timer;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class JCTimer {

	private Timer timer;
	private int connectDelay = 500, connectPeriod = 1500;
	private int receiveDelay = 200, receivePeriod = 250;
	private JCTimerSupport manager;
	private int idx;

	private ArrayList<TimerTask> tasks;

	//**********************************************************************************************
	public int addTask(int delay, int period) {
		idx++;
		JCTimerTask ttask = new JCTimerTask(manager, idx);
		tasks.add(ttask);
		timer.scheduleAtFixedRate(ttask, delay, period);
		return idx;
	}

	//**********************************************************************************************
	public JCTimer(JCTimerSupport manager) {
		tasks = new ArrayList<>();
		idx = 0;
		this.manager = manager;
		timer = new Timer();
	}
}
