package de.qyotta.eventstore.model;

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
@SuppressWarnings("nls")
public class Link {
   public static final String LAST = "last";
   public static final String PREVIOUS = "previous";
   public static final String EDIT = "edit";
   private String uri;
   private String relation;
}
