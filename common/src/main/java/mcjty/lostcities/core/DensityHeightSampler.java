package mcjty.lostcities.core;

/** Samples the native 5x33x5 density lattice with the same arithmetic order as full primer expansion. */
public final class DensityHeightSampler {
    private static final int[][] POINTS={{2,2},{13,2},{2,13},{13,13},{8,8}};
    public static int[] heights(double[] density,int waterLevel){
        int[] result=new int[POINTS.length];
        for(int point=0;point<POINTS.length;point++){
            int x=POINTS[point][0],z=POINTS[point][1],cx=x/4,cz=z/4,px=x&3,pz=z&3;
            int a=(cx*5+cz)*33,b=a+33,c=a+165,d=c+33,height=Math.max(0,Math.min(255,waterLevel-1));
            for(int section=0;section<32;section++){
                double d1=density[a+section],d2=density[b+section],d3=density[c+section],d4=density[d+section];
                double dy1=(density[a+section+1]-d1)*.125D,dy2=(density[b+section+1]-d2)*.125D,dy3=(density[c+section+1]-d3)*.125D,dy4=(density[d+section+1]-d4)*.125D;
                for(int y=0;y<8;y++){
                    double left=d1,right=d2,dx1=(d3-d1)*.25D,dx2=(d4-d2)*.25D;
                    for(int ix=0;ix<px;ix++){left+=dx1;right+=dx2;}
                    double dz=(right-left)*.25D,value=left-dz;
                    for(int iz=0;iz<=pz;iz++)value+=dz;
                    if(value>0)height=Math.max(height,section*8+y);
                    d1+=dy1;d2+=dy2;d3+=dy3;d4+=dy4;
                }
            }
            result[point]=height;
        }
        return result;
    }
    public static int average(int[] heights){int count=0,sum=0;for(int height:heights)if(height>5){sum+=height;count++;}return count==0?0:sum/count;}
    private DensityHeightSampler(){}
}
