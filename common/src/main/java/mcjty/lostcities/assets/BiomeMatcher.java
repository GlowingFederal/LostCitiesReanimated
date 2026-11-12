package mcjty.lostcities.assets;

import com.google.gson.*;
import java.util.function.Predicate;

/** Modern biome selector syntax; adapters resolve names and Forge biome-dictionary tags natively. */
public final class BiomeMatcher {
    public static boolean matches(JsonElement expression,Predicate<String> matches){
        if(expression==null||expression.isJsonNull())return true;
        if(expression.isJsonPrimitive())return matches.test(expression.getAsString());
        if(expression.isJsonArray()){for(JsonElement value:expression.getAsJsonArray())if(matches.test(value.getAsString()))return true;return false;}
        JsonObject object=expression.getAsJsonObject();
        if(object.has("if_any")){boolean any=false;for(JsonElement value:object.getAsJsonArray("if_any"))any|=matches.test(value.getAsString());if(!any)return false;}
        if(object.has("if_all"))for(JsonElement value:object.getAsJsonArray("if_all"))if(!matches.test(value.getAsString()))return false;
        if(object.has("excluding"))for(JsonElement value:object.getAsJsonArray("excluding"))if(matches.test(value.getAsString()))return false;
        return true;
    }
    private BiomeMatcher(){}
}
