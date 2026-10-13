package com.evolvedmind.nero.compat;
import java.util.List;
import java.util.function.BiConsumer;

/** Bootstrap bridge: renderer is opt-in and supplied by the isolated RuneLite test plugin. */
public final class ParticleRuntime {
 private static volatile BiConsumer<Object,List<AttachmentRegistry.Triangle>> listener;
 private static volatile Runnable renderer;
 private static volatile java.util.function.Consumer<Object> begin;
 private static volatile java.util.function.Consumer<Object> hd;
 private static boolean inspected;
 private static volatile BiConsumer<Object,Object> appearance;
 public static boolean optedIn(){return Boolean.getBoolean("nero.compat.isolated")||Boolean.getBoolean("nero.compat.enabled");}
 public static void installAppearance(BiConsumer<Object,Object> apply) {
  if(!optedIn())throw new IllegalStateException("Compatibility runtime requires explicit opt-in");
  appearance=apply;
 }
 public static void preview(Object model) {
  var target=appearance;if(target==null||model==null)return;
  try{target.accept(model,null);}catch(Throwable e){appearance=null;ModelProbe.event("appearance-disabled",e.toString());}
 }
 public static void installHd(java.util.function.Consumer<Object> draw) {
  if(!optedIn())throw new IllegalStateException("Compatibility runtime requires explicit opt-in");
  hd=draw;
 }
 public static void hdScene(Object renderer) {
  var target=hd;if(target==null)return;
  try{target.accept(renderer);}catch(Throwable e){hd=null;ModelProbe.event("hd-particles-disabled",e.toString());}
 }
 public static synchronized void install(BiConsumer<Object,List<AttachmentRegistry.Triangle>> sample,java.util.function.Consumer<Object> before,Runnable draw) {
  if(!optedIn())throw new IllegalStateException("Compatibility runtime requires explicit opt-in");
  listener=sample;begin=before;renderer=draw;
 }
 public static synchronized void clear(){listener=null;renderer=null;begin=null;hd=null;appearance=null;}
 public static void beginScene(Object scene) {
  var target=begin;if(target==null)return;
  try{target.accept(scene);}catch(Throwable e){clear();ModelProbe.event("particles-disabled",e.getClass().getSimpleName());}
 }
 public static void sample(Object model,Object actor) {
  var apply=appearance;
  if(apply!=null && model!=null)try{apply.accept(model,actor);}catch(Throwable e){appearance=null;ModelProbe.event("appearance-disabled",e.toString());}
  var target=listener;if(target==null || model==null)return;
  try {
   var triangles=AttachmentRegistry.triangles(model);
   if(!inspected && !triangles.isEmpty() && Boolean.getBoolean("nero.compat.isolated")) {
    inspected=true;Class<?> type=model.getClass();
    byte[] priorities=(byte[])type.getMethod("getFaceRenderPriorities").invoke(model);
    java.util.Map<Integer,Integer> counts=new java.util.TreeMap<>();
    if(priorities!=null)for(byte p:priorities)counts.merge(p&255,1,Integer::sum);
    ModelProbe.event("avatar-priorities",counts.toString()+" capeFaceStart="+(triangles.get(0).faceId-782));
   }
   target.accept(actor,triangles);
  }
  catch(Throwable e){clear();ModelProbe.event("particles-disabled",e.getClass().getSimpleName());}
 }
 public static void scene() {
  Runnable target=renderer;if(target==null)return;
  try {target.run();}catch(Throwable e){clear();ModelProbe.event("particles-disabled",e.getClass().getSimpleName());}
 }
}
