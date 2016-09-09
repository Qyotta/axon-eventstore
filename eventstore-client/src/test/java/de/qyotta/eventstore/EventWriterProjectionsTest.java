package de.qyotta.eventstore;

import de.qyotta.eventstore.communication.ESReader;
import de.qyotta.eventstore.communication.ESWriter;
import de.qyotta.eventstore.communication.EsReaderDefaultImpl;
import de.qyotta.eventstore.communication.EsWriterDefaultImpl;
import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.model.EventStreamFeed;
import de.qyotta.eventstore.model.Link;
import de.qyotta.eventstore.utils.HttpClientFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("nls")
public class EventWriterProjectionsTest extends AbstractEsTest {
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
   public void shouldAppendEvent() {
      final String eventId = UUID.randomUUID()
            .toString();

      final Event expected = Event.builder()
            .eventId(eventId)
            .eventType("Testtype")
            .data("{\"test\":\"test\"}")
            .metadata("{\"test\":\"test\"}")
            .build();
      writer.appendEvent(streamUrl, expected);
      final EventStreamFeed feed = reader.readStream(streamUrl);

      for (final Link l : feed.getEntries()
            .get(0)
            .getLinks()) {
         if (!l.getRelation()
               .equals(Link.EDIT)) {
            continue;
         }
         final EventResponse readEvent = reader.readEvent(l.getUri());
         final Event actual = readEvent.getContent();

         assertThat(actual.getEventId(), is(equalTo(expected.getEventId())));
         assertThat(actual.getEventType(), is(equalTo(expected.getEventType())));
         assertThat(actual.getMetadata(), is(equalTo(expected.getMetadata())));
         assertThat(actual.getData(), is(equalTo(expected.getData())));
         return;
      }
      fail();
   }

   @Test
   @Ignore
   public void shouldCreateLinkedProjection() {
      writer.createLinkedProjection(HOST, "MY_TEST_STREAM", "conversion-e8dd68d4-2b6b-4736-9084-c213dd987284", "conversionlogs-63e8d038-286b-3e9c-b73a-c97e8cf20dec");
   }

}
