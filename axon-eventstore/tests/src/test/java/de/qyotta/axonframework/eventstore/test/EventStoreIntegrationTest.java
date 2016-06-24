package de.qyotta.axonframework.eventstore.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.domain.MetaData;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import de.qyotta.axonframework.eventstore.config.AbstractIntegrationTest;
import de.qyotta.axonframework.eventstore.domain.ChangeTestAggregate;
import de.qyotta.axonframework.eventstore.domain.CreateTestAggregate;
import de.qyotta.axonframework.eventstore.domain.TestAggregate;

@SuppressWarnings("nls")
public class EventStoreIntegrationTest extends AbstractIntegrationTest {

   @Autowired
   private CommandGateway commandGateway;

   @After
   public final void tearDown() {
      deleteEventStream(TestAggregate.class, myAggregateId);
   }

   @Test
   public void shouldHandleCommands() throws Exception {
      commandGateway.sendAndWait(new CreateTestAggregate(myAggregateId));
      commandGateway.sendAndWait(new ChangeTestAggregate(myAggregateId));
      commandGateway.sendAndWait(new ChangeTestAggregate(myAggregateId));
      commandGateway.sendAndWait(new ChangeTestAggregate(myAggregateId));
      commandGateway.sendAndWait(new ChangeTestAggregate(myAggregateId));
   }

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void shouldRehydrateAndThrow() throws Exception {
      thrown.expect(CommandExecutionException.class);
      thrown.expectCause(isA(IllegalStateException.class));

      commandGateway.sendAndWait(new CreateTestAggregate(myAggregateId));
      for (int i = 0; i < 10; i++) {
         commandGateway.sendAndWait(new ChangeTestAggregate(myAggregateId));
      }
   }

   @Test
   public void shouldSaveEventsWithMetadata() {
      final Map<String, String> expected = new HashMap<>();
      expected.put("Test", "Test");
      final GenericCommandMessage<CreateTestAggregate> command = new GenericCommandMessage<CreateTestAggregate>(new CreateTestAggregate(myAggregateId));
      commandGateway.sendAndWait(command.andMetaData(expected));
      final DomainEventStream readEvents = eventStore.readEvents(TestAggregate.class.getSimpleName(), myAggregateId);
      assertTrue(readEvents.hasNext());
      final MetaData actual = readEvents.next()
            .getMetaData();
      assertThat(actual.get("Test"), is(notNullValue()));
      assertThat(actual.get("Test"), is(equalTo("Test")));
   }

}