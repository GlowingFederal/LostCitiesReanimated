package mcjty.lostcities.legacy;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.util.ResourceLocation;
import java.util.*;

/** Legacy biomes have numeric IDs and names, rather than a namespaced Forge registry. */
public final class LegacyBiomes {
    public static final BiomeGenBase OCEAN=BiomeGenBase.ocean,DEEP_OCEAN=BiomeGenBase.deepOcean,FROZEN_OCEAN=BiomeGenBase.frozenOcean,
            RIVER=BiomeGenBase.river,FROZEN_RIVER=BiomeGenBase.frozenRiver,BEACH=BiomeGenBase.beach,COLD_BEACH=BiomeGenBase.coldBeach;
    private static final Map<Integer,String> VANILLA=new HashMap<>();
    static {
        int[] ids={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39};
        String[] names={"ocean","plains","desert","extreme_hills","forest","taiga","swampland","river","hell","sky","frozen_ocean","frozen_river","ice_flats","ice_mountains","mushroom_island","mushroom_island_shore","beaches","desert_hills","forest_hills","taiga_hills","smaller_extreme_hills","jungle","jungle_hills","jungle_edge","deep_ocean","stone_beach","cold_beach","birch_forest","birch_forest_hills","roofed_forest","cold_taiga","cold_taiga_hills","redwood_taiga","redwood_taiga_hills","extreme_hills_with_trees","savanna","savanna_rock","mesa","mesa_rock","mesa_clear_rock"};
        for(int i=0;i<ids.length;i++)VANILLA.put(ids[i],names[i]);
    }
    public static ResourceLocation name(BiomeGenBase biome){
        String vanilla=VANILLA.get(biome.biomeID);
        if(vanilla!=null&&biome.getClass().getName().startsWith("net.minecraft."))return new ResourceLocation("minecraft",vanilla);
        String pkg=biome.getClass().getName().toLowerCase(Locale.ROOT);
        String domain=pkg.contains("biomesoplenty")?"biomesoplenty":pkg.contains("etfuturum")?"etfuturum":"legacy";
        return new ResourceLocation(domain,biome.biomeName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]","_"));
    }
    public static BiomeGenBase find(ResourceLocation id){
        String text=id.toString();
        for(BiomeGenBase biome:BiomeGenBase.getBiomeGenArray())if(biome!=null&&(name(biome).toString().equals(text)||biome.biomeName.equalsIgnoreCase(id.getResourcePath())))return biome;
        return null;
    }
    private LegacyBiomes(){}
}
