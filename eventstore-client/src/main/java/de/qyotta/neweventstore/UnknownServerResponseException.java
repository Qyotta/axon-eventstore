package de.qyotta.neweventstore;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public final class UnknownServerResponseException extends Exception {
   private static final long serialVersionUID = 1L;

   private final String streamName;

   public UnknownServerResponseException(final String streamName, String msg) {
      super("Reading from '" + streamName + "' failed. " + msg);
      this.streamName = streamName;
   }

}
