package dev.monsoonstory.billiards.client;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.monsoonstory.billiards.*;
import dev.monsoonstory.billiards.core.*;
import dev.monsoonstory.billiards.platform.client.WorldRenderContext;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
public final class GuideRenderer {
    // UI-like geometry: preserve white instead of applying entity diffuse lighting.
    private static final RenderType LAYER=RenderType.entityTranslucentEmissive(BilliardsMod.id("textures/gui/guide.png"));
    public static boolean allowed(TableData t){return BilliardsClient.guide&&(t.game.practice||t.guidePolicy!=GuidePolicy.OFF);}
    public static boolean extended(TableData t){return ClientPreferences.guideExtensions&&(t.game.practice||t.guidePolicy==GuidePolicy.EXTENDED);}
    public static void render(WorldRenderContext ctx,PoseStack m,TableData t){
        if(!allowed(t)){GuidePreviewController.clear();return;}
        var result=GuidePreviewController.update(t);var fallback=GuidePreviewController.fallback();
        var out=ctx.consumers().getBuffer(LAYER);var e=m.last();GuidePath.Contact contact=null;
        if(result!=null){for(var track:result.tracks())draw(out,e,track);contact=result.contact();}
        else if(fallback!=null){for(var s:fallback.segments())draw(out,e,new ShotPrediction.Track(0,s.kind(),java.util.List.of(new ShotPrediction.Point(s.x(),s.z()),new ShotPrediction.Point(s.ex(),s.ez())),ShotPrediction.End.CONTACT));contact=fallback.contact();}
        Ball cue=t.game.balls[0];ring(out,e,cue.x,cue.z,Physics.R*1.20,180);
        if(ClientPreferences.guideStyle==ClientPreferences.GuideStyle.CONTACT&&contact!=null)ring(out,e,contact.x(),contact.z(),Physics.R,190);
    }
    /** Dash phase uses arc length across all physics samples, including reversals. */
    private static void draw(VertexConsumer out,PoseStack.Pose e,ShotPrediction.Track track){
        var points=track.points();var style=ClientPreferences.guideStyle;boolean main=track.kind()==GuidePath.Kind.MAIN;
        double total=0;for(int i=1;i<points.size();i++)total+=Math.hypot(points.get(i).x()-points.get(i-1).x(),points.get(i).z()-points.get(i-1).z());
        double cursor=0,period=style==ClientPreferences.GuideStyle.DOTS?.13:.28,on=style==ClientPreferences.GuideStyle.DOTS?.025:.17;
        for(int i=1;i<points.size();i++){
            var a=points.get(i-1);var b=points.get(i);double len=Math.hypot(b.x()-a.x(),b.z()-a.z());if(len<1e-8)continue;
            double dx=(b.x()-a.x())/len,dz=(b.z()-a.z())/len,start=Math.max(0,(main?Physics.R:0)-cursor);
            if(style==ClientPreferences.GuideStyle.BAND&&main&&start<len)strip(out,e,a.x(),a.z(),dx,dz,start,len,-Physics.R,Physics.R,alpha(24,track,total,cursor+(start+len)/2),0xffffff);
            for(double p=start;p<len-1e-8;){
                double phase=(cursor+p)%period;boolean visible=phase<on;double end=Math.min(len,p+(visible?on:period)-phase);
                if(end-p<1e-8){p+=1e-8;continue;}
                if(visible){int opacity=alpha(main?190:160,track,total,cursor+(p+end)/2);
                    if(style==ClientPreferences.GuideStyle.BAND&&main){strip(out,e,a.x(),a.z(),dx,dz,p,end,-Physics.R-.008,-Physics.R+.008,opacity,0xffffff);strip(out,e,a.x(),a.z(),dx,dz,p,end,Physics.R-.008,Physics.R+.008,opacity,0xffffff);}
                    else {double half=style==ClientPreferences.GuideStyle.DOTS?.014:.009;strip(out,e,a.x(),a.z(),dx,dz,p,end,-half,half,opacity,0xffffff);}
                }
                p=end;
            }
            cursor+=len;
        }
    }
    private static int alpha(int base,ShotPrediction.Track track,double length,double at){return track.fade()?(int)(base*Math.max(0,Math.min(1,(length-at)/.35))):base;}
    private static void ring(VertexConsumer v,PoseStack.Pose e,double x,double z,double r,int alpha){
        for(int i=0;i<32;i++){double a=i*Math.PI/16,b=(i+1)*Math.PI/16;quad(v,e,x+Math.cos(a)*r,z+Math.sin(a)*r,x+Math.cos(b)*r,z+Math.sin(b)*r,x+Math.cos(b)*(r+.012),z+Math.sin(b)*(r+.012),x+Math.cos(a)*(r+.012),z+Math.sin(a)*(r+.012),alpha,0xffffff);}
    }
    private static void strip(VertexConsumer v,PoseStack.Pose e,double x,double z,double dx,double dz,double start,double end,double left,double right,int alpha,int color){quad(v,e,x+dx*start-dz*left,z+dz*start+dx*left,x+dx*end-dz*left,z+dz*end+dx*left,x+dx*end-dz*right,z+dz*end+dx*right,x+dx*start-dz*right,z+dz*start+dx*right,alpha,color);}
    private static void quad(VertexConsumer v,PoseStack.Pose e,double x0,double z0,double x1,double z1,double x2,double z2,double x3,double z3,int a,int c){vertex(v,e,x0,z0,a,c);vertex(v,e,x1,z1,a,c);vertex(v,e,x2,z2,a,c);vertex(v,e,x3,z3,a,c);}
    private static void vertex(VertexConsumer v,PoseStack.Pose e,double x,double z,int a,int c){v.addVertex(e.pose(),(float)x,(float)Physics.SURFACE+.018f,(float)z).setColor((c>>16)&255,(c>>8)&255,c&255,a).setUv(.5f,.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(e,0,1,0);}
}
