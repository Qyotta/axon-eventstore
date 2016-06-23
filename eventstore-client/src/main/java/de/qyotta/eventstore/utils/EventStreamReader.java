package de.qyotta.eventstore.utils;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.qyotta.eventstore.EsContext;
import de.qyotta.eventstore.EventStream;
import de.qyotta.eventstore.EventStreamImpl;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.model.EventStreamReaderException;

@SuppressWarnings("nls")
public class EventStreamReader {
   private static final Logger LOGGER = Logger.getLogger(EventStreamReader.class.getName());

   private long catchUpTerminationPeriodMillis = 30000;
   private final int intervalMillis;

   private boolean paused = false;
   private boolean isCatchingUp = false;

   private final String streamurl;
   private final EsContext context;

   private ScheduledExecutorService scheduler;
   private Runnable currentTask;

   private final EventStreamReaderCallback callback;
   private EventStreamReaderErrorCallback errorCallback = new EventStreamReaderErrorCallback() {
      @Override
      public void onError(String errorMessage, Throwable cause) {
         LOGGER.log(Level.SEVERE, errorMessage, cause);
         throw new EventStreamReaderException(cause);
      }
   };

   public interface EventStreamReaderCallback {
      void readEvent(final EventResponse event);
   }

   public interface EventStreamReaderErrorCallback {
      void onError(final String errorMessage, final Throwable cause);
   }

   /**
    * Creates a new {@link EventStreamReader} that reads the given stream in intervals. Catch up is scheduled at the given interval. If the given interval is 0 or negative it will not be scheduled but
    * can be invoked manually.
    *
    * @param streamurl
    * @param context
    * @param intervalMillis
    * @param callback
    */
   public EventStreamReader(final String streamurl, final EsContext context, final int intervalMillis, final EventStreamReaderCallback callback) {
      this.streamurl = streamurl;
      this.context = context;
      this.intervalMillis = intervalMillis;
      this.callback = callback;
   }

   public EventStreamReader(final String streamurl, final EsContext context, final int intervalMillis, final EventStreamReaderCallback callback, final EventStreamReaderErrorCallback errorCallback) {
      this.streamurl = streamurl;
      this.context = context;
      this.intervalMillis = intervalMillis;
      this.callback = callback;
      this.errorCallback = errorCallback;
   }

   /**
    * Start after the given eventId (exclusive)
    */
   public void start(final String eventId) {
      try {
         shutdownIfNeeded();
         final EventStream eventStream = new EventStreamImpl(streamurl, context);
         eventStream.setAfterEventId(eventId);
         start(eventStream);
      } catch (final Throwable t) {
         errorCallback.onError("Error initializog event stream.", t);
      }
   }

   /**
    * Start at the beginning of the stream.
    */
   public void start() {
      try {
         shutdownIfNeeded();
         final EventStream eventStream = new EventStreamImpl(streamurl, context);
         start(eventStream);
      } catch (final Throwable t) {
         errorCallback.onError("Error initializog event stream.", t);
      }
   }

   /**
    * Start after the given timestamp (exclusive)
    */
   public void start(final Date timestamp) {
      try {
         shutdownIfNeeded();
         final EventStream eventStream = new EventStreamImpl(streamurl, context);
         eventStream.setAfterTimestamp(timestamp);
         start(eventStream);
      } catch (final Throwable t) {
         errorCallback.onError("Error initializog event stream.", t);
      }
   }

   /**
    * Manually trigger a catch up. If the reader is in the middle of a catchup this will do nothing
    */
   public void catchUp() {
      if (currentTask != null) {
         currentTask.run();
      }
   }

   private void catchUp(final EventStream eventStream) {
      try {
         if (isPaused()) {
            return;
         }
         if (isCatchingUp) {
            return;
         }
         LOGGER.warning("Catching up.");
         isCatchingUp = true;
         eventStream.loadNext();
         while (eventStream.hasNext()) {
            callback.readEvent(eventStream.next());
         }
         isCatchingUp = false;
      } catch (final Throwable t) {
         errorCallback.onError("Error catching up to event stream.", t);
      }
   }

   private void shutdownIfNeeded() throws InterruptedException {
      if (scheduler != null) {
         scheduler.shutdown();
         scheduler.awaitTermination(catchUpTerminationPeriodMillis, TimeUnit.MILLISECONDS);
      }
   }

   private void start(final EventStream eventStream) {
      while (eventStream.hasNext()) {
         callback.readEvent(eventStream.next());
      }
      currentTask = () -> catchUp(eventStream);
      if (intervalMillis > 0) {
         scheduler = Executors.newScheduledThreadPool(1);
         scheduler.scheduleAtFixedRate(currentTask, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
      }
   }

   public boolean isPaused() {
      return paused;
   }

   public void setPaused(boolean paused) {
      this.paused = paused;
   }

   /**
    * If you restart this reader at any point this is the maximum amount of time the reader waits for running catchUp operations to finish.
    *
    * @param catchUpTerminationPeriodMillis
    */
   public void setCatchUpTerminationPeriodMillis(long catchUpTerminationPeriodMillis) {
      this.catchUpTerminationPeriodMillis = catchUpTerminationPeriodMillis;
   }

}
