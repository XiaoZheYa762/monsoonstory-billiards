package dev.monsoonstory.billiards;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import java.util.List;

public final class CueItem extends Item {
    public final CueStyle style;
    private final Multimap<EntityAttribute,EntityAttributeModifier> attributes;
    public CueItem(){this(CueStyle.GREEN);}
    public CueItem(TableStyle style){this(CueStyle.from(style.asString()));}
    public CueItem(CueStyle style){
        super(new Settings().maxDamage(style.durability));this.style=style;
        attributes=ImmutableMultimap.of(
                EntityAttributes.GENERIC_ATTACK_DAMAGE,new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID,"Cue damage",1,EntityAttributeModifier.Operation.ADDITION),
                EntityAttributes.GENERIC_ATTACK_SPEED,new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID,"Cue speed",-2.4,EntityAttributeModifier.Operation.ADDITION));
    }
    public static boolean isCue(ItemStack stack){return stack.getItem() instanceof CueItem;}
    @Override public Text getName(ItemStack stack){return super.getName(stack).copy().styled(s->s.withColor(style.nameColor));}
    @Override public Multimap<EntityAttribute,EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot){return slot==EquipmentSlot.MAINHAND?attributes:super.getAttributeModifiers(slot);}
    private boolean pickup(PlayerEntity player){if(player instanceof ServerPlayerEntity p){TableData t=Tables.participation(p);return t!=null&&CueBallItem.pickup(p,t);}return false;}
    @Override public ActionResult useOnBlock(ItemUsageContext c){return pickup(c.getPlayer())?ActionResult.SUCCESS:ActionResult.PASS;}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand){
        ItemStack stack=player.getStackInHand(hand);
        if(hand!=Hand.MAIN_HAND)return TypedActionResult.pass(stack);
        if(pickup(player))return TypedActionResult.success(stack);
        if(player instanceof ServerPlayerEntity p&&!CueCombat.begin(p,stack))return TypedActionResult.fail(stack);
        player.setCurrentHand(hand);return TypedActionResult.consume(stack);
    }
    @Override public int getMaxUseTime(ItemStack stack){return 72000;}
    @Override public UseAction getUseAction(ItemStack stack){return UseAction.NONE;}
    @Override public void onStoppedUsing(ItemStack stack,World world,LivingEntity user,int remainingUseTicks){if(user instanceof ServerPlayerEntity p)CueCombat.release(p,stack);}
    @Override public boolean postHit(ItemStack stack,LivingEntity target,LivingEntity attacker){
        if(!attacker.getWorld().isClient)stack.damage(1,attacker,e->e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        return true;
    }
    @Override public void appendTooltip(ItemStack stack,World world,List<Text> lines,TooltipContext context){
        lines.add(Text.translatable("tooltip.monsoonstory_billiards.cue_combat").formatted(Formatting.GRAY));
        lines.add(Text.translatable("tooltip.monsoonstory_billiards.cue_stats",(int)Math.round(style.maxPower*100),style.knockback==3?"III":"I",stack.getMaxDamage()-stack.getDamage(),stack.getMaxDamage()).formatted(Formatting.GRAY));
        lines.add(Text.translatable("tooltip.monsoonstory_billiards.cue_durability").formatted(Formatting.DARK_GRAY));
    }
}
