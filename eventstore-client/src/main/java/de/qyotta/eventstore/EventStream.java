package de.qyotta.eventstore;

import java.io.IOException;
import java.util.List;
import java.util.Queue;

import de.qyotta.eventstore.model.Entry;
import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventStreamFeed;
import de.qyotta.eventstore.model.Link;

public class EventStream {

   private static final String STREAMS_PATH = "/streams/"; //$NON-NLS-1$
   private final EsContext context;
   private final String streamUrl;
   private List<Link> currentLinks;
   private Queue<Entry> currentEntries;

   public EventStream(final String streamName, EsContext context) {
      this.context = context;
      streamUrl = context.getSettings()
            .getHost() + STREAMS_PATH + streamName;
      try {
         final EventStreamFeed initialFeed = context.getReader()
               .readStream(streamUrl);

         final Link last = find(Link.LAST, initialFeed.getLinks());
         final EventStreamFeed lastFeed = context.getReader()
               .readStream(last.getUri());
         initFromFeed(lastFeed);
      } catch (final IOException e) {
         throw new RuntimeException("Could not initialize EventStream.", e); //$NON-NLS-1$
      }
   }

   private void initFromFeed(EventStreamFeed feed) {
      currentLinks = feed.getLinks();
      for (final Entry entry : feed.getEntries()) {
         // the first element is the newest so we simply add them to the queue (last in first out) to both reverse the processing order as well as easier handling
         currentEntries.add(entry);
      }
   }

   boolean hasNext() {
      // If 'links' contains a 'previous' link, we have elements left
      return !currentEntries.isEmpty();
   }

   public final Event next() {
      return readEvent(currentEntries.poll());
   }

   public final Event peek() {
      return readEvent(currentEntries.peek());
   }

   private Event readEvent(Entry entry) {
      // TODO
      return null;
   }

   private Link find(String relation, final List<Link> links) {
      return links.stream()
            .filter(l -> relation.equals(l.getRelation()))
            .findFirst()
            .get();
   }
}
