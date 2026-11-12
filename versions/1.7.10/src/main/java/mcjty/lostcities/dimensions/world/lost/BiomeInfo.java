package mcjty.lostcities.dimensions.world.lost;

import mcjty.lostcities.dimensions.world.LostCityChunkGenerator;
import mcjty.lostcities.varia.ChunkCoord;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.HashMap;
import java.util.Map;

public class BiomeInfo {

    private static Map<ChunkCoord, BiomeInfo> biomeInfoMap = new mcjty.lostcities.core.ScopedMetadataCache<>(32768);

    private BiomeGenBase[] biomesForBiomeCheck = null;


    public static void cleanCache() {
        biomeInfoMap.clear();
    }

    public static BiomeInfo getBiomeInfo(LostCityChunkGenerator provider, ChunkCoord coord) {
        if(!mcjty.lostcities.core.GenerationScope.owns(provider))return provider.contextual(()->getBiomeInfo(provider,coord));
        if (!biomeInfoMap.containsKey(coord)) {
            BiomeInfo info = new BiomeInfo();
            int chunkX = coord.getChunkX();
            int chunkZ = coord.getChunkZ();
            info.biomesForBiomeCheck = provider.worldObj.getWorldChunkManager().getBiomesForGeneration(info.biomesForBiomeCheck, (mcjty.lostcities.assets.AssetContext.modern() ? chunkX : chunkX - 1) * 4 - 2, chunkZ * 4 - 2, 10, 10);
            biomeInfoMap.put(coord, info);
        }
        return biomeInfoMap.get(coord);
    }

    public BiomeGenBase[] getBiomes() {
        return biomesForBiomeCheck;
    }
}
