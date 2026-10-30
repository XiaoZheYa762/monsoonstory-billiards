package dev.monsoonstory.billiards.client;
import dev.monsoonstory.billiards.*;
import dev.monsoonstory.billiards.core.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import java.util.*;

public final class BallRenderer {
    private static final RenderLayer[] LAYERS=new RenderLayer[16];
    private static final RenderLayer WOOD=RenderLayer.getEntityCutoutNoCull(BilliardsMod.id("textures/block/walnut.png")),SHAFT=RenderLayer.getEntityCutoutNoCull(new Identifier("minecraft","textures/block/stripped_birch_log.png")),BRASS=RenderLayer.getEntityCutoutNoCull(BilliardsMod.id("textures/block/brass.png"));
    private static final int[][] CUBE_FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{0,1,5,4},{3,7,6,2}};
    private static final float[][] CUBE_NORMALS={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,-1,0},{0,1,0}};
    private static final RenderLayer[] HANDLES=new RenderLayer[3],SHAFTS=new RenderLayer[3];
    static{for(TableStyle style:TableStyle.values()){HANDLES[style.ordinal()]=RenderLayer.getEntityCutoutNoCull(BilliardsMod.id("textures/block/"+(style==TableStyle.GREEN?"walnut":style.felt)+".png"));SHAFTS[style.ordinal()]=style==TableStyle.GREEN?SHAFT:RenderLayer.getEntityCutoutNoCull(BilliardsMod.id("textures/block/wood_ivory.png"));}}
    private static final RenderLayer GHOST=RenderLayer.getEntityTranslucent(BilliardsMod.id("textures/ball/0.png"));
    private static final float[][][] MESHES={mesh(12,24),mesh(8,16),mesh(5,10)};
    private static final float[][] CIRCLE=new float[33][2];
    static{for(int i=0;i<16;i++)LAYERS[i]=RenderLayer.getEntityCutoutNoCull(BilliardsMod.id("textures/ball/"+i+".png"));for(int i=0;i<=32;i++){CIRCLE[i][0]=(float)Math.cos(i*Math.PI/16);CIRCLE[i][1]=(float)Math.sin(i*Math.PI/16);}}
    public static long renderedBalls,renderedCues,renderedGhosts;
    private static float[][] mesh(int lats,int longs){List<float[]> result=new ArrayList<>();for(int lat=0;lat<lats;lat++)for(int lon=0;lon<longs;lon++){add(result,lat,lon,lats,longs);add(result,lat,lon+1,lats,longs);add(result,lat+1,lon+1,lats,longs);add(result,lat+1,lon,lats,longs);}return result.toArray(float[][]::new);}
    private static void add(List<float[]> result,int lat,int lon,int lats,int longs){double theta=lat*Math.PI/lats,phi=lon*Math.PI*2/longs;result.add(new float[]{(float)(Math.sin(theta)*Math.cos(phi)),(float)Math.cos(theta),(float)(Math.sin(theta)*Math.sin(phi)),1-lon/(float)longs,lat/(float)lats});}
    private static void sphere(VertexConsumer v,MatrixStack.Entry e,float[][] mesh,int light,int red,int green,int blue,int alpha){for(float[] p:mesh)v.vertex(e.getPositionMatrix(),p[0]*(float)Physics.R,p[1]*(float)Physics.R,p[2]*(float)Physics.R).color(red,green,blue,alpha).texture(p[3],p[4]).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e.getNormalMatrix(),p[0],p[1],p[2]).next();}
    public static void render(WorldRenderContext ctx){MinecraftClient c=MinecraftClient.getInstance();if(c.world==null||ctx.consumers()==null)return;MatrixStack m=ctx.matrixStack();Vec3d camera=ctx.camera().getPos();
        for(BilliardsClient.View view:BilliardsClient.TABLES.values()){
            TableData t=view.table;double distance=t.distance(camera);if(distance>4096||!c.world.isChunkLoaded(t.pos)||!c.world.getBlockState(t.pos).isOf(BilliardsMod.TABLE_PART))continue;Box bounds=new Box(t.world(0,0,0),t.world(Geometry.LENGTH,2,Geometry.WIDTH));if(ctx.frustum()!=null&&!ctx.frustum().isVisible(bounds.expand(2)))continue;
            view.orient();float[][] mesh=MESHES[distance<256?0:distance<1024?1:2];m.push();Vec3d origin=t.world(0,0,0);m.translate(origin.x-camera.x,origin.y-camera.y,origin.z-camera.z);m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-t.rotation*90));
            for(Ball ball:t.game.balls){if(ball.pocketed)continue;double x=view.x(ball.id),z=view.z(ball.id);m.push();m.translate(x,Physics.SURFACE+Physics.R,z);m.multiply(view.rotation[ball.id]);int light=WorldRenderer.getLightmapCoordinates(c.world,BlockPos.ofFloored(t.world(x,1,z)));sphere(ctx.consumers().getBuffer(LAYERS[ball.id]),m.peek(),mesh,light,255,255,255,255);renderedBalls++;m.pop();}
            if(CueAnimation.table(t.id)!=null&&distance<1024)cue(ctx,m,view);
            if(c.player!=null&&c.player.getMainHandStack().isOf(BilliardsMod.CUE_BALL)&&t.game.ballInHand&&t.seat(c.player.getUuid())==t.game.turn&&!t.paused){Vec3d spot=CueBallItem.target(c.player,t);if(spot!=null){boolean valid=CueBallItem.legal(t,spot);m.push();m.translate(spot.x,Physics.SURFACE+Physics.R,spot.z);sphere(ctx.consumers().getBuffer(GHOST),m.peek(),MESHES[1],LightmapTextureManager.MAX_LIGHT_COORDINATE,valid?150:255,valid?255:100,valid?150:100,115);m.pop();renderedGhosts++;}}
            if(AimController.aiming()&&t.pos.equals(AimController.pos)&&BilliardsClient.guide)GuideRenderer.render(ctx,m,t);
            m.pop();
        }
    }
    private static void cue(WorldRenderContext ctx,MatrixStack m,BilliardsClient.View v){
        var p=CueAnimation.table(v.table.id);if(p==null)return;
        double angle=p.angle,side=p.side,top=p.top,draw=p.draw(),length=Math.max(2.35,p.reach+.65),weight=p.weight();
        int light=WorldRenderer.getLightmapCoordinates(MinecraftClient.getInstance().world,BlockPos.ofFloored(v.table.world(p.x,1,p.z)));
        m.push();m.translate(p.x,Physics.SURFACE+Physics.R+top*Physics.R+(1-weight)*.5,p.z);m.multiply(RotationAxis.POSITIVE_Y.rotation((float)-angle));m.translate(-Math.sqrt(Math.max(0,1-side*side-top*top))*Physics.R-draw,0,side*Physics.R);
        drawCue(p.style,length,m,ctx.consumers(),light);m.pop();renderedCues++;
    }
    public static void drawCue(CueStyle style,double length,MatrixStack m,VertexConsumerProvider consumers,int light){
        cuboid(consumers.getBuffer(HANDLES[style.ordinal()]),m.peek(),-length,-.027,-.027,-length+.8,.027,.027,light,255,255,255);
        cuboid(consumers.getBuffer(BRASS),m.peek(),-length+.79,-.024,-.024,-length+.85,.024,.024,light,255,255,255);
        cuboid(consumers.getBuffer(SHAFTS[style.ordinal()]),m.peek(),-length+.85,-.018,-.018,-.025,.018,.018,light,255,255,255);
        cuboid(consumers.getBuffer(SHAFT),m.peek(),-.025,-.021,-.021,0,.021,.021,light,110,190,210);
    }
    private static void cuboid(VertexConsumer v,MatrixStack.Entry e,double x0,double y0,double z0,double x1,double y1,double z1,int light,int r,int g,int b){
        for(int f=0;f<6;f++)for(int i=0;i<4;i++){
            int corner=CUBE_FACES[f][i];float x=(float)(((corner+1)&2)!=0?x1:x0),y=(float)((corner&2)!=0?y1:y0),z=(float)(corner>=4?z1:z0);
            v.vertex(e.getPositionMatrix(),x,y,z).color(r,g,b,255).texture(i==0||i==3?0:1,i<2?0:1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e.getNormalMatrix(),CUBE_NORMALS[f][0],CUBE_NORMALS[f][1],CUBE_NORMALS[f][2]).next();
        }
    }
}
