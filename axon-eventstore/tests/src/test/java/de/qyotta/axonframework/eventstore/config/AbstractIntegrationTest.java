package de.qyotta.axonframework.eventstore.config;

import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.axonframework.domain.EventMessage;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventListener;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import de.qyotta.axonframework.eventstore.utils.EsEventStoreUtils;
import de.qyotta.eventstore.EventStoreClient;
import de.qyotta.eventstore.EventStoreSettings;

@RunWith(SpringJUnit4ClassRunner.class)
//@formatter:off
@ContextConfiguration(classes = {
      TestConfiguration.class,
}, loader = AnnotationConfigContextLoader.class)
//@formatter:on
@SuppressWarnings("nls")
public abstract class AbstractIntegrationTest {
   protected String myAggregateId;
   static {
      final Logger rootLogger = Logger.getRootLogger();
      rootLogger.setLevel(Level.WARN);
      rootLogger.addAppender(new ConsoleAppender(new PatternLayout("%-6r [%p] %c - %m%n")));
   }

   protected final List<Object> actualEvents = new LinkedList<>();
   private final EventListener EVENT_LISTENER = new EventListener() {
      @Override
      public void handle(@SuppressWarnings("rawtypes") final EventMessage event) {
         actualEvents.add(event.getPayload());
      }
   };

   @Autowired
   protected EventBus eventBus;
   private EventStoreClient client;

   @Before
   public final void initTest() {
      myAggregateId = UUID.randomUUID().toString();
      eventBus.subscribe(EVENT_LISTENER);
      client = new EventStoreClient(EventStoreSettings.withDefaults().build());
   }

   protected <T extends AbstractAnnotatedAggregateRoot<?>> void deleteEventStream(final Class<T> classOfT, final String aggregateId) {
      client.deleteStream(EsEventStoreUtils.getStreamName(classOfT.getSimpleName(), aggregateId), true);
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
            if (actual.getClass().equals(expectedEvent.getClass())) {
               eventsOfType.add(actual);
            }
         }
         if (eventsOfType.isEmpty()) {
            fail("Expected an event of type: " + expectedEvent.getClass().getName() + " but none was published.");
         }
         fail("Expected event: " + expectedEvent + ". Found the following events of the same type but none matched: " + eventsOfType);
      }
   }
}
