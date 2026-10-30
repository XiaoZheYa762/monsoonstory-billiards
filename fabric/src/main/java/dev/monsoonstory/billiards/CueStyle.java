package dev.monsoonstory.billiards;

import net.minecraft.util.StringIdentifiable;

/** Standard cues available in the public edition. */
public enum CueStyle implements StringIdentifiable {
    GREEN("green", "cue", 0xD9C695),
    PINK("pink", "cue_pink", 0xF0B8CD),
    DEEP_BLUE("deep_blue", "cue_deep_blue", 0x8CB8E8);
    public final String key, itemPath;
    public final int nameColor, durability, knockback, combatChargeTicks;
    public final double maxPower;
    CueStyle(String key, String itemPath, int nameColor) {
        this.key=key;this.itemPath=itemPath;this.nameColor=nameColor;
        this.maxPower=1.0;this.durability=128;this.knockback=1;
        this.combatChargeTicks=30;
    }
    @Override public String asString(){return key;}
    public static CueStyle from(String key){for(CueStyle style:values())if(style.key.equals(key))return style;return GREEN;}
}
