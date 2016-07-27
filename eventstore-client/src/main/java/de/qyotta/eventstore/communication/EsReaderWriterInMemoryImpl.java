package de.qyotta.eventstore.communication;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.model.EventStreamFeed;

@SuppressWarnings("nls")
public class EsReaderWriterInMemoryImpl implements ESReader, ESWriter {
   private static Map<String, List<Event>> synchronizedMap = Collections.synchronizedMap(new HashMap<String, List<Event>>());

   @Override
   public EventStreamFeed readStream(String url) {
      throw new UnsupportedOperationException("In Memory version is not implemented yet.");
   }

   @Override
   public EventResponse readEvent(String url) {
      throw new UnsupportedOperationException("In Memory version is not implemented yet.");
   }

   @Override
   public void appendEvents(String url, Collection<Event> collection) {
      throw new UnsupportedOperationException("In Memory version is not implemented yet.");
   }

   @Override
   public void appendEvent(String url, Event event) {
      throw new UnsupportedOperationException("In Memory version is not implemented yet.");
   }

   @Override
   public void deleteStream(String url, boolean deletePermanently) {
      throw new UnsupportedOperationException("In Memory version is not implemented yet.");
   }

   @Override
   public void createLinkedProjection(String host, String name, String... includedStreams) {
      throw new UnsupportedOperationException("In Memory version is not implemented yet.");
   }

   public void reset() {
      synchronizedMap.clear();
   }

}
