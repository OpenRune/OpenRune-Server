package com.evolvedmind.nero.particles;

import com.evolvedmind.nero.compat.AttachmentRegistry;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.*;

/** Uses the real worn cache mesh, current appearance and the same particle simulator. */
final class NativeCapePreview extends Overlay {
 private final Client client;
 private final CapeStyles styles;
 private final Map<Integer,ParticleSystem.Definition> definitions;
 private final ParticleSystem system=new ParticleSystem(646);
 private Model model;
 private boolean hood;
 private String styleKey="";
 private long lastFrame;
 NativeCapePreview(Client client,CapeStyles styles,Map<Integer,ParticleSystem.Definition> definitions) {
  this.client=client;this.styles=styles;this.definitions=definitions;
  setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ABOVE_WIDGETS);setPriority(OverlayPriority.HIGH);
 }
 @Override public Dimension render(Graphics2D graphics) {
  CapeStyles.Style style=styles.style(null);Widget widget=client.getWidget(3000,36);
  if(!style.editing||widget==null||widget.isHidden()) {system.clear();lastFrame=0;return null;}
  Rectangle bounds=widget.getBounds();if(bounds.width<=0||bounds.height<=0)return null;
  try {
   if(model==null||hood!=style.previewHood){hood=style.previewHood;ModelData data=client.loadModelData(hood?62107:62101);if(data==null)return null;model=data.light(64,850,-30,-50,-30);system.clear();styleKey="";}
   styles.apply(model,null);
   String key=style.key()+":"+hood+":"+style.previewParticles;
   if(!key.equals(styleKey)){system.clear();styleKey=key;lastFrame=0;}
   CapePreviewRaster raster=new CapePreviewRaster(bounds.width,bounds.height);raster.model(model,hood);
   long now=System.nanoTime();double units=lastFrame==0?1:Math.min(4,Math.max(.001,(now-lastFrame)/20_000_000.0));lastFrame=now;
   if(style.previewParticles&&!hood) {
    var emitters=new ArrayList<ParticleSystem.Emitter>();
    for(var t:AttachmentRegistry.triangles(model)) {
     var definition=definitions.get(t.definitionId);if(definition==null)continue;
     // The UI shows an unposed mesh: its original emitter faces already follow
     // the hem. The world-only animation lift moved this preview above the hem.
     float[] xyz=t.xyz.clone();
     emitters.add(new ParticleSystem.Emitter(t.faceId,xyz,styles.customize(definition,style)));
    }
    system.step(emitters,units);raster.particles(system.particles());
   } else system.clear();
   graphics.drawImage(raster.image,bounds.x,bounds.y,null);
  } catch(Exception error) {system.clear();com.evolvedmind.nero.compat.ModelProbe.event("customiser-preview-failed",error.toString());}
  return null;
 }
}
