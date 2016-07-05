package de.qyotta.eventstore.utils;

import java.util.Date;

public interface EventStreamReader {

   /**
    * Start at the beginning of the stream.
    */
   void start();

   /**
    * Start after the given title (exclusive)
    */
   void start(String title);

   /**
    * Start after the given timestamp (exclusive)
    */
   void start(Date timestamp);

   /**
    * Manually trigger a catch up. If the reader is in the middle of a catchup this will do nothing
    */
   void catchUp();

   boolean isPaused();

   void setPaused(boolean paused);

   /**
    * If you restart this reader at any point this is the maximum amount of time the reader waits for running catchUp operations to finish.
    *
    * @param catchUpTerminationPeriodMillis
    */
   void setCatchUpTerminationPeriodMillis(long catchUpTerminationPeriodMillis);

}