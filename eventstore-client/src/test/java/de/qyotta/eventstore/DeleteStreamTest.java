package de.qyotta.eventstore;

import static com.jayway.restassured.RestAssured.given;
import static de.qyotta.eventstore.utils.Constants.ACCEPT_EVENTSTORE_ATOM_JSON;
import static de.qyotta.eventstore.utils.Constants.ACCEPT_HEADER;
import static de.qyotta.eventstore.utils.Constants.CONTENT_TYPE_HEADER;
import static de.qyotta.eventstore.utils.Constants.CONTENT_TYPE_JSON;
import static de.qyotta.eventstore.utils.Constants.ES_EVENT_ID_HEADER;
import static de.qyotta.eventstore.utils.Constants.ES_EVENT_TYPE_HEADER;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.qyotta.eventstore.communication.EsWriter;
import de.qyotta.eventstore.utils.EventDataSerializer;
import de.qyotta.eventstore.utils.HttpClientFactory;

@SuppressWarnings("nls")
public class DeleteStreamTest extends AbstractEsTest {
   private EsWriter writer;
   private String streamUrl;

   @Before
   public void setUp() {
      writer = new EsWriter(HttpClientFactory.newClosableHttpClient(EventStoreSettings.withDefaults()
            .build()), new EventDataSerializer());
      streamUrl = BASE_STREAMS_URL + EventWriterTest.class.getSimpleName() + "-" + UUID.randomUUID();
      createStream(); // create the stream we will try to delete in the tests
   }

   @After
   public void tearDown() {
      // try hard deleting created streams. This might not do anything depending on the test
      deleteStream(streamUrl);
   }

   @Test
   public void shouldSoftDelete() {
      writer.deleteStream(streamUrl, false);
      // check if reading the stream returns 404 (see: http://docs.geteventstore.com/http-api/3.6.0/deleting-a-stream/)
      given().headers(ACCEPT_HEADER, ACCEPT_EVENTSTORE_ATOM_JSON)
            .when()
            .get(streamUrl)
            .then()
            .assertThat()
            .statusCode(404);
   }

   @Test
   public void shouldHardDelete() {
      writer.deleteStream(streamUrl, true);
      // check if reading the stream returns 410 (see: http://docs.geteventstore.com/http-api/3.6.0/deleting-a-stream/)
      given().headers(ACCEPT_HEADER, ACCEPT_EVENTSTORE_ATOM_JSON)
            .when()
            .get(streamUrl)
            .then()
            .assertThat()
            .statusCode(410);
   }

   private void createStream() {
      final int statusCode = given().headers(ES_EVENT_ID_HEADER, UUID.randomUUID(), ES_EVENT_TYPE_HEADER, "Test", CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON)
            .when()
            .post(streamUrl)
            .andReturn()
            .statusCode();
      assert statusCode == 201; // just to make sure
      LOGGER.warning("Creating stream '" + streamUrl + "' returned status code: " + statusCode);
   }
}
