package dev.monsoonstory.billiards.platform.client;
import dev.monsoonstory.billiards.client.BilliardsClient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
public final class ClientPlatform {
    public static void initialize(IEventBus modBus){
        
        modBus.addListener(BilliardsClient::registerRenderers);
        modBus.addListener((FMLClientSetupEvent event)->event.enqueueWork(()->{ BilliardsClient.registerScreens(); new BilliardsClient().onInitializeClient(); dev.monsoonstory.billiards.music.ForgeMusicNetwork.registerClient(); }));
        modBus.addListener(BilliardsClient::registerKeys);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent event)->{if(event.phase==net.minecraftforge.event.TickEvent.Phase.END)ClientTickEvents.END_CLIENT_TICK.listeners.forEach(callback->callback.accept(Minecraft.getInstance()));});
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event)->ClientPlayConnectionEvents.DISCONNECT.listeners.forEach(callback->callback.accept(Minecraft.getInstance().getConnection(),Minecraft.getInstance())));
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event)->{
            if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
            var c=Minecraft.getInstance();var buffers=c.renderBuffers().bufferSource();
            var context=new WorldRenderContext(event.getPoseStack(),event.getCamera(),buffers,event.getFrustum(),event.getPartialTick());
            WorldRenderEvents.AFTER_ENTITIES.listeners.forEach(callback->callback.accept(context));buffers.endBatch();
        });
        MinecraftForge.EVENT_BUS.addListener((RenderHighlightEvent.Block event)->{if(dev.monsoonstory.billiards.client.AimController.active())event.setCanceled(true);});
        MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post event)->HudRenderCallback.EVENT.listeners.forEach(callback->callback.render(event.getGuiGraphics(),event.getPartialTick())));
    }
}
