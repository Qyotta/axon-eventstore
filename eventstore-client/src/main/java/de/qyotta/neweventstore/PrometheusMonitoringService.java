package de.qyotta.neweventstore;

import io.prometheus.client.Histogram;

@SuppressWarnings("nls")
public class PrometheusMonitoringService {

   private static Histogram eventReadHistogram = Histogram.build()
         .labelNames("stream", "identifier", "host")
         .help("Read time per event")
         .name("de_qyotta_http_reader_event_read_time")
         .buckets(25.0, 100.0)
         .register();

   private static Histogram eventRequestHistogram = Histogram.build()
         .labelNames("stream", "identifier", "host")
         .help("Time for a single event request")
         .name("de_qyotta_http_reader_event_request_time")
         .buckets(25.0, 100.0)
         .register();

   private static Histogram sliceReadHistogram = Histogram.build()
         .labelNames("stream", "identifier", "host")
         .help("Read time per event slice")
         .name("de_qyotta_http_reader_slice_read_time")
         .buckets(25.0, 100.0)
         .register();

   public static void eventReadDuration(final long duration, final String streamName, final String identifier, final String host) {
      eventReadHistogram.labels(streamName, identifier, host)
            .observe(duration);
   }

   public static void eventRequestDuration(final long duration, final String streamName, final String identifier, final String host) {
      eventRequestHistogram.labels(streamName, identifier, host)
            .observe(duration);
   }

   public static void eventSliceDuration(final long duration, final String streamName, final String identifier, final String host) {
      sliceReadHistogram.labels(streamName, identifier, host)
            .observe(duration);
   }
}
