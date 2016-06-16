package de.qyotta.axonframework.eventstore.config;

import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.axonframework.eventsourcing.annotation.AggregateIdentifier;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TestAggregate extends AbstractAnnotatedAggregateRoot<String> {
   private static final long serialVersionUID = 1L;

   @AggregateIdentifier
   private String aggregateId;

   @CommandHandler
   public TestAggregate(final CreateTestAggregate command) {
      this.aggregateId = command.getAggregateId();
      apply(new TestAggregateCreated(command.getAggregateId()));
   }

   @CommandHandler
   public void onCommand(final ChangeTestAggregate command) {
      apply(new TestAggregateChanged(command.getAggregateId()));
   }

   @EventSourcingHandler
   public void onEvent(final TestAggregateCreated event) {
      this.aggregateId = event.getAggregateId();
   }

   @EventSourcingHandler
   public void onEvent(final TestAggregateChanged event) {
      //
   }
}
