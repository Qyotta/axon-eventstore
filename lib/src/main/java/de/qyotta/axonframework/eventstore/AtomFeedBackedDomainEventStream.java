package de.qyotta.axonframework.eventstore;

import java.net.URISyntaxException;
import java.util.Iterator;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.ParseException;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.eventstore.EventStreamNotFoundException;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.gson.Gson;

@SuppressWarnings({ "nls", "rawtypes" })
public class AtomFeedBackedDomainEventStream implements DomainEventStream {

   private static final String ACCEPT_APPLICATION_ATOM_XML = "application/atom+xml";
   private final AbderaClient client;
   private PeekingIterator<Entry> feedEntriesIterator;
   private final Gson gson = new Gson();

   public AtomFeedBackedDomainEventStream(Settings settings, String streamName) {
      client = new AbderaClient(new Abdera());
      try {
         client.addCredentials(settings.getHost(), settings.getRealm(), settings.getScheme(), new UsernamePasswordCredentials(settings.getUserName(), settings.getPassword()));
      } catch (final URISyntaxException e) {
         throw new IllegalArgumentException("Given host is not a valid url", e);
      }
      final RequestOptions options = new RequestOptions();
      options.setAccept(ACCEPT_APPLICATION_ATOM_XML);
      final ClientResponse resp = client.get(settings.getHost() + "/streams/" + streamName, options);
      Document<Feed> doc;
      try {
         doc = resp.getDocument();
         final Feed feed = doc.getRoot();
         if (feed == null) {
            return;
         }
         final Iterator<Entry> iterator = feed.getEntries()
               .iterator();
         if (iterator == null) {
            return;
         }
         feedEntriesIterator = Iterators.peekingIterator(iterator);
      } catch (final ParseException e) {
         throw new EventStreamNotFoundException("Could not parse document.", e);
      }
   }

   @Override
   public boolean hasNext() {
      return feedEntriesIterator != null && feedEntriesIterator.hasNext();
   }

   @Override
   public DomainEventMessage next() {
      return domainEventMessageFrom(feedEntriesIterator.next());
   }

   @Override
   public DomainEventMessage peek() {
      return domainEventMessageFrom(feedEntriesIterator.peek());
   }

   private DomainEventMessage domainEventMessageFrom(Entry entry) {
      return parse(entry.getContent());
   }

   private DomainEventMessage parse(String content) {
      return gson.fromJson(content, DomainEventMessage.class);
   }

}
