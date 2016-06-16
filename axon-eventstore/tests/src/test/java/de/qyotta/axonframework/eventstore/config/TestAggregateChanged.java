package de.qyotta.axonframework.eventstore.config;

import lombok.Value;

@Value
public class TestAggregateChanged {
   private String aggregateId;
}
