package de.qyotta.neweventstore;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public final class EventNotFoundException extends RuntimeException {

   private static final long serialVersionUID = 1L;

   private final String streamName;

   private final int version;

   public EventNotFoundException(final String streamName, final int eventNumber) {
      super("Version " + eventNumber + " does not exist on stream '" + streamName + "'");

      this.streamName = streamName;
      this.version = eventNumber;
   }

}
