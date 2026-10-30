package dev.monsoonstory.billiards;

import net.minecraft.item.*;
import net.minecraft.text.Text;

public final class SouvenirBallItem extends Item {
    // Tooltip colours stay recognisable on the dark vanilla tooltip background.
    private static final int[] COLORS={0xf4f1e5,0xeabd23,0x528eec,0xef605a,0xb575db,0xf29a48,0x55b880,0xb96a7b,0x8b9099};
    public final int number;
    public SouvenirBallItem(int number){super(new Settings());this.number=number;}
    @Override public Text getName(ItemStack stack){return super.getName(stack).copy().styled(s->s.withColor(COLORS[number>8?number-8:number]));}
}
