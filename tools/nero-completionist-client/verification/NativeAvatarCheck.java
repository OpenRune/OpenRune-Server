package com.evolvedmind.nero.particles;

import com.evolvedmind.nero.compat.AttachmentRegistry;
import java.lang.reflect.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.util.*;
import net.runelite.api.*;

/** Tests complete native body + equipment merges, not only isolated cape previews. */
final class NativeAvatarCheck {
 static Constructor<?> decode,merge;
 static Method buffer,clip,origin;
 static Class<?> dataType;
 static final int SIZE=400;

 static void run(Path fixtures,Path raw,Path output)throws Exception {
  Files.createDirectories(output);dataType=Class.forName("er");decode=dataType.getDeclaredConstructor(byte[].class);decode.setAccessible(true);
  merge=dataType.getDeclaredConstructor(Array.newInstance(dataType,0).getClass(),int.class);merge.setAccessible(true);
  Class<?> raster=Class.forName("fh");buffer=raster.getMethod("ap",int[].class,int.class,int.class,float[].class);clip=raster.getMethod("au");origin=raster.getMethod("aw",int.class,int.class);raster.getMethod("bt",double.class).invoke(null,.8);
  Class<?> unsafeType=Class.forName("sun.misc.Unsafe");Field singleton=unsafeType.getDeclaredField("theUnsafe");singleton.setAccessible(true);Object unsafe=singleton.get(null);
  Object client=unsafeType.getMethod("allocateInstance",Class.class).invoke(unsafe,Class.forName("client"));Field nativeClient=Class.forName("oe").getDeclaredField("cz");nativeClient.setAccessible(true);Object previous=nativeClient.get(null);nativeClient.set(null,client);
  int models=0,quiverPixels=0;
  try {
   for(int gender=0;gender<2;gender++) {
    List<Path> bodies=new ArrayList<>();
    try(var paths=Files.list(fixtures)){final int sex=gender;paths.filter(p->{String n=p.getFileName().toString();if(!n.startsWith("kit-")||!n.endsWith(".dat"))return false;int part=Integer.parseInt(n.split("-")[1]);return sex==0?part<=6:part==7||part>=9&&part<=13;}).sorted().forEach(bodies::add);}
    if(bodies.size()!=(gender==0?7:6))throw new AssertionError("Incomplete native body fixtures");
    Model ordinary=avatar(bodies,null);byte[] bodyPriorities=ordinary.getFaceRenderPriorities().clone();AttachmentRegistry.normalizeAvatarPriorities(ordinary);
    if(!Arrays.equals(bodyPriorities,ordinary.getFaceRenderPriorities()))throw new AssertionError("Ordinary avatar changed");
    int[] control=render(ordinary,0);
    int[] capeModels=gender==0?new int[]{62101,62104,62207,62213,62219,62225,62231,62243,62249,62255,62261,62267,62273,62279}:new int[]{62102,62105,62208,62214,62220,62226,62232,62244,62250,62256,62262,62268,62274,62280};
    for(int id:capeModels) {
     Model source=avatar(bodies,raw.resolve("cache/live/7/"+id+"/0.dat"));byte[] original=source.getFaceRenderPriorities().clone();byte[] sourceBias=source.getFaceBias()==null?null:source.getFaceBias().clone();
     Model animated=(Model)source.getClass().getMethod("ac",boolean.class).invoke(source,true);AttachmentRegistry.normalizeAvatarPriorities(animated);
     if(!Arrays.equals(original,source.getFaceRenderPriorities()))throw new AssertionError("Shared sequence mutated cached source");
     if(!Arrays.equals(sourceBias,animated.getFaceBias()))throw new AssertionError("Native avatar bias changed");
     int[] front=render(animated,0),torso=front;int checked=0;Model without=null;
     if(id==62279||id==62280) {
      // The native quiver includes a legitimate front shoulder strap. Remove
      // only its faces on another model before testing cape-versus-body coverage.
      without=avatar(bodies,raw.resolve("cache/live/7/"+id+"/0.dat"));AttachmentRegistry.normalizeAvatarPriorities(without);
      Arrays.fill(without.getFaceColors3(),without.getFaceCount()-344,without.getFaceCount(),-2);torso=render(without,0);
     }
     for(int y=120;y<160;y++)for(int x=190;x<211;x++){int p=y*SIZE+x;if(control[p]!=0){checked++;if(torso[p]!=control[p])throw new AssertionError("Cape covers native torso: "+id+" pixel="+p);}}
     if(checked<300)throw new AssertionError("Missing native torso control");
     int[] back=render(animated,1024);save(front,output.resolve("avatar-"+id+"-front.png"));save(back,output.resolve("avatar-"+id+"-back.png"));
     if(id==62279||id==62280) {
      // This fixture's last 344 faces are the native Dizana quiver. Hide it
      // on a separate decoded model to measure actual quiver visibility.
      int[] base=render(without,1024);int visible=0;for(int p=0;p<base.length;p++)if(back[p]!=base[p])visible++;
      if(visible<300)throw new AssertionError("Native quiver remains hidden: "+id+" pixels="+visible);
      quiverPixels+=visible;System.out.println("PASS_NATIVE_QUIVER_VISIBILITY model="+id+" changedPixels="+visible+" originalScale=true");
     }
     models++;
    }
   }
  }finally{nativeClient.set(null,previous);}
  System.out.println("PASS_NATIVE_AVATAR_OCCLUSION models="+models+" genders=2 frontTorsoPreserved=true quiverPixels="+quiverPixels+" sourceArraysUnchanged=true ordinaryAvatarUnchanged=true");
 }
 static Model avatar(List<Path> bodies,Path cape)throws Exception {
  Object parts=Array.newInstance(dataType,bodies.size()+(cape==null?0:1));int i=0;
  for(Path p:bodies)Array.set(parts,i++,decode.newInstance((Object)Files.readAllBytes(p)));
  if(cape!=null)Array.set(parts,i,decode.newInstance((Object)Files.readAllBytes(cape)));
  Model lit=((ModelData)merge.newInstance(parts,Array.getLength(parts))).light(64,768,-50,-10,-50);lit.calculateBoundsCylinder();return lit;
 }
 static int[] render(Model model,int yaw)throws Exception {
  model.calculateBoundsCylinder();
  int[] pixels=new int[SIZE*SIZE];buffer.invoke(null,pixels,SIZE,SIZE,null);clip.invoke(null);origin.invoke(null,SIZE/2,SIZE/2);
  model.drawFrustum(0,yaw,0,0,0,model.getModelHeight()/2,360);return pixels;
 }
 static void save(int[] pixels,Path path)throws Exception {
  BufferedImage image=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB);for(int p=0;p<pixels.length;p++)if(pixels[p]!=0)image.setRGB(p%SIZE,p/SIZE,0xff000000|pixels[p]);javax.imageio.ImageIO.write(image,"png",path.toFile());
 }
}
