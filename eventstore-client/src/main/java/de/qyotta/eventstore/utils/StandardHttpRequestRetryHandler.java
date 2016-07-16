package de.qyotta.eventstore.utils;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLException;

import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.annotation.Immutable;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

/**
 * {@link org.apache.http.client.HttpRequestRetryHandler} which assumes that all requested HTTP methods which should be idempotent according to RFC-2616 are in fact idempotent and can be retried.
 * <p>
 * According to RFC-2616 section 9.1.2 the idempotent HTTP methods are: GET, HEAD, PUT, DELETE, OPTIONS, and TRACE
 * </p>
 *
 * @since 4.2
 */
@Immutable
public class StandardHttpRequestRetryHandler extends DefaultHttpRequestRetryHandler {

   private final Map<String, Boolean> idempotentMethods;

   /**
    * Default constructor
    */
   public StandardHttpRequestRetryHandler(final int retryCount, final boolean requestSentRetryEnabled) {
      super(retryCount, requestSentRetryEnabled, Arrays.asList(InterruptedIOException.class, UnknownHostException.class, ConnectException.class, SSLException.class, NoHttpResponseException.class));
      this.idempotentMethods = new ConcurrentHashMap<String, Boolean>();
      this.idempotentMethods.put("GET", Boolean.TRUE);
      this.idempotentMethods.put("HEAD", Boolean.TRUE);
      this.idempotentMethods.put("POST", Boolean.TRUE);
      this.idempotentMethods.put("PUT", Boolean.TRUE);
      this.idempotentMethods.put("DELETE", Boolean.TRUE);
      this.idempotentMethods.put("OPTIONS", Boolean.TRUE);
      this.idempotentMethods.put("TRACE", Boolean.TRUE);
   }

   /**
    * Default constructor
    */
   public StandardHttpRequestRetryHandler() {
      this(3, false);
   }

   @Override
   protected boolean handleAsIdempotent(final HttpRequest request) {
      final String method = request.getRequestLine()
            .getMethod()
            .toUpperCase(Locale.ROOT);
      final Boolean b = this.idempotentMethods.get(method);
      return b != null && b.booleanValue();
   }

}
