package de.qyotta.axonframework.eventstore;

import org.axonframework.serializer.SerializedDomainEventData;
import org.axonframework.serializer.SerializedMetaData;
import org.axonframework.serializer.SerializedObject;
import org.axonframework.serializer.SimpleSerializedObject;
import org.joda.time.DateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@SuppressWarnings("rawtypes")
@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class EventEntry implements SerializedDomainEventData {
   private String aggregateIdentifier;
   private long sequenceNumber;
   private String timeStamp;
   private String aggregateType;
   private String serializedPayload;
   private String payloadType;
   private String payloadRevision;
   private String serializedMetaData;
   private String eventIdentifier;

   @Override
   public DateTime getTimestamp() {
      return new DateTime(timeStamp);
   }

   @Override
   @SuppressWarnings("unchecked")
   public SerializedObject getMetaData() {
      return new SerializedMetaData(serializedMetaData, String.class);
   }

   @Override
   @SuppressWarnings("unchecked")
   public SerializedObject getPayload() {
      return new SimpleSerializedObject(serializedPayload, String.class, payloadType, payloadRevision);
   }
}
