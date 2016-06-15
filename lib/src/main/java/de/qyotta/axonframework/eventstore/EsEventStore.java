package de.qyotta.axonframework.eventstore;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.eventstore.EventStore;
import org.axonframework.eventstore.EventStreamNotFoundException;
import org.axonframework.upcasting.UpcasterAware;
import org.axonframework.upcasting.UpcasterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "nls", "rawtypes" })
public class EsEventStore implements EventStore, UpcasterAware {
   private static final Logger LOGGER = LoggerFactory.getLogger(EsEventStore.class);
   private final Settings settings;
   private final EsEventSerializer eventSerializer;
   private UpcasterChain upcasterChain;

   public EsEventStore(final Settings settings) {
      this.settings = settings;
      eventSerializer = new EsEventSerializer(settings);
   }

   @Override
   public void setUpcasterChain(UpcasterChain upcasterChain) {
      this.upcasterChain = upcasterChain;
   }

   @Override
   public void appendEvents(String type, DomainEventStream events) {
      while (events.hasNext()) {
         final DomainEventMessage message = events.next();
         eventSerializer.serialize(getStreamName(type, message.getAggregateIdentifier()), message);
      }
   }

   @Override
   public DomainEventStream readEvents(String type, Object identifier) {
      // TODO upcast events
      final DomainEventStream stream = new AtomFeedBackedDomainEventStream(settings, getStreamName(type, identifier));
      if (!stream.hasNext()) {
         throw new EventStreamNotFoundException(type, identifier);
      }
      return stream;
   }

   private String getStreamName(String type, Object identifier) {
      return type.toLowerCase() + "_" + identifier.toString();
   }

}
