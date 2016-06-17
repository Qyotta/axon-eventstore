package de.qyotta.eventstore.utils;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import de.qyotta.eventstore.model.SerializableEventData;

@SuppressWarnings("nls")
public class EventDataSerializer implements JsonDeserializer<SerializableEventData>, JsonSerializer<SerializableEventData> {

   private static final String DATA = "data";
   private static final String TYPE = "type";

   @Override
   public SerializableEventData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      final SerializableEventData.SerializableEventDataBuilder builder = SerializableEventData.builder();
      final JsonObject object = json.getAsJsonObject();
      final JsonElement typeElement = object.get(TYPE);
      final JsonElement dataElement = object.get(DATA);
      // fallback
      if (typeElement == null || dataElement == null) {
         final Object data = context.deserialize(json, Object.class);
         return builder.data(data)
               .build();
      }
      final String type = typeElement.getAsString();
      try {
         final Class<?> classOfData = Class.forName(type);
         builder.type(type)
               .data(context.deserialize(dataElement, classOfData));
         return builder.build();
      } catch (final ClassNotFoundException e) {
         throw new RuntimeException("SerializableEventData was serialized in an unknown type: " + type, e);
      }
   }

   @Override
   public JsonElement serialize(SerializableEventData src, Type typeOfSrc, JsonSerializationContext context) {
      final JsonObject object = new JsonObject();
      object.addProperty(TYPE, src.getData()
            .getClass()
            .getName());
      object.add(DATA, context.serialize(src.getData()));
      return object;
   }

}
