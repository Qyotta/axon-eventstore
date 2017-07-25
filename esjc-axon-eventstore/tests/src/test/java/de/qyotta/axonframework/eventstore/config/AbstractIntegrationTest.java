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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.github.msemys.esjc.ExpectedVersion;

import de.qyotta.axonframework.eventstore.utils.EsjcEventstoreUtil;
import de.qyotta.eventstore.InMemoryEventstoreProvider;

@RunWith(SpringJUnit4ClassRunner.class)
//@formatter:off
@ContextConfiguration(classes = {
      TestConfiguration.class,
}, loader = AnnotationConfigContextLoader.class)
//@formatter:on
@SuppressWarnings("nls")
public abstract class AbstractIntegrationTest {
   private static final InMemoryEventstoreProvider EVENT_STORE_PROVIDER = new InMemoryEventstoreProvider();
   protected String myAggregateId;

   protected final List<Object> actualEvents = new LinkedList<>();
   private final EventListener EVENT_LISTENER = new EventListener() {
      @Override
      public void handle(@SuppressWarnings("rawtypes") final EventMessage event) {
         actualEvents.add(event.getPayload());
      }
   };

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

   @Autowired
   protected EventBus eventBus;
   @Autowired
   protected com.github.msemys.esjc.EventStore eventStore;

   @Before
   public final void initTest() {
      myAggregateId = UUID.randomUUID()
            .toString();
      eventBus.subscribe(EVENT_LISTENER);
   }

   protected <T extends AbstractAnnotatedAggregateRoot<?>> void deleteEventStream(final Class<T> classOfT, final String aggregateId) {
      eventStore.deleteStream(EsjcEventstoreUtil.getStreamName(classOfT.getSimpleName(), aggregateId, "domain"), ExpectedVersion.ANY);
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
