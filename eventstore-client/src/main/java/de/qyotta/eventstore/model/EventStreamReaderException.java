package de.qyotta.eventstore.model;

public class EventStreamReaderException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   public EventStreamReaderException(final Throwable cause) {
      super(cause);
   }
}
