package ch.n1b.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.TreeMap;

public class JsonAssertions {

  private static final JsonMapper MAPPER =
      JsonMapper.builder()
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
          .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
          .defaultLeniency(false)
          .nodeFactory(new SortingNodeFactory())
          .build();

  private JsonAssertions() {}

  public static void assertMatchesJson(String expected, byte[] actual) {

    try {
      var expectedTree = MAPPER.readTree(expected);
      var actualTree = MAPPER.readTree(actual);

      assertEquals(MAPPER.writeValueAsString(expectedTree), MAPPER.writeValueAsString(actualTree));
    } catch (IOException e) {
      fail(e);
    }
  }

  static class SortingNodeFactory extends JsonNodeFactory {
    @Override
    public ObjectNode objectNode() {
      return new ObjectNode(this, new TreeMap<>());
    }
  }
}
