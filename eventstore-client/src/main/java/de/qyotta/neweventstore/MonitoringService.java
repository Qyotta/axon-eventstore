package de.qyotta.neweventstore;

public interface MonitoringService {
   void eventReadDuration(int duration, String streamName, String identifier, String host);

   void eventSliceDuration(int duration, String streamName, String identifier, String host);
}
