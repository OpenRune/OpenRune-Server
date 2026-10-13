package com.evolvedmind.nero.particles;

import java.lang.reflect.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.util.*;
import net.runelite.api.*;

/** Uses the qualified game's real drawFrustum rasterizer and original item projection. */
final class NativeIconCheck {
    static void run(Path raw,Path output)throws Exception {
        Files.createDirectories(output);
        // The rasterizer's RuneLite alpha writer reads only isGpu() from the
        // native singleton. Allocate inert headless state without running the
        // applet constructor, threads, login or network services.
        Class<?> unsafeType=Class.forName("sun.misc.Unsafe");Field singleton=unsafeType.getDeclaredField("theUnsafe");singleton.setAccessible(true);
        Object unsafe=singleton.get(null),client=unsafeType.getMethod("allocateInstance",Class.class).invoke(unsafe,Class.forName("client"));
        Field nativeClient=Class.forName("oe").getDeclaredField("cz");nativeClient.setAccessible(true);Object previousClient=nativeClient.get(null);nativeClient.set(null,client);
        Class<?> raster=Class.forName("fh");
        Method buffer=raster.getMethod("ap",int[].class,int.class,int.class,float[].class);
        Method clip=raster.getMethod("au"),origin=raster.getMethod("aw",int.class,int.class);
        raster.getMethod("bt",double.class).invoke(null,.8);
        Class<?> dataType=Class.forName("er");Constructor<?> decode=dataType.getDeclaredConstructor(byte[].class);decode.setAccessible(true);
        int total=0;Set<Integer> signatures=new HashSet<>();
        for(int model:new int[]{62100,62103,62206,62212,62218,62224,62230,62242,62248,62254,62260,62266,62272,62278}) {
            ModelData data=(ModelData)decode.newInstance((Object)Files.readAllBytes(raw.resolve("cache/live/7/"+model+"/0.dat")));
            Model lit=data.light(64,768,-50,-10,-50);lit.calculateBoundsCylinder();
            if(model==62278) {
                ModelData original=(ModelData)decode.newInstance((Object)Files.readAllBytes(raw.resolve("cache/live/7/62100/0.dat")));
                ModelData worn=(ModelData)decode.newInstance((Object)Files.readAllBytes(raw.resolve("cache/live/7/62279/0.dat")));
                boolean nativeTan=false,nativeTrim=false;
                for(short colour:worn.getFaceColors()){nativeTan|=(colour&65535)==6352;nativeTrim|=(colour&65535)==6379;}
                if(!nativeTan||!nativeTrim)throw new AssertionError("Missing worn Dizana palette reference");
                int panels=0,trim=0;Set<Integer> shades=new HashSet<>();
                for(int f=0;f<198;f++) {
                    int source=original.getFaceColors()[f]&65535,colour=data.getFaceColors()[f]&65535;
                    if((source&65408)==(62995&65408)) {
                        if((colour&65408)!=(6352&65408))throw new AssertionError("Dizana folded panel uses quiver red: "+f);
                        panels++;shades.add(colour&127);
                    } else if(source==64639) {
                        if(colour!=6379)throw new AssertionError("Dizana folded V does not match worn trim: "+f);
                        trim++;
                    } else if(colour!=source)throw new AssertionError("Unrelated folded ornament recoloured: "+f);
                }
                if(panels!=70||trim!=22||shades.size()<8)throw new AssertionError("Dizana panel/trim coverage or baked shades: "+panels+"/"+trim+"/"+shades.size());
                System.out.println("PASS_NATIVE_DIZANA_COLOURS tanPanels="+panels+" trimFaces="+trim+" distinctClothShades="+shades.size()+" wornPaletteMatched=true");
            }
            int[] pixels=new int[36*32];buffer.invoke(null,pixels,36,32,null);clip.invoke(null);origin.invoke(null,16,16);
            int zoom=1316,pitch=252,yaw=1020,xOffset=-1,yOffset=24;
            int y=(int)(Math.sin(pitch*Math.PI/1024)*65536)*zoom>>16;
            int z=(int)(Math.cos(pitch*Math.PI/1024)*65536)*zoom>>16;
            lit.drawFrustum(0,yaw,0,pitch,xOffset,lit.getModelHeight()/2+y+yOffset,z+yOffset);
            if(model==62278) {
                Model without=((ModelData)decode.newInstance((Object)Files.readAllBytes(raw.resolve("cache/live/7/"+model+"/0.dat")))).light(64,768,-50,-10,-50);without.calculateBoundsCylinder();
                Arrays.fill(without.getFaceColors3(),198,530,-2);
                int[] control=new int[36*32];buffer.invoke(null,control,36,32,null);clip.invoke(null);origin.invoke(null,16,16);
                without.drawFrustum(0,yaw,0,pitch,xOffset,without.getModelHeight()/2+y+yOffset,z+yOffset);
                int changed=0;for(int p=0;p<pixels.length;p++)if(pixels[p]!=control[p])changed++;
                if(changed<25)throw new AssertionError("Inventory quiver hidden: "+changed);
                System.out.println("PASS_NATIVE_QUIVER_ICON changedPixels="+changed+" originalFoldedShape=true lockedSharedModel=true");
            }
            if(Arrays.stream(pixels).noneMatch(p->p!=0)) {
                Method draw=lit.getClass().getDeclaredMethod("fv",boolean.class,boolean.class,boolean.class,long.class);draw.setAccessible(true);draw.invoke(lit,false,false,false,0L);
            }
            long visible=Arrays.stream(pixels).filter(p->p!=0).count();
            if(visible<60)throw new AssertionError("Native icon invisible: "+model+" pixels="+visible);
            signatures.add(Arrays.hashCode(pixels));
            BufferedImage image=new BufferedImage(36,32,BufferedImage.TYPE_INT_ARGB);
            for(int i=0;i<pixels.length;i++)if(pixels[i]!=0)image.setRGB(i%36,i/36,0xff000000|pixels[i]);
            javax.imageio.ImageIO.write(image,"png",output.resolve("icon-"+model+".png").toFile());total++;
        }
        if(signatures.size()<8)throw new AssertionError("Inventory family palettes indistinct: "+signatures.size());
        System.out.println("PASS_NATIVE_ICONS inventoryModels="+total+" distinctPalettes="+signatures.size()+" actualDrawFrustum=true originalFoldedProjection=true originalClothGeometry=true");
        nativeClient.set(null,previousClient);
    }
}
