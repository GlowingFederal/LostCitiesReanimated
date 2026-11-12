package mcjty.lostcities.compat;

import mcjty.lostcities.api.IChunkPrimerFactory;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.*;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.*;

/** Optional boundary to BOP 7's actual terrain and structure instances; no provider classes are bundled. */
final class BopTerrain implements IChunkPrimerFactory {
    private final Random random;
    private final MethodHandle terrain,surface;
    private final Map<String,MapGenStructure> structures=new HashMap<>();
    BopTerrain(IChunkGenerator generator){
        try {
            Class<?> type=generator.getClass();Random found=null;
            for(Field field:type.getDeclaredFields()){
                if(Modifier.isStatic(field.getModifiers()))continue;
                if(field.getType()==Random.class){field.setAccessible(true);if(found!=null)throw new IllegalStateException("Ambiguous BOP terrain random");found=(Random)field.get(generator);}
                if(MapGenStructure.class.isAssignableFrom(field.getType())){
                    field.setAccessible(true);MapGenStructure value=(MapGenStructure)field.get(generator);
                    String key=value instanceof MapGenStronghold?"stronghold":value instanceof MapGenVillage?"village":value instanceof MapGenMineshaft?"mineshaft":value instanceof MapGenScatteredFeature?"temple":value instanceof StructureOceanMonument?"monument":value instanceof WoodlandMansion?"mansion":null;
                    if(key!=null&&structures.put(key,value)!=null)throw new IllegalStateException("Ambiguous BOP structure "+key);
                }
            }
            if(found==null)throw new IllegalStateException("Missing BOP terrain random");random=found;
            terrain=MethodHandles.lookup().unreflect(type.getMethod("setChunkAirStoneWater",int.class,int.class,ChunkPrimer.class)).bindTo(generator);
            surface=MethodHandles.lookup().unreflect(type.getMethod("replaceBlocksForBiome",int.class,int.class,ChunkPrimer.class,Biome[].class)).bindTo(generator);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Unsupported Biomes O' Plenty terrain API",error);}
    }
    private void seed(int x,int z){random.setSeed(x*341873128712L+z*132897987541L);}
    @Override public void fillChunk(int x,int z,ChunkPrimer primer){seed(x,z);try{terrain.invokeExact(x,z,primer);}catch(Throwable error){throw new IllegalStateException("BOP base terrain failed at "+x+","+z,error);}}
    @Override public boolean replaceSurface(int x,int z,ChunkPrimer primer,Biome[] biomes){seed(x,z);try{surface.invokeExact(x,z,primer,biomes);return true;}catch(Throwable error){throw new IllegalStateException("BOP biome surface failed at "+x+","+z,error);}}
    @Override public MapGenStructure getStructureGenerator(String feature){return structures.get(feature);}
    @Override public int getHeight(int x,int z,int localX,int localZ){ChunkPrimer primer=new ChunkPrimer();fillChunk(x,z,primer);for(int y=255;y>0;y--)if(primer.getBlockState(localX,y,localZ).getBlock()!=Blocks.AIR)return y;return 0;}
}
