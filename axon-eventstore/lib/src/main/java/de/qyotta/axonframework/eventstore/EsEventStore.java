package de.qyotta.axonframework.eventstore;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.eventstore.EventStore;
import org.axonframework.eventstore.EventStreamNotFoundException;
import org.axonframework.serializer.Serializer;
import org.axonframework.upcasting.UpcasterAware;
import org.axonframework.upcasting.UpcasterChain;

@SuppressWarnings({ "nls", "rawtypes" })
public class EsEventStore implements EventStore, UpcasterAware {
   private final Settings settings;
   private final EsEventStorage eventSerializer;
   private UpcasterChain upcasterChain;
   private final Serializer serializer;

   public EsEventStore(final Settings settings, final Serializer serializer) {
      this.settings = settings;
      this.serializer = serializer;
      eventSerializer = new EsEventStorage(settings, serializer);
   }

   @Override
   public void setUpcasterChain(UpcasterChain upcasterChain) {
      this.upcasterChain = upcasterChain;
   }

   @Override
   public void appendEvents(String type, DomainEventStream events) {
      while (events.hasNext()) {
         final DomainEventMessage message = events.next();
         eventSerializer.store(getStreamName(type, message.getAggregateIdentifier()), type, message);
      }
   }

   @Override
   public DomainEventStream readEvents(String type, Object identifier) {
      // TODO upcast events
      final DomainEventStream stream = new AtomFeedBackedDomainEventStream(settings, getStreamName(type, identifier), serializer, upcasterChain);
      if (!stream.hasNext()) {
         throw new EventStreamNotFoundException(type, identifier);
      }
      return stream;
   }

   private String getStreamName(String type, Object identifier) {
      return type.toLowerCase() + "-" + identifier.toString();
   }
}
