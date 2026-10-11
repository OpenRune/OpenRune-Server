package com.evolvedmind.nero.particles;

import com.evolvedmind.nero.compat.*;
import com.google.gson.*;
import java.net.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import net.runelite.api.*;

final class CapeStyles implements AutoCloseable {
 static final int[] ORIGINAL={65214,65200,65186,62995};
 static final Style DEFAULT=new Style(ORIGINAL,0xffffff,100,false);
 private volatile Map<String,Style> profiles=Collections.emptyMap();
 private ScheduledExecutorService executor;
 private final Map<int[],Colours> meshes=new WeakHashMap<>();
 private final Client client;
 CapeStyles(Client client){this.client=client;}
 void start() {
  executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"Nero appearance reader");t.setDaemon(true);return t;});
  executor.scheduleWithFixedDelay(this::poll,0,200,TimeUnit.MILLISECONDS);
 }
 public void close(){if(executor!=null)executor.shutdownNow();profiles=Collections.emptyMap();meshes.clear();}
 private void poll() {
  try {
   HttpURLConnection c=(HttpURLConnection)new URL(System.getProperty("nero.compat.appearance.url","http://127.0.0.1:46601/appearance")).openConnection();
   c.setConnectTimeout(750);c.setReadTimeout(750);
   try(InputStream in=c.getInputStream()) {
    byte[] bytes=in.readNBytes(1024*1024+1);if(bytes.length>1024*1024)throw new IOException("Appearance response too large");
    JsonObject object=new JsonParser().parse(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
    if(object.get("schema").getAsInt()!=1)throw new IOException("Unknown appearance schema");
    Map<String,Style> result=new HashMap<>();
    for(var entry:object.getAsJsonObject("profiles").entrySet()) {
     if(result.size()>=2048)break;
     JsonObject j=entry.getValue().getAsJsonObject();JsonArray colours=j.getAsJsonArray("colours");
     if(colours.size()!=4)continue;int[] hsl=new int[4];boolean valid=true;
     for(int n=0;n<4;n++){hsl[n]=colours.get(n).getAsInt();valid &= hsl[n]>=0&&hsl[n]<=65535;}
     int rgb=j.get("particleColour").getAsInt(),size=j.get("size").getAsInt();
     if(valid&&rgb>=0&&rgb<=0xffffff&&size>=25&&size<=300)result.put(entry.getKey(),new Style(hsl,rgb,size,j.get("editing").getAsBoolean()));
    }
    profiles=Collections.unmodifiableMap(result);
   } finally {c.disconnect();}
  } catch(Exception ignored) {}
 }
 Style style(Object actor) {
  Player p=actor instanceof Player?(Player)actor:actor==null?client.getLocalPlayer():null;
  if(p==null||p.getName()==null)return DEFAULT;
  return profiles.getOrDefault(p.getName().toLowerCase(Locale.ROOT).replace(' ','_').replace('\u00a0','_'),DEFAULT);
 }
 void apply(Object model,Object actor) {
  try {
   List<AttachmentRegistry.PaletteFace> palette=AttachmentRegistry.palette(model);if(palette.isEmpty())return;
   Class<?> type=model.getClass();int[] indices=(int[])type.getMethod("getFaceIndices1").invoke(model);
   Style style=style(actor);Colours cache=meshes.get(indices);
   if(cache==null) {
    int[][] baseline=new int[3][];
    for(int c=0;c<3;c++)baseline[c]=((int[])type.getMethod("getFaceColors"+(c+1)).invoke(model)).clone();
    cache=new Colours(baseline);meshes.put(indices,cache);
   }
   String key=Arrays.toString(style.hsl);int[][] target=cache.variants.get(key);
   if(target==null) {
    target=new int[3][];for(int c=0;c<3;c++)target[c]=cache.baseline[c].clone();
    for(var face:palette)for(int corner=0;corner<3;corner++) {
     int old=target[corner][face.faceId];if(old<0)continue;
     int source=ORIGINAL[face.channel],dest=style.hsl[face.channel];
     if(source==dest)continue;
     int light=Math.max(2,Math.min(126,(old&127)*(dest&127)/Math.max(1,source&127)));
     target[corner][face.faceId]=(dest&65408)|light;
    }
    if(cache.variants.size()>=8)cache.variants.clear();cache.variants.put(key,target);
   }
   String[] fields={"bz","cr","cu"};
   for(int c=0;c<3;c++){Field field=type.getDeclaredField(fields[c]);field.setAccessible(true);field.set(model,target[c]);}
  } catch(Exception e){throw new IllegalStateException("Cape colour application failed",e);}
 }
 ParticleSystem.Definition customize(ParticleSystem.Definition source,Style style) {
  ParticleSystem.Definition d=new ParticleSystem.Definition();
  d.minRate=source.minRate;d.maxRate=source.maxRate;d.minSpeed=source.minSpeed;d.maxSpeed=source.maxSpeed;d.endSpeed=source.endSpeed;
  d.minLife=source.minLife;d.maxLife=source.maxLife;d.speedChange=source.speedChange;
  // The customiser's 100% is the former 300%; 300% is nine times the source size.
  double scale=3.0*(source.fixedStyle?100:style.size)/100.0;
  d.minSize=(int)(source.minSize*scale);d.maxSize=(int)(source.maxSize*scale);d.endSize=(int)(source.endSize*scale);
  d.fixedStyle=source.fixedStyle;
  d.minColour=source.fixedStyle?source.minColour:(source.minColour&0xff000000)|style.particleColour;
  d.maxColour=source.fixedStyle?source.maxColour:(source.maxColour&0xff000000)|style.particleColour;
  d.fadeColour=source.fixedStyle?source.fadeColour:(source.fadeColour&0xff000000)|style.particleColour;
  d.angles=source.angles;d.globalDirectionRaw=source.globalDirectionRaw;d.globalConstantSpeed=source.globalConstantSpeed;d.validate();return d;
 }
 static final class Style {
  final int[] hsl;final int particleColour,size;final boolean editing;
  Style(int[] hsl,int rgb,int size,boolean editing){this.hsl=hsl.clone();this.particleColour=rgb;this.size=size;this.editing=editing;}
  String key(){return Arrays.toString(hsl)+":"+particleColour+":"+size;}
 }
 private static final class Colours {
  final int[][] baseline;final Map<String,int[][]> variants=new HashMap<>();
  Colours(int[][] baseline){this.baseline=baseline;}
 }
}
