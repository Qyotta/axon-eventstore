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
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("rawtypes")
public class DeserializedDomainEventMessage<T> implements DomainEventMessage<T> {
   private static final long serialVersionUID = 1L;
   private String identifier;
   private DateTime timestamp;
   private MetaData metaData;
   private T payload;
   private Class payloadType;
   private long sequenceNumber;
   private Object aggregateIdentifier;

   @Override
   public DomainEventMessage<T> withMetaData(Map<String, ?> newMetaData) {
      return this.toBuilder()
            .metaData(MetaData.from(newMetaData))
            .build();
   }

   @Override
   public DomainEventMessage<T> andMetaData(Map<String, ?> additionalMetaData) {
      final MetaData newMetaData = getMetaData().mergedWith(additionalMetaData);
      return withMetaData(newMetaData);
   }

}
