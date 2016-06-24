package de.qyotta.eventstore;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.qyotta.eventstore.communication.EsReader;
import de.qyotta.eventstore.communication.EsWriter;
import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.model.EventStreamFeed;
import de.qyotta.eventstore.model.Link;
import de.qyotta.eventstore.model.SerializableEventData;
import de.qyotta.eventstore.utils.EventDataSerializer;
import de.qyotta.eventstore.utils.HttpClientFactory;

@SuppressWarnings("nls")
public class EventWriterTest extends AbstractEsTest {
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
   public void shouldAppendEvent() {
      final String eventId = UUID.randomUUID()
            .toString();

      final Event expected = Event.builder()
            .eventId(eventId)
            .eventType("Testtype")
            .data(SerializableEventData.builder()
                  .type(String.class)
                  .data("TEST")
                  .build())
            .metadata("Test")
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

}
