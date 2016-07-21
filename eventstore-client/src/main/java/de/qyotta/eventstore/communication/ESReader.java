package de.qyotta.eventstore.communication;

import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.model.EventStreamFeed;

public interface ESReader {

   EventStreamFeed readStream(String url);

   EventResponse readEvent(String url);

}