package de.qyotta.eventstore.utils;

import java.time.Instant;
import java.util.Date;

import de.qyotta.eventstore.model.Entry;
import de.qyotta.eventstore.model.EventResponse;
import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("nls")
public class EsUtils {
   private static final String SEQUENCE_NUMBER_TITLE_SEPERATOR = "@";

   public static long getEventNumber(final Entry entry) {
      final String title = entry.getTitle();
      final String subsequenceNumber = title.substring(0, title.indexOf(SEQUENCE_NUMBER_TITLE_SEPERATOR));
      return Long.valueOf(subsequenceNumber);
   }

   public static Date timestampOf(final Entry entry) {
      return Date.from(Instant.parse(entry.getUpdated()));
   }

   public static Date timestampOf(final EventResponse entry) {
      return Date.from(Instant.parse(entry.getUpdated()));
   }

}
