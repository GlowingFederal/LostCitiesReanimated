package mcjty.lostcities.assets;

import java.util.*;

/** Resolves dynamic palette aliases in linear time without recursive calls or inherited stale values. */
public final class PaletteAliases {
    public static <T> List<Character> resolve(Map<Character,Character> aliases,Map<Character,T> values){
        for(Character character:aliases.keySet())values.remove(character);
        Map<Character,List<Character>> dependents=new HashMap<>();
        ArrayDeque<Character> ready=new ArrayDeque<>();
        for(Map.Entry<Character,Character> entry:aliases.entrySet()){
            dependents.computeIfAbsent(entry.getValue(),key->new ArrayList<>()).add(entry.getKey());
            if(values.containsKey(entry.getValue()))ready.add(entry.getKey());
        }
        List<Character> ordered=new ArrayList<>(aliases.size());
        while(!ready.isEmpty()){
            Character character=ready.removeFirst();values.put(character,values.get(aliases.get(character)));ordered.add(character);
            List<Character> children=dependents.get(character);if(children!=null)ready.addAll(children);
        }
        if(ordered.size()!=aliases.size()){
            List<String> errors=new ArrayList<>();
            for(Map.Entry<Character,Character> entry:aliases.entrySet())if(!values.containsKey(entry.getKey())&&errors.size()<8)errors.add(entry.getKey()+" -> "+entry.getValue());
            throw new IllegalArgumentException("Missing or cyclic palette references: "+errors);
        }
        return ordered;
    }
    private PaletteAliases(){}
}
