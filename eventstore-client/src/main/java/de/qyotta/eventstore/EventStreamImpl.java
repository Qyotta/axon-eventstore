package de.qyotta.eventstore;

import static de.qyotta.eventstore.model.Link.EDIT;
import static de.qyotta.eventstore.model.Link.PREVIOUS;
import static de.qyotta.eventstore.model.Link.SELF;

import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import de.qyotta.eventstore.model.Entry;
import de.qyotta.eventstore.model.EventDeletedException;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.model.EventStreamFeed;
import de.qyotta.eventstore.model.Link;
import de.qyotta.eventstore.utils.EsUtils;

public class EventStreamImpl implements EventStream {
   private static final Logger LOGGER = Logger.getLogger(EventStreamImpl.class.getName());

   public interface EntryMatchingStrategy {
      boolean matches(final Entry e);
   }

   private final EsContext context;
   private List<Link> currentLinks;
   private Deque<Entry> currentEntries;
   private EventResponse next;
   private EventResponse previous;
   private final String streamUrl;

   /**
    * Initialize this stream at the very beginning
    */
   public EventStreamImpl(final String streamUrl, final EsContext context) {
      this.streamUrl = streamUrl;
      this.context = context;
      loadFirstFeed();
      loadNextEvent();
   }

   @Override
   public void setAfterTitle(final String title) {
      setTo(e -> e.getTitle()
            .equals(title));
      loadNextEvent();

   }

   @Override
   public void setAfterTimestamp(final Date timestamp) {
      final Entry entry = setTo(e -> timestamp.before(EsUtils.timestampOf(e)));
      next = readEvent(entry);
   }

   private Entry setTo(final EntryMatchingStrategy matcher) {
      loadFirstFeed();
      while (true) {
         final Entry e = pollNextEntry();
         if (e == null) {
            return null;
         }
         if (matcher.matches(e)) {
            return e;
         }
      }
   }

   private Entry pollNextEntry() {
      if (!currentEntries.isEmpty()) {
         return currentEntries.pollLast();
      }
      loadNextFeed();
      if (!currentEntries.isEmpty()) {
         return currentEntries.pollLast();
      }
      return null;
   }

   private void loadFeed(final String feedUrl) {
      final EventStreamFeed feed = context.getReader()
            .readStream(feedUrl);
      currentLinks = feed.getLinks();
      currentEntries = new LinkedList<>(feed.getEntries());
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

   private void loadNextEvent() {
      try {
         previous = next;
         if (!currentEntries.isEmpty()) {
            next = readEvent(currentEntries.pollLast());
            return;
         }

         if (loadNextFeed() && !currentEntries.isEmpty()) {
            next = readEvent(currentEntries.pollLast());
            return;
         }
         next = null; // no more events
      } catch (final EventDeletedException e) {
         loadNextEvent();
      }
   }

   private boolean loadNextFeed() {
      final Link previousLink = find(PREVIOUS, currentLinks);
      if (previousLink != null) {
         loadFeed(previousLink.getUri());
         return true;
      }
      return false;
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

   @SuppressWarnings("nls")
   private EventResponse readEvent(final Entry entry) {
      if (entry == null) {
         LOGGER.info("No more events");
         return null;
      }
      final Link find = find(EDIT, entry.getLinks());
      if (find == null) {
         LOGGER.info("No more events");
         return null;
      }
      final EventResponse event = context.getReader()
            .readEvent(find.getUri());
      LOGGER.info("Loaded event with number: " + event.getContent()
            .getEventNumber());
      return event;
   }

   private Link find(final String relation, final List<Link> links) {
      for (final Link link : links) {
         if (relation.equals(link.getRelation())) {
            return link;
         }
      }
      return null;
   }

   private void loadFirstFeed() {
      loadFeed(streamUrl);
      final Link last = find(Link.LAST, currentLinks);
      if (last != null) {
         loadFeed(last.getUri());
         return;
      }
   }

   @Override
   public void loadNext() {
      if (hasNext()) {
         return;
      }
      // reload the current feed
      loadFeed(find(SELF, currentLinks).getUri());
      if (!currentEntries.isEmpty()) {
         if (previous == null) {
            next = readEvent(currentEntries.pollLast());
            return;
         }
         while (!currentEntries.isEmpty()) {
            final Entry current = currentEntries.pollLast();
            if (EsUtils.getEventNumber(current) > previous.getContent()
                  .getEventNumber()) {
               next = readEvent(current);
               return;
            }
         }
      }
   }

}
