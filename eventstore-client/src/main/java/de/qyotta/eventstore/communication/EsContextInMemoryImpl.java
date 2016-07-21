package de.qyotta.eventstore.communication;

import de.qyotta.eventstore.EventStoreSettings;

public class EsContextInMemoryImpl implements ESContext {

   private final ESReader reader;
   private final EventStoreSettings settings;
   private final ESWriter writer;

   public EsContextInMemoryImpl(final EventStoreSettings settings) {
      this.settings = settings;

      final EsReaderWriterInMemoryImpl esReaderWriterInMemoryImpl = new EsReaderWriterInMemoryImpl();
      reader = esReaderWriterInMemoryImpl;
      writer = esReaderWriterInMemoryImpl;
   }

   @Override
   public ESReader getReader() {
      return reader;
   }

   @Override
   public EventStoreSettings getSettings() {
      return settings;
   }

   @Override
   public ESWriter getWriter() {
      return writer;
   }

}
