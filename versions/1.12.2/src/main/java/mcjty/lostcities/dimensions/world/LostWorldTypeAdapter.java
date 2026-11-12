package mcjty.lostcities.dimensions.world;

import mcjty.lostcities.api.IChunkPrimerFactory;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.IChunkGenerator;

public class LostWorldTypeAdapter extends LostWorldType {

    public LostWorldTypeAdapter(String other) {
        super("lc_" + other);
        this.otherWorldtype = other;
    }

    private final String otherWorldtype;
    private WorldType target(){for(WorldType type:WorldType.WORLD_TYPES)if(type!=null&&otherWorldtype.equals(type.getName()))return type;throw new IllegalArgumentException("Missing adapted world type "+otherWorldtype);}
    @Override public IChunkGenerator getChunkGenerator(World world,String options){
        WorldType previous=world.getWorldInfo().getTerrainType();IChunkGenerator nativeGenerator;
        try{WorldType type=target();world.getWorldInfo().setTerrainType(type);nativeGenerator=type.getChunkGenerator(world,options);}finally{world.getWorldInfo().setTerrainType(previous);}
        if(!(nativeGenerator instanceof IChunkPrimerFactory))throw new IllegalArgumentException("Adapted world type "+otherWorldtype+" must implement IChunkPrimerFactory");
        return new LostCityChunkGenerator(world,(IChunkPrimerFactory)nativeGenerator);
    }
    @Override protected BiomeProvider getInternalBiomeProvider(World world){
        WorldType previous=world.getWorldInfo().getTerrainType();
        try{WorldType type=target();world.getWorldInfo().setTerrainType(type);return type.getBiomeProvider(world);}finally{world.getWorldInfo().setTerrainType(previous);}
    }
}
