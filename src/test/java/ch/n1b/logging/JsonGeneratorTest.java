package ch.n1b.logging;

import static ch.n1b.logging.JsonAssertions.assertMatchesJson;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class JsonGeneratorTest {

  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private final JsonGenerator gen = new JsonGenerator(out);

  @Test
  void helloWorld() {

    gen.writeString("Hello World!");
    assertMatchesJson("\"Hello World!\"", out.toByteArray());
  }

  @Test
  void escaping() {

    gen.writeString("Hello\r\n\t\0World 💣");
    assertMatchesJson("\"Hello\\r\\n\\t\\u0000World 💣\"", out.toByteArray());
  }

  @Test
  void simple() {

    gen.writeStartObject();
    gen.writeStringField("message", "Hello World!");
    gen.writeStringField("severity", "INFO");
    gen.writeEndObject();

    assertMatchesJson("""
{"message":"Hello World!","severity":"INFO"}
""", out.toByteArray());
  }

  @Test
  void nullField() {

    gen.writeStartObject();
    gen.writeStringField("message", "Hello World!");
    gen.writeStringField("severity", null);
    gen.writeEndObject();

    assertMatchesJson(
        """
    {"message":"Hello World!","severity":null}
    """, out.toByteArray());
  }

  @Test
  void noOpenContext() {

    gen.writeStringField("a", "b");
    assertThrows(IllegalStateException.class, gen::writeEndObject);
  }

  @Test
  void nested() {

    gen.writeStartObject();
    gen.writeStringField("severity", "SILLY");
    gen.writeObjectFieldStart("values");
    gen.writeStringField("ma_key", "a value");
    gen.writeEndObject();
    gen.writeStringField("msg", "extra text!");
    gen.writeEndObject();

    assertMatchesJson(
        """
    {"msg":"extra text!","severity":"SILLY","values":{"ma_key":"a value"}}
    """,
        out.toByteArray());
  }

  @Test
  void deeplyNested() {

    var N = 16;
    gen.writeStartObject();
    for (int i = 0; i < N; i++) {
      gen.writeStringField("a", "b");
      gen.writeStringField("c", null);
      gen.writeObjectFieldStart("n");
    }
    for (int i = 0; i < N; i++) {
      gen.writeEndObject();
    }
    gen.writeEndObject();

    assertMatchesJson(
        """
        {"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{"a":"b","c":null,"n":{}}}}}}}}}}}}}}}}}
        """,
        out.toByteArray());
  }
}
