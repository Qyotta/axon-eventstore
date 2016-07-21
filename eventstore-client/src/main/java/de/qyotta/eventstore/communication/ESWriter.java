package de.qyotta.eventstore.communication;

import java.util.Collection;

import de.qyotta.eventstore.model.Event;

public interface ESWriter {

   void appendEvents(String url, Collection<Event> collection);

   void appendEvent(String url, Event event);

   void deleteStream(String url, boolean deletePermanently);

}