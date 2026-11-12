package mcjty.lostcities.dimensions.world;

import mcjty.lostcities.api.IChunkPrimerFactory;
import net.minecraft.world.*;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.IChunkProvider;

/** The terrain factory and biome manager are always owned by the world being constructed. */
public final class LostWorldTypeAdapter extends LostWorldType {
    private final String target;
    public LostWorldTypeAdapter(String target){super("lc_"+target,false);this.target=target;}
    private WorldType target(){for(WorldType type:WorldType.worldTypes)if(type!=null&&target.equals(type.getWorldTypeName()))return type;throw new IllegalArgumentException("Missing adapted world type "+target);}
    @Override public WorldChunkManager getChunkManager(World world){
        WorldType previous=world.getWorldInfo().getTerrainType();
        try{WorldType type=target();world.getWorldInfo().setTerrainType(type);return LostWorldFilteredBiomeProvider.wrap(world,type.getChunkManager(world));}finally{world.getWorldInfo().setTerrainType(previous);}
    }
    @Override public IChunkProvider getChunkGenerator(World world,String options){
        WorldType previous=world.getWorldInfo().getTerrainType();IChunkProvider nativeGenerator;
        try{WorldType type=target();world.getWorldInfo().setTerrainType(type);nativeGenerator=type.getChunkGenerator(world,options);}finally{world.getWorldInfo().setTerrainType(previous);}
        if(!(nativeGenerator instanceof IChunkPrimerFactory))throw new IllegalArgumentException("Adapted world type "+target+" must implement the native Lost Cities IChunkPrimerFactory API");
        LostCityChunkGenerator generator=new LostCityChunkGenerator(world,world.getSeed());generator.otherGenerator=(IChunkPrimerFactory)nativeGenerator;return generator;
    }
}
