package de.qyotta.neweventstore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import de.qyotta.eventstore.model.Entry;
import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.model.EventStreamFeed;

public class AtomFeedJsonReader {

   private Gson gson;

   public AtomFeedJsonReader() {
      final GsonBuilder gsonBuilder = new GsonBuilder();
      gsonBuilder.registerTypeAdapter(Event.class, new JsonDeserializer<Event>() {

         @Override
         public Event deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final Event.EventBuilder e = Event.builder();
            final JsonObject object = json.getAsJsonObject();
            final JsonElement dataElement = object.get("data");
            final JsonElement metadataElement = object.get("metadata");

            final String eventStreamId = object.get("eventStreamId")
                  .getAsString();
            final String eventId = object.get("eventId")
                  .getAsString();
            final Long eventNumber = object.get("eventNumber")
                  .getAsLong();
            final String eventType = object.get("eventType")
                  .getAsString();

            final String data = gson.toJson(dataElement);
            final String metadata = gson.toJson(metadataElement);

            return e.eventStreamId(eventStreamId)
                  .eventId(eventId)
                  .eventNumber(eventNumber)
                  .eventType(eventType)
                  .data(data)
                  .metadata(metadata)
                  .build();
         }
      });
      gson = gsonBuilder.create();
   }

   public List<Entry> readAtomFeed(InputStream in) {
      final EventStreamFeed eventStreamFeed = gson.fromJson(new BufferedReader(new InputStreamReader(in)), EventStreamFeed.class);
      return eventStreamFeed.getEntries();
   }

   public EventResponse readEvent(InputStream in) throws JsonSyntaxException, IOException {
      return gson.fromJson(read(in), EventResponse.class);
   }

   private static String read(InputStream input) throws IOException {
      try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
         return buffer.lines()
               .collect(Collectors.joining("\n"));
      }
   }

}
