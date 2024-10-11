package ch.n1b.logging;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonQuotingTest {

  private static final int MAX_CODE_POINT = 0x10FFFF;

  @ParameterizedTest
  @MethodSource("testCases")
  void writeQuotedUtf8_random(String raw, String expected) throws IOException {
    var got = new String(quoteUtf8(raw), StandardCharsets.UTF_8);
    assertEquals(expected, got);
  }

  static Stream<Arguments> testCases() {
    return Stream.of(
        Arguments.of("a", "a"),
        Arguments.of("🔥", "🔥"),
        Arguments.of("\r\"", "\\r\\\""),
        Arguments.of(" \t \n \0 ", " \\t \\n \\u0000 "));
  }

  @Test
  void writeQuotedUtf8_random() throws IOException {

    var rand = new Random(12345);

    for (int i = 0; i < 1024; i++) {
      var s = generateUtf8(rand);
      assertArrayEquals(referenceQuoteUtf8(s), quoteUtf8(s));
    }
  }

  private String generateUtf8(Random rand) {

    var sb = new StringBuilder();
    while (sb.length() < 128) {

      var c = rand.nextInt(MAX_CODE_POINT);
      if (Character.isAlphabetic(c)
          || Character.isDigit(c)
          || Character.isWhitespace(c)
          || Character.isISOControl(c)) {
        sb.appendCodePoint(c);
      }
    }

    return sb.toString();
  }

  private byte[] referenceQuoteUtf8(String s) {
    return JsonStringEncoder.getInstance().quoteAsUTF8(s);
  }

  private byte[] quoteUtf8(String s) throws IOException {
    var baos = new ByteArrayOutputStream();
    JsonQuoting.writeQuotedUtf8(s, baos);
    return baos.toByteArray();
  }
}
