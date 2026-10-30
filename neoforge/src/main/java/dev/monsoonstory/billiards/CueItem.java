package dev.monsoonstory.billiards;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;




import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

public final class CueItem extends Item {
    public final CueStyle style;

    public CueItem(){this(CueStyle.GREEN);}
    public CueItem(TableStyle style){this(CueStyle.from(style.getSerializedName()));}
    public CueItem(CueStyle style){
        super(new Properties().durability(style.durability).attributes(attributes()));this.style=style;

    }
    private static net.minecraft.world.item.component.ItemAttributeModifiers attributes(){return net.minecraft.world.item.component.ItemAttributeModifiers.builder()
        .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(BASE_ATTACK_DAMAGE_ID,1,AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
        .add(Attributes.ATTACK_SPEED,new AttributeModifier(BASE_ATTACK_SPEED_ID,-2.4,AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build();}
    public static boolean isCue(ItemStack stack){return stack.getItem() instanceof CueItem;}
    @Override public Component getName(ItemStack stack){return super.getName(stack).copy().withStyle(s->s.withColor(style.nameColor));}

    private boolean pickup(Player player){if(player instanceof ServerPlayer p){TableData t=Tables.participation(p);return t!=null&&CueBallItem.pickup(p,t);}return false;}
    @Override public InteractionResult useOn(UseOnContext c){return pickup(c.getPlayer())?InteractionResult.SUCCESS:InteractionResult.PASS;}
    @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResultHolder.pass(stack);
        if(pickup(player))return InteractionResultHolder.success(stack);
        if(player instanceof ServerPlayer p&&!CueCombat.begin(p,stack))return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 72000;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.NONE;}
    @Override public void releaseUsing(ItemStack stack,Level world,LivingEntity user,int remainingUseTicks){if(user instanceof ServerPlayer p)CueCombat.release(p,stack);}
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
        if(!attacker.level().isClientSide)stack.hurtAndBreak(1,attacker,EquipmentSlot.MAINHAND);
        return true;
    }
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext world,List<Component> lines,TooltipFlag context){
        lines.add(Component.translatable("tooltip.monsoonstory_billiards.cue_combat").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.monsoonstory_billiards.cue_stats",(int)Math.round(style.maxPower*100),style.knockback==3?"III":"I",stack.getMaxDamage()-stack.getDamageValue(),stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.monsoonstory_billiards.cue_durability").withStyle(ChatFormatting.DARK_GRAY));
    }
}
