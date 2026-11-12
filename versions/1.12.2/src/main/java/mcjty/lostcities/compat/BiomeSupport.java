package mcjty.lostcities.compat;

import com.google.gson.*;
import mcjty.lostcities.assets.*;
import mcjty.lostcities.dimensions.world.LostCityChunkGenerator;
import mcjty.lostcities.dimensions.world.lost.BiomeInfo;
import mcjty.lostcities.varia.ChunkCoord;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import java.util.*;

public final class BiomeSupport {
    public static boolean matches(Biome biome,String selector){
        String name=String.valueOf(Biome.REGISTRY.getNameForObject(biome));
        if(selector.startsWith("#")){
            String tag=selector.substring(selector.indexOf(':')+1).replaceFirst("^is_","").toUpperCase(Locale.ROOT);
            if(tag.equals("DEEP_OCEAN"))return name.contains("deep_ocean");
            for(BiomeDictionary.Type type:BiomeDictionary.getTypes(biome))if(type.getName().equalsIgnoreCase(tag))return true;
            return false;
        }
        if(selector.equals("minecraft:badlands"))selector="minecraft:mesa";
        if(selector.equals("minecraft:beach"))selector="minecraft:beaches";
        return selector.equals(name)||selector.equalsIgnoreCase(biome.biomeName);
    }
    public static boolean matches(Biome[] biomes,JsonElement expression){
        return BiomeMatcher.matches(expression,selector->{for(int index:new int[]{55,54,56,5,95})if(matches(biomes[index],selector))return true;return false;});
    }
    public static float maximumMultiplier(LostCityChunkGenerator provider){
        float maximum=Math.max(1,provider.getProfile().CITY_DEFAULT_BIOME_FACTOR);
        for(float factor:provider.getProfile().getBiomeFactorMap().values())maximum=Math.max(maximum,factor);
        JsonObject style=ModernAssetPack.definition("worldstyles",provider.getProfile().getWorldStyle());
        if(style!=null&&style.has("citybiomemultipliers"))for(JsonElement entry:style.getAsJsonArray("citybiomemultipliers"))maximum=Math.max(maximum,entry.getAsJsonObject().get("multiplier").getAsFloat());
        return maximum;
    }
    public static float modify(LostCityChunkGenerator provider,int x,int z,float factor){
        if(provider.getProfile().isDefault()){
            int height=provider.planningHeight(x,z);
            if(height<provider.getProfile().modern.cityMinHeight||height>provider.getProfile().modern.cityMaxHeight)return 0;
        }
        Biome[] biomes=BiomeInfo.getBiomeInfo(provider,new ChunkCoord(provider.dimensionId,x,z)).getBiomes();
        Map<String,Float> explicit=provider.getProfile().getBiomeFactorMap();
        for(Biome biome:biomes){Float value=explicit.get(String.valueOf(Biome.REGISTRY.getNameForObject(biome)));if(value!=null)return factor*value;}
        JsonObject style=ModernAssetPack.definition("worldstyles",provider.getProfile().getWorldStyle());
        if(style!=null&&style.has("citybiomemultipliers"))for(JsonElement value:style.getAsJsonArray("citybiomemultipliers")){
            JsonObject entry=value.getAsJsonObject();if(matches(biomes,entry.get("biomes")))return factor*entry.get("multiplier").getAsFloat();
        }
        return factor*provider.getProfile().CITY_DEFAULT_BIOME_FACTOR;
    }
    private BiomeSupport(){}
}
