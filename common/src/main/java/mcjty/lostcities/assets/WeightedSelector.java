package mcjty.lostcities.assets;

import com.google.gson.JsonObject;
import java.util.*;

/** Modern weighted selectors, including spawn-distance feathering without squared-integer overflow. */
public final class WeightedSelector {
    public static String select(List<JsonObject> entries,Random random,int x,int z){
        double distance=Math.hypot(x*16D,z*16D),total=0;double[] weights=new double[entries.size()];
        for(int i=0;i<entries.size();i++){
            JsonObject entry=entries.get(i);double factor=number(entry,"factor",1),minimum=number(entry,"minSpawnDistance",0),maximum=number(entry,"maxSpawnDistance",Integer.MAX_VALUE),feather=number(entry,"feather",0);
            if(factor<0||maximum<minimum||feather<0)throw new IllegalArgumentException("Invalid weighted selector "+entry);
            if(distance<minimum)factor*=feather==0?0:Math.max(0,1-(minimum-distance)/feather);
            else if(distance>maximum)factor*=feather==0?0:Math.max(0,1-(distance-maximum)/feather);
            total+=weights[i]=factor;
        }
        if(total<=0)return null;double value=random.nextFloat()*total;
        for(int i=0;i<entries.size();i++){value-=weights[i];if(value<0)return entries.get(i).get("value").getAsString();}
        return entries.get(entries.size()-1).get("value").getAsString();
    }
    private static double number(JsonObject object,String key,double fallback){return object.has(key)?object.get(key).getAsDouble():fallback;}
    private WeightedSelector(){}
}
