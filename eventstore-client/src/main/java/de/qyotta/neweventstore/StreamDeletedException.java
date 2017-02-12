package de.qyotta.neweventstore;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public final class StreamDeletedException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   private final String streamName;

   public StreamDeletedException(final String streamName) {
      super("Stream '" + streamName + "' previously existed but was deleted");
      this.streamName = streamName;
   }

}
