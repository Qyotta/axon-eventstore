package de.qyotta.axonframework.eventstore.domain;

import org.axonframework.serializer.Revision;

import lombok.Value;

@Value
@Revision("1")
public class TestAggregateCreated {
   private String aggregateId;
}
