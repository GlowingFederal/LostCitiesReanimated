package mcjty.lostcities.dimensions.world;
import net.minecraft.world.*;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.IChunkProvider;

public class LostWorldType extends WorldType {
    private final boolean bop;
    public LostWorldType(String name,boolean bop){super(name);this.bop=bop;}
    @Override public boolean isCustomizable(){return true;}
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public void onCustomizeButton(net.minecraft.client.Minecraft minecraft,net.minecraft.client.gui.GuiCreateWorld parent){minecraft.displayGuiScreen(new mcjty.lostcities.client.CityProfileScreen(parent));}
    @Override public IChunkProvider getChunkGenerator(World world,String options){return new LostCityChunkGenerator(world,world.getSeed());}
    @Override public WorldChunkManager getChunkManager(World world){
        if(bop)for(WorldType type:WorldType.worldTypes)if(type!=null&&(type.getWorldTypeName().equalsIgnoreCase("BIOMESOP")||type.getWorldTypeName().equalsIgnoreCase("biomesoplenty"))){
            WorldType original=world.getWorldInfo().getTerrainType();
            try{world.getWorldInfo().setTerrainType(type);return LostWorldFilteredBiomeProvider.wrap(world,type.getChunkManager(world));}finally{world.getWorldInfo().setTerrainType(original);}
        }
        return LostWorldFilteredBiomeProvider.wrap(world,new WorldChunkManager(world));
    }
    @Override public double getHorizon(World world){double horizon=mcjty.lostcities.LostCities.proxy.horizon(world);return horizon<0?super.getHorizon(world):horizon;}
    @Override public int getMinimumSpawnHeight(World world){return mcjty.lostcities.LostCities.proxy.ground(world);}
}
