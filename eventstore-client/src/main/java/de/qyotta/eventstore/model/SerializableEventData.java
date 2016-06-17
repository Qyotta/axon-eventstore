package de.qyotta.eventstore.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class SerializableEventData {
   private String type;
   private Object data;

   public static <T> SerializableEventData of(T data) {
      return SerializableEventData.builder()
            .type(data.getClass()
                  .getName())
            .data(data)
            .build();
   }
}
