package de.qyotta.axonframework.eventstore.domain;

import lombok.Value;

@Value
public class TestAggregateCreated {
   private String aggregateId;
}
