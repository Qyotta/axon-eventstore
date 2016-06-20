package de.qyotta.axonframework.eventstore.domain;

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
   private int numberofevents;

   @CommandHandler
   public TestAggregate(final CreateTestAggregate command) {
      this.aggregateId = command.getAggregateId();
      apply(new TestAggregateCreated(command.getAggregateId()));
   }

   @CommandHandler
   public void onCommand(final ChangeTestAggregate command) {
      if (numberofevents > 9) {
         throw new IllegalStateException("Maximum number of events reached ;)"); //$NON-NLS-1$
      }
      apply(new TestAggregateChanged(command.getAggregateId()));
   }

   @EventSourcingHandler
   public void onEvent(final TestAggregateCreated event) {
      this.aggregateId = event.getAggregateId();
      numberofevents++;
   }

   @EventSourcingHandler
   public void onEvent(@SuppressWarnings("unused") final TestAggregateChanged event) {
      numberofevents++;
   }
}
