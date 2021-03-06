package de.qyotta.eventstore;

import static de.qyotta.eventstore.model.Link.EDIT;
import static de.qyotta.eventstore.model.Link.NEXT;
import static de.qyotta.eventstore.model.Link.PREVIOUS;
import static de.qyotta.eventstore.model.Link.SELF;

import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.qyotta.eventstore.communication.ESContext;
import de.qyotta.eventstore.model.Entry;
import de.qyotta.eventstore.model.EventDeletedException;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.model.EventStreamFeed;
import de.qyotta.eventstore.model.Link;
import de.qyotta.eventstore.utils.EsUtils;

@SuppressWarnings("nls")
public class EventStreamImpl implements EventStream {
   private static final Logger LOGGER = LoggerFactory.getLogger(EventStreamImpl.class.getName());

   public interface EntryMatchingStrategy {
      boolean matches(final Entry e);
   }

   private final ESContext context;
   private List<Link> currentLinks;
   private Deque<Entry> currentEntries;
   private EventResponse next;
   private EventResponse previous;
   private final String streamUrl;

   /**
    * Initialize this stream at the very beginning
    */
   public EventStreamImpl(final String streamUrl, final ESContext context) {
      this.streamUrl = streamUrl;
      this.context = context;
      loadFirstFeed();
      loadNextEvent(null);
   }

   @Override
   public synchronized void setAfterTitle(final String title) {
      final Entry entry = setTo(e -> e.getTitle()
            .equals(title));
      loadNextEvent(readEvent(entry));
   }

   @Override
   public synchronized void setAfterTimestamp(final Date timestamp) {
      final Entry entry = setTo(e -> timestamp.before(EsUtils.timestampOf(e)));
      next = readEvent(entry);
   }

   private synchronized Entry setTo(final EntryMatchingStrategy matcher) {
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

   private synchronized Entry pollNextEntry() {
      if (!currentEntries.isEmpty()) {
         return currentEntries.pollLast();
      }
      loadNextFeed();
      if (!currentEntries.isEmpty()) {
         return currentEntries.pollLast();
      }
      return null;
   }

   private synchronized void loadFeed(final String feedUrl) {
      final EventStreamFeed feed = context.getReader()
            .readStream(feedUrl);
      currentLinks = feed.getLinks();
      currentEntries = new LinkedList<>(feed.getEntries());
   }

   @Override
   public synchronized boolean hasNext() {
      return next != null;
   }

   @Override
   public final synchronized EventResponse next() {
      final EventResponse result = next;
      loadNextEvent(result);
      return result;
   }

   private synchronized void loadNextEvent(final EventResponse pPrevious) {
      try {
         previous = pPrevious;
         LOGGER.info("Setting previous event to: " + previous);
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
         loadNextEvent(pPrevious);
      }
   }

   private synchronized boolean loadNextFeed() {
      final Link previousLink = find(PREVIOUS, currentLinks);
      if (previousLink != null) {
         loadFeed(previousLink.getUri());
         return true;
      }
      return false;
   }

   @Override
   public final synchronized EventResponse peek() {
      return next;
   }

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

      event.getContent()
            .setEventId(entry.getEventId());
      event.getContent()
            .setEventType(entry.getEventType());
      event.getContent()
            .setEventNumber(entry.getEventNumber());
      event.getContent()
            .setStreamId(entry.getStreamId());
      event.getContent()
            .setIsLinkMetaData(entry.getIsLinkMetaData());
      event.getContent()
            .setPositionEventNumber(entry.getPositionEventNumber());
      event.getContent()
            .setPositionStreamId(entry.getPositionStreamId());
      event.getContent()
            .setTitle(entry.getTitle());
      event.getContent()
            .setId(entry.getId());
      event.getContent()
            .setUpdated(entry.getUpdated());
      event.getContent()
            .setUpdated(entry.getUpdated());
      event.getContent()
            .setAuthor(entry.getAuthor());
      event.getContent()
            .setSummary(entry.getSummary());

      LOGGER.info("Loaded event with number: " + event.getContent()
            .getEventNumber());
      return event;
   }

   private synchronized Link find(final String relation, final List<Link> links) {
      for (final Link link : links) {
         if (relation.equals(link.getRelation())) {
            return link;
         }
      }
      return null;
   }

   private synchronized void loadFirstFeed() {
      loadFeed(streamUrl);
      final Link last = find(Link.LAST, currentLinks);
      if (last != null) {
         loadFeed(last.getUri());
      }
   }

   @Override
   public synchronized void loadNext() {
      if (hasNext()) {
         return;
      }
      // reload the current feed
      loadFeed(find(SELF, currentLinks).getUri());
      if (currentEntries.isEmpty()) {
         return;
      }
      // if the last event in the stream is the previous event there can be no new events
      if (previous.getTitle()
            .equals(currentEntries.peekFirst()
                  .getTitle())) {
         return;
      }

      if (previous == null) {
         // If previous is null this is the first event
         next = readEvent(currentEntries.pollLast());
         return;
      }
      findNext();

   }

   private void findNext() {
      while (!currentEntries.isEmpty()) {
         final Entry current = currentEntries.pollLast();
         final String currentTitle = current.getTitle();
         final String previousTitle = previous.getTitle();
         if (currentTitle.equals(previousTitle) && !currentEntries.isEmpty()) {
            // load the next one or not =)
            next = readEvent(currentEntries.pollLast());
            LOGGER.info("Found new event:" + next);
            return;
         }
      }
      loadFeed(find(NEXT, currentLinks).getUri());
      findNext();
   }

}
