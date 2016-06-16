package de.qyotta.axonframework.eventstore.config;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.TWO_SECONDS;

import java.util.UUID;

import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("nls")
public class SimpleEventStoreIntegrationTest extends AbstractIntegrationTest {

   private static final String MY_AGGREGATE_ID = UUID.randomUUID()
         .toString();
   @Autowired
   private CommandGateway commandGateway;

   @Test
   public void shouldOnlyHandleCommandAndStoreEvent() throws Exception {

      System.out.println(MY_AGGREGATE_ID);
      commandGateway.send(new CreateTestAggregate(MY_AGGREGATE_ID), new CommandCallback<String>() {
         @Override
         public void onSuccess(final String result) {
            commandGateway.send(new ChangeTestAggregate(MY_AGGREGATE_ID), new CommandCallback<String>() {
               @Override
               public void onSuccess(final String result) {
                  //
               }

               @Override
               public void onFailure(final Throwable cause) {
                  throw new RuntimeException(cause);
               }

            });
            commandGateway.send(new ChangeTestAggregate(MY_AGGREGATE_ID), new CommandCallback<String>() {
               @Override
               public void onSuccess(final String result) {
                  //
               }

               @Override
               public void onFailure(final Throwable cause) {
                  throw new RuntimeException(cause);
               }

            });
            commandGateway.send(new ChangeTestAggregate(MY_AGGREGATE_ID), new CommandCallback<String>() {
               @Override
               public void onSuccess(final String result) {
                  //
               }

               @Override
               public void onFailure(final Throwable cause) {
                  throw new RuntimeException(cause);
               }

            });
            commandGateway.send(new ChangeTestAggregate(MY_AGGREGATE_ID), new CommandCallback<String>() {
               @Override
               public void onSuccess(final String result) {
                  //
               }

               @Override
               public void onFailure(final Throwable cause) {
                  throw new RuntimeException(cause);
               }

            });
         }

         @Override
         public void onFailure(final Throwable cause) {
            throw new RuntimeException(cause);
         }

      });

      await().atMost(TWO_SECONDS);
   }

}