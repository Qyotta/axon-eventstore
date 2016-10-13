package de.qyotta.axonframework.eventstore.config;

import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.axonframework.domain.EventMessage;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventListener;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.axonframework.eventstore.EventStore;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import de.qyotta.axonframework.eventstore.utils.EsEventStoreUtils;
import de.qyotta.eventstore.EventStoreClient;
import de.qyotta.eventstore.EventStoreSettings;
import de.qyotta.eventstore.EventstoreProvider;
import de.qyotta.eventstore.communication.ESContext;
import de.qyotta.eventstore.communication.EsContextDefaultImpl;

@RunWith(SpringJUnit4ClassRunner.class)
//@formatter:off
@ContextConfiguration(classes = {
      TestConfiguration.class,
}, loader = AnnotationConfigContextLoader.class)
//@formatter:on
@SuppressWarnings("nls")
public abstract class AbstractIntegrationTest {
   private static final EventstoreProvider EVENT_STORE_PROVIDER = new EventstoreProvider();
   private static final int PORT = 4445;
   private static final String BASE_URL = "http://127.0.0.1";
   protected static final String HOST = BASE_URL + ":" + PORT;
   protected String myAggregateId;

   protected final List<Object> actualEvents = new LinkedList<>();
   private final EventListener EVENT_LISTENER = new EventListener() {
      @Override
      public void handle(@SuppressWarnings("rawtypes") final EventMessage event) {
         actualEvents.add(event.getPayload());
      }
   };

   @Autowired
   protected EventBus eventBus;
   @Autowired
   protected EventStore eventStore;
   private EventStoreClient client;
   protected EventStoreSettings settings;

   @BeforeClass
   public static void beforeClass() {
      if (!EVENT_STORE_PROVIDER.isRunning()) {
         EVENT_STORE_PROVIDER.start();
      }
   }

   @AfterClass
   public static void afterClass() {
      EVENT_STORE_PROVIDER.stop();
   }

   @Before
   public final void initTest() {
      myAggregateId = UUID.randomUUID()
            .toString();
      eventBus.subscribe(EVENT_LISTENER);
      settings = EventStoreSettings.withDefaults()
            .host(HOST)
            .build();
      final ESContext esContext = new EsContextDefaultImpl(settings);
      client = new EventStoreClient(esContext);
   }

   protected <T extends AbstractAnnotatedAggregateRoot<?>> void deleteEventStream(final Class<T> classOfT, final String aggregateId) {
      client.deleteStream(EsEventStoreUtils.getStreamName(classOfT.getSimpleName(), aggregateId, "domain"), true);
   }

   protected <T> void expectEventsMatchingExactlyOnce(final List<T> expectedEvents) {
      expectEvents(expectedEvents, true);
   }

   protected <T> void expectEvents(final List<T> expectedEvents) {
      expectEvents(expectedEvents, false);
   }

   private <T> void expectEvents(final List<T> expectedEvents, final boolean exactlyOnce) {
      for (final Object expectedEvent : expectedEvents) {
         final AtomicInteger numberOfEventsMatching = new AtomicInteger(0);
         actualEvents.forEach(e -> {
            if (e.equals(expectedEvent)) {
               numberOfEventsMatching.incrementAndGet();
            }
         });
         if (numberOfEventsMatching.get() == 1) {
            continue;
         }
         if (exactlyOnce && numberOfEventsMatching.get() > 1) {
            fail("Expected event: " + expectedEvent + "found, but was matched multible times. (It matched " + numberOfEventsMatching.get() + " times.)");
         }
         final List<Object> eventsOfType = new LinkedList<>();
         for (final Object actual : actualEvents) {
            if (actual.getClass()
                  .equals(expectedEvent.getClass())) {
               eventsOfType.add(actual);
            }
         }
         if (eventsOfType.isEmpty()) {
            fail("Expected an event of type: " + expectedEvent.getClass()
                  .getName() + " but none was published.");
         }
         fail("Expected event: " + expectedEvent + ". Found the following events of the same type but none matched: " + eventsOfType);
      }
   }
}
