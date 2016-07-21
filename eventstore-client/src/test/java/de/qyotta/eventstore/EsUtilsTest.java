package de.qyotta.eventstore;

import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.qyotta.eventstore.communication.ESReader;
import de.qyotta.eventstore.communication.ESWriter;
import de.qyotta.eventstore.communication.EsReaderDefaultImpl;
import de.qyotta.eventstore.communication.EsWriterDefaultImpl;
import de.qyotta.eventstore.model.Entry;
import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventStreamFeed;
import de.qyotta.eventstore.utils.EsUtils;
import de.qyotta.eventstore.utils.HttpClientFactory;

@SuppressWarnings("nls")
public class EsUtilsTest extends AbstractEsTest {
   static final Logger LOGGER = LoggerFactory.getLogger(EsUtilsTest.class.getName());
   private ESWriter writer;
   private ESReader reader;

   @Before
   public void setUp() {
      writer = new EsWriterDefaultImpl(HttpClientFactory.httpClient(EventStoreSettings.withDefaults()
            .build()));
      reader = new EsReaderDefaultImpl(HttpClientFactory.httpClient(EventStoreSettings.withDefaults()
            .build()));
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
               .data("{\"test\": \"test\"}")
               .metadata("{\"test\": \"test\"}")
               .build();
         writer.appendEvent(streamUrl, expected);
      }

      final EventStreamFeed feed = reader.readStream(streamUrl);

      for (final Entry entry : feed.getEntries()) {
         final long sequenceNumber = EsUtils.getEventNumber(entry);
         LOGGER.warn("Sequence number: " + sequenceNumber);
         assertTrue(sequenceNumber > -1 && sequenceNumber < 10);
      }
   }

}
