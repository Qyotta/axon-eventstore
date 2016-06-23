package de.qyotta.eventstore;

import static com.jayway.restassured.RestAssured.given;
import static de.qyotta.eventstore.utils.Constants.ES_HARD_DELETE_HEADER;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;

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
      Logger.getGlobal()
            .getParent()
            .getHandlers()[0].setLevel(Level.WARNING);
   }
   private static final Logger LOGGER = Logger.getLogger(AbstractEsTest.class.getName());
   private static final int PORT = 2113;
   private static final String BASE_URL = "http://127.0.0.1";
   private static final String STREAMS = "/streams";
   protected static String BASE_STREAMS_URL = BASE_URL + ":" + PORT + STREAMS + "/";
   protected static final String STATS_STREAM = "$stats-127.0.0.1:2113";

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
      LOGGER.warning("Deleting stream '" + streamUrl + "' returned status code: " + statusCode);
   }

   @Getter
   @ToString
   @EqualsAndHashCode
   @NoArgsConstructor(access = AccessLevel.PUBLIC)
   @AllArgsConstructor(access = AccessLevel.PUBLIC)
   public static final class MyEvent {
      private String value;
   }
}
