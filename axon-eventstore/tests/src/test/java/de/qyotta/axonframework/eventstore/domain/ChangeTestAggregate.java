package de.qyotta.axonframework.eventstore.domain;

import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;

import lombok.Value;

@Value
public class ChangeTestAggregate {
   @TargetAggregateIdentifier
   private String aggregateId;
}
