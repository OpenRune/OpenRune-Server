package com.evolvedmind.nero.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Properties;

/** Revision-qualified software framebuffer access. GPU buffers remain owned by their renderer. */
public final class NativeDepthSurface {
    private final Field pixels, depth, width, height;
    private final Method encodeDepth;
    private final Method selectDepthRasterizer;
    private final Field sceneProjection;

    public NativeDepthSurface(ClassLoader loader, Properties profile) throws Exception {
        if (!"240".equals(profile.getProperty("revision"))) throw new IllegalArgumentException("Revision mismatch");
        Class<?> raster = qualify(loader, profile, "yw");
        Class<?> projection = qualify(loader, profile, "je");
        Class<?> rasterizer3d=qualify(loader,profile,"fh");
        Class<?> scene=qualify(loader,profile,"ez");
        sceneProjection=scene.getField("pu");
        if(!sceneProjection.getType().getName().equals("fv"))throw new IllegalArgumentException("Unexpected scene projection");
        selectDepthRasterizer=rasterizer3d.getMethod("bx",boolean.class);
        pixels=field(raster,"aj",int[].class);
        depth=field(raster,"ad",float[].class);
        width=field(raster,"ay",int.class);
        height=field(raster,"aq",int.class);
        encodeDepth=projection.getDeclaredMethod("ag",float.class,int.class);
        encodeDepth.setAccessible(true);
    }

    public void selectDepthRasterizer(boolean enable) throws Exception {
        selectDepthRasterizer.invoke(null,enable);
    }

    public boolean hasDepth() throws Exception { return depth.get(null)!=null; }
    public Object projection(Object scene) throws Exception { return sceneProjection.get(scene); }

    /** Start a new software camera view without retaining occluders from the
     * preceding frame. Clear depth only; native scene/UI colour stays owned by
     * the game. The caller excludes GPU and nested WorldView scene passes.
     */
    public void clearViewportDepth(int x,int y,int w,int h) throws Exception {
        Surface frame=capture();
        if(x<0||y<0||w<=0||h<=0||(long)x+w>frame.width||(long)y+h>frame.height)
            throw new IllegalArgumentException("Invalid scene depth viewport");
        for(int row=y;row<y+h;row++)java.util.Arrays.fill(frame.depth,row*frame.width+x,row*frame.width+x+w,0f);
    }

    public Surface capture() throws Exception {
        int[] rgb=(int[])pixels.get(null);
        float[] z=(float[])depth.get(null);
        int w=width.getInt(null),h=height.getInt(null);
        if (w<=0 || h<=0 || w>8192 || h>8192 || rgb==null || z==null
                || (long)w*h>rgb.length || (long)w*h>z.length)
            throw new IllegalStateException("No valid software depth surface");
        return new Surface(rgb,z,w,h);
    }

    public float depth(float cameraZ) throws Exception {
        // The real gamepack's transform includes its z-75 perspective offset.
        if (!Float.isFinite(cameraZ) || cameraZ<=75) throw new IllegalArgumentException("Behind near plane");
        return (Float)encodeDepth.invoke(null,cameraZ,0);
    }

    static Class<?> qualify(ClassLoader loader,Properties profile,String name) throws Exception {
        String expected=profile.getProperty("class."+name+".sha256");
        if(expected==null) throw new IllegalArgumentException("Unqualified renderer class: "+name);
        byte[] bytes;
        try(var input=loader.getResourceAsStream(name+".class")) {
            if(input==null) throw new IllegalArgumentException("Missing renderer class");
            bytes=input.readAllBytes();
        }
        StringBuilder actual=new StringBuilder();
        for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes)) actual.append(String.format("%02x",b&255));
        if(!expected.equals(actual.toString())) throw new IllegalArgumentException("Renderer fingerprint mismatch: "+name);
        return Class.forName(name,false,loader);
    }

    private static Field field(Class<?> type,String name,Class<?> expected) throws Exception {
        Field field=type.getDeclaredField(name);
        if(field.getType()!=expected || !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
            throw new IllegalArgumentException("Unexpected rasterizer field");
        field.setAccessible(true);
        return field;
    }

    public static final class Surface {
        private final int[] pixels;
        private final float[] depth;
        public final int width,height;
        private Surface(int[] p,float[] z,int w,int h) {pixels=p;depth=z;width=w;height=h;}

        /** Blend a fallback soft particle, clipped to the scene viewport and per-pixel native depth.
         * Farther particles must be submitted first; translucent particles do not write depth.
         * Original material 744 is not implemented by this fallback.
         */
        public int softParticle(float x,float y,float radius,float nativeDepth,int argb,
                                int clipX,int clipY,int clipW,int clipH) {
            if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(radius)||radius<=0||radius>128
                    || !Float.isFinite(nativeDepth)||nativeDepth<=0 || clipW<=0||clipH<=0) return 0;
            int alpha=argb>>>24;
            if(alpha==0) return 0;
            int left=Math.max(Math.max(0,clipX),(int)Math.floor(x-radius));
            int right=Math.min(Math.min(width,clipX+clipW),(int)Math.ceil(x+radius));
            int top=Math.max(Math.max(0,clipY),(int)Math.floor(y-radius));
            int bottom=Math.min(Math.min(height,clipY+clipH),(int)Math.ceil(y+radius));
            int drawn=0;
            for(int sy=top;sy<bottom;sy++) for(int sx=left;sx<right;sx++) {
                int at=sy*width+sx;
                float scene=depth[at];
                // Gamepack depth is larger for nearer geometry; 0 means an empty pixel.
                // Unknown/NaN depth fails closed instead of drawing through geometry.
                if(!Float.isFinite(scene)||scene>nativeDepth) continue;
                float dx=(sx+0.5f-x)/radius,dy=(sy+0.5f-y)/radius;
                float falloff=1-dx*dx-dy*dy;
                if(falloff<=0) continue;
                int a=Math.round(alpha*falloff*falloff);
                if(a==0) continue;
                int old=pixels[at],inv=255-a;
                int r=(((argb>>>16)&255)*a+((old>>>16)&255)*inv+127)/255;
                int g=(((argb>>>8)&255)*a+((old>>>8)&255)*inv+127)/255;
                int b=((argb&255)*a+(old&255)*inv+127)/255;
                pixels[at]=0xff000000|(r<<16)|(g<<8)|b;
                drawn++;
            }
            return drawn;
        }
    }
}
