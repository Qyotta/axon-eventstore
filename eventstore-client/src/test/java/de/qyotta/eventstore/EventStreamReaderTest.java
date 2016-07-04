package de.qyotta.eventstore;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_SECOND;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gson.Gson;

import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.utils.EventStreamReader;
import de.qyotta.eventstore.utils.EventStreamReaderImpl.EventStreamReaderCallback;

@SuppressWarnings("nls")
public class EventStreamReaderTest extends AbstractEsTest {
   private String streamName;
   private EventStoreClient client;
   private static Map<String, Event> expectedEvents;
   private String streamUrl;

   @Before
   public void setUp() {
      client = new EventStoreClient(EventStoreSettings.withDefaults()
            .build());

      streamName = EventStreamReaderTest.class.getSimpleName() + "-" + UUID.randomUUID();
      streamUrl = BASE_STREAMS_URL + streamName;
      expectedEvents = new HashMap<>();
   }

   private void createEvents(int numberOfEvents) throws InterruptedException {
      for (int i = 0; i < numberOfEvents; i++) {
         final String eventUuid = UUID.randomUUID()
               .toString();
         final Event event = Event.builder()
               .eventId(eventUuid)
               .eventType("Testtype")
               .data(new Gson().toJson(new MyEvent(eventUuid)))
               .metadata(metaData())
               .build();
         expectedEvents.put(eventUuid, event);
         client.appendEvent(streamName, event);
      }
   }

   @After
   public void tearDown() {
      // try hard deleting created streams. This might not do anything depending on the test
      deleteStream(streamUrl);
   }

   @Test
   public void shouldTraverseAllEventsInOrder() throws InterruptedException {
      createEvents(10);
      final EventStreamReader eventStreamReader = client.newEventStreamReader(streamName, -1, new EventStreamReaderCallback() {
         private long previousEventNumber = -1;

         @Override
         public void readEvent(final EventResponse event) {
            final Event expected = expectedEvents.remove(event.getContent()
                  .getEventId());
            assertThat(expected, is(notNullValue()));
            final Long currentEventNumber = event.getContent()
                  .getEventNumber();
            assertThat(currentEventNumber, is(greaterThan(previousEventNumber)));
            previousEventNumber = currentEventNumber;
         }
      });
      eventStreamReader.start();

   }

   @Test
   public void shouldCatchUpOnStream() throws InterruptedException {

      final String firsteventId = prepareAnEventInStream();

      final EventStreamReaderCallback callback = mock(EventStreamReaderCallback.class);
      final EventStreamReader eventStreamReader = client.newEventStreamReader(streamName, -1, callback);

      eventStreamReader.start(firsteventId);
      verify(callback, never()).readEvent(any(EventResponse.class));

      final Event expected = Event.builder()
            .eventId(UUID.randomUUID()
                  .toString())
            .eventType("something")
            .data(new Gson().toJson(new MyEvent(UUID.randomUUID()
                  .toString())))
            .metadata(metaData())
            .build();
      client.appendEvent(streamName, expected);
      eventStreamReader.catchUp();
      final ArgumentCaptor<EventResponse> argument = ArgumentCaptor.forClass(EventResponse.class);
      verify(callback).readEvent(argument.capture());
      final Event actual = argument.getValue()
            .getContent();
      assertThat(actual.getEventId(), is(equalTo(expected.getEventId())));
      assertThat(actual.getEventType(), is(equalTo(expected.getEventType())));
      assertThat(actual.getMetadata(), is(equalTo(expected.getMetadata())));
      assertThat(actual.getData(), is(equalTo(expected.getData())));
   }

   @Test
   public void shouldCatchUpAfterIntervalExpires() throws InterruptedException {
      final String firsteventId = prepareAnEventInStream();
      final EventStreamReaderCallback callback = mock(EventStreamReaderCallback.class);
      final int intervalMillis = 500;
      final EventStreamReader eventStreamReader = client.newEventStreamReader(streamName, intervalMillis, callback);

      eventStreamReader.start(firsteventId);
      verify(callback, never()).readEvent(any(EventResponse.class));
      createEvents(1);

      final AtomicBoolean condition = new AtomicBoolean(false);
      doAnswer(new Answer<Void>() {
         @Override
         public Void answer(InvocationOnMock invocation) throws Throwable {
            condition.set(true);
            return null;
         }
      }).when(callback)
            .readEvent(any(EventResponse.class));
      await().atMost(ONE_SECOND)
            .until(() -> condition.get()); // just wait twice the time we scheduled the reader
   }

   @Test
   public void shouldCatchUpOnStreamWithMultipleEventsInBetween() throws InterruptedException {
      final String firsteventId = prepareAnEventInStream();

      final EventStreamReaderCallback callback = mock(EventStreamReaderCallback.class);
      final EventStreamReader eventStreamReader = client.newEventStreamReader(streamName, -1, callback);

      eventStreamReader.start(firsteventId);
      verify(callback, never()).readEvent(any(EventResponse.class));

      createEvents(10);
      eventStreamReader.catchUp();
      verify(callback, times(10)).readEvent(any(EventResponse.class));
   }

   private String prepareAnEventInStream() {
      final Event given = Event.builder()
            .eventId(UUID.randomUUID()
                  .toString())
            .eventType("Testtype")
            .data(new Gson().toJson(new MyEvent(UUID.randomUUID()
                  .toString())))
            .metadata(metaData())
            .build();
      client.appendEvent(streamName, given);
      return client.readEvents(streamName)
            .next()
            .getId();
   }

}
