package general.Messages;

public enum SysMessage {
	CONNECT_CHAR ('\u00e6'),
	START_CHAR   ('\u00dd'),
	SEPAR_CHAR   ('\u00dc'),
	END_CHAR     ('\u00de');

	private Character value;

	private SysMessage (Character value) {
		this.value = value;
	}

	@Override
	public String toString () {
		return this.toString();
	}
}