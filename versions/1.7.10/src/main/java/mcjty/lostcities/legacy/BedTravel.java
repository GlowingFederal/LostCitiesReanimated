package mcjty.lostcities.legacy;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcjty.lostcities.config.LostCityConfiguration;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;

/** Native dimension transfer using the legacy diamond-block bed and six-skull recipe. */
public final class BedTravel {
    private static final int[][] DIRECTIONS={{0,1},{-1,0},{0,-1},{1,0}};
    public static boolean valid(World world,int x,int y,int z){
        if(world.getBlock(x,y,z)!=Blocks.bed)return false;
        int metadata=world.getBlockMetadata(x,y,z),direction=metadata&3,dx=DIRECTIONS[direction][0],dz=DIRECTIONS[direction][1];
        if((metadata&8)==0){x+=dx;z+=dz;}
        Block foundation=LegacyRegistries.BLOCKS.getValue(new net.minecraft.util.ResourceLocation(LostCityConfiguration.SPECIAL_BED_BLOCK));
        if(foundation==null||world.getBlock(x,y-1,z)!=foundation||world.getBlock(x-dx,y-1,z-dz)!=foundation)return false;
        return world.getBlock(x+dx,y,z+dz)==Blocks.skull&&world.getBlock(x-dz,y,z+dx)==Blocks.skull&&world.getBlock(x+dz,y,z-dx)==Blocks.skull
                &&world.getBlock(x-2*dx,y,z-2*dz)==Blocks.skull&&world.getBlock(x-dx-dz,y,z-dz+dx)==Blocks.skull&&world.getBlock(x-dx+dz,y,z-dz-dx)==Blocks.skull;
    }
    @SubscribeEvent public void sleep(PlayerSleepInBedEvent event){
        if(!(event.entityPlayer instanceof EntityPlayerMP)||event.entityPlayer.worldObj.isRemote||LostCityConfiguration.DIMENSION_ID==-1)return;
        EntityPlayerMP player=(EntityPlayerMP)event.entityPlayer;if(!valid(player.worldObj,event.x,event.y,event.z))return;
        int dimension=player.dimension==LostCityConfiguration.DIMENSION_ID?0:LostCityConfiguration.DIMENSION_ID;
        WorldServer destination=DimensionManager.getWorld(dimension);if(destination==null){DimensionManager.initDimension(dimension);destination=DimensionManager.getWorld(dimension);}
        if(destination==null)throw new IllegalStateException("Lost Cities destination dimension is unavailable: "+dimension);
        BlockPos position=find(destination,event.x,event.y,event.z);final double tx=position.getX()+.5,tz=position.getZ()+.5;final int ty=position.getY();
        event.result=EntityPlayer.EnumStatus.OTHER_PROBLEM;
        final WorldServer target=destination;
        MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(player,dimension,new Teleporter(target){
            @Override public void placeInPortal(Entity entity,double x,double y,double z,float yaw){entity.setPosition(tx,ty,tz);entity.motionX=entity.motionY=entity.motionZ=0;}
        });
        player.setPositionAndUpdate(tx,ty,tz);
    }
    private static BlockPos find(WorldServer world,int x,int y,int z){
        int cx=x>>4,cz=z>>4;
        // Prefer a matching destination bed, then safe standing space in the same chunk.
        int inspected=0;
        search:for(int radius=0;radius<50;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(radius>0&&Math.abs(dx)!=radius&&Math.abs(dz)!=radius)continue;
            if(++inspected>64)break search;
            int chunkX=cx+dx,chunkZ=cz+dz;world.getChunkFromChunkCoords(chunkX,chunkZ);BlockPos safe=null;
            for(int delta=0;delta<255;delta++)for(int sign:new int[]{1,-1}){
                int height=y+delta*sign;if(height<1||height>252)continue;
                for(int bx=0;bx<16;bx++)for(int bz=0;bz<16;bz++){
                    int wx=chunkX*16+bx,wz=chunkZ*16+bz;
                    if(valid(world,wx,height,wz))return new BlockPos(wx,height+1,wz);
                    if(safe==null&&world.getBlock(wx,height,wz).getMaterial().isSolid()&&world.isAirBlock(wx,height+1,wz)&&world.isAirBlock(wx,height+2,wz))safe=new BlockPos(wx,height+1,wz);
                }
            }
            if(safe!=null)return safe;
        }
        int height=Math.min(250,Math.max(2,world.getTopSolidOrLiquidBlock(x,z)));
        world.setBlock(x,height-1,z,Blocks.cobblestone,0,2);world.setBlockToAir(x,height,z);world.setBlockToAir(x,height+1,z);return new BlockPos(x,height,z);
    }
}
