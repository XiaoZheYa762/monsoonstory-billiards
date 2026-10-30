package dev.monsoonstory.billiards.platform.client;
import dev.monsoonstory.billiards.client.BilliardsClient;
import dev.monsoonstory.billiards.BilliardsMod;
import dev.monsoonstory.billiards.client.ExtractedGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.context.ContextKey;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
public final class ClientPlatform {
    private static final ContextKey<ExtractedGeometry.Frame> GEOMETRY=new ContextKey<>(BilliardsMod.id("table_geometry"));
    public static void initialize(IEventBus modBus){
        modBus.addListener(BilliardsClient::registerScreens);
        modBus.addListener(BilliardsClient::registerRenderers);
        modBus.addListener((FMLClientSetupEvent event)->event.enqueueWork(()->new BilliardsClient().onInitializeClient()));
        modBus.addListener(BilliardsClient::registerKeys);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->ClientTickEvents.END_CLIENT_TICK.listeners.forEach(callback->callback.accept(Minecraft.getInstance())));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event)->ClientPlayConnectionEvents.DISCONNECT.listeners.forEach(callback->callback.accept(Minecraft.getInstance().getConnection(),Minecraft.getInstance())));
        NeoForge.EVENT_BUS.addListener((ExtractLevelRenderStateEvent event)->{
            var geometry=new ExtractedGeometry();
            var context=new WorldRenderContext(new PoseStack(),event.getCamera(),geometry,event.getFrustum(),event.getDeltaTracker().getGameTimeDeltaPartialTick(false));
            WorldRenderEvents.AFTER_ENTITIES.listeners.forEach(callback->callback.accept(context));
            event.getRenderState().setRenderData(GEOMETRY,geometry.finish());
        });
        NeoForge.EVENT_BUS.addListener((SubmitCustomGeometryEvent event)->{
            var frame=event.getLevelRenderState().getRenderData(GEOMETRY);
            if(frame!=null)frame.submit(event.getPoseStack(),event.getSubmitNodeCollector());
        });
        NeoForge.EVENT_BUS.addListener((ExtractBlockOutlineRenderStateEvent event)->{if(dev.monsoonstory.billiards.client.AimController.active())event.setCanceled(true);});
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event)->HudRenderCallback.EVENT.listeners.forEach(callback->callback.render(event.getGuiGraphics(),event.getPartialTick().getGameTimeDeltaPartialTick(false))));
    }
}
