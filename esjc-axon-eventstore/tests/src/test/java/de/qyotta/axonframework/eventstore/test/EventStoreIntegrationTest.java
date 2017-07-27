package de.qyotta.axonframework.eventstore.test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.msemys.esjc.StreamEventsSlice;

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
   public void shouldHandleCommands() {
      final Map<String, String> m = new HashMap<>();
      m.put("networkId", "55");
      commandGateway.sendAndWait(new GenericCommandMessage<CreateTestAggregate>(new CreateTestAggregate(myAggregateId), m));
      commandGateway.sendAndWait(new GenericCommandMessage<ChangeTestAggregate>(new ChangeTestAggregate(myAggregateId), m));
      commandGateway.sendAndWait(new GenericCommandMessage<ChangeTestAggregate>(new ChangeTestAggregate(myAggregateId), m));
      commandGateway.sendAndWait(new GenericCommandMessage<ChangeTestAggregate>(new ChangeTestAggregate(myAggregateId), m));
      commandGateway.sendAndWait(new GenericCommandMessage<ChangeTestAggregate>(new ChangeTestAggregate(myAggregateId), m));
      commandGateway.sendAndWait(new GenericCommandMessage<ChangeTestAggregate>(new ChangeTestAggregate(myAggregateId), m));

   }

   @Test(expected = CommandExecutionException.class)
   public void shouldThrowOn5001Changes() throws InterruptedException, ExecutionException {
      final Map<String, String> m = new HashMap<>();
      m.put("networkId", "55");
      commandGateway.sendAndWait(new GenericCommandMessage<CreateTestAggregate>(new CreateTestAggregate(myAggregateId), m));

      for (int i = 0; i < 5001; i++) {
         commandGateway.sendAndWait(new GenericCommandMessage<ChangeTestAggregate>(new ChangeTestAggregate(myAggregateId), m));
         System.out.println("Send " + (i + 1) + "events.");
      }
   }

   @Test()
   public void shouldNotThrowOn5000Changes() throws InterruptedException, ExecutionException {
      final Map<String, String> m = new HashMap<>();
      m.put("networkId", "55");
      commandGateway.sendAndWait(new GenericCommandMessage<CreateTestAggregate>(new CreateTestAggregate(myAggregateId), m));

      final long nrOfCommands = 4097;

      for (int i = 0; i < nrOfCommands; i++) {
         commandGateway.sendAndWait(new GenericCommandMessage<ChangeTestAggregate>(new ChangeTestAggregate(myAggregateId), m));
         System.out.println("Send " + (i + 1) + "events.");
      }

      final String stream = "domain-" + MyTestAggregate.class.getSimpleName()
            .toLowerCase() + "-" + myAggregateId;

      long events = 0;
      long from = 0;

      while (true) {
         final StreamEventsSlice streamEventsSlice = eventStore.readStreamEventsForward(stream, from, 4096, true)
               .get();

         events += streamEventsSlice.events.size();
         from = from + events;

         if (events >= 1 /* crate aggregate */ + nrOfCommands) {
            break;
         }
      }

   }

}