package de.qyotta.axonframework.eventstore.domain;

import lombok.Value;

@Value
public class TestAggregateChanged {
   private String aggregateId;
}
