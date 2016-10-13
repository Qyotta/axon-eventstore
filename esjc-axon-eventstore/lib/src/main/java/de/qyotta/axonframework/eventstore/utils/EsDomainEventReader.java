package de.qyotta.axonframework.eventstore.utils;

import de.qyotta.eventstore.EventStoreClient;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.utils.EventStreamReader;
import de.qyotta.eventstore.utils.EventStreamReaderImpl;
import de.qyotta.eventstore.utils.EventStreamReaderImpl.EventStreamReaderCallback;

import java.util.Date;

import org.axonframework.domain.DomainEventMessage;

public class EsDomainEventReader implements EventStreamReader {
   private final EventStreamReader reader;

   public interface EsDomainEventReaderCallback {
      void onEvent(final DomainEventMessage<?> event);
   }

   public interface EsDomainEventReaderErrorCallback {
      void onEvent(final String messge, final Throwable cause);
   }

   private EsDomainEventReaderCallback callback = new EsDomainEventReaderCallback() {
      @Override
      public void onEvent(DomainEventMessage<?> event) {
         // do nothing by default
      }
   };

   private EsDomainEventReaderErrorCallback errorCallback = new EsDomainEventReaderErrorCallback() {
      @Override
      public void onEvent(final String message, final Throwable cause) {
         // do nothing by default
      }
   };

   public void setCallback(EsDomainEventReaderCallback callback) {
      this.callback = callback;
   }

   public void setErrorCallback(EsDomainEventReaderErrorCallback errorCallback) {
      this.errorCallback = errorCallback;
   }

   public EsDomainEventReader(final EventStoreClient client, final String streamName, final int intervalMillis) {
      this.reader = client.newEventStreamReader(streamName, intervalMillis, new EventStreamReaderCallback() {
         @Override
         public void readEvent(EventResponse event) {
            callback.onEvent(EsEventStoreUtils.domainEventMessageOf(event));
         }
      }, new EventStreamReaderImpl.EventStreamReaderErrorCallback() {
         @Override
         public void onError(final String errorMessage, final Throwable cause) {
            errorCallback.onEvent(errorMessage, cause);
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
