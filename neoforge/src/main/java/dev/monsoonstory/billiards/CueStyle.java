package dev.monsoonstory.billiards;


import net.minecraft.util.StringRepresentable;

/** Standard cues available in the public edition. */
public enum CueStyle implements StringRepresentable {
    GREEN("green", "cue", 0, 0, 0xD9C695),
    PINK("pink", "cue_pink", 0, 0, 0xF0B8CD),
    DEEP_BLUE("deep_blue", "cue_deep_blue", 0, 0, 0x8CB8E8);


    public final String key, itemPath;
    public final int nameColor, durability, knockback, combatChargeTicks;
    public final double maxPower, shaftStart, shaftEnd;
    CueStyle(String key, String itemPath, double shaftStart, double shaftEnd, int nameColor) {
        this.key=key;this.itemPath=itemPath;this.shaftStart=shaftStart;this.shaftEnd=shaftEnd;this.nameColor=nameColor;
        this.maxPower=1.0;this.durability=128;this.knockback=1;
        this.combatChargeTicks=30;
    }
    @Override public String getSerializedName(){return key;}
    public boolean hasModel(){return shaftEnd>0;}
    public static CueStyle from(String key){for(CueStyle style:values())if(style.key.equals(key))return style;return GREEN;}
}
