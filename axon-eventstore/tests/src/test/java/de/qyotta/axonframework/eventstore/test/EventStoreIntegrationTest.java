package de.qyotta.axonframework.eventstore.test;

import static org.hamcrest.Matchers.isA;

import java.util.UUID;

import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import de.qyotta.axonframework.eventstore.config.AbstractIntegrationTest;
import de.qyotta.axonframework.eventstore.domain.ChangeTestAggregate;
import de.qyotta.axonframework.eventstore.domain.CreateTestAggregate;

public class EventStoreIntegrationTest extends AbstractIntegrationTest {

   private static final String MY_AGGREGATE_ID = UUID.randomUUID()
         .toString();
   @Autowired
   private CommandGateway commandGateway;

   @Test
   public void shouldHandleCommands() throws Exception {
      commandGateway.sendAndWait(new CreateTestAggregate(MY_AGGREGATE_ID));
      commandGateway.sendAndWait(new ChangeTestAggregate(MY_AGGREGATE_ID));
      commandGateway.sendAndWait(new ChangeTestAggregate(MY_AGGREGATE_ID));
      commandGateway.sendAndWait(new ChangeTestAggregate(MY_AGGREGATE_ID));
      commandGateway.sendAndWait(new ChangeTestAggregate(MY_AGGREGATE_ID));
   }

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void shouldRehydrateAndThrow() throws Exception {
      thrown.expect(CommandExecutionException.class);
      thrown.expectCause(isA(IllegalStateException.class));

      commandGateway.sendAndWait(new CreateTestAggregate(MY_AGGREGATE_ID));
      for (int i = 0; i < 10; i++) {
         commandGateway.sendAndWait(new ChangeTestAggregate(MY_AGGREGATE_ID));
      }
   }

}