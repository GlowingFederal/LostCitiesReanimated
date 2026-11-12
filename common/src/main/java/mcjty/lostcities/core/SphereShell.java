package mcjty.lostcities.core;

/** Modern two-block shell and optional clearing, bounded to a legacy chunk's 0–255 height. */
public final class SphereShell {
    public interface Blocks { char get(int x,int y,int z);void set(int x,int y,int z,char block); }
    public static void generate(Blocks blocks,int cx,int cy,int cz,int radius,char glass,char side,char air,char liquid,int seaLevel,int outsideSeaLevel,int clearAbove,boolean aboveUntilAir,int clearBelow,boolean belowUntilAir,boolean clearRim){
        double squared=(double)radius*radius,inner=(double)(radius-2)*(radius-2),outer=(double)(radius+2)*(radius+2);
        int min=Math.max(0,cy-radius-1),max=Math.min(256,cy+radius+1);
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            double horizontal=(double)(x-cx)*(x-cx)+(double)(z-cz)*(z-cz);int bottom=-1,top=-1;
            if(horizontal<=squared){
                for(int y=min;y<max;y++){double distance=horizontal+(double)(y-cy)*(y-cy);if(distance<=squared&&distance>=inner){blocks.set(x,y,z,y>cy?glass:side);if(bottom<0)bottom=y-1;}if(y>cy&&distance>squared){top=y;break;}}
                if(top>=0){int y=top;for(int end=Math.min(255,top+clearAbove);clearAbove>0&&y<=end;y++)blocks.set(x,y,z,y<=outsideSeaLevel?liquid:air);if(aboveUntilAir)while(y<256&&blocks.get(x,y,z)!=air){blocks.set(x,y,z,y<=outsideSeaLevel?liquid:air);y++;}}
                if(bottom>=0){int y=bottom;for(int end=Math.max(0,bottom-clearBelow);clearBelow>0&&y>=end;y--)blocks.set(x,y,z,y<=outsideSeaLevel?liquid:air);if(belowUntilAir)while(y>=0&&blocks.get(x,y,z)!=(y<=seaLevel?liquid:air)){blocks.set(x,y,z,y<=outsideSeaLevel?liquid:air);y--;}}
            }else if(clearRim&&horizontal<=outer)for(int y=min;y<max;y++)blocks.set(x,y,z,y<=outsideSeaLevel?liquid:air);
        }
    }
    private SphereShell(){}
}
