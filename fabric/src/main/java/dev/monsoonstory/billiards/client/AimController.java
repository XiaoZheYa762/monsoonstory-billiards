package dev.monsoonstory.billiards.client;
import dev.monsoonstory.billiards.*;
import dev.monsoonstory.billiards.core.Physics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.*;
import org.lwjgl.glfw.GLFW;
import java.util.UUID;

/** Camera ownership outlives the server's short cue animation. */
public final class AimController {
    public enum Stage { IDLE,AIM,STROKE,FOLLOW,SETTLE,RETURN }
    public static BlockPos pos;
    public static double angle,side,top,pitch=31,distance=2.5;
    public static boolean releasing;
    public static Stage stage=Stage.IDLE;
    public static float cameraYaw,cameraPitch;
    private static boolean charging,armed,accepted,scratched;
    private static double lastAngle,lastSide,lastTop,followUntil=-1;
    private static OverviewCamera overview;
    private static Vec3d collisionOffset;private static long collisionTick=-100;
    private static long sentTick,releaseTick,settledTick,returnTime,lastFrame;
    private static int shot;
    private static float health,returnYaw,returnPitch;
    private static UUID tableId;
    private static long tableEpoch;
    private static Vec3d cameraPos,target,returnPos;
    private static Perspective perspective;
    private static net.minecraft.registry.RegistryKey<net.minecraft.world.World> dimension;
    public static boolean active(){return pos!=null;}
    public static boolean observing(){return (stage==Stage.FOLLOW||stage==Stage.SETTLE)&&ClientPreferences.shotCamera==ClientPreferences.ShotCamera.OVERVIEW;}
    public static void cycleCamera(){var v=view();if(observing()&&overview!=null&&v!=null)overview.cycle(v.table);}
    public static boolean aiming(){return stage==Stage.AIM;}
    public static BilliardsClient.View view(){return pos==null?null:BilliardsClient.TABLES.get(pos.asLong());}
    public static void begin(BlockPos p){
        var c=MinecraftClient.getInstance();var v=BilliardsClient.TABLES.get(p.asLong());if(c.player==null||v==null)return;
        TableData t=v.table;if(!t.active()||t.paused||t.game.running||t.game.ballInHand||t.seat(c.player.getUuid())!=t.game.turn)return;
        end(false);c.setScreen(null);pos=p;tableId=t.id;tableEpoch=t.epoch;dimension=c.world.getRegistryKey();health=c.player.getHealth();
        angle=t.aimAngle;side=top=0;charging=armed=releasing=accepted=scratched=false;lastAngle=Double.NaN;sentTick=0;shot=t.game.shotNumber;followUntil=-1;
        overview=new OverviewCamera();collisionOffset=null;collisionTick=-100;perspective=c.options.getPerspective();stage=Stage.AIM;lastFrame=System.nanoTime();
        cameraPos=c.gameRenderer.getCamera().getPos();cameraYaw=c.gameRenderer.getCamera().getYaw();cameraPitch=c.gameRenderer.getCamera().getPitch();
        target=t.world(v.x(0),Physics.SURFACE+Physics.R+.08,v.z(0));CueCombatClient.camera(tableId,true);BetterCombatBridge.suspend();c.mouse.lockCursor();ScopedKeys.clear();
    }
    public static double power(){var v=view();if(v==null)return 0;if(releasing)return v.table.strikePower;if(!charging||v.table.charging==null)return 0;return dev.monsoonstory.billiards.core.CuePower.charged(v.serverNow()-v.table.chargeTick,v.table.cueStyle.maxPower,v.table.chargeProfile);}
    public static boolean previewReference(){return !charging;}
    public static double previewPower(){return charging?Math.max(.04,power()):.5;}
    public static void mouse(double dx){mouse(dx,0);}
    public static void mouse(double dx,double dy){if(!aiming())return;var c=MinecraftClient.getInstance();double s=c.options.getMouseSensitivity().getValue()*.6+.2,m=s*s*s*8*(BilliardsClient.precisionKey.isPressed()?.18:1);
        if(BilliardsClient.spinKey.isPressed()){side+=dx*m*.004;top-=dy*m*.004;double r=Math.hypot(side,top);if(r>.5){side*=.5/r;top*=.5/r;}}
        else{angle=Math.IEEEremainder(angle+dx*m*.15*Math.PI/180,Math.PI*2);pitch=MathHelper.clamp(pitch+dy*m*.15,10,75);}}
    public static void scroll(double dy){if(aiming())distance=MathHelper.clamp(distance-dy*.25,1.2,5);}
    public static void button(int button,int action){
        if(button!=1||!aiming()||!armed)return;var v=view();if(v==null)return;
        if(action==GLFW.GLFW_PRESS&&!charging){charging=true;BilliardsClient.send(v.table,Network.Action.CHARGE,0,0);}
        if(action==GLFW.GLFW_RELEASE&&charging){double p=Math.max(.04,power());BilliardsClient.send(v.table,Network.Action.STRIKE,angle,p,side,top);charging=false;releasing=true;stage=Stage.STROKE;GuidePreviewController.clear();releaseTick=BilliardsClient.clientTicks;}
    }
    public static void tick(MinecraftClient c){
        if(!active())return;var v=view();
        if(c.player==null||c.world==null||!c.world.getRegistryKey().equals(dimension)||v==null||!v.table.id.equals(tableId)||!c.player.isAlive()){end(true);return;}
        TableData t=v.table;if(t.epoch!=tableEpoch){restore(false);return;}if(BilliardsClient.clientTicks%20==0)CueCombatClient.camera(tableId,true);
        if(!c.isWindowFocused()){end(true);return;}
        if(c.player.getHealth()<health||t.distance(c.player.getPos())>25||t.paused)restore(true);
        if(c.currentScreen instanceof GameMenuScreen){restore(true);c.setScreen(null);}
        else if(c.currentScreen!=null&&!(c.currentScreen instanceof PreferencesScreen)&&!(c.currentScreen instanceof GuideSettingsScreen)&&!(c.currentScreen instanceof PowerSettingsScreen))restore(true);
        if(stage==Stage.AIM&&charging&&(c.currentScreen instanceof PreferencesScreen||c.currentScreen instanceof GuideSettingsScreen||c.currentScreen instanceof PowerSettingsScreen)){
            // GUI mouse handling can consume the release event. Cancel this charge without
            // leaving aim so closing settings can never turn that lost release into a shot.
            BilliardsClient.send(t,Network.Action.CANCEL_CHARGE,0,0);charging=false;armed=false;GuidePreviewController.clear();
        }
        if(stage==Stage.AIM){
            if(!CueItem.isCue(c.player.getMainHandStack())||!t.active()||t.seat(c.player.getUuid())!=t.game.turn||t.aimOwner==null||t.game.running)restore(true);
            else{
                if(c.currentScreen==null&&GLFW.glfwGetMouseButton(c.getWindow().getHandle(),1)==GLFW.GLFW_RELEASE)armed=true;
                if(BilliardsClient.clientTicks-sentTick>=4&&(Double.isNaN(lastAngle)||Math.abs(lastAngle-angle)>1e-5||Math.abs(lastSide-side)>1e-4||Math.abs(lastTop-top)>1e-4)){
                    BilliardsClient.send(t,Network.Action.AIM,angle,0,side,top);lastAngle=angle;lastSide=side;lastTop=top;sentTick=BilliardsClient.clientTicks;
                }
            }
        }else if(stage!=Stage.RETURN){
            if(t.strikeAt>=0){accepted=true;followUntil=t.followUntil;}
            if(t.game.running||t.game.shotNumber!=shot)accepted=true;
            if(!accepted&&t.aimOwner==null)restore(false);
            else if(!accepted&&BilliardsClient.clientTicks-releaseTick>60)restore(true);
            else if(accepted){
                boolean strokeDone=followUntil>=0?v.visualTick()>=followUntil:BilliardsClient.clientTicks-releaseTick>8;
                if(strokeDone&&ClientPreferences.shotCamera==ClientPreferences.ShotCamera.PLAYER)restore(false);
                else if(strokeDone&&t.game.running)stage=Stage.FOLLOW;
                else if(strokeDone&&!t.game.running){if(stage!=Stage.SETTLE){stage=Stage.SETTLE;settledTick=BilliardsClient.clientTicks;}else if(BilliardsClient.clientTicks-settledTick>=6)restore(false);}
            }
        }
    }
    public static void pocket(TableData t,Physics.Impact event){if(active()&&releasing&&t.id.equals(tableId)&&event.kind()==2&&event.ballId()==0){scratched=true;target=t.world(event.x(),Physics.SURFACE+Physics.R+.08,event.z());}}
    public static void restore(boolean cancel){
        if(!active()||stage==Stage.RETURN)return;var v=view();var c=MinecraftClient.getInstance();
        if(cancel&&v!=null&&c.getNetworkHandler()!=null)BilliardsClient.send(v.table,Network.Action.CANCEL,0,0);
        charging=false;stage=Stage.RETURN;GuidePreviewController.clear();returnTime=System.nanoTime();returnPos=cameraPos;returnYaw=cameraYaw;returnPitch=cameraPitch;c.options.setPerspective(perspective);
    }
    public static Vec3d camera(Vec3d vanilla,float vanillaYaw,float vanillaPitch){
        var v=view();if(v==null)return vanilla;long now=System.nanoTime();double dt=MathHelper.clamp((now-lastFrame)/1e9,0,.1);lastFrame=now;
        if(stage==Stage.RETURN){double f=MathHelper.clamp((now-returnTime)/600_000_000.0,0,1),ease=f*f*(3-2*f);
            cameraPos=returnPos.lerp(vanilla,ease);cameraYaw=returnYaw+MathHelper.wrapDegrees(vanillaYaw-returnYaw)*(float)ease;cameraPitch=MathHelper.lerp((float)ease,returnPitch,vanillaPitch);
            if(f>=1){end(false);cameraYaw=vanillaYaw;cameraPitch=vanillaPitch;return vanilla;}return cameraPos;
        }
        Vec3d desired;float wantedYaw,wantedPitch;
        boolean wide=(stage==Stage.FOLLOW||stage==Stage.SETTLE)&&ClientPreferences.shotCamera==ClientPreferences.ShotCamera.OVERVIEW;
        if(wide){
            var pose=overview.reachable(v.table,cameraPos);
            if(pose==null){var c=MinecraftClient.getInstance();c.player.sendMessage(net.minecraft.text.Text.translatable("message.monsoonstory_billiards.overview_blocked"),true);restore(false);return cameraPos;}
            desired=pose.position();wantedYaw=pose.yaw();wantedPitch=pose.pitch();
        }else{
            if(!scratched&&!v.table.game.balls[0].pocketed&&stage!=Stage.SETTLE)target=v.table.world(v.x(0),Physics.SURFACE+Physics.R+.08,v.z(0));
            double a=angle+v.table.rotation*Math.PI/2,rad=Math.toRadians(pitch),horizontal=distance*Math.cos(rad);
            Vec3d offset=new Vec3d(-Math.cos(a)*horizontal,distance*Math.sin(rad),-Math.sin(a)*horizontal);var c=MinecraftClient.getInstance();
            if(collisionOffset==null||BilliardsClient.clientTicks-collisionTick>=4){
                collisionTick=BilliardsClient.clientTicks;collisionOffset=offset;
                if(c.world!=null&&c.player!=null){var hit=c.world.raycast(new net.minecraft.world.RaycastContext(target,target.add(offset),net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,c.player));if(hit.getType()!=net.minecraft.util.hit.HitResult.Type.MISS)collisionOffset=hit.getPos().subtract(target).subtract(offset.normalize().multiply(.15));}
            }
            desired=target.add(collisionOffset);wantedYaw=(float)Math.toDegrees(a)-90;wantedPitch=(float)pitch;
        }
        double blend=1-Math.exp(-dt*(stage==Stage.AIM?20:wide?7:14));Vec3d next=cameraPos.lerp(desired,blend);
        if(wide){var c=MinecraftClient.getInstance();var hit=c.world.raycast(new net.minecraft.world.RaycastContext(cameraPos,next,net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,c.player));if(hit.getType()!=net.minecraft.util.hit.HitResult.Type.MISS){next=cameraPos;}}
        cameraPos=next;
        cameraYaw+=MathHelper.wrapDegrees(wantedYaw-cameraYaw)*(float)blend;cameraPitch=MathHelper.lerp((float)blend,cameraPitch,wantedPitch);return cameraPos;
    }
    public static Vec3d camera(){return cameraPos==null?Vec3d.ZERO:cameraPos;}
    public static float yaw(){var v=view();return (float)Math.toDegrees(angle+(v==null?0:v.table.rotation*Math.PI/2))-90;}
    public static void end(boolean cancel){GuidePreviewController.clear();var v=view();var c=MinecraftClient.getInstance();if(cancel&&v!=null&&c.getNetworkHandler()!=null)BilliardsClient.send(v.table,Network.Action.CANCEL,0,0);if(active()){CueCombatClient.camera(tableId,false);ScopedKeys.clear();if(perspective!=null)c.options.setPerspective(perspective);}pos=null;stage=Stage.IDLE;charging=armed=releasing=false;cameraPos=target=returnPos=null;}
}
