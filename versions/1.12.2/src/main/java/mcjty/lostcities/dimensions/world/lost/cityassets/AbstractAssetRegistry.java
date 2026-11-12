package mcjty.lostcities.dimensions.world.lost.cityassets;

import mcjty.lostcities.api.ILostCityAsset;
import mcjty.lostcities.api.ILostCityAssetRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractAssetRegistry<T extends ILostCityAsset> implements ILostCityAssetRegistry<T>  {

    private final Map<String,T> assets = new java.util.LinkedHashMap<>();
    private final Map<String,T> modernAssets = new java.util.LinkedHashMap<>();
    private Map<String,T> selected() { if (!mcjty.lostcities.assets.AssetContext.modern()) return assets; Map<String,T> combined=new java.util.LinkedHashMap<>(assets);combined.putAll(modernAssets);return combined; }
    private final List<String> assetNames = new ArrayList<>();

    public void register(T building) {
        if (mcjty.lostcities.assets.AssetContext.modern()) {modernAssets.put(mcjty.lostcities.assets.ModernAssetPack.name(building.getName()),building);return;}
        assets.put(building.getName(), building);
        if (!assetNames.contains(building.getName())) assetNames.add(building.getName());
    }

    public <S extends ILostCityAsset> ILostCityAssetRegistry<S> cast() {
        return (ILostCityAssetRegistry<S>) this;
    }

    @Override
    public T get(String name) {
        if (name == null) {
            return null;
        }
        name=mcjty.lostcities.assets.ModernAssetPack.name(name);
        T t=mcjty.lostcities.assets.AssetContext.modern()?modernAssets.get(name):null;
        if(t==null)t=assets.get(name);
        if (t != null) {
            t.init();
        }
        return t;
    }

    public boolean contains(String name){if(name==null)return false;name=mcjty.lostcities.assets.ModernAssetPack.name(name);return modernAssets.containsKey(name)||assets.containsKey(name);}

    public T get(int i) {
        return get(getName(i));
    }

    @Override
    public Iterable<T> getIterable() {
        return selected().values();
    }

    public int getCount() {
        return selected().size();
    }

    public String getName(int i) {
        return mcjty.lostcities.assets.AssetContext.modern()?new java.util.ArrayList<>(selected().keySet()).get(i):assetNames.get(i);
    }

    public void reset() {
        assets.clear();
        modernAssets.clear();
        assetNames.clear();
    }
}
