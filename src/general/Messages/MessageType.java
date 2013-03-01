package general.Messages;

public enum MessageType {
	SYS_MSG  (0),
	DATA_MSG (1);

	private int value;

	private MessageType (int value) {
		this.value = value;
	}

	public int getInt () {
		return this.value;
	}
}

