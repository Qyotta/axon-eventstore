package de.qyotta.axonframework.eventstore.config;

import lombok.Value;

@Value
public class TestAggregateCreated {
   private String aggregateId;
}
