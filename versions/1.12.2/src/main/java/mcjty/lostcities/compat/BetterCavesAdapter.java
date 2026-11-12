package mcjty.lostcities.compat;

import mcjty.lostcities.LostCities;
import mcjty.lostcities.core.OptionalCaveBackend;
import mcjty.lostcities.setup.ModSetup;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.*;

/** Reuses YUNG's Legacy native terrain/carving implementation without changing the world's provider. */
public final class BetterCavesAdapter {
    private final OptionalCaveBackend backend;
    public static boolean selected(MapGenBase generator){return generator.getClass().getName().startsWith("com.glowingfederal.bettercaves.NativeHooks$");}
    public BetterCavesAdapter(World world,boolean supported,MapGenBase caves,MapGenBase ravines){
        OptionalCaveBackend found=null;
        if(supported&&(selected(caves)||selected(ravines)))try{
            found=OptionalCaveBackend.open(BetterCavesAdapter.class.getClassLoader(),ModSetup.modConfigDir,world.getSeed(),world.provider.getDimension(),World.class,int.class,int.class,ChunkPrimer.class);
        }catch(ReflectiveOperationException|LinkageError error){LostCities.setup.getLogger().warn("Better Caves native adapter is unavailable; retaining native Lost Cities caves",error);}
        backend=found;
        if(backend!=null)LostCities.setup.getLogger().info("Using YUNG's Legacy Better Caves native adapter in dimension "+world.provider.getDimension());
    }
    public MapGenBase caves(MapGenBase selected,MapGenBase fallback){return selected(selected)?backend==null?fallback:new Caves(backend):selected;}
    public MapGenBase ravines(MapGenBase selected,MapGenBase fallback){return selected(selected)?backend==null?fallback:new Ravines(backend):selected;}
    private static final class Caves extends MapGenBase {
        private final OptionalCaveBackend backend;
        Caves(OptionalCaveBackend backend){this.backend=backend;}
        @Override public void generate(World world,int x,int z,ChunkPrimer buffer){Object terrain=backend.terrain(world,x,z,buffer);backend.caves(terrain,x,z);}
    }
    private static final class Ravines extends MapGenRavine {
        private final OptionalCaveBackend backend;
        private Object terrain;
        Ravines(OptionalCaveBackend backend){this.backend=backend;}
        @Override public void generate(World world,int x,int z,ChunkPrimer buffer){
            if(!backend.ravines)return;
            try{terrain=backend.terrain(world,x,z,buffer);super.generate(world,x,z,buffer);}finally{terrain=null;this.world=null;}
        }
        @Override protected boolean isOceanBlock(ChunkPrimer buffer,int x,int y,int z,int chunkX,int chunkZ){return !backend.floodedRavines&&super.isOceanBlock(buffer,x,y,z,chunkX,chunkZ);}
        @Override protected void digBlock(ChunkPrimer buffer,int x,int y,int z,int chunkX,int chunkZ,boolean foundTop){
            if(backend.floodedRavines&&backend.distance(terrain,x,z)<=.25F)return;
            backend.carve(terrain,x,y,z,backend.liquid(chunkX*16+x,chunkZ*16+z),backend.floodedRavines&&backend.ocean(terrain,x,z));
        }
    }
}
