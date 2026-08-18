import com.google.gson.*;
import java.lang.reflect.Type;

public class MenuItemDeserializer implements JsonDeserializer<MenuItem> {
  @Override
  public MenuItem deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
      throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();
    String category = obj.has("category") ? obj.get("category").getAsString() : "";

    if ("Beverage".equalsIgnoreCase(category)) {
      return ctx.deserialize(obj, Beverage.class);
    } else if ("Pastry".equalsIgnoreCase(category)) {
      return ctx.deserialize(obj, Pastry.class);
    } else {
      throw new JsonParseException("Unknown MenuItem category: " + category);
    }
  }
}

/** honestly i don't really understand what ts does but whatever, it fixes the problem 
 * of deserializing the abstract class MunuItem into its concrete subclasses Beverage and Pastry.
 * - JV
 */