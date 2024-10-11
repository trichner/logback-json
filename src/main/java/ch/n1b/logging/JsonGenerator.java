package ch.n1b.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Build properly escaped JSON structures. Reference: <a
 * href="https://www.ietf.org/rfc/rfc4627.txt">RFC-4627</a>
 */
public class JsonGenerator {
  private final OutputStream out;

  // accounting to append commas when needed
  private final Deque<Boolean> contexts = new LinkedList<>();
  private boolean currentContext = false;

  public JsonGenerator(OutputStream out) {
    this.out = out;
  }

  public void writeStartObject() {
    tryWrite('{');
    openContext();
  }

  public void writeEndObject() {
    tryWrite('}');
    closeContext();
  }

  public void writeRaw(char c) {
    tryWrite(c);
  }

  public void writeObjectFieldStart(String key) {
    if (currentContext) {
      tryWrite(',');
    }
    currentContext = true;
    writeString(key);
    tryWrite(':');
    writeStartObject();
  }

  public void writeStringField(String key, String string) {
    if (currentContext) {
      tryWrite(',');
    }
    currentContext = true;
    writeString(key);
    tryWrite(':');
    writeString(string);
  }

  public void writeString(String s) {
    if (s == null) {
      writeNull();
    } else {
      tryWrite('"');
      tryWriteQuoted(s);
      tryWrite('"');
    }
  }

  public void writeNull() {
    tryWriteUtf8("null");
  }

  private void openContext() {
    contexts.push(currentContext);
    currentContext = false;
  }

  private void closeContext() {
    if (contexts.isEmpty()) {
      throw new IllegalStateException("closed more objects than opened");
    }
    currentContext = contexts.pop();
  }

  private void tryWrite(int b) {
    try {
      out.write(b);
    } catch (IOException e) {
      throw new WriteException(e);
    }
  }

  private void tryWriteUtf8(String s) {
    tryWrite(s.getBytes(StandardCharsets.UTF_8));
  }

  private void tryWriteQuoted(String s) {
    try {
      JsonQuoting.writeQuotedUtf8(s, out);
    } catch (IOException e) {
      throw new WriteException(e);
    }
  }

  private void tryWrite(byte[] bytes) {
    try {
      out.write(bytes);
    } catch (IOException e) {
      throw new WriteException(e);
    }
  }

  public static class WriteException extends RuntimeException {
    public WriteException(Throwable cause) {
      super(cause);
    }
  }
}
