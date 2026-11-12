package mcjty.lostcities.setup;

import mcjty.lostcities.dimensions.world.WorldTypeTools;
import mcjty.lostcities.network.ProfileNetwork;
import net.minecraft.world.World;

/** Dedicated-server boundary: no client classes or render state. */
public class CommonProxy {
    public void init(){}
    public void receive(ProfileNetwork.Reply reply){}
    public void receive(ProfileNetwork.Reply reply,Object connection){receive(reply);}
    public void clear(){}
    public float horizon(World world){return WorldTypeTools.getProfile(world).HORIZON;}
    public int ground(World world){return WorldTypeTools.getProfile(world).GROUNDLEVEL;}
}
