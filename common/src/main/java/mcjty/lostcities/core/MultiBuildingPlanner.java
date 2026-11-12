package mcjty.lostcities.core;

import java.util.*;

/** Bounded region placement, ported from modern MultiChunk; no world access or chunk loading. */
public final class MultiBuildingPlanner {
    public interface Environment {
        String style(int x,int z);
        Template select(String style,int x,int z,Random random);
        boolean valid(int x,int z,Template template,String style);
    }
    public static final class Template {
        public final String name;
        public final int width,depth;
        public Template(String name,int width,int depth){this.name=name;this.width=width;this.depth=depth;if(width<1||depth<1||width>64||depth>64)throw new IllegalArgumentException("Invalid multibuilding footprint "+name);}
    }
    public static final class Placement {
        public final Template template;
        public final int rootX,rootZ;
        public Placement(Template template,int rootX,int rootZ){this.template=template;this.rootX=rootX;this.rootZ=rootZ;}
        public boolean contains(int x,int z){return x>=rootX&&z>=rootZ&&x<rootX+template.width&&z<rootZ+template.depth;}
        public int section(int x,int z){return (z-rootZ)*template.width+x-rootX;}
    }
    private static final class Choice {
        final Template template;final String style;
        Choice(Template template,String style){this.template=template;this.style=style;}
    }
    private final Environment environment;
    private final int areaSize,minimum,maximum,attempts;
    private final float correctStyleFactor;
    private final Map<Long,Placement[]> regions=new LinkedHashMap<Long,Placement[]>(64,.75F,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Long,Placement[]> entry){return size()>256;}
    };
    public MultiBuildingPlanner(Environment environment,int areaSize,int minimum,int maximum,int attempts,float correctStyleFactor){
        if(areaSize<2||areaSize>64||minimum<0||maximum<minimum||maximum>64||attempts<1||attempts>256||correctStyleFactor<0||correctStyleFactor>1)throw new IllegalArgumentException("Invalid multibuilding planning settings");
        this.environment=environment;this.areaSize=areaSize;this.minimum=minimum;this.maximum=maximum;this.attempts=attempts;this.correctStyleFactor=correctStyleFactor;
    }
    public Placement at(int x,int z){
        int rx=Math.floorDiv(x,areaSize),rz=Math.floorDiv(z,areaSize);long key=((long)rx<<32)^(rz&0xffffffffL);
        Placement[] region=regions.get(key);if(region==null){region=calculate(rx,rz);regions.put(key,region);}
        return region[Math.floorMod(z,areaSize)*areaSize+Math.floorMod(x,areaSize)];
    }
    private Placement[] calculate(int rx,int rz){
        Placement[] grid=new Placement[areaSize*areaSize];int rootX=rx*areaSize,rootZ=rz*areaSize;
        Random random=new Random(rx*797013493L+rz*295085213L);
        int count=minimum+random.nextInt(maximum-minimum+1);if(count==0)return grid;
        Map<String,Integer> styles=new TreeMap<>();
        for(int x=0;x<areaSize;x++)for(int z=0;z<areaSize;z++){String style=environment.style(rootX+x,rootZ+z);styles.put(style,styles.containsKey(style)?styles.get(style)+1:1);}
        List<Choice> choices=new ArrayList<>();
        for(int i=0;i<count;i++){
            int index=random.nextInt(areaSize*areaSize);String selected=null;
            for(Map.Entry<String,Integer> entry:styles.entrySet()){index-=entry.getValue();if(index<0){selected=entry.getKey();break;}}
            Template template=environment.select(selected,rootX,rootZ,random);if(template!=null&&template.width<=areaSize&&template.depth<=areaSize)choices.add(new Choice(template,selected));
        }
        choices.sort(Comparator.<Choice>comparingInt(choice->choice.template.width+choice.template.depth).reversed().thenComparing(choice->choice.template.name));
        for(Choice choice:choices)for(int attempt=0;attempt<attempts;attempt++){
            int x=random.nextInt(areaSize-choice.template.width+1),z=random.nextInt(areaSize-choice.template.depth+1),correct=0;boolean valid=true;
            for(int dx=0;valid&&dx<choice.template.width;dx++)for(int dz=0;valid&&dz<choice.template.depth;dz++){
                int cx=rootX+x+dx,cz=rootZ+z+dz;
                if(grid[(z+dz)*areaSize+x+dx]!=null||!environment.valid(cx,cz,choice.template,choice.style)){valid=false;break;}
                if(choice.style.equals(environment.style(cx,cz)))correct++;
            }
            if(!valid||correct<choice.template.width*choice.template.depth*correctStyleFactor)continue;
            Placement placement=new Placement(choice.template,rootX+x,rootZ+z);
            for(int dx=0;dx<choice.template.width;dx++)for(int dz=0;dz<choice.template.depth;dz++)grid[(z+dz)*areaSize+x+dx]=placement;
            break;
        }
        return grid;
    }
}
