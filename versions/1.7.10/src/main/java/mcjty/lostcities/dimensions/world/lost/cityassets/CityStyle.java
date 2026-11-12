package mcjty.lostcities.dimensions.world.lost.cityassets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.lostcities.api.ILostCityCityStyle;
import mcjty.lostcities.varia.Tools;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CityStyle implements ILostCityCityStyle {

    private com.google.gson.JsonObject rawDefinition;
    public com.google.gson.JsonElement setting(String group,String key){
        if(rawDefinition!=null&&rawDefinition.has(group)){com.google.gson.JsonObject object=rawDefinition.getAsJsonObject(group);if(object.has(key))return object.get(key);}
        return inherit==null?null:AssetRegistries.CITYSTYLES.get(inherit).setting(group,key);
    }
    public com.google.gson.JsonElement setting(String key){if(rawDefinition!=null&&rawDefinition.has(key))return rawDefinition.get(key);return inherit==null?null:AssetRegistries.CITYSTYLES.get(inherit).setting(key);}
    public Character getGrassBlock(){com.google.gson.JsonElement value=setting("parkblocks","grass");return value==null?null:value.getAsCharacter();}
    public float decimal(String group,String key,float fallback){com.google.gson.JsonElement value=setting(group,key);return value==null?fallback:value.getAsFloat();}
    private final java.util.Map<String,java.util.List<com.google.gson.JsonObject>> portableSelectors=new java.util.HashMap<>();
    public String select(String key,Random random,int x,int z){init();return mcjty.lostcities.assets.WeightedSelector.select(portableSelectors.getOrDefault(key,java.util.Collections.emptyList()),random,x,z);}
    private String name;

    private final List<Pair<Float, String>> buildingSelector = new ArrayList<>();
    private final List<Pair<Float, String>> bridgeSelector = new ArrayList<>();
    private final List<Pair<Float, String>> parkSelector = new ArrayList<>();
    private final List<Pair<Float, String>> fountainSelector = new ArrayList<>();
    private final List<Pair<Float, String>> stairSelector = new ArrayList<>();
    private final List<Pair<Float, String>> frontSelector = new ArrayList<>();
    private final List<Pair<Float, String>> railDungeonSelector = new ArrayList<>();
    private final List<Pair<Float, String>> multiBuildingSelector = new ArrayList<>();
    private String style;

    private Integer streetWidth;
    private Integer minFloorCount;
    private Integer minCellarCount;
    private Integer maxFloorCount;
    private Integer maxCellarCount;

    private Float explosionChance;

    private Character streetBlock;
    private Character streetBaseBlock;
    private Character streetVariantBlock;
    private Character parkElevationBlock;
    private Character corridorRoofBlock;
    private Character corridorGlassBlock;
    private Character railMainBlock;
    private Character borderBlock;
    private Character wallBlock;
    private Character sphereBlock;          // Used for 'space' landscape type
    private Character sphereSideBlock;      // Used for 'space' landscape type
    private Character sphereGlassBlock;     // Used for 'space' landscape type

    private String inherit;
    private boolean resolving;
    private boolean resolveInherit = false;

    public CityStyle(JsonObject object) {
        readFromJSon(object);
    }

    public CityStyle(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getStyle() {
        return style;
    }

    @Override
    public Float getExplosionChance() {
        return explosionChance;
    }

    @Override
    public int getStreetWidth() {
        return streetWidth;
    }

    @Override
    public Integer getMinFloorCount() {
        return minFloorCount;
    }

    @Override
    public Integer getMinCellarCount() {
        return minCellarCount;
    }

    @Override
    public Integer getMaxFloorCount() {
        return maxFloorCount;
    }

    @Override
    public Integer getMaxCellarCount() {
        return maxCellarCount;
    }

    @Override
    public Character getStreetBlock() {
        return streetBlock;
    }

    @Override
    public Character getStreetBaseBlock() {
        return streetBaseBlock;
    }

    @Override
    public Character getStreetVariantBlock() {
        return streetVariantBlock;
    }

    @Override
    public Character getRailMainBlock() {
        return railMainBlock;
    }

    @Override
    public Character getParkElevationBlock() {
        return parkElevationBlock;
    }

    @Override
    public Character getCorridorRoofBlock() {
        return corridorRoofBlock;
    }

    @Override
    public Character getCorridorGlassBlock() {
        return corridorGlassBlock;
    }

    @Override
    public Character getBorderBlock() {
        return borderBlock;
    }

    @Override
    public Character getWallBlock() {
        return wallBlock;
    }

    public Character getSphereBlock() {
        return sphereBlock;
    }

    public Character getSphereSideBlock() {
        return sphereSideBlock;
    }

    public Character getSphereGlassBlock() {
        return sphereGlassBlock;
    }

    @Override
    public void init() {
        if(resolving)throw new IllegalArgumentException("Cyclic city-style inheritance at "+name);
        if (!resolveInherit) {
            resolving=true;
            try {
            if (inherit != null) {
                CityStyle inheritFrom = AssetRegistries.CITYSTYLES.get(inherit);
                if (inheritFrom == null) {
                    throw new RuntimeException("Cannot find citystyle '" + inherit + "' to inherit from!");
                }
                if (style == null) {
                    style = inheritFrom.getStyle();
                }
                for(java.util.Map.Entry<String,java.util.List<com.google.gson.JsonObject>> entry:inheritFrom.portableSelectors.entrySet())portableSelectors.computeIfAbsent(entry.getKey(),key->new java.util.ArrayList<>()).addAll(entry.getValue());
                buildingSelector.addAll(inheritFrom.buildingSelector);
                bridgeSelector.addAll(inheritFrom.bridgeSelector);
                parkSelector.addAll(inheritFrom.parkSelector);
                fountainSelector.addAll(inheritFrom.fountainSelector);
                stairSelector.addAll(inheritFrom.stairSelector);
                frontSelector.addAll(inheritFrom.frontSelector);
                railDungeonSelector.addAll(inheritFrom.railDungeonSelector);
                multiBuildingSelector.addAll(inheritFrom.multiBuildingSelector);
                if (explosionChance == null) {
                    explosionChance = inheritFrom.explosionChance;
                }
                if (streetWidth == null) {
                    streetWidth = inheritFrom.streetWidth;
                }
                if (minFloorCount == null) {
                    minFloorCount = inheritFrom.minFloorCount;
                }
                if (minCellarCount == null) {
                    minCellarCount = inheritFrom.minCellarCount;
                }
                if (maxFloorCount == null) {
                    maxFloorCount = inheritFrom.maxFloorCount;
                }
                if (maxCellarCount == null) {
                    maxCellarCount = inheritFrom.maxCellarCount;
                }
                if (streetBlock == null) {
                    streetBlock = inheritFrom.streetBlock;
                }
                if (streetBaseBlock == null) {
                    streetBaseBlock = inheritFrom.streetBaseBlock;
                }
                if (streetVariantBlock == null) {
                    streetVariantBlock = inheritFrom.streetVariantBlock;
                }
                if (parkElevationBlock == null) {
                    parkElevationBlock = inheritFrom.parkElevationBlock;
                }
                if (corridorRoofBlock == null) {
                    corridorRoofBlock = inheritFrom.corridorRoofBlock;
                }
                if (corridorGlassBlock == null) {
                    corridorGlassBlock = inheritFrom.corridorGlassBlock;
                }
                if (railMainBlock == null) {
                    railMainBlock = inheritFrom.railMainBlock;
                }
                if (borderBlock == null) {
                    borderBlock = inheritFrom.borderBlock;
                }
                if (wallBlock == null) {
                    wallBlock = inheritFrom.wallBlock;
                }
                if (sphereBlock == null) {
                    sphereBlock = inheritFrom.sphereBlock;
                }
                if (sphereSideBlock == null) {
                    sphereSideBlock = inheritFrom.sphereSideBlock;
                }
                if (sphereGlassBlock == null) {
                    sphereGlassBlock = inheritFrom.sphereGlassBlock;
                }
            }
            resolveInherit=true;
            }finally{resolving=false;}
        }
    }

    @Override
    public void readFromJSon(JsonObject object) {
        name = object.get("name").getAsString();
        rawDefinition=object;
        portableSelectors.clear();
        if(object.has("selectors"))for(java.util.Map.Entry<String,com.google.gson.JsonElement> entry:object.getAsJsonObject("selectors").entrySet()){
            java.util.List<com.google.gson.JsonObject> values=new java.util.ArrayList<>();for(com.google.gson.JsonElement value:entry.getValue().getAsJsonArray())values.add(value.getAsJsonObject());portableSelectors.put(entry.getKey(),values);
        }else for(String key:new String[]{"buildings","multibuildings","parks","fountains","stairs","fronts","bridges","raildungeons"})if(object.has(key)){
            java.util.List<com.google.gson.JsonObject> values=new java.util.ArrayList<>();String field=mcjty.lostcities.assets.ModernAssetPack.selectorValueKey(key);
            for(com.google.gson.JsonElement value:object.getAsJsonArray(key)){com.google.gson.JsonObject legacy=value.getAsJsonObject(),choice=new com.google.gson.JsonParser().parse(legacy.toString()).getAsJsonObject();choice.add("value",legacy.get(field));values.add(choice);}portableSelectors.put(key,values);
        }


        if (object.has("inherit")) {
            inherit = object.get("inherit").getAsString();
        }

        if (object.has("style")) {
            style = object.get("style").getAsString();
        }

        if (object.has("explosionchance")) {
            explosionChance = object.get("explosionchance").getAsFloat();
        }

        if (object.has("streetblocks")) {
            JsonObject s = object.get("streetblocks").getAsJsonObject();
            if (s.has("border")) {
                borderBlock = s.get("border").getAsCharacter();
            }
            if (s.has("wall")) {
                wallBlock = s.get("wall").getAsCharacter();
            }
            if (s.has("street")) {
                streetBlock = s.get("street").getAsCharacter();
            }
            if (s.has("streetvariant")) {
                streetVariantBlock = s.get("streetvariant").getAsCharacter();
            }
            if (s.has("streetbase")) {
                streetBaseBlock = s.get("streetbase").getAsCharacter();
            }
            if (s.has("width")) {
                streetWidth = s.get("width").getAsInt();
            }
        }
        if (object.has("buildingsettings")) {
            JsonObject s = object.get("buildingsettings").getAsJsonObject();
            if (s.has("maxfloors")) {
                maxFloorCount = s.get("maxfloors").getAsInt();
            }
            if (s.has("maxcellars")) {
                maxCellarCount = s.get("maxcellars").getAsInt();
            }
            if (s.has("minfloors")) {
                minFloorCount = s.get("minfloors").getAsInt();
            }
            if (s.has("mincellars")) {
                minCellarCount = s.get("mincellars").getAsInt();
            }
        }
        if (object.has("railblocks")) {
            JsonObject s = object.get("railblocks").getAsJsonObject();
            if(s.has("railmain"))railMainBlock = s.get("railmain").getAsCharacter();
        }
        if (object.has("parkblocks")) {
            JsonObject s = object.get("parkblocks").getAsJsonObject();
            if(s.has("elevation"))parkElevationBlock = s.get("elevation").getAsCharacter();
        }
        if (object.has("corridorblocks")) {
            JsonObject s = object.get("corridorblocks").getAsJsonObject();
            if (s.has("roof")) {
                corridorRoofBlock = s.get("roof").getAsCharacter();
            }
            if (s.has("glass")) {
                corridorGlassBlock = s.get("glass").getAsCharacter();
            }
        }
        if (object.has("sphereblocks")) {
            JsonObject s = object.get("sphereblocks").getAsJsonObject();
            if (s.has("glass")) {
                sphereGlassBlock = s.get("glass").getAsCharacter();
            }
            if (s.has("border")) {
                sphereSideBlock = s.get("border").getAsCharacter();
            }
            if (s.has("inner")) {
                sphereBlock = s.get("inner").getAsCharacter();
            }
        }
        parseArraySafe(object, buildingSelector, "buildings", "building");
        parseArraySafe(object, multiBuildingSelector, "multibuildings", "multibuilding");
        parseArraySafe(object, parkSelector, "parks", "park");
        parseArraySafe(object, fountainSelector, "fountains", "fountain");
        parseArraySafe(object, stairSelector, "stairs", "stair");
        parseArraySafe(object, frontSelector, "fronts", "front");
        parseArraySafe(object, bridgeSelector, "bridges", "bridge");
        parseArraySafe(object, railDungeonSelector, "raildungeons", "dungeon");
    }

    private void parseArraySafe(JsonObject object, List<Pair<Float, String>> selector, String arrayName, String elName) {
        JsonArray array = getArraySafe(object, arrayName);
        for (JsonElement element : array) {
            if (element.getAsJsonObject().has("clear")) {
                selector.clear();
            } else {
                float factor = element.getAsJsonObject().get("factor").getAsFloat();
                String el = element.getAsJsonObject().get(elName).getAsString();
                selector.add(Pair.of(factor, el));
            }
        }
    }

    private JsonArray getArraySafe(JsonObject object, String key) {
        if (object.has(key)) {
            return object.get(key).getAsJsonArray();
        } else {
            return new JsonArray(); // Empty array
        }
    }

    public JsonObject writeToJSon() {
        JsonObject object = new JsonObject();
        object.add("type", new JsonPrimitive("citystyle"));
        object.add("name", new JsonPrimitive(name));
        object.add("style", new JsonPrimitive(style));

        JsonObject s = new JsonObject();
        s.add("street", new JsonPrimitive(streetBlock));
        s.add("streetvariant", new JsonPrimitive(streetVariantBlock));
        s.add("streetbase", new JsonPrimitive(streetBaseBlock));
        s.add("width", new JsonPrimitive(streetWidth));
        object.add("streetblocks", s);

        JsonArray array = new JsonArray();
        for (Pair<Float, String> pair : buildingSelector) {
            JsonObject o = new JsonObject();
            o.add("factor", new JsonPrimitive(pair.getKey()));
            o.add("building", new JsonPrimitive(pair.getValue()));
            array.add(o);
        }
        object.add("buildings", array);

        array = new JsonArray();
        for (Pair<Float, String> pair : multiBuildingSelector) {
            JsonObject o = new JsonObject();
            o.add("factor", new JsonPrimitive(pair.getKey()));
            o.add("multibuilding", new JsonPrimitive(pair.getValue()));
            array.add(o);
        }
        object.add("multibuildings", array);

        array = new JsonArray();
        for (Pair<Float, String> pair : parkSelector) {
            JsonObject o = new JsonObject();
            o.add("factor", new JsonPrimitive(pair.getKey()));
            o.add("park", new JsonPrimitive(pair.getValue()));
            array.add(o);
        }
        object.add("parks", array);

        array = new JsonArray();
        for (Pair<Float, String> pair : fountainSelector) {
            JsonObject o = new JsonObject();
            o.add("factor", new JsonPrimitive(pair.getKey()));
            o.add("fountain", new JsonPrimitive(pair.getValue()));
            array.add(o);
        }
        object.add("fountains", array);

        array = new JsonArray();
        for (Pair<Float, String> pair : stairSelector) {
            JsonObject o = new JsonObject();
            o.add("factor", new JsonPrimitive(pair.getKey()));
            o.add("stair", new JsonPrimitive(pair.getValue()));
            array.add(o);
        }
        object.add("stairs", array);

        array = new JsonArray();
        for (Pair<Float, String> pair : frontSelector) {
            JsonObject o = new JsonObject();
            o.add("factor", new JsonPrimitive(pair.getKey()));
            o.add("front", new JsonPrimitive(pair.getValue()));
            array.add(o);
        }
        object.add("fronts", array);

        array = new JsonArray();
        for (Pair<Float, String> pair : bridgeSelector) {
            JsonObject o = new JsonObject();
            o.add("factor", new JsonPrimitive(pair.getKey()));
            o.add("bridge", new JsonPrimitive(pair.getValue()));
            array.add(o);
        }
        object.add("bridges", array);

        array = new JsonArray();
        for (Pair<Float, String> pair : railDungeonSelector) {
            JsonObject o = new JsonObject();
            o.add("factor", new JsonPrimitive(pair.getKey()));
            o.add("dungeon", new JsonPrimitive(pair.getValue()));
            array.add(o);
        }
        object.add("raildungeons", array);

        return object;
    }

    public String getRandomStair(Random random,int x,int z){return mcjty.lostcities.assets.AssetContext.modern()?select("stairs",random,x,z):getRandomStair(random);}
    public String getRandomStair(Random random) {
        return Tools.getRandomFromList(random, stairSelector);
    }

    public String getRandomFront(Random random,int x,int z){return mcjty.lostcities.assets.AssetContext.modern()?select("fronts",random,x,z):getRandomFront(random);}
    public String getRandomFront(Random random) {
        return Tools.getRandomFromList(random, frontSelector);
    }

    public String getRandomRailDungeon(Random random,int x,int z){return mcjty.lostcities.assets.AssetContext.modern()?select("raildungeons",random,x,z):getRandomRailDungeon(random);}
    public String getRandomRailDungeon(Random random) {
        return Tools.getRandomFromList(random, railDungeonSelector);
    }

    public String getRandomPark(Random random,int x,int z){return mcjty.lostcities.assets.AssetContext.modern()?select("parks",random,x,z):getRandomPark(random);}
    public String getRandomPark(Random random) {
        return Tools.getRandomFromList(random, parkSelector);
    }

    public String getRandomBridge(Random random,int x,int z){return mcjty.lostcities.assets.AssetContext.modern()?select("bridges",random,x,z):getRandomBridge(random);}
    public String getRandomBridge(Random random) {
        return Tools.getRandomFromList(random, bridgeSelector);
    }

    public String getRandomFountain(Random random,int x,int z){return mcjty.lostcities.assets.AssetContext.modern()?select("fountains",random,x,z):getRandomFountain(random);}
    public String getRandomFountain(Random random) {
        return Tools.getRandomFromList(random, fountainSelector);
    }

    public String getRandomBuilding(Random random,int x,int z){return mcjty.lostcities.assets.AssetContext.modern()?select("buildings",random,x,z):getRandomBuilding(random);}
    public String getRandomBuilding(Random random) {
        return Tools.getRandomFromList(random, buildingSelector);
    }

    public String getRandomMultiBuilding(Random random,int x,int z){return mcjty.lostcities.assets.AssetContext.modern()?select("multibuildings",random,x,z):getRandomMultiBuilding(random);}
    public String getRandomMultiBuilding(Random random) {
        return Tools.getRandomFromList(random, multiBuildingSelector);
    }
}
