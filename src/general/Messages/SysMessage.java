package general.Messages;

public enum SysMessage {
//	CONNECT_CHAR ('\u00e6'),
//	START_CHAR   ('\u00dd'),
//	SEPAR_CHAR   ('\u00dc'),
//	END_CHAR     ('\u00de');
	CONNECT_CHAR ('\u0004'),
	START_CHAR   ('\u0005'),
	SEPAR_CHAR   ('\u0006'),
	END_CHAR     ('\u0007');

	private Character value;

	private SysMessage (Character value) {
		this.value = value;
	}

	public Character getChar () {
		return this.value;
	}

}