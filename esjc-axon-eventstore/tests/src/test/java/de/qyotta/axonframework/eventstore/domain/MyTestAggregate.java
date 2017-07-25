package de.qyotta.axonframework.eventstore.domain;

import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.domain.MetaData;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.axonframework.eventsourcing.annotation.AggregateIdentifier;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MyTestAggregate extends AbstractAnnotatedAggregateRoot<String> {
   private static final long serialVersionUID = 1L;

   @AggregateIdentifier
   private String aggregateId;

   private int timesChanged;

   @CommandHandler
   public MyTestAggregate(final CreateTestAggregate command, final MetaData metaData) {
      this.aggregateId = command.getAggregateId();
      apply(new TestAggregateCreated(command.getAggregateId()), metaData);
   }

   @CommandHandler
   public void onCommand(final ChangeTestAggregate command, final MetaData metaData) {
      if (timesChanged == 5000) {
         throw new IllegalStateException("More than 5000 changes");
      }
      apply(new TestAggregateChanged(aggregateId), metaData);
   }

   @EventSourcingHandler
   public void onEvent(final TestAggregateCreated event) {
      this.aggregateId = event.getAggregateId();
   }

   @EventSourcingHandler
   public void onEvent(@SuppressWarnings("unused") final TestAggregateChanged event) {
      timesChanged++;
   }
}
