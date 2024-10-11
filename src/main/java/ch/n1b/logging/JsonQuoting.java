package ch.n1b.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/* heavily borrowed from com.fasterxml.jackson.core.io.JsonStringEncoder */
public final class JsonQuoting {

  private static final byte[] HEX_BYTES = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
  private static final int SURR1_FIRST = 0xD800;
  private static final int SURR1_LAST = 0xDBFF;
  private static final int SURR2_FIRST = 0xDC00;
  private static final int SURR2_LAST = 0xDFFF;

  public static final int SURROGATE_BASE = 0x10000 - SURR2_FIRST - (SURR1_FIRST << 10);

  private static final int ESCAPE = -1;

  private static final int[] ESCAPE_LUT;

  static {
    int[] table = new int[128];
    // Control chars need generic escape sequence
    for (int i = 0; i < 32; ++i) {
      table[i] = ESCAPE;
    }
    // Others (and some within that range too) have explicit shorter sequences
    table['"'] = '"';
    table['\\'] = '\\';
    // Escaping of slash is optional, so let's not add it
    table[0x08] = 'b';
    table[0x09] = 't';
    table[0x0C] = 'f';
    table[0x0A] = 'n';
    table[0x0D] = 'r';
    ESCAPE_LUT = table;
  }

  private JsonQuoting() {}

  /**
   * Method that will quote text contents using JSON standard quoting,
   *
   * @param text Value {@link String} to process
   * @param output {@link OutputStream} to append escaped and encoded utf8 bytes to
   */
  public static void writeQuotedUtf8(String text, OutputStream output) throws IOException {
    int inputPtr = 0;
    int inputEnd = text.length();

    main:
    while (inputPtr < inputEnd) {
      final int[] escCodes = ESCAPE_LUT;

      inner_loop: // ASCII and escapes
      while (true) {
        int ch = text.charAt(inputPtr);
        if (ch > 0x7F || escCodes[ch] != 0) {
          break inner_loop;
        }
        output.write((byte) ch);
        if (++inputPtr >= inputEnd) {
          break main;
        }
      }

      // Ok, so what did we hit?
      int ch = text.charAt(inputPtr++);
      if (ch <= 0x7F) { // needs quoting
        int escape = escCodes[ch];
        // ctrl-char, 6-byte escape...

        appendEscapedBytes(ch, escape, output);
        continue main;
      }
      if (ch <= 0x7FF) { // fine, just needs 2 byte output
        output.write((byte) (0xc0 | (ch >> 6)));
        ch = (0x80 | (ch & 0x3f));
      } else { // 3 or 4 bytes
        // Surrogates?
        if (ch < SURR1_FIRST || ch > SURR2_LAST) { // nope
          output.write((byte) (0xe0 | (ch >> 12)));
          output.write((byte) (0x80 | ((ch >> 6) & 0x3f)));
          ch = (0x80 | (ch & 0x3f));
        } else { // yes, surrogate pair
          if (ch > SURR1_LAST) { // must be from first range
            throwIllegalCodepoint(ch);
          }
          // and if so, followed by another from next range
          if (inputPtr >= inputEnd) {
            throwIllegalCodepoint(ch);
          }
          ch = convertSurrogatePair(ch, text.charAt(inputPtr++));
          if (ch > 0x10FFFF) { // illegal, as per RFC 4627
            throwIllegalCodepoint(ch);
          }
          output.write((byte) (0xf0 | (ch >> 18)));
          output.write((byte) (0x80 | ((ch >> 12) & 0x3f)));
          output.write((byte) (0x80 | ((ch >> 6) & 0x3f)));
          ch = (0x80 | (ch & 0x3f));
        }
      }
      output.write((byte) ch);
    }
  }

  private static void appendEscapedBytes(int ch, int esc, OutputStream out) throws IOException {
    out.write('\\');
    if (esc < 0) { // standard escape
      out.write('u');
      if (ch > 0xFF) {
        int hi = (ch >> 8);
        out.write(HEX_BYTES[hi >> 4]);
        out.write(HEX_BYTES[hi & 0xF]);
        ch &= 0xFF;
      } else {
        out.write('0');
        out.write('0');
      }
      out.write(HEX_BYTES[ch >> 4]);
      out.write(HEX_BYTES[ch & 0xF]);
    } else { // 2-char simple escape
      out.write((byte) esc);
    }
  }

  private static int convertSurrogatePair(int p1, int p2) {
    // Ok, then, is the second part valid?
    if (p2 < SURR2_FIRST || p2 > SURR2_LAST) {
      throw new IllegalArgumentException(
          "Broken surrogate pair: first char 0x"
              + Integer.toHexString(p1)
              + ", second 0x"
              + Integer.toHexString(p2)
              + "; illegal combination");
    }
    return (p1 << 10) + p2 + SURROGATE_BASE;
  }

  private static void throwIllegalCodepoint(int c) {
    throw new IllegalArgumentException("invalid unicode codepoint 0x" + Integer.toHexString(c));
  }
}
