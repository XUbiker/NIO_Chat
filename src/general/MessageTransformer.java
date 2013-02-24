package general;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class MessageTransformer {

	private static ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

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
}
