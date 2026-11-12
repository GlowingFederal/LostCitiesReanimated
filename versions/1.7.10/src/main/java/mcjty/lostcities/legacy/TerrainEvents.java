package mcjty.lostcities.legacy;
import cpw.mods.fml.common.eventhandler.*;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.dimensions.world.*;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;

public final class TerrainEvents {
    @SubscribeEvent public void decorate(DecorateBiomeEvent.Decorate event){
        if(event.world.isRemote)return;
        LostCityChunkGenerator generator=WorldTypeTools.getLostCityChunkGenerator(event.world);if(generator==null)return;
        if(!BuildingInfo.isCity(event.chunkX>>4,event.chunkZ>>4,generator))return;
        LostCityProfile p=BuildingInfo.getProfile(event.chunkX>>4,event.chunkZ>>4,generator);boolean deny=false;
        switch(event.type){
            case TREE:deny=p.AVOID_GENERATED_TREES||p.AVOID_FOLIAGE;break;
            case FLOWERS:deny=p.AVOID_GENERATED_FLOWERS;break;
            case GRASS:deny=p.AVOID_GENERATED_GRASS;break;
            case SHROOM:case BIG_SHROOM:deny=p.AVOID_GENERATED_MUSHROOMS;break;
            case REED:deny=p.AVOID_GENERATED_REEDS;break;
            case LILYPAD:deny=p.AVOID_GENERATED_LILYPADS;break;
            case PUMPKIN:deny=p.AVOID_GENERATED_PUMPKINS;break;
            case CACTUS:deny=p.AVOID_GENERATED_CACTII;break;
            case LAKE:deny=p.AVOID_GENERATED_LAKE_WATER;break;
            default:break;
        }
        if(deny)event.setResult(Event.Result.DENY);
    }
}
