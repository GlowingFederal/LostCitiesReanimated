package mcjty.lostcities.compat;

import mcjty.lostcities.LostCities;
import mcjty.lostcities.core.OptionalCaveBackend;
import mcjty.lostcities.setup.ModSetup;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.*;

/** Keeps the 1.7.10 block and metadata buffers together through optional cave carving. */
public final class BetterCavesAdapter {
    private final OptionalCaveBackend backend;
    public static boolean selected(MapGenBase generator){return generator.getClass().getName().startsWith("com.glowingfederal.bettercaves.NativeHooks$");}
    public BetterCavesAdapter(World world,boolean supported,MapGenBase caves,MapGenBase ravines){
        OptionalCaveBackend found=null;
        if(supported&&(selected(caves)||selected(ravines)))try{
            found=OptionalCaveBackend.open(BetterCavesAdapter.class.getClassLoader(),ModSetup.modConfigDir,world.getSeed(),world.provider.dimensionId,World.class,int.class,int.class,Block[].class,byte[].class);
        }catch(ReflectiveOperationException|LinkageError error){LostCities.setup.getLogger().warn("Better Caves native adapter is unavailable; retaining native Lost Cities caves",error);}
        backend=found;
        if(backend!=null)LostCities.setup.getLogger().info("Using YUNG's Legacy Better Caves native adapter in dimension "+world.provider.dimensionId);
    }
    public MapGenBase caves(MapGenBase selected,MapGenBase fallback){return selected(selected)?backend==null?fallback:new Caves(backend,fallback):selected;}
    public MapGenBase ravines(MapGenBase selected,MapGenBase fallback){return selected(selected)?backend==null?fallback:new Ravines(backend):selected;}
    public static void generate(MapGenBase generator,IChunkProvider provider,World world,int x,int z,Block[] blocks,byte[] metadata){
        if(generator instanceof Caves)((Caves)generator).carve(world,x,z,blocks,metadata);
        else if(generator instanceof Ravines)((Ravines)generator).generate(provider,world,x,z,blocks,metadata);
        else generator.func_151539_a(provider,world,x,z,blocks);
    }
    private static final class Caves extends MapGenBase {
        private final OptionalCaveBackend backend;
        private final MapGenBase fallback;
        Caves(OptionalCaveBackend backend,MapGenBase fallback){this.backend=backend;this.fallback=fallback;}
        void carve(World world,int x,int z,Block[] blocks,byte[] metadata){Object terrain=backend.terrain(world,x,z,blocks,metadata);backend.caves(terrain,x,z);}
        @Override public void func_151539_a(IChunkProvider provider,World world,int x,int z,Block[] blocks){fallback.func_151539_a(provider,world,x,z,blocks);}
    }
    private static final class Ravines extends MapGenRavine {
        private final OptionalCaveBackend backend;
        private Object terrain;
        Ravines(OptionalCaveBackend backend){this.backend=backend;}
        void generate(IChunkProvider provider,World world,int x,int z,Block[] blocks,byte[] metadata){
            if(!backend.ravines)return;
            try{terrain=backend.terrain(world,x,z,blocks,metadata);super.func_151539_a(provider,world,x,z,blocks);}finally{terrain=null;this.worldObj=null;}
        }
        @Override protected boolean isOceanBlock(Block[] blocks,int index,int x,int y,int z,int chunkX,int chunkZ){return terrain==null||!backend.floodedRavines?super.isOceanBlock(blocks,index,x,y,z,chunkX,chunkZ):false;}
        @Override protected void digBlock(Block[] blocks,int index,int x,int y,int z,int chunkX,int chunkZ,boolean foundTop){
            if(terrain==null){super.digBlock(blocks,index,x,y,z,chunkX,chunkZ,foundTop);return;}
            if(backend.floodedRavines&&backend.distance(terrain,x,z)<=.25F)return;
            backend.carve(terrain,x,y,z,backend.liquid(chunkX*16+x,chunkZ*16+z),backend.floodedRavines&&backend.ocean(terrain,x,z));
        }
    }
}
