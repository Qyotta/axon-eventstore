package de.qyotta.eventstore;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import de.qyotta.eventstore.communication.ESReader;
import de.qyotta.eventstore.communication.EsReaderDefaultImpl;
import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventStreamFeed;
import de.qyotta.eventstore.model.Link;
import de.qyotta.eventstore.utils.HttpClientFactory;

public class EventReaderTest extends AbstractEsTest {
   private ESReader reader;

   @Before
   public void setUp() throws InterruptedException {
      reader = new EsReaderDefaultImpl(HttpClientFactory.httpClient(EventStoreSettings.withDefaults()
            .host(HOST)
            .build()));
      createEvents(100);
   }

   @Test
   public void shouldReadFeed() {
      // let's read the stats stream as it is already there for the taking
      final EventStreamFeed feed = reader.readStream(streamUrl);
      assertTrue(feed.getLinks()
            .stream()
            .anyMatch(l -> l.getRelation()
                  .equals(Link.LAST)));
      assertTrue(!feed.getEntries()
            .isEmpty());
   }

   @Test
   public void shouldReadEvent() {
      final EventStreamFeed feed = reader.readStream(streamUrl);
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
