package general.Messages;

public enum MessageType {
	SYS_MSG   ('\u0000'),
	DATA_MSG  ('\u0001'),
	INPUT_MSG ('\u0002');

	private Character value;

	private MessageType (Character value) {
		this.value = value;
	}

	public Character getCharacter () {
		return this.value;
	}
}

