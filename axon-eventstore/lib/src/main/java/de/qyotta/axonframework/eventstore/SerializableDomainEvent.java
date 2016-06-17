package de.qyotta.axonframework.eventstore;

import java.util.Map;

import de.qyotta.eventstore.model.SerializableEventData;
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
class SerializableDomainEvent {
   private Object aggregateIdentifier;
   private String timestamp;
   private Map<String, ?> metaData;
   private SerializableEventData payload;
}
