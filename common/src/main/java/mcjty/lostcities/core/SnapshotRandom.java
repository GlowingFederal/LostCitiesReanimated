package mcjty.lostcities.core;

import java.util.Random;

/** Java Random's exact LCG, with an explicit snapshot for nested terrain sampling. Server-thread owned. */
public final class SnapshotRandom extends Random {
    private long state;
    private boolean gaussianReady;
    private double gaussian;
    public SnapshotRandom(long seed){super(seed);}
    @Override public synchronized void setSeed(long seed){state=(seed^0x5DEECE66DL)&((1L<<48)-1);gaussianReady=false;}
    @Override protected synchronized int next(int bits){state=(state*0x5DEECE66DL+0xBL)&((1L<<48)-1);return (int)(state>>>(48-bits));}
    public static final class Snapshot {
        final long state;final boolean ready;final double gaussian;
        Snapshot(long state,boolean ready,double gaussian){this.state=state;this.ready=ready;this.gaussian=gaussian;}
    }
    public Snapshot snapshot(){return new Snapshot(state,gaussianReady,gaussian);}
    public void restore(Snapshot snapshot){state=snapshot.state;gaussianReady=snapshot.ready;gaussian=snapshot.gaussian;}
    @Override public synchronized double nextGaussian(){
        if(gaussianReady){gaussianReady=false;return gaussian;}
        double x,y,s;do{x=2*nextDouble()-1;y=2*nextDouble()-1;s=x*x+y*y;}while(s>=1||s==0);
        double multiplier=StrictMath.sqrt(-2*StrictMath.log(s)/s);gaussian=y*multiplier;gaussianReady=true;return x*multiplier;
    }
}
