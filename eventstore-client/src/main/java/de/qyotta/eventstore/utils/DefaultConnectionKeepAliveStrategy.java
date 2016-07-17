package de.qyotta.eventstore.utils;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.annotation.Immutable;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of a strategy deciding duration that a connection can remain idle.
 *
 * The default implementation looks solely at the 'Keep-Alive' header's timeout token.
 *
 * @since 4.0
 */
@Immutable
public class DefaultConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {
   private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConnectionKeepAliveStrategy.class.getName());

   public static final DefaultConnectionKeepAliveStrategy INSTANCE = new DefaultConnectionKeepAliveStrategy();

   private static final long DEFAULT_KEEP_ALIVE = 15 * 1000;

   @Override
   public long getKeepAliveDuration(final HttpResponse response, final HttpContext context) {
      Args.notNull(response, "HTTP response");
      final HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
      while (it.hasNext()) {
         final HeaderElement he = it.nextElement();
         final String param = he.getName();
         final String value = he.getValue();
         if (value != null && param.equalsIgnoreCase("timeout")) {
            try {
               return Long.parseLong(value) * 1000;
            } catch (final NumberFormatException ignore) {
               LOGGER.warn("keep alive timeout could not be parsed: param=" + param + " value:" + value, ignore);
            }
         }
      }
      return DEFAULT_KEEP_ALIVE;
   }

}
