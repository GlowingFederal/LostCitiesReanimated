package mcjty.lostcities.compat;

import mcjty.lostcities.api.IChunkPrimerFactory;
import mcjty.lostcities.config.*;
import mcjty.lostcities.dimensions.world.*;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.*;
import net.minecraftforge.event.world.WorldEvent;
import java.util.Random;

/** Installs before fresh spawn generation and before an existing world's first chunk load. */
public final class TerrainOverlay {
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void spawn(WorldEvent.CreateSpawnPosition event){install(event.getWorld());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void load(WorldEvent.Load event){install(event.getWorld());}
    private void install(World world){
        if(world.isRemote||!(world instanceof WorldServer)||!LostCityConfiguration.OVERLAY_DIMENSIONS.containsKey(world.provider.getDimension()))return;
        if(world.getWorldInfo().getTerrainType() instanceof LostWorldType||mcjty.lostcities.dimensions.ModDimensions.dimensionProfileMap.containsKey(world.provider.getDimension()))return;
        WorldServer server=(WorldServer)world;
        ChunkProviderServer chunks=server.getChunkProvider();
        IChunkGenerator original=chunks.chunkGenerator;
        if(original instanceof LostCityChunkGenerator)return;
        IChunkPrimerFactory factory;
        if(original instanceof IChunkPrimerFactory)factory=(IChunkPrimerFactory)original;
        else if(original.getClass()==ChunkGeneratorOverworld.class)factory=new NativeTerrain((ChunkGeneratorOverworld)original);
        else if(original.getClass().getName().equals("biomesoplenty.common.world.ChunkGeneratorOverworldBOP"))factory=new BopTerrain(original);
        else throw new IllegalArgumentException("Lost Cities overlay for dimension "+world.provider.getDimension()+" requires the native overworld generator or IChunkPrimerFactory; found "+original.getClass().getName());
        LostCityProfile profile=WorldTypeTools.getProfile(world);
        if(!profile.isDefault())throw new IllegalArgumentException("Lost Cities terrain overlays require a default-landscape profile, found "+profile.getName());
        MaterialWorldData data=MaterialWorldData.get(world);data.modern=true;data.markDirty();
        int seaLevel=world.getSeaLevel();
        LostCityChunkGenerator generator=new LostCityChunkGenerator(world,factory);
        world.setSeaLevel(seaLevel);
        chunks.chunkGenerator=generator;
        mcjty.lostcities.LostCities.setup.getLogger().info("Enabled Lost Cities overlay in dimension "+world.provider.getDimension()+" using "+profile.getName()+" over "+original.getClass().getName());
    }
    private static final class NativeTerrain implements IChunkPrimerFactory {
        private final ChunkGeneratorOverworld generator;
        private final Random random;
        NativeTerrain(ChunkGeneratorOverworld generator){
            this.generator=generator;
            try{
                java.lang.reflect.Field found=null;
                for(java.lang.reflect.Field field:ChunkGeneratorOverworld.class.getDeclaredFields())if(field.getType()==Random.class){if(found!=null)throw new IllegalStateException("Ambiguous native terrain random");found=field;}
                if(found==null)throw new IllegalStateException("Missing native terrain random");found.setAccessible(true);random=(Random)found.get(generator);
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot access native terrain random",error);}
        }
        private void seed(int x,int z){random.setSeed(x*341873128712L+z*132897987541L);}
        @Override public void fillChunk(int x,int z,ChunkPrimer primer){seed(x,z);generator.setBlocksInChunk(x,z,primer);}
        @Override public boolean replaceSurface(int x,int z,ChunkPrimer primer,Biome[] biomes){seed(x,z);generator.replaceBiomeBlocks(x,z,primer,biomes);return true;}
        @Override public int getHeight(int x,int z,int localX,int localZ){ChunkPrimer primer=new ChunkPrimer();fillChunk(x,z,primer);for(int y=255;y>0;y--)if(primer.getBlockState(localX,y,localZ).getBlock()!=net.minecraft.init.Blocks.AIR)return y;return 0;}
    }
}
