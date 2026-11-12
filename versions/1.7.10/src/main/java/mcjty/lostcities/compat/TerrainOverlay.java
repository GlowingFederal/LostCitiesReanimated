package mcjty.lostcities.compat;

import mcjty.lostcities.api.IChunkPrimerFactory;
import mcjty.lostcities.config.*;
import mcjty.lostcities.dimensions.world.*;
import cpw.mods.fml.common.eventhandler.*;
import net.minecraft.world.*;
import net.minecraft.world.biome.BiomeGenBase;
import mcjty.lostcities.legacy.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.*;
import net.minecraftforge.event.world.WorldEvent;
import java.util.Random;

/** Installs before fresh spawn generation and before an existing world's first chunk load. */
public final class TerrainOverlay {
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void spawn(WorldEvent.CreateSpawnPosition event){install(event.world);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void load(WorldEvent.Load event){install(event.world);}
    private void install(World world){
        if(world.isRemote||!(world instanceof WorldServer)||!LostCityConfiguration.OVERLAY_DIMENSIONS.containsKey(world.provider.dimensionId))return;
        if(world.getWorldInfo().getTerrainType() instanceof LostWorldType||mcjty.lostcities.LostCities.dimensionProfiles.containsKey(world.provider.dimensionId))return;
        WorldServer server=(WorldServer)world;
        ChunkProviderServer chunks=server.theChunkProviderServer;
        IChunkProvider original=chunks.currentChunkProvider;
        if(original instanceof LostCityChunkGenerator)return;
        IChunkPrimerFactory factory;
        if(original instanceof IChunkPrimerFactory)factory=(IChunkPrimerFactory)original;
        else if(original.getClass()==ChunkProviderGenerate.class)factory=new NativeTerrain((ChunkProviderGenerate)original);
        else throw new IllegalArgumentException("Lost Cities overlay for dimension "+world.provider.dimensionId+" requires the native overworld generator or IChunkPrimerFactory; found "+original.getClass().getName());
        LostCityProfile profile=WorldTypeTools.getProfile(world);
        if(!profile.isDefault())throw new IllegalArgumentException("Lost Cities terrain overlays require a default-landscape profile, found "+profile.getName());
        MaterialWorldData data=MaterialWorldData.get(world);data.modern=true;data.markDirty();
        LostCityChunkGenerator generator=new LostCityChunkGenerator(world,world.getSeed());generator.otherGenerator=factory;
        chunks.currentChunkProvider=generator;
        mcjty.lostcities.LostCities.setup.getLogger().info("Enabled Lost Cities overlay in dimension "+world.provider.dimensionId+" using "+profile.getName()+" over "+original.getClass().getName());
    }
    private static final class NativeTerrain implements IChunkPrimerFactory {
        private final ChunkProviderGenerate generator;
        private final Random random;
        NativeTerrain(ChunkProviderGenerate generator){
            this.generator=generator;
            try{
                java.lang.reflect.Field found=null;
                for(java.lang.reflect.Field field:ChunkProviderGenerate.class.getDeclaredFields())if(field.getType()==Random.class){if(found!=null)throw new IllegalStateException("Ambiguous native terrain random");found=field;}
                if(found==null)throw new IllegalStateException("Missing native terrain random");found.setAccessible(true);random=(Random)found.get(generator);
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot access native terrain random",error);}
        }
        private void seed(int x,int z){random.setSeed(x*341873128712L+z*132897987541L);}
        @Override public void fillChunk(int x,int z,ChunkPrimer primer){seed(x,z);net.minecraft.block.Block[] blocks=new net.minecraft.block.Block[65536];generator.func_147424_a(x,z,blocks);primer.load(blocks,new byte[65536]);}
        @Override public boolean replaceSurface(int x,int z,ChunkPrimer primer,BiomeGenBase[] biomes){seed(x,z);net.minecraft.block.Block[] blocks=primer.blocks();byte[] metadata=primer.metadata();generator.replaceBlocksForBiome(x,z,blocks,metadata,biomes);primer.load(blocks,metadata);return true;}
        @Override public int getHeight(int x,int z,int localX,int localZ){ChunkPrimer primer=new ChunkPrimer();fillChunk(x,z,primer);for(int y=255;y>0;y--)if(primer.getBlockState(localX,y,localZ).getBlock()!=net.minecraft.init.Blocks.air)return y;return 0;}
    }
}
