package de.qyotta.axonframework.eventstore;

import java.util.Map;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.MetaData;
import org.joda.time.DateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@SuppressWarnings("unchecked")
public class EsDomainEventMessage<T> implements DomainEventMessage<T> {
   private static final long serialVersionUID = 1L;

   private String identifier;
   private DateTime timestamp;
   private MetaData metaData;
   private T payload;
   private long sequenceNumber;

   private Object aggregateIdentifier;

   @Override
   public DomainEventMessage<T> withMetaData(Map<String, ?> newMetaData) {
      return (DomainEventMessage<T>) EsDomainEventMessage.builder()
            .identifier(identifier)
            .timestamp(timestamp)
            .metaData(new MetaData(newMetaData))
            .payload(payload)
            .aggregateIdentifier(aggregateIdentifier)
            .build();
   }

   @Override
   public DomainEventMessage<T> andMetaData(Map<String, ?> newMetaData) {
      return (DomainEventMessage<T>) EsDomainEventMessage.builder()
            .identifier(identifier)
            .timestamp(timestamp)
            .metaData(metaData.mergedWith(newMetaData))
            .payload(payload)
            .aggregateIdentifier(aggregateIdentifier)
            .build();
   }

   @Override
   @SuppressWarnings("rawtypes")
   public Class getPayloadType() {
      return payload.getClass();
   }

}
