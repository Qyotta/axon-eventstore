package de.qyotta.eventstore;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import de.qyotta.eventstore.communication.EsReader;
import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventStreamFeed;
import de.qyotta.eventstore.model.Link;
import de.qyotta.eventstore.utils.EventDataSerializer;
import de.qyotta.eventstore.utils.HttpClientFactory;

public class EventReaderTest extends AbstractEsTest {
   private EsReader reader;

   @Before
   public void setUp() {
      reader = new EsReader(HttpClientFactory.newClosableHttpClient(EventStoreSettings.withDefaults()
            .build()), new EventDataSerializer());
   }

   @Test
   public void shouldReadFeed() {
      // let's read the stats stream as it is already there for the taking
      final EventStreamFeed feed = reader.readStream(BASE_STREAMS_URL + STATS_STREAM);
      assertTrue(feed.getLinks()
            .stream()
            .anyMatch(l -> l.getRelation()
                  .equals(Link.LAST)));
      assertTrue(!feed.getEntries()
            .isEmpty());
   }

   @Test
   public void shouldReadEvent() {
      final EventStreamFeed feed = reader.readStream(BASE_STREAMS_URL + STATS_STREAM);
      feed.getEntries()
            .get(0)
            .getLinks()
            .forEach(l -> {
               if (l.getRelation()
                     .equals(Link.EDIT)) {
                  final Event event = reader.readEvent(l.getUri())
                        .getContent();
                  assertThat(event.getEventNumber(), notNullValue());
                  assertThat(event.getEventStreamId(), notNullValue());
                  assertThat(event.getEventId(), notNullValue());
                  assertThat(event.getEventType(), notNullValue());
                  assertThat(event.getData(), notNullValue());
                  assertThat(event.getMetadata(), notNullValue());
               }
            });
   }
}
