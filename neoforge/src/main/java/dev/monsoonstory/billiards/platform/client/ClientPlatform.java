package dev.monsoonstory.billiards.platform.client;
import dev.monsoonstory.billiards.client.BilliardsClient;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
public final class ClientPlatform {
    public static void initialize(IEventBus modBus){
        modBus.addListener(BilliardsClient::registerScreens);
        modBus.addListener(BilliardsClient::registerRenderers);
        modBus.addListener((FMLClientSetupEvent event)->event.enqueueWork(()->new BilliardsClient().onInitializeClient()));
        modBus.addListener(BilliardsClient::registerKeys);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->ClientTickEvents.END_CLIENT_TICK.listeners.forEach(callback->callback.accept(Minecraft.getInstance())));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event)->ClientPlayConnectionEvents.DISCONNECT.listeners.forEach(callback->callback.accept(Minecraft.getInstance().getConnection(),Minecraft.getInstance())));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent event)->{
            if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
            var c=Minecraft.getInstance();var buffers=c.renderBuffers().bufferSource();
            var context=new WorldRenderContext(event.getPoseStack(),event.getCamera(),buffers,event.getFrustum(),event.getPartialTick().getGameTimeDeltaPartialTick(false));
            WorldRenderEvents.AFTER_ENTITIES.listeners.forEach(callback->callback.accept(context));buffers.endBatch();
        });
        NeoForge.EVENT_BUS.addListener((RenderHighlightEvent.Block event)->{if(dev.monsoonstory.billiards.client.AimController.active())event.setCanceled(true);});
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event)->HudRenderCallback.EVENT.listeners.forEach(callback->callback.render(event.getGuiGraphics(),event.getPartialTick().getGameTimeDeltaPartialTick(false))));
    }
}
