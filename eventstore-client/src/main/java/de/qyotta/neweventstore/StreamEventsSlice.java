package de.qyotta.neweventstore;

import java.util.List;

import de.qyotta.eventstore.model.EventResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class StreamEventsSlice {

   private final int fromEventNumber;

   private final int nextEventNumber;

   private final boolean endOfStream;

   private final List<EventResponse> events;

}
