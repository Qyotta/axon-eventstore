package de.qyotta.eventstore.model;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class Entry {
   private String eventId;
   private String eventType;
   private Long eventNumber;
   private String data;
   private String metaData;
   private String streamId;
   private Boolean isLinkMetaData;
   private Long positionEventNumber;
   private String positionStreamId;
   private String title;
   private String id;
   private String updated;
   private Author author;
   private String summary;
   private List<Link> links;

   private Event content;
}