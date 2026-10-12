package com.evolvedmind.nero.particles;

import com.evolvedmind.nero.compat.AttachmentRegistry;
import java.lang.reflect.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.util.*;
import net.runelite.api.*;

/** Real gamepack rasterization, including native shared animation model copies. */
final class NativeWornCheck {
 static void run(Path raw,Path output)throws Exception {
  Files.createDirectories(output);
  Class<?> unsafeType=Class.forName("sun.misc.Unsafe");Field singleton=unsafeType.getDeclaredField("theUnsafe");singleton.setAccessible(true);
  Object unsafe=singleton.get(null),client=unsafeType.getMethod("allocateInstance",Class.class).invoke(unsafe,Class.forName("client"));
  Field nativeClient=Class.forName("oe").getDeclaredField("cz");nativeClient.setAccessible(true);Object previous=nativeClient.get(null);nativeClient.set(null,client);
  Class<?> raster=Class.forName("fh");raster.getMethod("bt",double.class).invoke(null,.8);
  Method buffer=raster.getMethod("ap",int[].class,int.class,int.class,float[].class),clip=raster.getMethod("au"),origin=raster.getMethod("aw",int.class,int.class);
  Constructor<?> decode=Class.forName("er").getDeclaredConstructor(byte[].class);decode.setAccessible(true);
  int renders=0;
  try {
   for(int id:new int[]{62101,62102,62104,62105,62207,62213,62219,62225,62231,62243,62249,62255,62261,62267,62273,62279}) {
    ModelData data=(ModelData)decode.newInstance((Object)Files.readAllBytes(raw.resolve("cache/live/7/"+id+"/0.dat")));
    Model lit=data.light(64,768,-50,-10,-50);lit.calculateBoundsCylinder();
    for(int yaw:new int[]{0,256,512,768,1024,1280,1536,1792}) {
     int[] expected=null;
     for(int repeat=0;repeat<12;repeat++) {
      // Native shared sequence allocation, not a private preview rasterizer.
      Model model=(Model)lit.getClass().getMethod("ac",boolean.class).invoke(lit,true);
      AttachmentRegistry.normalizeAvatarPriorities(model);
      int[] pixels=new int[400*400];buffer.invoke(null,pixels,400,400,null);clip.invoke(null);origin.invoke(null,200,200);
      model.drawFrustum(0,yaw,0,0,0,lit.getModelHeight()/2,360);
      long visible=Arrays.stream(pixels).filter(p->p!=0).count();
      if(visible<300)throw new AssertionError("Native worn cape invisible: "+id+" yaw="+yaw);
      if(expected==null)expected=pixels.clone();else if(!Arrays.equals(expected,pixels))throw new AssertionError("Native worn raster changes across shared copies: "+id+" yaw="+yaw);
      renders++;
     }
     if(yaw==0||yaw==1024) {
      BufferedImage image=new BufferedImage(400,400,BufferedImage.TYPE_INT_ARGB);
      for(int i=0;i<expected.length;i++)if(expected[i]!=0)image.setRGB(i%400,i/400,0xff000000|expected[i]);
      javax.imageio.ImageIO.write(image,"png",output.resolve("cape-"+id+"-"+yaw+".png").toFile());
     }
    }
   }
  } finally {nativeClient.set(null,previous);}
  System.out.println("PASS_NATIVE_WORN_RENDER models=16 angles=8 sharedCopies=12 frames="+renders+" actualDrawFrustum=true nativePriorities=true");
 }
}
