package general.Messages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class MessageProcesser {

	private final static int bufferSize = 1024;
	private static ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

	//**********************************************************************************************
	public static int getBufferSize () {
		return bufferSize;
	}

	//**********************************************************************************************
	public static ByteBuffer String2ByteBuffer(String message) {
		try {
			//buffer.clear();
			Charset charset = Charset.forName("UTF-8");
			CharsetEncoder encoder = charset.newEncoder();
			encoder.reset();
			buffer = encoder.encode(CharBuffer.wrap(message));
			//encoder.flush(buffer);
			return buffer;
		} catch (CharacterCodingException ex) {
			System.err.println(ex.getMessage());
			return null;
		}
	}

	//**********************************************************************************************
	public static String ByteBuffer2String(ByteBuffer buf) {
		try {
			Charset charset = Charset.forName("UTF-8");
			CharsetDecoder decoder = charset.newDecoder();

			CharBuffer charBuffer = decoder.decode(buf);
			// remove '\n' from the end of the line
			if (charBuffer.get(charBuffer.length() - 1) == '\n') {
				charBuffer = charBuffer.subSequence(0, charBuffer.length() - 1);
			}
			return charBuffer.toString();
		} catch (CharacterCodingException ex) {
			System.err.println(ex.getMessage());
			return null;
		}
	}

	public static String getRemoteAddr (SocketChannel channel) {
		String addr;
		try {
			addr = channel.getRemoteAddress().toString();
		} catch (IOException ex) {
			return null;
		}
		return addr;
	}

	public static String getLocalAddr (SocketChannel channel) {
		String addr;
		try {
			addr = channel.getLocalAddress().toString();
		} catch (IOException ex) {
			return null;
		}
		return addr;
	}

	//**********************************************************************************************
	public static String readStringFromChannel (ReadableByteChannel sChannel, ByteBuffer buffer) {
		// reads string from input channel. returns null in case of failure
		try {
			String message = "";
			buffer.clear();
			while (sChannel.read(buffer) > 0) {
				sChannel.read(buffer);
				buffer.flip();
				if (buffer.limit() == 0) {
					return null;
				}
				message += MessageProcesser.ByteBuffer2String(buffer);
				buffer.clear();
			}
			return message;
		} catch (IOException ex) {
			return null;
		}
	}

	//**********************************************************************************************
	public static boolean sendMessage(SocketChannel channel, String exciter, String message, MessageType type) {
		// type == 0 - sys message, type == 0 - data message
		ByteBuffer buf = null;
		if (type.equals(MessageType.DATA_MSG)) {
			buf = MessageProcesser.String2ByteBuffer(SysMessage.START_CHAR +
					exciter + SysMessage.SEPAR_CHAR + message + SysMessage.END_CHAR);
		} else if (type.equals(MessageType.SYS_MSG)) {
			buf = MessageProcesser.String2ByteBuffer(message);
		}
		try {
			if (buf != null) {
				channel.write(buf);
			}
			return true;
		} catch (IOException ex) {
			return false;
		}
	}

}
