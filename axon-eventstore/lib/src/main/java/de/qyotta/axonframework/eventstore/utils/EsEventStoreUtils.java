package de.qyotta.axonframework.eventstore.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("nls")
public final class EsEventStoreUtils {
   public static final String getStreamName(String type, Object identifier) {
      return type.toLowerCase() + "-" + identifier.toString();
   }
}
