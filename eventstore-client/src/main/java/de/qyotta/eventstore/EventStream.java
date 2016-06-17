package de.qyotta.eventstore;

import de.qyotta.eventstore.model.EventResponse;

public interface EventStream {

   boolean hasNext();

   EventResponse next();

   EventResponse peek();

}