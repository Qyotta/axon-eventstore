package de.qyotta.axonframework.eventstore.domain;

import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.axonframework.serializer.Revision;

import lombok.Value;

@Value
@Revision("1")
public class CreateTestAggregate {
   @TargetAggregateIdentifier
   private String aggregateId;
}
