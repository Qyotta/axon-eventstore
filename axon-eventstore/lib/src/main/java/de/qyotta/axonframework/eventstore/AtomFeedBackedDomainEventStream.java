package de.qyotta.axonframework.eventstore;

import java.net.URISyntaxException;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.eventstore.EventStreamNotFoundException;
import org.axonframework.serializer.Serializer;
import org.axonframework.upcasting.UpcastUtils;
import org.axonframework.upcasting.UpcasterChain;

import com.google.common.collect.PeekingIterator;
import com.google.gson.Gson;

@SuppressWarnings({ "nls", "rawtypes" })
public class AtomFeedBackedDomainEventStream implements DomainEventStream {

   private static final String ACCEPT_APPLICATION_ATOM_XML = "application/atom+xml";
   private final AbderaClient client;
   private PeekingIterator<Entry> feedEntriesIterator;
   private final Serializer serializer;
   private final UpcasterChain upcasterChain;
   private final Gson gson = new Gson();

   public AtomFeedBackedDomainEventStream(final Settings settings, final String streamName, final Serializer serializer, final UpcasterChain upcasterChain) {
      this.serializer = serializer;
      this.upcasterChain = upcasterChain;
      client = new AbderaClient(new Abdera());
      try {
         client.addCredentials(settings.getHost(), settings.getRealm(), settings.getScheme(), new UsernamePasswordCredentials(settings.getUserName(), settings.getPassword()));
      } catch (final URISyntaxException e) {
         throw new IllegalArgumentException("Given host is not a valid url", e);
      }
      final RequestOptions options = new RequestOptions();
      options.setAccept(ACCEPT_APPLICATION_ATOM_XML);
      final String uri = settings.getHost() + "/streams/" + streamName;
      final ClientResponse resp = client.get(uri, options);
      try {
         final Document<Feed> doc = resp.getDocument();
         final Feed feed = doc.getRoot();

         final Document<Feed> document = client.get(feed.getLink("first")
               .getHref()
               .toURL()
               .toString(), options)
               .getDocument();
         final Feed root = document.getRoot();

         // final Iterator<Entry> iterator = root.iterator();
         // if (iterator == null) {
         // return;
         // }
         // feedEntriesIterator = Iterators.peekingIterator(iterator);
      } catch (final Exception e) {
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

   private DomainEventMessage domainEventMessageFrom(Entry next) {
      final String content = next.getContent();
      final EventEntry eventEntry = gson.fromJson(content, EventEntry.class);
      // TODO upcaster chain might return multiple events
      final List<DomainEventMessage> upcastAndDeserialize = UpcastUtils.upcastAndDeserialize(eventEntry, eventEntry.getAggregateIdentifier(), serializer, upcasterChain, true);
      return upcastAndDeserialize.get(0);
   }

   @Override
   public DomainEventMessage peek() {
      return domainEventMessageFrom(feedEntriesIterator.peek());
   }
}
