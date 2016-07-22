package de.qyotta.eventstore.communication;

import de.qyotta.eventstore.EventStoreSettings;

public class EsContextInMemoryImpl implements ESContext {

   private final ESReader reader;
   private final EventStoreSettings settings;
   private final ESWriter writer;
   private final EsReaderWriterInMemoryImpl esReaderWriterInMemoryImpl;

   public EsContextInMemoryImpl(final EventStoreSettings settings) {
      this.settings = settings;

      esReaderWriterInMemoryImpl = new EsReaderWriterInMemoryImpl();
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

   public void reset() {
      esReaderWriterInMemoryImpl.reset();
   }

}
