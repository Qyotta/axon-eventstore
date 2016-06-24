package de.qyotta.eventstore;

import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.qyotta.eventstore.communication.EsReader;
import de.qyotta.eventstore.communication.EsWriter;
import de.qyotta.eventstore.model.Entry;
import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventStreamFeed;
import de.qyotta.eventstore.model.SerializableEventData;
import de.qyotta.eventstore.utils.EsUtils;
import de.qyotta.eventstore.utils.EventDataSerializer;
import de.qyotta.eventstore.utils.HttpClientFactory;

@SuppressWarnings("nls")
public class EsUtilsTest extends AbstractEsTest {
   static final Logger LOGGER = Logger.getLogger(EsUtilsTest.class.getName());
   private EsWriter writer;
   private EsReader reader;
   private String streamUrl;

   @Before
   public void setUp() {
      writer = new EsWriter(HttpClientFactory.httpClient(EventStoreSettings.withDefaults()
            .build()), new EventDataSerializer());
      reader = new EsReader(HttpClientFactory.httpClient(EventStoreSettings.withDefaults()
            .build()), new EventDataSerializer());
      streamUrl = BASE_STREAMS_URL + EventWriterTest.class.getSimpleName() + "-" + UUID.randomUUID();
   }

   @After
   public void tearDown() {
      deleteStream(streamUrl);
   }

   @Test
   public void shouldReadValidSequenceNumberFromTitle() {
      for (int i = 0; i < 10; i++) {
         final Event expected = Event.builder()
               .eventId(UUID.randomUUID()
                     .toString())
               .eventType("Testtype")
               .data(SerializableEventData.builder()
                     .type(String.class)
                     .data("TEST")
                     .build())
               .metadata("Test")
               .build();
         writer.appendEvent(streamUrl, expected);
      }

      final EventStreamFeed feed = reader.readStream(streamUrl);

      for (final Entry entry : feed.getEntries()) {
         final long sequenceNumber = EsUtils.getEventNumber(entry);
         LOGGER.warning("Sequence number: " + sequenceNumber);
         assertTrue(sequenceNumber > -1 && sequenceNumber < 10);
      }
   }

}
