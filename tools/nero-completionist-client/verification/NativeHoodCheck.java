package com.evolvedmind.nero.particles;

import com.evolvedmind.nero.compat.AttachmentRegistry;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.runelite.api.*;

/** Complete equipment-masked heads, native bone transforms and depth rendering. */
final class NativeHoodCheck {
 static void run(Path fixtures,Path raw,Path output)throws Exception {
  Files.createDirectories(output);
  Class<?> dataType=Class.forName("er"),raster=Class.forName("fh");
  Constructor<?> decode=dataType.getDeclaredConstructor(byte[].class);decode.setAccessible(true);
  Constructor<?> merge=dataType.getDeclaredConstructor(Array.newInstance(dataType,0).getClass(),int.class);merge.setAccessible(true);
  Field singleton=Class.forName("oe").getDeclaredField("cz");singleton.setAccessible(true);Object previous=singleton.get(null);
  Class<?> unsafeType=Class.forName("sun.misc.Unsafe");Field unsafeField=unsafeType.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
  singleton.set(null,unsafeType.getMethod("allocateInstance",Class.class).invoke(unsafeField.get(null),Class.forName("client")));
  Method buffer=raster.getMethod("ap",int[].class,int.class,int.class,float[].class),depthRaster=raster.getMethod("bx",boolean.class);
  int checked=0,renders=0;
  try {
   for(int gender=0;gender<2;gender++) {
    ModelData reference=(ModelData)decode.newInstance((Object)Files.readAllBytes(fixtures.resolve(gender==0?"native-max-hood-male.dat":"native-max-hood-female.dat")));
    int firstHeadFace=gender==0?416:366;
    List<Path> bodies=new ArrayList<>();
    try(var paths=Files.list(fixtures)){final int sex=gender;paths.filter(p->{String n=p.getFileName().toString();if(!n.matches("kit-\\d+-.*\\.dat"))return false;int part=Integer.parseInt(n.split("-")[1]);return sex==0?part>=2&&part<=6:part>=9&&part<=13;}).sorted().forEach(bodies::add);}
    require(bodies.size()==5,"body fixtures without masked hair/jaw");
    List<Integer> ids=new ArrayList<>(gender==0?List.of(62107,62110):List.of(62108,62111));
    for(int id=62200;id<=62283;id++)if((id-62200)%6==(gender==0?4:5))ids.add(id);
    for(int id:ids) {
     Path hood=raw.resolve("cache/live/7/"+id+"/0.dat");
     ModelData source=(ModelData)decode.newInstance((Object)Files.readAllBytes(hood));
     require(source.getFaceCount()==427+(gender==0?88:111),"authored hood with native max-hood head "+id);
     for(int f=427;f<source.getFaceCount();f++)require(source.getFaceColors()[f]==4550||source.getFaceColors()[f]==0,"native face/eye colours outside cape palette "+id);
     Set<String> triangles=new HashSet<>();
     for(int f=0;f<427;f++){int[] v={source.getFaceIndices1()[f],source.getFaceIndices2()[f],source.getFaceIndices3()[f]};Arrays.sort(v);require(triangles.add(Arrays.toString(v)),"coincident authored cloth face "+id);}
     // The native female head includes two intentionally sided triangles.
     // Preserve the native head's winding, coordinates and bone labels exactly.
     Field skins=dataType.getDeclaredField("bk");skins.setAccessible(true);
     int[] sourceSkins=(int[])skins.get(source),referenceSkins=(int[])skins.get(reference);
     int[][] sourceFaces={source.getFaceIndices1(),source.getFaceIndices2(),source.getFaceIndices3()},referenceFaces={reference.getFaceIndices1(),reference.getFaceIndices2(),reference.getFaceIndices3()};
     for(int f=427;f<source.getFaceCount();f++) {
      int original=firstHeadFace+f-427;
      require(source.getFaceColors()[f]==reference.getFaceColors()[original],"native skin recolour base "+id);
      for(int corner=0;corner<3;corner++) {
       int v=sourceFaces[corner][f],r=referenceFaces[corner][original];
       require(source.getVerticesX()[v]==reference.getVerticesX()[r]&&source.getVerticesY()[v]==reference.getVerticesY()[r]&&source.getVerticesZ()[v]==reference.getVerticesZ()[r],"native head coordinates and winding "+id);
       require(sourceSkins[v]==referenceSkins[r],"native head bone labels "+id);
      }
     }
     int cape=gender==0?62101:62102;
     if(id==62282||id==62283)cape=gender==0?62279:62280;
     for(int turn:new int[]{-8,0,8}) {
      Object parts=Array.newInstance(dataType,bodies.size()+2);int n=0;
      for(Path p:bodies)Array.set(parts,n++,decode.newInstance((Object)Files.readAllBytes(p)));
      Array.set(parts,n++,decode.newInstance((Object)Files.readAllBytes(raw.resolve("cache/live/7/"+cape+"/0.dat"))));
      Array.set(parts,n,decode.newInstance((Object)Files.readAllBytes(hood)));
      Model model=((ModelData)merge.newInstance(parts,Array.getLength(parts))).light(64,768,-50,-10,-50);
      Model animated=(Model)model.getClass().getMethod("ac",boolean.class).invoke(model,true);
      Method transform=model.getClass().getMethod("qa",int.class,int[].class,int.class,int.class,int.class);
      transform.invoke(animated,0,new int[]{1,2,3},0,0,0);
      transform.invoke(animated,2,new int[]{1,2,3},0,turn&255,0);
      for(int v=0;v<animated.getVerticesCount();v++)require(Float.isFinite(animated.getVerticesX()[v])&&Float.isFinite(animated.getVerticesY()[v])&&Float.isFinite(animated.getVerticesZ()[v])&&Math.abs(animated.getVerticesX()[v])<100&&Math.abs(animated.getVerticesZ()[v])<100&&Math.abs(animated.getVerticesY()[v])<240,"head/body bone bounds "+id);
      AttachmentRegistry.normalizeAvatarPriorities(animated);
      for(boolean depth:new boolean[]{false,true})for(int yaw:new int[]{0,512,1024,1536}) {
       int[] pixels=new int[400*400];float[] z=depth?new float[pixels.length]:null;
       buffer.invoke(null,pixels,400,400,z);depthRaster.invoke(null,depth);raster.getMethod("au").invoke(null);raster.getMethod("aw",int.class,int.class).invoke(null,200,200);
       animated.calculateBoundsCylinder();animated.drawFrustum(0,yaw,0,0,0,animated.getModelHeight()/2,360);
       require(Arrays.stream(pixels).filter(p->p!=0).count()>1000,"visible complete hood avatar "+id+" depth="+depth);
       if(z!=null)for(float value:z)require(Float.isFinite(value),"finite native depth "+id);
       // A model must not leave a poisoned rasterizer for subsequent geometry.
       int[] control=new int[400*400];buffer.invoke(null,control,400,400,depth?new float[control.length]:null);raster.getMethod("au").invoke(null);raster.getMethod("aw",int.class,int.class).invoke(null,200,200);
       Model ordinary=NativeAvatarCheck.avatar(bodies,null);ordinary.drawFrustum(0,yaw,0,0,0,ordinary.getModelHeight()/2,360);
       require(Arrays.stream(control).filter(p->p!=0).count()>1000,"subsequent native geometry "+id);
       if(turn==0&&!depth)NativeAvatarCheck.save(pixels,output.resolve("hood-"+id+"-"+yaw+".png"));
       renders++;
      }
     }
     checked++;
    }
   }
  }finally{depthRaster.invoke(null,false);singleton.set(null,previous);}
  System.out.println("PASS_NATIVE_HOODS models="+checked+" renders="+renders+" nativeHeadBoneTransforms=true nativeHeadGeometry=true nativeSkinBase=true maskedHairJaw=true singleAuthoredHead=true finiteSoftwareDepth=true");
 }
 static void require(boolean test,String message){if(!test)throw new AssertionError(message);}
}
