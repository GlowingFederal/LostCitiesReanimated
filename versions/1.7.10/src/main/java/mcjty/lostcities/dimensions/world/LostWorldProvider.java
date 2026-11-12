package mcjty.lostcities.dimensions.world;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.IChunkProvider;
public final class LostWorldProvider extends WorldProviderSurface {
    @Override protected void registerWorldChunkManager(){worldChunkMgr=new WorldChunkManager(worldObj);
        if(mcjty.lostcities.setup.ModSetup.biomesoplenty&&mcjty.lostcities.config.LostCityConfiguration.DIMENSION_BOP)for(net.minecraft.world.WorldType type:net.minecraft.world.WorldType.worldTypes)if(type!=null&&(type.getWorldTypeName().equalsIgnoreCase("BIOMESOP")||type.getWorldTypeName().equalsIgnoreCase("biomesoplenty"))){net.minecraft.world.WorldType previous=worldObj.getWorldInfo().getTerrainType();try{worldObj.getWorldInfo().setTerrainType(type);worldChunkMgr=type.getChunkManager(worldObj);}finally{worldObj.getWorldInfo().setTerrainType(previous);}break;}worldChunkMgr=LostWorldFilteredBiomeProvider.wrap(worldObj,worldChunkMgr);}
    @Override public IChunkProvider createChunkGenerator(){return new LostCityChunkGenerator(worldObj,worldObj.getSeed());}
    @Override public String getDimensionName(){return "Lost Cities";}
}
