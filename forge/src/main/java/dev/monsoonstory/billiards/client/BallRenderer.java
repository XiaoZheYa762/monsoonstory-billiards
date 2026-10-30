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
import com.mojang.math.Axis;
import dev.monsoonstory.billiards.*;
import dev.monsoonstory.billiards.core.*;
import dev.monsoonstory.billiards.platform.client.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class BallRenderer {
    private static final RenderType[] LAYERS=new RenderType[16];
    private static final RenderType WOOD=RenderType.entityCutoutNoCull(BilliardsMod.id("textures/block/walnut.png")),SHAFT=RenderType.entityCutoutNoCull(new ResourceLocation("minecraft","textures/block/stripped_birch_log.png")),BRASS=RenderType.entityCutoutNoCull(BilliardsMod.id("textures/block/brass.png"));
    private static final int[][] CUBE_FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{0,1,5,4},{3,7,6,2}};
    private static final float[][] CUBE_NORMALS={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,-1,0},{0,1,0}};
    private static final RenderType[] HANDLES=new RenderType[3],SHAFTS=new RenderType[3];
    static{for(TableStyle style:TableStyle.values()){HANDLES[style.ordinal()]=RenderType.entityCutoutNoCull(BilliardsMod.id("textures/block/"+(style==TableStyle.GREEN?"walnut":style.felt)+".png"));SHAFTS[style.ordinal()]=style==TableStyle.GREEN?SHAFT:RenderType.entityCutoutNoCull(BilliardsMod.id("textures/block/wood_ivory.png"));}}
    private static final RenderType GHOST=RenderType.entityTranslucent(BilliardsMod.id("textures/ball/0.png"));
    private static final float[][][] MESHES={mesh(12,24),mesh(8,16),mesh(5,10)};
    private static final float[][] CIRCLE=new float[33][2];
    static{for(int i=0;i<16;i++)LAYERS[i]=RenderType.entityCutoutNoCull(BilliardsMod.id("textures/ball/"+i+".png"));for(int i=0;i<=32;i++){CIRCLE[i][0]=(float)Math.cos(i*Math.PI/16);CIRCLE[i][1]=(float)Math.sin(i*Math.PI/16);}}
    public static long renderedBalls,renderedCues,renderedGhosts;
    private static float[][] mesh(int lats,int longs){List<float[]> result=new ArrayList<>();for(int lat=0;lat<lats;lat++)for(int lon=0;lon<longs;lon++){add(result,lat,lon,lats,longs);add(result,lat,lon+1,lats,longs);add(result,lat+1,lon+1,lats,longs);add(result,lat+1,lon,lats,longs);}return result.toArray(float[][]::new);}
    private static void add(List<float[]> result,int lat,int lon,int lats,int longs){double theta=lat*Math.PI/lats,phi=lon*Math.PI*2/longs;result.add(new float[]{(float)(Math.sin(theta)*Math.cos(phi)),(float)Math.cos(theta),(float)(Math.sin(theta)*Math.sin(phi)),1-lon/(float)longs,lat/(float)lats});}
    private static void sphere(VertexConsumer v,PoseStack.Pose e,float[][] mesh,int light,int red,int green,int blue,int alpha){for(float[] p:mesh)v.vertex(e.pose(),p[0]*(float)Physics.R,p[1]*(float)Physics.R,p[2]*(float)Physics.R).color(red,green,blue,alpha).uv(p[3],p[4]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(e.normal(),p[0],p[1],p[2]).endVertex();}
    public static void render(WorldRenderContext ctx){Minecraft c=Minecraft.getInstance();if(c.level==null||ctx.consumers()==null)return;PoseStack m=ctx.matrixStack();Vec3 camera=ctx.camera().getPosition();
        for(BilliardsClient.View view:BilliardsClient.TABLES.values()){
            TableData t=view.table;double distance=t.distance(camera);if(distance>4096||!c.level.hasChunkAt(t.pos)||!c.level.getBlockState(t.pos).is(BilliardsMod.TABLE_PART))continue;AABB bounds=new AABB(t.world(0,0,0),t.world(Geometry.LENGTH,2,Geometry.WIDTH));if(ctx.frustum()!=null&&!ctx.frustum().isVisible(bounds.inflate(2)))continue;
            view.orient();float[][] mesh=MESHES[distance<256?0:distance<1024?1:2];m.pushPose();Vec3 origin=t.world(0,0,0);m.translate(origin.x-camera.x,origin.y-camera.y,origin.z-camera.z);m.mulPose(Axis.YP.rotationDegrees(-t.rotation*90));
            
            for(Ball ball:t.game.balls){if(ball.pocketed)continue;double x=view.x(ball.id),z=view.z(ball.id);m.pushPose();m.translate(x,Physics.SURFACE+Physics.R,z);m.mulPose(view.rotation[ball.id]);int light=LevelRenderer.getLightColor(c.level,BlockPos.containing(t.world(x,1,z)));sphere(ctx.consumers().getBuffer(LAYERS[ball.id]),m.last(),mesh,light,255,255,255,255);renderedBalls++;m.popPose();}
            if(CueAnimation.table(t.id)!=null&&distance<1024)cue(ctx,m,view);
            if(c.player!=null&&c.player.getMainHandItem().is(BilliardsMod.CUE_BALL)&&t.game.ballInHand&&t.seat(c.player.getUUID())==t.game.turn&&!t.paused){Vec3 spot=CueBallItem.target(c.player,t);if(spot!=null){boolean valid=CueBallItem.legal(t,spot);m.pushPose();m.translate(spot.x,Physics.SURFACE+Physics.R,spot.z);sphere(ctx.consumers().getBuffer(GHOST),m.last(),MESHES[1],LightTexture.FULL_BRIGHT,valid?150:255,valid?255:100,valid?150:100,115);m.popPose();renderedGhosts++;}}
            if(AimController.aiming()&&t.pos.equals(AimController.pos)&&BilliardsClient.guide)GuideRenderer.render(ctx,m,t);
            m.popPose();
        }
    }
    private static void cue(WorldRenderContext ctx,PoseStack m,BilliardsClient.View v){
        var p=CueAnimation.table(v.table.id);if(p==null)return;
        double angle=p.angle,side=p.side,top=p.top,draw=p.draw(),length=Math.max(2.35,p.reach+.65),weight=p.weight();
        int light=LevelRenderer.getLightColor(Minecraft.getInstance().level,BlockPos.containing(v.table.world(p.x,1,p.z)));
        m.pushPose();m.translate(p.x,Physics.SURFACE+Physics.R+top*Physics.R+(1-weight)*.5,p.z);m.mulPose(Axis.YP.rotation((float)-angle));m.translate(-Math.sqrt(Math.max(0,1-side*side-top*top))*Physics.R-draw,0,side*Physics.R);
        drawCue(p.style,length,m,ctx.consumers(),light);m.popPose();renderedCues++;
    }
    public static void drawCue(CueStyle style,double length,PoseStack m,MultiBufferSource consumers,int light){
        
        cuboid(consumers.getBuffer(HANDLES[style.ordinal()]),m.last(),-length,-.027,-.027,-length+.8,.027,.027,light,255,255,255);
        cuboid(consumers.getBuffer(BRASS),m.last(),-length+.79,-.024,-.024,-length+.85,.024,.024,light,255,255,255);
        cuboid(consumers.getBuffer(SHAFTS[style.ordinal()]),m.last(),-length+.85,-.018,-.018,-.025,.018,.018,light,255,255,255);
        cuboid(consumers.getBuffer(SHAFT),m.last(),-.025,-.021,-.021,0,.021,.021,light,110,190,210);
    }
    private static void cuboid(VertexConsumer v,PoseStack.Pose e,double x0,double y0,double z0,double x1,double y1,double z1,int light,int r,int g,int b){
        for(int f=0;f<6;f++)for(int i=0;i<4;i++){
            int corner=CUBE_FACES[f][i];float x=(float)(((corner+1)&2)!=0?x1:x0),y=(float)((corner&2)!=0?y1:y0),z=(float)(corner>=4?z1:z0);
            v.vertex(e.pose(),x,y,z).color(r,g,b,255).uv(i==0||i==3?0:1,i<2?0:1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(e.normal(),CUBE_NORMALS[f][0],CUBE_NORMALS[f][1],CUBE_NORMALS[f][2]).endVertex();
        }
    }
}
