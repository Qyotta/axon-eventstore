package de.qyotta.axonframework.eventstore.test;

import static org.hamcrest.Matchers.isA;

import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import de.qyotta.axonframework.eventstore.config.AbstractIntegrationTest;
import de.qyotta.axonframework.eventstore.domain.ChangeTestAggregate;
import de.qyotta.axonframework.eventstore.domain.CreateTestAggregate;
import de.qyotta.axonframework.eventstore.domain.TestAggregate;

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

}