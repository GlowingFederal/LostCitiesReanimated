package mcjty.lostcities.api;

import net.minecraft.world.chunk.ChunkPrimer;

/**
 * Implement this interface in your IChunkGenerator implementation to provide
 * a chunk. This should simply make the basic chunkprimer. No biome decoration or
 * anything else
 */
public interface IChunkPrimerFactory {

    // Fill a chunk primer with base data. No biome decoration
    void fillChunk(int chunkX, int chunkZ, ChunkPrimer primer);

    /** Optional native surface stage. Return true when this method handled biome surfaces and their Forge event. */
    default boolean replaceSurface(int chunkX,int chunkZ,net.minecraft.world.chunk.ChunkPrimer primer,net.minecraft.world.biome.Biome[] biomes){return false;}

    /** Optional canonical structure generator: stronghold, village, mineshaft, temple, monument or mansion. */
    default net.minecraft.world.gen.structure.MapGenStructure getStructureGenerator(String feature){return null;}

    // Get (an estimate) of the height (0-255) of a coordinate in a chunk
    int getHeight(int chunkX, int chunkZ, int x, int z);
}
