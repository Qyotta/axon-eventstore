package de.qyotta.eventstore;

import static com.jayway.restassured.RestAssured.given;
import static de.qyotta.eventstore.utils.Constants.ES_HARD_DELETE_HEADER;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.BeforeClass;

import com.google.gson.Gson;
import com.jayway.restassured.RestAssured;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@SuppressWarnings("nls")
public class AbstractEsTest {
   static {
      final Logger rootLogger = Logger.getRootLogger();
      rootLogger.setLevel(Level.WARN);
      rootLogger.addAppender(new ConsoleAppender(new PatternLayout("%-6r [%p] %c - %m%n")));
   }
   private static final Logger LOGGER = Logger.getLogger(AbstractEsTest.class.getName());
   private static final int PORT = 2113;
   private static final String BASE_URL = "http://127.0.0.1";
   private static final String STREAMS = "/streams";
   protected static String BASE_STREAMS_URL = BASE_URL + ":" + PORT + STREAMS + "/";
   protected static final String STATS_STREAM = "$stats-127.0.0.1:2113";

   protected static final Map<String, String> METADATA = new LinkedHashMap<>();
   static {
      METADATA.put("TEST", "TEST");
   }

   @BeforeClass
   public static void setupClass() {
      RestAssured.baseURI = BASE_URL;
      RestAssured.port = PORT;
      RestAssured.basePath = STREAMS;
   }

   protected static void deleteStream(final String streamUrl) {
      final int statusCode = given().headers(ES_HARD_DELETE_HEADER, true)
            .when()
            .delete(streamUrl)
            .andReturn()
            .statusCode();
      LOGGER.warn("Deleting stream '" + streamUrl + "' returned status code: " + statusCode);
   }

   @Getter
   @ToString
   @EqualsAndHashCode
   @NoArgsConstructor(access = AccessLevel.PUBLIC)
   @AllArgsConstructor(access = AccessLevel.PUBLIC)
   public static final class MyEvent {
      private String value;
   }

   protected static String metaData() {
      return new Gson().toJson(METADATA);
   }
}
