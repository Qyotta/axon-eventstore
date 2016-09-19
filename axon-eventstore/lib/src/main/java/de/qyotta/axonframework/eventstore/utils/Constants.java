package de.qyotta.axonframework.eventstore.utils;

import org.axonframework.domain.DomainEventMessage;

@SuppressWarnings("nls")
public interface Constants {
   public static final String ES_EVENT_TYPE_STREAM_PREFIX = "$et-";
   public static final String DOMAIN_EVENT_TYPE = DomainEventMessage.class.getSimpleName();
   public static final String AGGREGATE_ID_KEY = "AgregateIdentifier";
   public static final String PAYLOAD_REVISION_KEY = "PayloadRevision";
   public static final String EVENT_METADATA_KEY = "EventMetaData";
}
