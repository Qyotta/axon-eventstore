package de.qyotta.axonframework.eventstore.utils;

import static de.qyotta.axonframework.eventstore.utils.Constants.DOMAIN_EVENT_TYPE;
import static de.qyotta.axonframework.eventstore.utils.Constants.ES_EVENT_TYPE_STREAM_PREFIX;

import java.util.Date;

import org.axonframework.domain.DomainEventMessage;

import de.qyotta.eventstore.EventStoreClient;
import de.qyotta.eventstore.EventStoreSettings;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.utils.EventStreamReader;
import de.qyotta.eventstore.utils.EventStreamReaderImpl.EventStreamReaderCallback;

public class EsDomainEventReader implements EventStreamReader {
   private final EventStreamReader reader;

   public interface EsDomainEventReaderCallback {
      void onEvent(final DomainEventMessage<?> event);
   }

   private EsDomainEventReaderCallback callback = new EsDomainEventReaderCallback() {
      @Override
      public void onEvent(DomainEventMessage<?> event) {
         // do nothing by default
      }
   };

   public void setCallback(EsDomainEventReaderCallback callback) {
      this.callback = callback;
   }

   public EsDomainEventReader(final EventStoreSettings settings, final int intervalMillis) {
      this.reader = new EventStoreClient(settings).newEventStreamReader(ES_EVENT_TYPE_STREAM_PREFIX + DOMAIN_EVENT_TYPE, intervalMillis, new EventStreamReaderCallback() {
         @Override
         public void readEvent(EventResponse event) {
            callback.onEvent(EsEventStoreUtils.domainEventMessageOf(event.getContent()));
         }
      });
   }

   @Override
   public void start() {
      reader.start();
   }

   @Override
   public void start(String eventId) {
      reader.start(eventId);
   }

   @Override
   public void start(Date timestamp) {
      reader.start(timestamp);
   }

   @Override
   public void catchUp() {
      reader.catchUp();
   }

   @Override
   public boolean isPaused() {
      return reader.isPaused();
   }

   @Override
   public void setPaused(boolean paused) {
      reader.setPaused(paused);
   }

   @Override
   public void setCatchUpTerminationPeriodMillis(long catchUpTerminationPeriodMillis) {
      reader.setCatchUpTerminationPeriodMillis(catchUpTerminationPeriodMillis);
   }
}
