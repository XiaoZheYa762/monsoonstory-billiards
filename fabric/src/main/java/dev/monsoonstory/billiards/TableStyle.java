package dev.monsoonstory.billiards;

import net.minecraft.item.Item;
import net.minecraft.util.StringIdentifiable;

/** Stable names are shared by block states, saved tables and full snapshots. */
public enum TableStyle implements StringIdentifiable {
    GREEN("green",0x36532c,0x795334,0xe3dcb9,"felt","walnut"),
    PINK("pink",0xbd7485,0xe8ddc7,0x59344c,"felt_pink","wood_ivory"),
    DEEP_BLUE("deep_blue",0x183c69,0x342b23,0xdee5ec,"felt_deep_blue","wood_dark");
    public final String key,felt,wood;
    public final int clothColor,frameColor,inkColor;
    TableStyle(String key,int cloth,int frame,int ink,String felt,String wood){this.key=key;clothColor=cloth;frameColor=frame;inkColor=ink;this.felt=felt;this.wood=wood;}
    @Override public String asString(){return key;}
    public static TableStyle from(String name){for(var s:values())if(s.key.equals(name))return s;return GREEN;}
    public Item tableItem(){return switch(this){case GREEN->BilliardsMod.TABLE;case PINK->BilliardsMod.PINK_TABLE;case DEEP_BLUE->BilliardsMod.BLUE_TABLE;};}
}
