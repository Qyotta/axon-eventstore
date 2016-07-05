package de.qyotta.eventstore;

import java.util.Date;

import de.qyotta.eventstore.model.EventResponse;

public interface EventStream {

   boolean hasNext();

   EventResponse next();

   EventResponse peek();

   void setAfterTimestamp(final Date timestamp);

   void setAfterTitle(final String eventId);

   /**
    * Attempts to load the next event in the stream from the current point. If the next event was already loaded this method does nothing. This method is only useful after hasNext returned false.
    * After this method has been called use hasNext to check whether a new event arrived.
    */
   void loadNext();

}