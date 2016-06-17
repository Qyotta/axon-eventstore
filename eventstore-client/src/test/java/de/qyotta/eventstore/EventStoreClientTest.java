package de.qyotta.eventstore;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.model.SerializableEventData;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@SuppressWarnings("nls")
public class EventStoreClientTest extends AbstractEsTest {
   private String streamName;
   private EventStoreClient client;
   private Map<String, Event> expectedEvents;
   private String streamUrl;
   private int numberOfStoredEvents;

   @Before
   public void setUp() {
      client = new EventStoreClient(EventStoreSettings.withDefaults()
            .build());

      streamName = EventStoreClientTest.class.getSimpleName() + "-" + UUID.randomUUID();
      streamUrl = BASE_STREAMS_URL + streamName;
      expectedEvents = new HashMap<>();
      numberOfStoredEvents = 100;
      for (int i = 0; i < numberOfStoredEvents; i++) {
         final String eventUuid = UUID.randomUUID()
               .toString();
         expectedEvents.put(eventUuid, Event.builder()
               .eventId(eventUuid)
               .eventType("Testtype")
               .data(SerializableEventData.builder()
                     .type(MyEvent.class.getName())
                     .data(new MyEvent(eventUuid))
                     .build())
               .metadata("Test")
               .build());
      }
      client.appendEvents(streamName, expectedEvents.values());
   }

   @After
   public void tearDown() {
      // try hard deleting created streams. This might not do anything depending on the test
      deleteStream(streamUrl);
   }

   @Test
   public void shouldTraverseAllEventsInOrder() {
      final EventStream eventStream = client.readEvents(streamName);
      int count = 0;
      long previousEventNumber = -1;
      while (eventStream.hasNext()) {
         final EventResponse next = eventStream.next();
         final Event actual = next.getContent();
         final Event expected = expectedEvents.get(actual.getEventId());
         assertThat(actual.getEventId(), is(equalTo(expected.getEventId())));
         assertThat(actual.getEventType(), is(equalTo(expected.getEventType())));
         assertThat(actual.getMetadata(), is(equalTo(expected.getMetadata())));
         assertThat(actual.getData(), is(equalTo(expected.getData())));
         assertThat("Next should return the next event in the stream. Previous eventNumber was '" + previousEventNumber + "' but current eventNumber is '" + actual.getEventNumber() + "'.",
               actual.getEventNumber(), is(previousEventNumber + 1));
         previousEventNumber = actual.getEventNumber();
         count++;
      }
      assertThat("Expected to read '" + numberOfStoredEvents + "' events but got '" + count + "'.", count, is(equalTo(numberOfStoredEvents)));
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
