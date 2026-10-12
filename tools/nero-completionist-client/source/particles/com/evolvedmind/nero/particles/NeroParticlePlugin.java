package com.evolvedmind.nero.particles;

import com.evolvedmind.nero.compat.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.*;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(name="Nero Completionist Particles",description="Completionist appearance and particles with native scene depth",enabledByDefault=true)
public final class NeroParticlePlugin extends Plugin {
 @Inject private Client client;
 @Inject private OverlayManager overlays;
 @Inject private net.runelite.client.game.ItemManager itemManager;
 @Inject private net.runelite.client.ui.overlay.tooltip.TooltipManager tooltips;
 @Inject private net.runelite.client.config.ConfigManager configManager;
 private CapeItemStats itemStats;
 private NativeCapePreview preview;
 private NativeDepthSurface surface;
 private final ParticleSystem system=new ParticleSystem(646);
 private final Map<Integer,ParticleSystem.Definition> definitions=new HashMap<>();
 private List<ParticleSystem.Emitter> emitters=Collections.emptyList();
 private long lastFrame;
 private int frames;
 private final HdParticleSurface hd=new HdParticleSurface();
 private Boolean lastGpu;
 private CapeStyles styles;
 private final Map<Actor,ActorParticles> actors=new IdentityHashMap<>();

 @Override protected void startUp() throws Exception {
  if(!ParticleRuntime.optedIn())return;
  if(Boolean.getBoolean("nero.compat.diagnostic.painter"))return;
  Properties profile=new Properties();try(var input=Files.newInputStream(Path.of(System.getProperty("nero.compat.profile")))){profile.load(input);}
  surface=new NativeDepthSurface(client.getClass().getClassLoader(),profile);
  styles=new CapeStyles(client);styles.start();
  JsonObject all=new JsonParser().parse(Files.readString(Path.of(System.getProperty("nero.compat.definitions")))).getAsJsonObject();
  for(var entry:all.entrySet()) {
   JsonObject j=entry.getValue().getAsJsonObject();ParticleSystem.Definition d=new ParticleSystem.Definition();
   d.minRate=j.get("minParticleRateRaw").getAsInt();d.maxRate=j.get("maxParticleRateRaw").getAsInt();
   d.minSize=j.get("minSizeRaw").getAsInt();d.maxSize=j.get("maxSizeRaw").getAsInt();d.endSize=j.get("endSizeRaw").getAsInt();
   d.minSpeed=j.get("minSpeedRaw").getAsInt();d.maxSpeed=j.get("maxSpeedRaw").getAsInt();d.endSpeed=j.get("endSpeed").getAsInt();
   d.minLife=j.get("minLifetime").getAsInt();d.maxLife=j.get("maxLifetime").getAsInt();
   d.minColour=j.get("minStartColour").getAsInt();d.maxColour=j.get("maxStartColour").getAsInt();d.fadeColour=j.get("fadeColour").getAsInt();
   d.speedChange=j.get("speedChangePercentage").getAsInt();d.angles=new int[4];for(int i=0;i<4;i++)d.angles[i]=j.getAsJsonArray("anglesRaw").get(i).getAsInt();d.validate();
   d.globalDirectionRaw=new int[3];for(int i=0;i<3;i++)d.globalDirectionRaw[i]=j.getAsJsonArray("globalDirectionRaw").get(i).getAsInt();
   d.globalConstantSpeed=j.get("globalConstantSpeed").getAsBoolean();
   d.fixedStyle=j.has("fixedStyle")&&j.get("fixedStyle").getAsBoolean();
   definitions.put(Integer.parseInt(entry.getKey()),d);
  }
  // This is a test client with exclusive renderer ownership. The production client is never attached.
  if(!client.isGpu()) {
   Object provider=client.getBufferProvider();provider.getClass().getMethod("om",boolean.class).invoke(provider,true);
   provider.getClass().getMethod("rj").invoke(provider);
  }
  ParticleRuntime.install(this::sample,()->{try{beginScene();}catch(Exception e){throw new IllegalStateException(e);}},
      ()->{try{draw();}catch(Exception e){throw new IllegalStateException(e);}});
  ParticleRuntime.installHd(renderer->{try{drawHd(renderer);}catch(Exception e){throw new IllegalStateException(e);}});
  ParticleRuntime.installAppearance(styles::apply);
  preview=new NativeCapePreview(client,styles,definitions);overlays.add(preview);
  itemStats=new CapeItemStats(client,itemManager,tooltips);overlays.add(itemStats);itemStats.refresh();
  configManager.setConfiguration("itemstats","alwaysShowBaseStats",true);
  ModelProbe.event("particle-preview-ready","softwareDepth=true sourceMaterial744=false globalEffector6=true timeUnitMs=20 exact646Simulation=false");
 }
 @Override protected void shutDown() {
  ParticleRuntime.clear();system.clear();emitters=Collections.emptyList();
  actors.clear();if(styles!=null)styles.close();
  if(preview!=null){overlays.remove(preview);preview=null;}
  if(itemStats!=null){overlays.remove(itemStats);itemStats.close();itemStats=null;}
  if(surface!=null && !client.isGpu() && client.getDrawCallbacks()==null)
      try{surface.selectDepthRasterizer(false);}catch(Exception ignored){}
 }
 private void beginScene() throws Exception {
  boolean gpu=client.isGpu();
  if(lastGpu==null || lastGpu!=gpu){system.clear();actors.clear();lastFrame=0;emitters=Collections.emptyList();lastGpu=gpu;}
  if(gpu || client.getDrawCallbacks()!=null)return;
  if(!surface.hasDepth()) {
   Object provider=client.getBufferProvider();provider.getClass().getMethod("om",boolean.class).invoke(provider,true);
   provider.getClass().getMethod("rj").invoke(provider);
  }
  surface.selectDepthRasterizer(true);
 }
 @net.runelite.client.eventbus.Subscribe public void onGameTick(net.runelite.api.events.GameTick event){if(itemStats!=null)itemStats.refresh();}
 private void sample(Object actor,List<AttachmentRegistry.Triangle> triangles) {
  if(!(actor instanceof Actor))return;
  Actor player=(Actor)actor;LocalPoint local=player.getLocalLocation();
  if(local==null||triangles.isEmpty())return;
  if(!actors.containsKey(player)&&actors.size()>=16)return;
  ActorParticles current=actors.computeIfAbsent(player,k->new ActorParticles());
  CapeStyles.Style style=styles.style(actor);
  boolean fixed=true;for(var triangle:triangles){var def=definitions.get(triangle.definitionId);fixed &= def!=null&&def.fixedStyle;}
  String key=fixed?"fixed:"+triangles.get(0).definitionId:style.key();if(!key.equals(current.styleKey)){current.system.clear();current.styleKey=key;current.defs.clear();}
  current.seen=System.nanoTime();
  float height=Perspective.getTileHeight(client,local,client.getPlane());
  double angle=player.getOrientation()*Math.PI/1024,cos=Math.cos(angle),sin=Math.sin(angle);
  List<ParticleSystem.Emitter> result=new ArrayList<>();
  for(var t:triangles) {
   ParticleSystem.Definition source=definitions.get(t.definitionId);if(source==null)continue;
   ParticleSystem.Definition d=current.defs.computeIfAbsent(t.definitionId,k->styles.customize(source,style));
   float[] xyz=t.xyz.clone();for(int i=0;i<9;i+=3) {
    float x=xyz[i],z=xyz[i+2];xyz[i]=(float)(x*cos+z*sin)+local.getX();
    // Negative model Y is up: lift only the animated emitter anchors to the cape hem.
    xyz[i+1]+=height-12;xyz[i+2]=(float)(z*cos-x*sin)+local.getY();
   }
   result.add(new ParticleSystem.Emitter(t.faceId,xyz,d));
  }
  current.emitters=result;
 }
 private void draw() throws Exception {
  if(client.getGameState()!=GameState.LOGGED_IN){system.clear();actors.clear();emitters=Collections.emptyList();lastFrame=0;return;}
  if(client.isGpu()||client.getDrawCallbacks()!=null)return;
  long now=System.nanoTime();double units=lastFrame==0?1:Math.min(4,(now-lastFrame)/20_000_000.0);lastFrame=now;
  if(units<=0)return;List<ParticleSystem.Particle> particles=stepActors(units);
  NativeDepthSurface.Surface frame=surface.capture();
  Projection projection=(Projection)surface.projection(client.getScene());
  if(projection==null)return;
  int vx=client.getViewportXOffset(),vy=client.getViewportYOffset(),vw=client.getViewportWidth(),vh=client.getViewportHeight(),zoom=client.get3dZoom();
  List<Projected> projected=new ArrayList<>();
  for(var p:particles) {
   float[] camera=projection.project((float)p.x,(float)p.y,(float)p.z);
   double cx=camera[0],py=camera[1],pz=camera[2];
   if(pz<=75)continue;
   projected.add(new Projected((float)(vx+vw/2.0+cx*zoom/pz),(float)(vy+vh/2.0+py*zoom/pz),
        (float)Math.min(128,Math.max(.5,p.radius*zoom/pz)),(float)pz,p.colour));
  }
  projected.sort((a,b)->Float.compare(b.z,a.z));int drawn=0;
  for(var p:projected)drawn+=frame.softParticle(p.x,p.y,p.radius,surface.depth(p.z),p.colour,vx,vy,vw,vh);
  if(++frames%100==0)ModelProbe.event("particle-frame","emitters="+emitters.size()+" particles="+system.particles().size()+" blendedPixels="+drawn);
  // An actor not rendered in the next scene must not keep emitting from a stale model.
  emitters=Collections.emptyList();
 }
 private void drawHd(Object renderer)throws Exception {
  if(client.getGameState()!=GameState.LOGGED_IN || !client.isGpu()){system.clear();actors.clear();emitters=Collections.emptyList();lastFrame=0;return;}
  long now=System.nanoTime();double units=lastFrame==0?1:Math.min(4,(now-lastFrame)/20_000_000.0);lastFrame=now;
  if(units<=0)return;
  hd.draw(renderer,stepActors(units));emitters=Collections.emptyList();
 }
 private List<ParticleSystem.Particle> stepActors(double units) {
  List<ParticleSystem.Particle> result=new ArrayList<>();long now=System.nanoTime();
  for(Iterator<ActorParticles> i=actors.values().iterator();i.hasNext();) {
   ActorParticles a=i.next();if(now-a.seen>5_000_000_000L){i.remove();continue;}
   a.system.step(a.emitters,units);a.emitters=Collections.emptyList();
   for(var particle:a.system.particles())if(result.size()<ParticleSystem.LIMIT)result.add(particle);
  }
  return result;
 }
 private static final class ActorParticles {
  final ParticleSystem system=new ParticleSystem(646);long seen;String styleKey;
  List<ParticleSystem.Emitter> emitters=Collections.emptyList();
  final Map<Integer,ParticleSystem.Definition> defs=new HashMap<>();
 }
 private static final class Projected {
  final float x,y,radius,z;final int colour;
  Projected(float x,float y,float radius,float z,int colour){this.x=x;this.y=y;this.radius=radius;this.z=z;this.colour=colour;}
 }
}
