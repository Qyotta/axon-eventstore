package de.qyotta.axonframework.eventstore.utils;

import org.axonframework.domain.DomainEventMessage;

@SuppressWarnings("nls")
public interface Constants {
   public static final String ES_EVENT_TYPE_STREAM_PREFIX = "$et-";
   public static final String DOMAIN_EVENT_TYPE = DomainEventMessage.class.getSimpleName();
}
