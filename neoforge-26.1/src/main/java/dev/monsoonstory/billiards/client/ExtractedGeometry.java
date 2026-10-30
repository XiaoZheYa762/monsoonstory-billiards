package dev.monsoonstory.billiards.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Frame-local vertex snapshot: submission never reads mutable tables or a client world. */
public final class ExtractedGeometry implements MultiBufferSource {
    private static final int STRIDE=11;
    private final Map<RenderType,Builder> layers=new LinkedHashMap<>();
    @Override public VertexConsumer getBuffer(RenderType type){return layers.computeIfAbsent(type,key->new Builder());}
    public Frame finish(){
        List<Layer> result=new ArrayList<>(layers.size());
        layers.forEach((type,builder)->{if(builder.size>0)result.add(new Layer(type,Arrays.copyOf(builder.data,builder.size)));});
        return new Frame(List.copyOf(result));
    }
    public record Frame(List<Layer> layers){
        public void submit(PoseStack matrices,SubmitNodeCollector collector){
            for(Layer layer:layers)collector.submitCustomGeometry(matrices,layer.type(),layer::render);
        }
    }
    public record Layer(RenderType type,int[] vertices){
        private void render(PoseStack.Pose pose,VertexConsumer out){
            for(int i=0;i<vertices.length;i+=STRIDE)
                out.addVertex(pose,f(vertices[i]),f(vertices[i+1]),f(vertices[i+2]))
                    .setColor(vertices[i+3]).setUv(f(vertices[i+4]),f(vertices[i+5]))
                    .setOverlay(vertices[i+6]).setLight(vertices[i+7])
                    .setNormal(pose,f(vertices[i+8]),f(vertices[i+9]),f(vertices[i+10]));
        }
        private static float f(int bits){return Float.intBitsToFloat(bits);}
    }
    private static final class Builder implements VertexConsumer {
        private int[] data=new int[STRIDE*256];private int size,vertex=-STRIDE;
        @Override public VertexConsumer addVertex(float x,float y,float z){
            if(size+STRIDE>data.length)data=Arrays.copyOf(data,data.length*2);
            vertex=size;size+=STRIDE;data[vertex]=Float.floatToRawIntBits(x);data[vertex+1]=Float.floatToRawIntBits(y);data[vertex+2]=Float.floatToRawIntBits(z);data[vertex+3]=-1;return this;
        }
        @Override public VertexConsumer setColor(int r,int g,int b,int a){return setColor((a<<24)|(r<<16)|(g<<8)|b);}
        @Override public VertexConsumer setColor(int argb){data[vertex+3]=argb;return this;}
        @Override public VertexConsumer setUv(float u,float v){data[vertex+4]=Float.floatToRawIntBits(u);data[vertex+5]=Float.floatToRawIntBits(v);return this;}
        @Override public VertexConsumer setUv1(int u,int v){data[vertex+6]=(u&65535)|(v<<16);return this;}
        @Override public VertexConsumer setUv2(int u,int v){data[vertex+7]=(u&65535)|(v<<16);return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){data[vertex+8]=Float.floatToRawIntBits(x);data[vertex+9]=Float.floatToRawIntBits(y);data[vertex+10]=Float.floatToRawIntBits(z);return this;}
        @Override public VertexConsumer setLineWidth(float width){return this;}
    }
}
