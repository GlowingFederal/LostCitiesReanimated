package mcjty.lostcities.client;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.*;
import mcjty.lostcities.network.ProfileNetwork;
import mcjty.lostcities.setup.CommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

@SideOnly(Side.CLIENT)
public final class ClientProxy extends CommonProxy {
    private static final class Received {final ProfileNetwork.Reply reply;final Object connection;Received(ProfileNetwork.Reply reply,Object connection){this.reply=reply;this.connection=connection;}}
    private final ArrayBlockingQueue<Received> incoming=new ArrayBlockingQueue<>(32);
    private final Map<Integer,ProfileNetwork.Reply> profiles=new LinkedHashMap<Integer,ProfileNetwork.Reply>(16,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Integer,ProfileNetwork.Reply> entry){return size()>16;}};
    private final java.util.concurrent.atomic.AtomicBoolean reset=new java.util.concurrent.atomic.AtomicBoolean();
    private long requestAt;
    private int requestedDimension=Integer.MIN_VALUE;
    @Override public void init(){FMLCommonHandler.instance().bus().register(this);MinecraftForge.EVENT_BUS.register(this);}
    @Override public void receive(ProfileNetwork.Reply reply){receive(reply,null);}
    @Override public void receive(ProfileNetwork.Reply reply,Object connection){incoming.offer(new Received(reply,connection));}
    @Override public void clear(){reset.set(true);}
    @SubscribeEvent public void connected(FMLNetworkEvent.ClientConnectedToServerEvent event){reset.set(true);}
    @SubscribeEvent public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event){reset.set(true);}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        if(reset.getAndSet(false)){profiles.clear();incoming.clear();requestAt=0;requestedDimension=Integer.MIN_VALUE;}
        World world=Minecraft.getMinecraft().theWorld;
        Received received;while((received=incoming.poll())!=null){ProfileNetwork.Reply reply=received.reply;if(world!=null&&reply.dimension==world.provider.dimensionId&&(received.connection==null||received.connection==Minecraft.getMinecraft().getNetHandler()))profiles.put(reply.dimension,reply);}
    }
    private ProfileNetwork.Reply profile(World world){
        if(!world.isRemote)return null;
        ProfileNetwork.Reply reply=profiles.get(world.provider.dimensionId);
        if(reply==null&&Minecraft.getMinecraft().getNetHandler()!=null){long now=System.currentTimeMillis();if(requestedDimension!=world.provider.dimensionId||now-requestAt>=2000){requestedDimension=world.provider.dimensionId;requestAt=now;ProfileNetwork.channel.sendToServer(new ProfileNetwork.Request(world.provider.dimensionId));}}
        return reply;
    }
    @Override public float horizon(World world){if(!world.isRemote)return super.horizon(world);ProfileNetwork.Reply profile=profile(world);return profile!=null&&profile.city?profile.horizon:-1;}
    @Override public int ground(World world){if(!world.isRemote)return super.ground(world);ProfileNetwork.Reply profile=profile(world);return profile!=null&&profile.city?profile.ground:64;}
    @SubscribeEvent public void fog(EntityViewRenderEvent.FogColors event){ProfileNetwork.Reply profile=profile(event.entity.worldObj);if(profile!=null&&profile.city){if(profile.red>=0)event.red=profile.red;if(profile.green>=0)event.green=profile.green;if(profile.blue>=0)event.blue=profile.blue;}}
    @SubscribeEvent public void density(EntityViewRenderEvent.FogDensity event){ProfileNetwork.Reply profile=profile(event.entity.worldObj);if(profile!=null&&profile.city&&profile.density>=0){event.density=profile.density;event.setCanceled(true);}}
}
