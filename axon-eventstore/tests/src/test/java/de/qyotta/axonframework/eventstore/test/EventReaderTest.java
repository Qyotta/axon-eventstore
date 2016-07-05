package de.qyotta.axonframework.eventstore.test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.qyotta.axonframework.eventstore.config.AbstractIntegrationTest;
import de.qyotta.axonframework.eventstore.domain.ChangeTestAggregate;
import de.qyotta.axonframework.eventstore.domain.CreateTestAggregate;
import de.qyotta.axonframework.eventstore.domain.MyTestAggregate;
import de.qyotta.axonframework.eventstore.utils.EsDomainEventReader;
import de.qyotta.axonframework.eventstore.utils.EsDomainEventReader.EsDomainEventReaderCallback;
import de.qyotta.axonframework.eventstore.utils.EsEventStoreUtils;

public class EventReaderTest extends AbstractIntegrationTest {

   @Autowired
   private CommandGateway commandGateway;
   private EsDomainEventReader reader;

   @Before
   public void setUp() {
      reader = new EsDomainEventReader(settings, EsEventStoreUtils.getStreamName(MyTestAggregate.class.getSimpleName(), myAggregateId), -1);
   }

   @After
   public final void tearDown() {
      deleteEventStream(MyTestAggregate.class, myAggregateId);
   }

   @Test
   public void shouldReadAllEvents() throws Exception {
      commandGateway.sendAndWait(new CreateTestAggregate(myAggregateId));
      commandGateway.sendAndWait(new ChangeTestAggregate(myAggregateId));
      commandGateway.sendAndWait(new ChangeTestAggregate(myAggregateId));
      commandGateway.sendAndWait(new ChangeTestAggregate(myAggregateId));
      commandGateway.sendAndWait(new ChangeTestAggregate(myAggregateId));
      final EsDomainEventReaderCallback callback = mock(EsDomainEventReaderCallback.class);
      reader.setCallback(callback);
      reader.start();
      verify(callback, times(5)).onEvent(any());
   }

   @Test
   public void shouldOnlyOneEvent() throws Exception {
      commandGateway.sendAndWait(new CreateTestAggregate(myAggregateId));
      for (int i = 0; i < 76; i++) {
         commandGateway.sendAndWait(new ChangeTestAggregate(myAggregateId));
      }
      final EsDomainEventReaderCallback callback = mock(EsDomainEventReaderCallback.class);
      reader.setCallback(callback);
      reader.start("68@mytestaggregate-" + myAggregateId); //$NON-NLS-1$
      verify(callback, times(1)).onEvent(any());
   }
}