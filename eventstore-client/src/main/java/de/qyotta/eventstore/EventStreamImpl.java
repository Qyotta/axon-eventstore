package de.qyotta.eventstore;

import static de.qyotta.eventstore.model.Link.EDIT;
import static de.qyotta.eventstore.model.Link.PREVIOUS;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import de.qyotta.eventstore.model.Entry;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.model.EventStreamFeed;
import de.qyotta.eventstore.model.Link;

public class EventStreamImpl implements EventStream {

   private final EsContext context;
   private List<Link> currentLinks;
   private Deque<Entry> currentEntries;
   private EventResponse next;

   public EventStreamImpl(final String streamUrl, EsContext context) {
      this.context = context;
      final EventStreamFeed initialFeed = context.getReader()
            .readStream(streamUrl);

      final Link last = find(Link.LAST, initialFeed.getLinks());
      if (last == null) {
         // we already loaded the first stream (there is only one)
         setUpFeed(initialFeed);
         return;
      }
      loadFeed(last.getUri());
   }

   private void loadFeed(final String feedUrl) {
      final EventStreamFeed feed = context.getReader()
            .readStream(feedUrl);
      setUpFeed(feed);
   }

   private void setUpFeed(final EventStreamFeed feed) {
      currentLinks = feed.getLinks();
      currentEntries = new LinkedList<>(feed.getEntries());
      if (!currentEntries.isEmpty()) {
         loadNextEvent();
      } else {
         next = null;
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see de.qyotta.eventstore.EventStream#hasNext()
    */
   @Override
   public boolean hasNext() {
      // If 'links' contains a 'previous' link, we have elements left
      return next != null;
   }

   /*
    * (non-Javadoc)
    *
    * @see de.qyotta.eventstore.EventStream#next()
    */
   @Override
   public final EventResponse next() {
      final EventResponse result = next;
      loadNextEvent();
      return result;
   }

   /**
    * @return the next event in the stream or null if there are no more
    */
   private void loadNextEvent() {
      if (!currentEntries.isEmpty()) {
         next = readEvent(currentEntries.pollLast());
         return;
      }
      final Link previous = find(PREVIOUS, currentLinks);
      if (previous != null) {
         loadFeed(previous.getUri());
         return;
      }
      next = null; // no more events
   }

   /*
    * (non-Javadoc)
    *
    * @see de.qyotta.eventstore.EventStream#peek()
    */
   @Override
   public final EventResponse peek() {
      return next;
   }

   private EventResponse readEvent(Entry entry) {
      return context.getReader()
            .readEvent(find(EDIT, entry.getLinks()).getUri());
   }

   private Link find(final String relation, final List<Link> links) {
      for (final Link link : links) {
         if (relation.equals(link.getRelation())) {
            return link;
         }
      }
      return null;
   }
}
