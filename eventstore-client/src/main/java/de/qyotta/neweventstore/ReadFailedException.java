package de.qyotta.neweventstore;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public final class ReadFailedException extends Exception {
   private static final long serialVersionUID = 1L;

   private final String streamName;

   public ReadFailedException(final String streamName, String msg, Throwable cause) {
      super("Reading from '" + streamName + "' failed. " + msg, cause);
      this.streamName = streamName;
   }

   public ReadFailedException(final String streamName, String msg) {
      super("Reading from '" + streamName + "' failed. " + msg);
      this.streamName = streamName;
   }

}
