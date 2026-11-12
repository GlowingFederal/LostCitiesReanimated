package mcjty.lostcities;

import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.dimensions.world.LostCityChunkGenerator;
import mcjty.lostcities.dimensions.world.WorldTypeTools;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.CitySphere;
import mcjty.lostcities.dimensions.world.lost.cityassets.AssetRegistries;
import mcjty.lostcities.dimensions.world.lost.cityassets.PredefinedCity;
import mcjty.lostcities.dimensions.world.lost.cityassets.PredefinedSphere;
import mcjty.lostcities.varia.CustomTeleporter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.Random;
import java.util.function.Predicate;

public class ForgeEventHandlers {

    @SubscribeEvent
    public void onCreateSpawnPoint(WorldEvent.CreateSpawnPosition event) {
        if(event.getWorld().isRemote)return;
        LostCityChunkGenerator generator=WorldTypeTools.getLostCityChunkGenerator(event.getWorld());
        if(generator!=null)generator.contextual(()->{selectSpawnPoint(event);return null;});
    }

    private void selectSpawnPoint(WorldEvent.CreateSpawnPosition event) {
        World world = event.getWorld();
        if (!world.isRemote) {
            if (!WorldTypeTools.isLostCities(world)) {
                return;
            }

            LostCityProfile profile = WorldTypeTools.getProfile(world);
            if (profile == null) {
                return;
            }

            LostCityChunkGenerator provider = WorldTypeTools.getLostCityChunkGenerator(world);

            Predicate<BlockPos> isSuitable = pos -> true;
            boolean needsCheck = false;

            if (!profile.SPAWN_BIOME.isEmpty()) {
                final Biome spawnBiome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(profile.SPAWN_BIOME));
                if (spawnBiome == null) {
                    LostCities.setup.getLogger().error("Cannot find biome '" + profile.SPAWN_BIOME + "' for the player to spawn in !");
                } else {
                    isSuitable = blockPos -> world.getBiome(blockPos) == spawnBiome;
                    needsCheck = true;
                }
            } else if (!profile.SPAWN_CITY.isEmpty()) {
                final PredefinedCity city = AssetRegistries.PREDEFINED_CITIES.get(profile.SPAWN_CITY);
                if (city == null) {
                    LostCities.setup.getLogger().error("Cannot find city '" + profile.SPAWN_CITY + "' for the player to spawn in !");
                } else {
                    float sqradius = getSqRadius(city.getRadius(), 0.8f);
                    isSuitable = blockPos -> city.getDimension() == world.provider.getDimension() &&
                            CitySphere.squaredDistance(city.getChunkX()*16+8, city.getChunkZ()*16+8, blockPos.getX(), blockPos.getZ()) < sqradius;
                    needsCheck = true;
                }
            } else if (!profile.SPAWN_SPHERE.isEmpty()) {
                if ("<in>".equals(profile.SPAWN_SPHERE)) {
                    isSuitable = blockPos -> {
                        CitySphere sphere = CitySphere.getCitySphere(blockPos.getX() >> 4, blockPos.getZ() >> 4, provider);
                        if (!sphere.isEnabled()) {
                            return false;
                        }
                        float sqradius = getSqRadius((int) sphere.getRadius(), 0.8f);
                        return CitySphere.squaredDistance(sphere.getCenterPos().getX(),sphere.getCenterPos().getZ(),blockPos.getX(),blockPos.getZ()) < sqradius;
                    };
                    needsCheck = true;
                } else if ("<out>".equals(profile.SPAWN_SPHERE)) {
                    isSuitable = blockPos -> {
                        CitySphere sphere = CitySphere.getCitySphere(blockPos.getX() >> 4, blockPos.getZ() >> 4, provider);
                        if (!sphere.isEnabled()) {
                            return true;
                        }
                        float sqradius = sphere.getRadius() * sphere.getRadius();
                        return sphere.getCenterPos().distanceSq(new BlockPos(blockPos.getX(), sphere.getCenterPos().getY(), blockPos.getZ())) > sqradius;
                    };
                    needsCheck = true;
                } else {
                    final PredefinedSphere sphere = AssetRegistries.PREDEFINED_SPHERES.get(profile.SPAWN_SPHERE);
                    if (sphere == null) {
                        LostCities.setup.getLogger().error("Cannot find sphere '" + profile.SPAWN_SPHERE + "' for the player to spawn in !");
                    } else {
                        float sqradius = getSqRadius(sphere.getRadius(), 0.8f);
                        isSuitable = blockPos -> sphere.getDimension() == world.provider.getDimension() &&
                                CitySphere.squaredDistance(sphere.getChunkX() * 16 + 8, sphere.getChunkZ() * 16 + 8, blockPos.getX(), blockPos.getZ()) < sqradius;
                        needsCheck = true;
                    }
                }
            }

            if (profile.SPAWN_NOT_IN_BUILDING) {
                isSuitable = isSuitable.and(blockPos -> isOutsideBuilding(provider, blockPos));
                needsCheck = true;
            }

            // Potentially set the spawn point
            switch (profile.LANDSCAPE_TYPE) {
                case DEFAULT:
                case SPHERES:
                case CAVERN:
                case CAVERNSPHERES:
                    if (needsCheck) {
                        findSafeSpawnPoint(world, provider, isSuitable);
                        event.setCanceled(true);
                    }
                    break;
                case FLOATING:
                case SPACE:
                    findSafeSpawnPoint(world, provider, isSuitable);
                    event.setCanceled(true);
                    break;
            }
        }
    }

    private boolean isOutsideBuilding(LostCityChunkGenerator provider, BlockPos pos) {
        BuildingInfo info = BuildingInfo.getBuildingInfo(pos.getX() >> 4, pos.getZ() >> 4, provider);
        return !(info.isCity() && info.hasBuilding);
    }

    private int getSqRadius(int radius, float pct) {
        return (int) ((radius * pct) * (radius * pct));
    }

    private void findSafeSpawnPoint(World world, LostCityChunkGenerator provider, @Nonnull Predicate<BlockPos> isSuitable) {
        Random rand = new Random(world.getSeed());
        rand.nextFloat();
        rand.nextFloat();
        int radius = 200;
        int attempts = 0;
        int centerX=0,centerZ=0;
        LostCityProfile selected=provider.getProfile();
        if(selected.SPAWN_BIOME.isEmpty()){
            if(!selected.SPAWN_CITY.isEmpty()){PredefinedCity city=AssetRegistries.PREDEFINED_CITIES.get(selected.SPAWN_CITY);if(city!=null){centerX=city.getChunkX()*16+8;centerZ=city.getChunkZ()*16+8;}}
            else if(!selected.SPAWN_SPHERE.isEmpty()&&!selected.SPAWN_SPHERE.startsWith("<")){PredefinedSphere sphere=AssetRegistries.PREDEFINED_SPHERES.get(selected.SPAWN_SPHERE);if(sphere!=null){centerX=sphere.getChunkX()*16+8;centerZ=sphere.getChunkZ()*16+8;}}
        }
//        int bottom = world.getWorldType().getMinimumSpawnHeight(world);
        while (true) {
            for (int i = 0 ; i < 200 ; i++) {
                int x = centerX+rand.nextInt(radius * 2) - radius;
                int z = centerZ+rand.nextInt(radius * 2) - radius;
                attempts++;

                if (!isSuitable.test(new BlockPos(x, 128, z))) {
                    continue;
                }

                LostCityProfile profile = BuildingInfo.getProfile(x >> 4, z >> 4, provider);

                int first=Math.max(1,Math.min(253,profile.GROUNDLEVEL-5));
                for (int step=0;step<253;step++) {
                    int y=1+(first-1+step)%253;
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isValidStandingPosition(world, pos)) {
                        world.setSpawnPoint(pos.up());
                        return;
                    }
                }
            }
            radius += 100;
            if (attempts > 10000) {
                LostCities.setup.getLogger().error("Can't find a valid spawn position!");
                throw new RuntimeException("Can't find a valid spawn position!");
            }
        }
    }

    private boolean isValidStandingPosition(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().isTopSolid(state) && state.getBlock().isFullCube(state) && state.getBlock().isOpaqueCube(state) && world.isAirBlock(pos.up()) && world.isAirBlock(pos.up(2));
    }

    private boolean isValidSpawnBed(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockBed)) {
            return false;
        }
        EnumFacing direction = Blocks.BED.getBedDirection(state, world, pos);
        Block b1 = world.getBlockState(pos.down()).getBlock();
        Block b2 = world.getBlockState(pos.offset(direction.getOpposite()).down()).getBlock();
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(LostCityConfiguration.SPECIAL_BED_BLOCK));
        if (b1 != b || b2 != b) {
            return false;
        }
        // Check if the bed is surrounded by 6 skulls
        if (world.getBlockState(pos.offset(direction)).getBlock() != Blocks.SKULL) {
            return false;
        }
        if (world.getBlockState(pos.offset(direction.rotateY())).getBlock() != Blocks.SKULL) {
            return false;
        }
        if (world.getBlockState(pos.offset(direction.rotateYCCW())).getBlock() != Blocks.SKULL) {
            return false;
        }
        if (world.getBlockState(pos.offset(direction.getOpposite(), 2)).getBlock() != Blocks.SKULL) {
            return false;
        }
        if (world.getBlockState(pos.offset(direction.getOpposite()).offset(direction.getOpposite().rotateY())).getBlock() != Blocks.SKULL) {
            return false;
        }
        if (world.getBlockState(pos.offset(direction.getOpposite()).offset(direction.getOpposite().rotateYCCW())).getBlock() != Blocks.SKULL) {
            return false;
        }
        return true;
    }

    private BlockPos findValidTeleportLocation(World world, BlockPos start) {
        int cx=start.getX()>>4,cz=start.getZ()>>4,inspected=0;
        for(int radius=0;radius<50;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(radius>0&&Math.abs(dx)!=radius&&Math.abs(dz)!=radius)continue;
            if(++inspected>64)return null;
            BlockPos found=findValidTeleportLocation(world,cx+dx,cz+dz,start.getY());
            if(found!=null)return found;
        }
        return null;
    }

    private BlockPos findValidTeleportLocation(World world, int chunkX, int chunkZ, int y) {
        BlockPos bestSpot = null;
        for (int dy = 0 ; dy < 255 ; dy++) {
            for (int x = 0 ; x < 16 ; x++) {
                for (int z = 0 ; z < 16 ; z++) {
                    if ((y + dy) < 250) {
                        BlockPos p = new BlockPos(chunkX * 16 + x, y + dy, chunkZ * 16 + z);
                        if (isValidSpawnBed(world, p)) {
                            return p.up();
                        }
                        if (bestSpot == null && isValidStandingPosition(world, p)) {
                            bestSpot = p.up();
                        }
                    }
                    if ((y - dy) > 1) {
                        BlockPos p = new BlockPos(chunkX * 16 + x, y - dy, chunkZ * 16 + z);
                        if (isValidSpawnBed(world, p)) {
                            return p.up();
                        }
                        if (bestSpot == null && isValidStandingPosition(world, p)) {
                            bestSpot = p.up();
                        }
                    }
                }
            }
        }
        return bestSpot;
    }

    @SubscribeEvent
    public void onPlayerSleepInBedEvent(PlayerSleepInBedEvent event) {
        if (LostCityConfiguration.DIMENSION_ID == -1) {
            return;
        }

        World world = event.getEntityPlayer().getEntityWorld();
        if (world.isRemote) {
            return;
        }
        BlockPos bedLocation = event.getPos();
        if (!isValidSpawnBed(world, bedLocation)) {
            return;
        }

        if (world.provider.getDimension() == LostCityConfiguration.DIMENSION_ID) {
            event.setResult(Event.Result.DENY);
            WorldServer destWorld = DimensionManager.getWorld(0);
            BlockPos location = findLocation(bedLocation, destWorld);
            CustomTeleporter.teleportToDimension(event.getEntityPlayer(), 0, location);
        } else {
            event.setResult(Event.Result.DENY);
            WorldServer destWorld = event.getEntity().getEntityWorld().getMinecraftServer().getWorld(LostCityConfiguration.DIMENSION_ID);
            BlockPos location = findLocation(bedLocation, destWorld);
            CustomTeleporter.teleportToDimension(event.getEntityPlayer(), LostCityConfiguration.DIMENSION_ID, location);
        }
    }

    private BlockPos findLocation(BlockPos bedLocation, WorldServer destWorld) {
        BlockPos top = destWorld.getTopSolidOrLiquidBlock(bedLocation);
        BlockPos location = findValidTeleportLocation(destWorld, top);
        if (location == null) {
            int height=Math.max(2,Math.min(252,top.getY()>1?top.getY():bedLocation.getY()));
            location=new BlockPos(bedLocation.getX(),height,bedLocation.getZ());
            destWorld.setBlockState(location.down(),Blocks.COBBLESTONE.getDefaultState(),2);
            destWorld.setBlockToAir(location);destWorld.setBlockToAir(location.up());
        }
        return location;
    }
}
