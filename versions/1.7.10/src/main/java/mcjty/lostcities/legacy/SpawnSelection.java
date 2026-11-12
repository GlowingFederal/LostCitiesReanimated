package mcjty.lostcities.legacy;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.dimensions.world.*;
import mcjty.lostcities.dimensions.world.lost.*;
import mcjty.lostcities.dimensions.world.lost.cityassets.*;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.event.world.WorldEvent;
import java.util.Random;
import java.util.function.Predicate;

/** Native spawn ownership for profile constraints and landscapes without a vanilla surface. */
public final class SpawnSelection {
    @SubscribeEvent public void spawn(WorldEvent.CreateSpawnPosition event){
        World world=event.world;if(world.isRemote)return;
        LostCityChunkGenerator generator=WorldTypeTools.getLostCityChunkGenerator(world);if(generator==null)return;
        generator.contextual(()->{select(event,generator);return null;});
    }
    private void select(WorldEvent.CreateSpawnPosition event,LostCityChunkGenerator generator){
        World world=event.world;LostCityProfile profile=generator.getProfile();
        boolean required=profile.isFloating()||profile.isSpace();Predicate<BlockPos> suitable=pos->true;
        int centerX=0,centerZ=0;
        if(!profile.SPAWN_BIOME.isEmpty()){
            BiomeGenBase biome=LegacyRegistries.BIOMES.getValue(new net.minecraft.util.ResourceLocation(profile.SPAWN_BIOME));if(biome==null)throw new IllegalArgumentException("Unknown spawn biome "+profile.SPAWN_BIOME);
            suitable=pos->world.getBiomeGenForCoords(pos.getX(),pos.getZ())==biome;required=true;
        }else if(!profile.SPAWN_CITY.isEmpty()){
            PredefinedCity city=AssetRegistries.PREDEFINED_CITIES.get(profile.SPAWN_CITY);if(city==null||city.getDimension()!=world.provider.dimensionId)throw new IllegalArgumentException("Spawn city is unavailable in this dimension: "+profile.SPAWN_CITY);
            centerX=city.getChunkX()*16+8;centerZ=city.getChunkZ()*16+8;final int x=centerX,z=centerZ;final double radius=city.getRadius()*.8;
            suitable=pos->CitySphere.squaredDistance(x,z,pos.getX(),pos.getZ())<radius*radius;required=true;
        }else if(!profile.SPAWN_SPHERE.isEmpty()){
            if(profile.SPAWN_SPHERE.equals("<in>")||profile.SPAWN_SPHERE.equals("<out>")){
                boolean inside=profile.SPAWN_SPHERE.equals("<in>");suitable=pos->{CitySphere sphere=CitySphere.getCitySphere(pos.getX()>>4,pos.getZ()>>4,generator);double radius=sphere.getRadius()*(inside?.8:1);boolean hit=sphere.isEnabled()&&CitySphere.squaredDistance(sphere.getCenterPos().getX(),sphere.getCenterPos().getZ(),pos.getX(),pos.getZ())<radius*radius;return inside?hit:!hit;};
            }else{
                PredefinedSphere sphere=AssetRegistries.PREDEFINED_SPHERES.get(profile.SPAWN_SPHERE);if(sphere==null||sphere.getDimension()!=world.provider.dimensionId)throw new IllegalArgumentException("Spawn sphere is unavailable in this dimension: "+profile.SPAWN_SPHERE);
                centerX=sphere.getChunkX()*16+8;centerZ=sphere.getChunkZ()*16+8;final int x=centerX,z=centerZ;final double radius=sphere.getRadius()*.8;suitable=pos->CitySphere.squaredDistance(x,z,pos.getX(),pos.getZ())<radius*radius;
            }required=true;
        }
        if(profile.SPAWN_NOT_IN_BUILDING){suitable=suitable.and(pos->{BuildingInfo info=BuildingInfo.getBuildingInfo(pos.getX()>>4,pos.getZ()>>4,generator);return !info.isCity()||!info.hasBuilding;});required=true;}
        if(!required)return;
        Random random=new Random(world.getSeed());random.nextFloat();random.nextFloat();
        for(int attempt=0;attempt<10000;attempt++){
            int radius=200+(attempt/200)*100,x=centerX+random.nextInt(radius*2)-radius,z=centerZ+random.nextInt(radius*2)-radius;
            if(!suitable.test(new BlockPos(x,128,z)))continue;
            LostCityProfile local=BuildingInfo.getProfile(x>>4,z>>4,generator);int start=Math.max(1,Math.min(253,local.GROUNDLEVEL-5));
            for(int step=0;step<253;step++){int y=1+(start-1+step)%253;Block block=world.getBlock(x,y,z);if(block.getMaterial().isSolid()&&block.isOpaqueCube()&&block.renderAsNormalBlock()&&world.isAirBlock(x,y+1,z)&&world.isAirBlock(x,y+2,z)){world.setSpawnLocation(x,y+1,z);event.setCanceled(true);return;}}
        }
        throw new IllegalStateException("Cannot find a safe spawn for profile "+profile.getName()+" after 10000 candidates");
    }
}
