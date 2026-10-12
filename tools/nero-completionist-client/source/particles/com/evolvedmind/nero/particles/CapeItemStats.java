package com.evolvedmind.nero.particles;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import net.runelite.api.*;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.tooltip.*;

/** Custom item IDs are absent from RuneLite's public stats feed. Use cache params. */
final class CapeItemStats extends Overlay implements AutoCloseable {
 private final Client client;
 private final ItemManager items;
 private final TooltipManager tooltips;
 private final Field statsField;
 private Map<Integer,ItemStats> installed;
 private final Map<Integer,ItemStats> owned=new HashMap<>(),prior=new HashMap<>();
 CapeItemStats(Client client,ItemManager items,TooltipManager tooltips)throws Exception {
  this.client=client;this.items=items;this.tooltips=tooltips;
  statsField=ItemManager.class.getDeclaredField("itemStats");statsField.setAccessible(true);
  setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ABOVE_WIDGETS);
 }
 static boolean custom(int id){return id>=63000&&id<=63003||id>=63100&&id<=63143;}
 static ItemStats fromCache(ItemComposition item) {
  boolean hood=item.getId()==63002||item.getId()==63003||item.getId()>=63100&&(item.getId()-63100)%2==1;
  ItemEquipmentStats equipment=ItemEquipmentStats.builder().slot(hood?0:1)
   .astab(item.getIntValue(0)).aslash(item.getIntValue(1)).acrush(item.getIntValue(2)).amagic(item.getIntValue(3)).arange(item.getIntValue(4))
   .dstab(item.getIntValue(5)).dslash(item.getIntValue(6)).dcrush(item.getIntValue(7)).dmagic(item.getIntValue(8)).drange(item.getIntValue(9))
   .str(item.getIntValue(10)).rstr(item.getIntValue(12)+item.getIntValue(189)).mdmg(item.getIntValue(299)/10f).prayer(item.getIntValue(11)).aspeed(0).build();
  return new ItemStats(true,hood?.3:.453,0,equipment);
 }
 @SuppressWarnings("unchecked") void refresh() {
  if(client.getGameState()!=GameState.LOGGED_IN)return;
  try {
   Map<Integer,ItemStats> current=(Map<Integer,ItemStats>)statsField.get(items);if(current==null||current==installed)return;
   Map<Integer,ItemStats> copy=new HashMap<>(current);
   for(int id=63000;id<=63143;id++)if(custom(id)) {
    ItemStats existing=current.get(id);if(existing!=owned.get(id))prior.put(id,existing);
    ItemStats value=fromCache(client.getItemDefinition(id));owned.put(id,value);copy.put(id,value);
   }
   statsField.set(items,copy);installed=copy;
  }catch(Exception error){com.evolvedmind.nero.compat.ModelProbe.event("cape-stats-failed",error.toString());}
 }
 @Override public Dimension render(Graphics2D graphics) {
  // RuneLite Item Stats owns the single tooltip; this class only supplies data.
  return null;
 }
 private static String signed(int value){return value>=0?"+"+value:""+value;}
 @SuppressWarnings("unchecked") public void close() {
  try {
   Map<Integer,ItemStats> current=(Map<Integer,ItemStats>)statsField.get(items);if(current==null)return;
   Map<Integer,ItemStats> restored=new HashMap<>(current);
   for(var entry:owned.entrySet())if(current.get(entry.getKey())==entry.getValue()){ItemStats before=prior.get(entry.getKey());if(before==null)restored.remove(entry.getKey());else restored.put(entry.getKey(),before);}
   statsField.set(items,restored);
  }catch(Exception error){com.evolvedmind.nero.compat.ModelProbe.event("cape-stats-cleanup-failed",error.toString());}
  owned.clear();prior.clear();installed=null;
 }
}
