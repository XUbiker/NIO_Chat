package general.Messages;

import java.util.ArrayList;
import java.util.List;

public class Message {

	private String exciter = "";
	private String message = "";
	private MessageType type;

	public Message (MessageType type, String exciter, String message) {
		this.type = type;
		this.exciter = exciter;
		this.message = message;
	}

	public Message (MessageType type, String exciter) {
		this.type = type;
		this.exciter = exciter;
	}

	public Message (MessageType type) {
		this.type = type;
	}

	public String getMessage () {
		return message;
	}

	public String getExciter () {
		return exciter;
	}

	public void setExciter (String exciter) {
		this.exciter = exciter;
	}

	public MessageType getType () {
		return type;
	}

	//**********************************************************************************************
	public String encode () {
		String res;
		res = SysMessage.START_CHAR.getChar().toString()
			+ type.getCharacter()
			+ SysMessage.SEPAR_CHAR.getChar()
			+ exciter
			+ SysMessage.SEPAR_CHAR.getChar()
			+ message
			+ SysMessage.END_CHAR.getChar();
		return res;
	}

	//**********************************************************************************************
	@Override
	public String toString () {
		String res;
		res = "Message\n"
			+ "type: " + type + "\n"
			+ "exciter: " + exciter + "\n"
			+ "body: " + message + "\n";
		return res;
	}

	//**********************************************************************************************
	public static List<Message> parseMessages (String str) {
		System.out.println("parsing string: " + str);
		ArrayList<Message> msgList = new ArrayList<>();

		int startIdx = -1, endIdx, separ1Idx, separ2Idx;
		while (true) {
			startIdx = str.indexOf(SysMessage.START_CHAR.getChar(), startIdx + 1);
			separ1Idx = str.indexOf(SysMessage.SEPAR_CHAR.getChar(), startIdx + 1);
			separ2Idx = str.indexOf(SysMessage.SEPAR_CHAR.getChar(), separ1Idx + 1);
			endIdx = str.indexOf(SysMessage.END_CHAR.getChar(), separ2Idx + 1);
			if (startIdx == -1 || separ1Idx == -1 || separ2Idx == -1 || endIdx == -1) {
				break;
			}
			if (separ1Idx - startIdx != 2) {
				break;
			}
			String messageBody = str.substring(separ2Idx + 1, endIdx);
			String exciter = str.substring(separ1Idx + 1, separ2Idx);
			char c = str.charAt(startIdx + 1);
			MessageType type;
			if (MessageType.SYS_MSG.getCharacter() == c) {
				type = MessageType.SYS_MSG;
			} else if (MessageType.DATA_MSG.getCharacter() == c) {
				type = MessageType.DATA_MSG;
			} else if (MessageType.INPUT_MSG.getCharacter() == c) {
				type = MessageType.INPUT_MSG;
			} else {
				break;
			}
			Message msg = new Message(type, exciter, messageBody);
			msgList.add(msg);
			startIdx = endIdx;
		}
		System.out.println("Messages in list: " + msgList.size());
		return msgList;
	}

}
