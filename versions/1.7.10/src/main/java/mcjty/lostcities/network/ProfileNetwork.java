package mcjty.lostcities.network;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.*;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import mcjty.lostcities.LostCities;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.dimensions.world.*;
import net.minecraft.entity.player.EntityPlayerMP;
import java.util.concurrent.ArrayBlockingQueue;

/** Network threads only enqueue; profile/world reads and client publication use their owning ticks. */
public final class ProfileNetwork {
    public static SimpleNetworkWrapper channel;
    private static final ArrayBlockingQueue<Pending> requests=new ArrayBlockingQueue<>(256);
    private static final class Pending {final EntityPlayerMP player;final int dimension;Pending(EntityPlayerMP player,int dimension){this.player=player;this.dimension=dimension;}}
    public static void init(){
        channel=NetworkRegistry.INSTANCE.newSimpleChannel("lostcities");
        channel.registerMessage(RequestHandler.class,Request.class,0,Side.SERVER);
        channel.registerMessage(ReplyHandler.class,Reply.class,1,Side.CLIENT);
        FMLCommonHandler.instance().bus().register(new ProfileNetwork());
    }
    public static void clear(){requests.clear();LostCities.proxy.clear();}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(int count=0;count<16;count++){
            Pending pending=requests.poll();if(pending==null)break;EntityPlayerMP player=pending.player;
            if(player.playerNetServerHandler==null||!player.playerNetServerHandler.netManager.isChannelOpen()||player.worldObj==null||player.dimension!=pending.dimension)continue;
            LostCityChunkGenerator generator=WorldTypeTools.getLostCityChunkGenerator(player.worldObj);
            channel.sendTo(new Reply(player.dimension,generator==null?null:generator.getProfile()),player);
        }
    }
    public static final class Request implements IMessage {
        private int dimension;
        public Request(){}public Request(int dimension){this.dimension=dimension;}
        @Override public void fromBytes(ByteBuf buffer){dimension=buffer.readInt();}
        @Override public void toBytes(ByteBuf buffer){buffer.writeInt(dimension);}
    }
    public static final class RequestHandler implements IMessageHandler<Request,IMessage> {
        @Override public IMessage onMessage(Request message,MessageContext context){requests.offer(new Pending(context.getServerHandler().playerEntity,message.dimension));return null;}
    }
    public static final class Reply implements IMessage {
        public int dimension,ground=64;
        public boolean city;
        public float horizon=-1,red=-1,green=-1,blue=-1,density=-1;
        public Reply(){}
        public Reply(int dimension,LostCityProfile profile){this.dimension=dimension;if(profile!=null){city=true;ground=profile.GROUNDLEVEL;horizon=profile.HORIZON;red=profile.FOG_RED;green=profile.FOG_GREEN;blue=profile.FOG_BLUE;density=profile.FOG_DENSITY;}}
        @Override public void fromBytes(ByteBuf buffer){dimension=buffer.readInt();city=buffer.readBoolean();ground=buffer.readInt();horizon=buffer.readFloat();red=buffer.readFloat();green=buffer.readFloat();blue=buffer.readFloat();density=buffer.readFloat();if(ground<0||ground>255||!Float.isFinite(horizon)||horizon< -1||horizon>256||!valid(red)||!valid(green)||!valid(blue)||!valid(density))throw new IllegalArgumentException("Invalid Lost Cities presentation profile");}
        private static boolean valid(float value){return Float.isFinite(value)&&value>=-1&&value<=1;}
        @Override public void toBytes(ByteBuf buffer){buffer.writeInt(dimension);buffer.writeBoolean(city);buffer.writeInt(ground);buffer.writeFloat(horizon);buffer.writeFloat(red);buffer.writeFloat(green);buffer.writeFloat(blue);buffer.writeFloat(density);}
    }
    public static final class ReplyHandler implements IMessageHandler<Reply,IMessage> {
        @Override public IMessage onMessage(Reply message,MessageContext context){LostCities.proxy.receive(message,context.netHandler);return null;}
    }
}
