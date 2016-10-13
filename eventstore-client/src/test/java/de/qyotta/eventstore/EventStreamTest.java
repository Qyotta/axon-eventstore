package de.qyotta.eventstore;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.exparity.hamcrest.date.DateMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import de.qyotta.eventstore.communication.EsContextDefaultImpl;
import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.utils.EsUtils;

@SuppressWarnings("nls")
public class EventStreamTest extends AbstractEsTest {
   private int numberOfStoredEvents;

   @Before
   public void setUp() {
      client = new EventStoreClient(new EsContextDefaultImpl(EventStoreSettings.withDefaults()
            .host(HOST)
            .build()));

      streamName = EventStreamTest.class.getSimpleName() + "-" + UUID.randomUUID();
      streamUrl = BASE_STREAMS_URL + streamName;
      expectedEvents = new HashMap<>();
   }

   @After
   public void tearDown() {
      // try hard deleting created streams. This might not do anything depending on the test
      deleteStream(streamUrl);
   }

   @Test
   public void shouldTraverseAllEventsInOrder() throws InterruptedException {
      numberOfStoredEvents = 100;
      createEvents(numberOfStoredEvents);

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

   @Test
   public void shouldNotFailOnASingleStoredEvent() throws InterruptedException {
      numberOfStoredEvents = 1;
      createEvents(numberOfStoredEvents);
      final EventStream eventStream = client.readEvents(streamName);
      final EventResponse next = eventStream.next();
      final Event actual = next.getContent();
      final Event expected = expectedEvents.get(actual.getEventId());
      assertThat(actual.getEventId(), is(equalTo(expected.getEventId())));
      assertThat(actual.getEventType(), is(equalTo(expected.getEventType())));
      assertThat(actual.getMetadata(), is(equalTo(expected.getMetadata())));
      assertThat(actual.getData(), is(equalTo(expected.getData())));
      assertFalse(eventStream.hasNext());
   }

   @Test
   public void shouldSetToTheBeginning() throws InterruptedException {
      numberOfStoredEvents = 100;
      createEvents(numberOfStoredEvents);

      final EventStream eventStream = client.readEvents(streamName);
      // got to the end
      while (eventStream.hasNext()) {
         eventStream.next();
      }
      // now set to the beginning
      final Date fromDate = Date.from(Instant.EPOCH);
      eventStream.setAfterTimestamp(fromDate);
      final EventResponse next = eventStream.next();
      assertThat(EsUtils.timestampOf(next), is(DateMatchers.after(fromDate)));
      assertThat(next.getContent()
            .getEventNumber(), is(0L));
   }

   @Test
   public void shouldSetAfterTheGivenTime() throws InterruptedException {
      numberOfStoredEvents = 100;
      createEvents(numberOfStoredEvents);

      final EventStream eventStream = client.readEvents(streamName);
      // got to event 49
      for (int i = 0; i < 50; i++) {
         eventStream.next();
      }
      // take timestamp of event nr. 50
      final EventResponse next = eventStream.next();
      final Date fromDate = EsUtils.timestampOf(next);
      // now create a new stream
      final EventStream eventStream2 = client.readEvents(streamName);
      eventStream2.setAfterTimestamp(fromDate);

      final EventResponse eventNr51 = eventStream2.next();
      assertThat(EsUtils.timestampOf(eventNr51), is(DateMatchers.after(fromDate)));
      assertThat(eventNr51.getContent()
            .getEventNumber(), is(51L));
   }

   @Test
   public void shouldSetAfterTheGivenEventId() throws InterruptedException {
      numberOfStoredEvents = 100;
      createEvents(numberOfStoredEvents);

      for (int i = 0; i < 99; i++) {
         shouldSetTo(i);
      }
      final EventStream eventStream = setTo(99);

      assertThat(eventStream.next(), is(nullValue()));
   }

   private void shouldSetTo(int i) {
      final EventStream eventStream = setTo(i);
      final EventResponse event = eventStream.next();
      assertThat(event.getContent()
            .getEventNumber(), is(Long.valueOf(i + 1)));
   }

   private EventStream setTo(int i) {
      final EventStream eventStream = client.readEvents(streamName);
      eventStream.setAfterTitle(i + "@" + streamName);
      return eventStream;
   }

   @Test
   public void loadNextTest() throws InterruptedException {
      numberOfStoredEvents = 10;
      createEvents(numberOfStoredEvents);
      final EventStream eventStream = client.readEvents(streamName);
      // got to the end
      while (eventStream.hasNext()) {
         eventStream.next();
      }
      assertThat(eventStream.hasNext(), is(false));
      assertThat(eventStream.peek(), is(nullValue()));
      // now add an event
      final Event expected = Event.builder()
            .eventId(UUID.randomUUID()
                  .toString())
            .eventType("Testtype")
            .data(new Gson().toJson(new MyEvent(UUID.randomUUID()
                  .toString())))
            .metadata(metaData())
            .build();
      client.appendEvent(streamName, expected);
      eventStream.loadNext();
      assertThat(eventStream.hasNext(), is(true));
      final EventResponse next = eventStream.next();
      assertThat(next.getContent()
            .getEventId(), is(equalTo(expected.getEventId())));
      assertThat(next.getContent()
            .getData(), is(equalTo(expected.getData())));
   }
}
