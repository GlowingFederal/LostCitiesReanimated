package mcjty.lostcities.dimensions.world.lost.cityassets;

import com.google.gson.JsonElement;
import mcjty.lostcities.assets.AssetContext;
import mcjty.lostcities.config.LostCityProfile;

/** Inherited style values resolved once against the selected profile. */
public final class EffectiveCitySettings {
    public final float parkChance,openLotParkChance,fountainChance,frontChance,corridorChance;
    public final boolean avoidFoliage,parkElevation,parkBorder;
    public final int parkStreetThreshold;
    public EffectiveCitySettings(LostCityProfile profile,CityStyle style){
        boolean modern=AssetContext.modern();
        parkChance=modern?style.decimal("parkblocks","parkchance",profile.PARK_CHANCE):profile.PARK_CHANCE;
        openLotParkChance=modern?style.decimal("profileOverrides","openLotParkChance",profile.modern.openLotParkChance):profile.modern.openLotParkChance;
        fountainChance=modern?style.decimal("streetblocks","fountainchance",profile.FOUNTAIN_CHANCE):profile.FOUNTAIN_CHANCE;
        frontChance=modern?style.decimal("streetblocks","frontchance",profile.BUILDING_FRONTCHANCE):profile.BUILDING_FRONTCHANCE;
        corridorChance=modern?style.decimal("corridorblocks","corridorchance",profile.CORRIDOR_CHANCE):profile.CORRIDOR_CHANCE;
        avoidFoliage=modern?flag(style,"avoidfoliage",profile.AVOID_FOLIAGE):profile.AVOID_FOLIAGE;
        parkElevation=modern?flag(style,"parkelevation",profile.modern.parkElevation):true;
        parkBorder=modern?flag(style,"parkborder",profile.modern.parkBorder):true;
        JsonElement threshold=modern?style.setting("parkblocks","parkstreetthreshold"):null;
        parkStreetThreshold=threshold==null?profile.modern.parkStreetThreshold:threshold.getAsInt();
    }
    private static boolean flag(CityStyle style,String key,boolean fallback){JsonElement value=style.setting("parkblocks",key);return value==null?fallback:value.getAsBoolean();}
}
