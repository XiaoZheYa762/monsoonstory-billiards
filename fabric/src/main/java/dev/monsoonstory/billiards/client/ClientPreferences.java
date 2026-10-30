package dev.monsoonstory.billiards.client;
import com.google.gson.*;
import dev.monsoonstory.billiards.BilliardsMod;
import dev.monsoonstory.billiards.core.GuidePolicy;
import dev.monsoonstory.billiards.core.ChargeProfile;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
public final class ClientPreferences {
    public static double collisions=.8,cue=.8,notifications=.45;
    public static boolean rolling;
    public static double musicVolume=.25;
    public enum ShotCamera { PLAYER,FOLLOW,OVERVIEW; public ShotCamera next(){return values()[(ordinal()+1)%values().length];} }
    public static ShotCamera shotCamera=ShotCamera.OVERVIEW;
    public enum GuideStyle { THIN,DOTS,BAND,CONTACT; public GuideStyle next(){return values()[(ordinal()+1)%values().length];} }
    public static boolean guideEnabled=true,guideExtensions=false;
    public static GuidePolicy multiplayerGuide=GuidePolicy.STRAIGHT;
    public static GuideStyle guideStyle=GuideStyle.CONTACT;
    public enum PowerStyle { RULER,CUE_TRACK; public PowerStyle next(){return values()[(ordinal()+1)%values().length];} }
    public static PowerStyle powerStyle=PowerStyle.RULER;
    public static ChargeProfile chargeProfile=ChargeProfile.PRECISE;
    public static boolean cameraMigrated(JsonObject n){try{return n.has("cameraDefaultsVersion")&&n.get("cameraDefaultsVersion").getAsInt()>=1;}catch(Exception e){return false;}}
    public static ShotCamera readCamera(JsonObject n){if(cameraMigrated(n)&&n.has("shotCamera")){try{return ShotCamera.valueOf(n.get("shotCamera").getAsString());}catch(Exception ignored){}}return ShotCamera.OVERVIEW;}
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("monsoonstory_billiards-client.json");
    public static GuideStyle readGuideStyle(JsonObject n){try{if(n.has("guideStyle"))return GuideStyle.valueOf(n.get("guideStyle").getAsString());}catch(Exception ignored){}return GuideStyle.CONTACT;}
    public static PowerStyle readPowerStyle(JsonObject n){try{if(n.has("powerStyle"))return PowerStyle.valueOf(n.get("powerStyle").getAsString());}catch(Exception ignored){}return PowerStyle.RULER;}
    public static ChargeProfile readChargeProfile(JsonObject n){try{if(n.has("chargeProfile"))return ChargeProfile.from(n.get("chargeProfile").getAsString());}catch(Exception ignored){}return ChargeProfile.PRECISE;}
    public static double readMusicVolume(JsonObject n){try{if(n.has("music")&&!n.get("music").getAsBoolean())return 0;}catch(Exception ignored){}return volume(n,"musicVolume",.25);}
    public static void load(){try{
        JsonObject n=Files.exists(FILE)?JsonParser.parseString(Files.readString(FILE)).getAsJsonObject():new JsonObject();
        shotCamera=readCamera(n);guideStyle=readGuideStyle(n);musicVolume=readMusicVolume(n);
        powerStyle=readPowerStyle(n);chargeProfile=readChargeProfile(n);
        guideEnabled=!n.has("guideEnabled")||n.get("guideEnabled").getAsBoolean();guideExtensions=n.has("guideExtensions")&&n.get("guideExtensions").getAsBoolean();multiplayerGuide=GuidePolicy.from(n.has("multiplayerGuide")?n.get("multiplayerGuide").getAsString():"");
        collisions=volume(n,"collisions",.8);cue=volume(n,"cue",.8);notifications=volume(n,"notifications",.45);rolling=n.has("rolling")&&n.get("rolling").getAsBoolean();
        if(!cameraMigrated(n)||n.has("music")||!n.has("guideStyle")||!n.has("powerStyle")||!n.has("chargeProfile"))save();
    }catch(Exception e){BilliardsMod.LOG.warn("Could not read billiards preferences",e);}}
    private static double volume(JsonObject n,String key,double fallback){try{double v=n.has(key)?n.get(key).getAsDouble():fallback;return Double.isFinite(v)?Math.max(0,Math.min(1,v)):fallback;}catch(Exception e){return fallback;}}
    public static void save(){try{JsonObject n=new JsonObject();n.addProperty("cameraDefaultsVersion",1);n.addProperty("collisions",collisions);n.addProperty("cue",cue);n.addProperty("notifications",notifications);n.addProperty("rolling",rolling);n.addProperty("musicVolume",musicVolume);n.addProperty("shotCamera",shotCamera.name());n.addProperty("guideStyle",guideStyle.name());n.addProperty("guideEnabled",guideEnabled);n.addProperty("guideExtensions",guideExtensions);n.addProperty("multiplayerGuide",multiplayerGuide.name());n.addProperty("powerStyle",powerStyle.name());n.addProperty("chargeProfile",chargeProfile.name());Files.createDirectories(FILE.getParent());Files.writeString(FILE,new GsonBuilder().setPrettyPrinting().create().toJson(n));}catch(Exception e){BilliardsMod.LOG.warn("Could not save billiards preferences",e);}}
    private ClientPreferences(){}
}
