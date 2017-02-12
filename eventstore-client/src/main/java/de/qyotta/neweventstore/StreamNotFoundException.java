package de.qyotta.neweventstore;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public final class StreamNotFoundException extends RuntimeException {

   private static final long serialVersionUID = 1L;

   private final String streamName;

   public StreamNotFoundException(final String streamName) {
      super("Stream '" + streamName + "' does not exist");
      this.streamName = streamName;
   }

}
