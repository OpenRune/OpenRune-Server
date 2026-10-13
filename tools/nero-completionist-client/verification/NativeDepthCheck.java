package com.evolvedmind.nero.particles;

import com.evolvedmind.nero.compat.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.runelite.api.*;

/** Actual native z-buffer plus the plugin's root-scene/GPU guards. */
final class NativeDepthCheck {
 static void run()throws Exception {
  Properties profile=new Properties();try(var in=Files.newInputStream(Path.of(System.getProperty("nero.compat.profile")))){profile.load(in);}
  NativeDepthSurface surface=new NativeDepthSurface(NativeDepthCheck.class.getClassLoader(),profile);
  Class<?> raster=Class.forName("fh");Method buffer=raster.getMethod("ap",int[].class,int.class,int.class,float[].class);
  int[] pixels=new int[64];Arrays.fill(pixels,0x123456);float[] z=new float[64];Arrays.fill(z,Float.NaN);
  buffer.invoke(null,pixels,8,8,z);
  surface.clearViewportDepth(2,2,4,4);
  for(int y=0;y<8;y++)for(int x=0;x<8;x++){int i=y*8+x;require(pixels[i]==0x123456,"native colour ownership");require(x>=2&&x<6&&y>=2&&y<6?z[i]==0:Float.isNaN(z[i]),"viewport-only stale depth reset");}
  Scene root=(Scene)proxy(Scene.class,(o,m,a)->defaultValue(m.getReturnType()));
  Scene nested=(Scene)proxy(Scene.class,(o,m,a)->defaultValue(m.getReturnType()));boolean[] gpu={false};
  Client client=(Client)proxy(Client.class,(o,m,a)->{
   switch(m.getName()){case "isGpu":return gpu[0];case "getGameState":return GameState.LOGGED_IN;case "getScene":return root;case "getViewportXOffset":case "getViewportYOffset":return 2;case "getViewportWidth":case "getViewportHeight":return 4;default:return defaultValue(m.getReturnType());}
  });
  NeroParticlePlugin plugin=new NeroParticlePlugin();set(plugin,"client",client);set(plugin,"surface",surface);
  Method begin=NeroParticlePlugin.class.getDeclaredMethod("beginScene",Object.class);begin.setAccessible(true);
  ParticleRuntime.install((a,t)->{},scene->{try{begin.invoke(plugin,scene);}catch(Exception e){throw new IllegalStateException(e);}},()->{});
  try {
   Arrays.fill(z,1f);ParticleRuntime.beginScene(nested);for(float d:z)require(d==1f,"nested view leaves parent depth");
   gpu[0]=true;ParticleRuntime.beginScene(root);for(float d:z)require(d==1f,"GPU owns depth");
   gpu[0]=false;ParticleRuntime.beginScene(root);require(z[3*8+3]==0&&z[0]==1f,"software root clears viewport");
   z[3*8+3]=Float.MAX_VALUE;ParticleRuntime.beginScene(root);require(z[3*8+3]==0,"next camera frame clears previous occluder");
  }finally{ParticleRuntime.clear();raster.getMethod("bx",boolean.class).invoke(null,false);}
  System.out.println("PASS_NATIVE_DEPTH_RESET staleFrames=true sceneConsumer=true viewportOnly=true rgbUnchanged=true nestedViewPreserved=true gpuUnchanged=true");
 }
 static Object proxy(Class<?> type,InvocationHandler handler){return Proxy.newProxyInstance(NativeDepthCheck.class.getClassLoader(),new Class<?>[]{type},handler);}
 static Object defaultValue(Class<?> type){if(!type.isPrimitive())return null;if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;if(type==float.class)return 0f;if(type==double.class)return 0.;return null;}
 static void set(Object target,String name,Object value)throws Exception{Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
 static void require(boolean test,String message){if(!test)throw new AssertionError(message);}
}
