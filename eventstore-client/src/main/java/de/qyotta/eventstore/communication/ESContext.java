package de.qyotta.eventstore.communication;

import de.qyotta.eventstore.EventStoreSettings;

public interface ESContext {

   ESReader getReader();

   EventStoreSettings getSettings();

   ESWriter getWriter();

}