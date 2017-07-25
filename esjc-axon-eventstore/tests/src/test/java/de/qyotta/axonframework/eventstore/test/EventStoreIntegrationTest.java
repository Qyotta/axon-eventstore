package de.qyotta.axonframework.eventstore.test;

import java.util.HashMap;
import java.util.Map;

import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import de.qyotta.axonframework.eventstore.config.AbstractIntegrationTest;
import de.qyotta.axonframework.eventstore.domain.ChangeTestAggregate;
import de.qyotta.axonframework.eventstore.domain.CreateTestAggregate;
import de.qyotta.axonframework.eventstore.domain.MyTestAggregate;

@SuppressWarnings("nls")
public class EventStoreIntegrationTest extends AbstractIntegrationTest {

   @Autowired
   private CommandGateway commandGateway;

   @After
   public final void tearDown() {
      deleteEventStream(MyTestAggregate.class, myAggregateId);
   }

   @Test
   public void shouldHandleCommands() throws Exception {
      final Map<String, String> m = new HashMap<>();
      m.put("networkId", "55");
      commandGateway.sendAndWait(new GenericCommandMessage<CreateTestAggregate>(new CreateTestAggregate(myAggregateId), m));
      commandGateway.sendAndWait(new GenericCommandMessage<ChangeTestAggregate>(new ChangeTestAggregate(myAggregateId), m));
      commandGateway.sendAndWait(new GenericCommandMessage<ChangeTestAggregate>(new ChangeTestAggregate(myAggregateId), m));
      commandGateway.sendAndWait(new GenericCommandMessage<ChangeTestAggregate>(new ChangeTestAggregate(myAggregateId), m));
      commandGateway.sendAndWait(new GenericCommandMessage<ChangeTestAggregate>(new ChangeTestAggregate(myAggregateId), m));
      commandGateway.sendAndWait(new GenericCommandMessage<ChangeTestAggregate>(new ChangeTestAggregate(myAggregateId), m));

   }

   @Test
   public void shouldThrowOn5000Changes() throws Exception {
      thrown.expect(CommandExecutionException.class);
      final Map<String, String> m = new HashMap<>();
      m.put("networkId", "55");
      commandGateway.sendAndWait(new GenericCommandMessage<CreateTestAggregate>(new CreateTestAggregate(myAggregateId), m));

      for (int i = 0; i < 5001; i++) {
         commandGateway.sendAndWait(new GenericCommandMessage<ChangeTestAggregate>(new ChangeTestAggregate(myAggregateId), m));
         System.out.println("Send " + (i + 1) + "events.");
      }
   }

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void shouldRehydrateAndThrow() throws Exception {
      thrown.expect(CommandExecutionException.class);

      commandGateway.sendAndWait(new CreateTestAggregate(myAggregateId));
      commandGateway.sendAndWait(new ChangeTestAggregate(myAggregateId));
   }

}