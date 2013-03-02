package general;

import general.Messages.Message;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.List;

public class ChannelProcesser {

	private final static int bufferSize = 1024;
	private static ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

	//**********************************************************************************************
	public static int getBufferSize () {
		return bufferSize;
	}

	//**********************************************************************************************
	private static ByteBuffer StringToByteBuffer(String message) {
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
	private static String ByteBufferToString(ByteBuffer buf) {
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

	//**********************************************************************************************
	public static String getRemoteAddr (SocketChannel channel) {
		String addr;
		try {
			addr = channel.getRemoteAddress().toString();
		} catch (IOException ex) {
			return null;
		}
		return addr;
	}

	//**********************************************************************************************
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
	public static List<Message> receiveMessagesFromChannel (ReadableByteChannel channel, ByteBuffer buffer) {
		try {
			String message = "";
			buffer.clear();
			while (channel.read(buffer) > 0) {
				channel.read(buffer);
				buffer.flip();
				if (buffer.limit() == 0) {
					return null;
				}
				message += ChannelProcesser.ByteBufferToString(buffer);
				buffer.clear();
			}
			System.out.println(message);
			return Message.parseMessages(message);
		} catch (IOException ex) {
			System.err.println(ex.toString());
			return null;
		}
	}

	//**********************************************************************************************
	public static boolean sendMessageToChannel (WritableByteChannel channel, Message msg) {
		ByteBuffer buf;
		buf = ChannelProcesser.StringToByteBuffer(msg.encode());
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
