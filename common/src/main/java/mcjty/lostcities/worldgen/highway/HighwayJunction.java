package mcjty.lostcities.worldgen.highway;

/** Asset selection/orientation from actual routed exits, including bends and T junctions. */
public final class HighwayJunction {
    public final String suffix;
    public final int quarterTurns;
    private HighwayJunction(String suffix,int quarterTurns){this.suffix=suffix;this.quarterTurns=quarterTurns;}
    public static HighwayJunction at(HighwayInfo info,int x,int z,int level){
        int exits=0;
        for(HighwayInfo.RouteHit hit:info.routeHits())if(hit.route().highwayLevel()==level)for(HighwaySegment segment:hit.route().segments())if(segment.contains(x,z)){
            if(segment.axis()==HighwayAxis.X){if(x>Math.min(segment.startX(),segment.endX()))exits|=8;if(x<Math.max(segment.startX(),segment.endX()))exits|=2;}
            else {if(z>Math.min(segment.startZ(),segment.endZ()))exits|=1;if(z<Math.max(segment.startZ(),segment.endZ()))exits|=4;}
        }
        if(Integer.bitCount(exits)==2&&exits!=5&&exits!=10)return new HighwayJunction("_bend",exits==12?0:exits==9?1:exits==3?2:3);
        if(Integer.bitCount(exits)==3)return new HighwayJunction("_t",(exits&1)==0?0:(exits&2)==0?1:(exits&4)==0?2:3);
        return new HighwayJunction("_bi",0);
    }
}
