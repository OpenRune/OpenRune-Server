package com.evolvedmind.nero.particles;

import com.evolvedmind.nero.compat.AttachmentRegistry;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.awt.image.DataBufferInt;
import net.runelite.api.*;

/** Executes the real revision-240 decoder/hooks offline; it never starts a game or writes a cache. */
public final class NativeAssetCheck {
    public static void main(String[] args) {
        try { run(args); System.exit(0); }
        catch(Throwable error) { error.printStackTrace(); System.exit(1); }
    }
    private static void run(String[] args) throws Exception {
        Path raw=Path.of(args[0]);
        Class<?> dataType=Class.forName("er");
        Constructor<?> decode=dataType.getDeclaredConstructor(byte[].class);decode.setAccessible(true);
        Field enabled=AttachmentRegistry.class.getDeclaredField("enabled");enabled.setAccessible(true);
        Properties bindings=new Properties();try(var in=Files.newInputStream(Path.of(System.getProperty("nero.compat.bindings")))){bindings.load(in);}
        List<Path> files=new ArrayList<>();try(var stream=Files.walk(raw.resolve("cache/live/7"))){stream.filter(Files::isRegularFile).sorted().forEach(files::add);}
        int textured=0,emitting=0,folded=0,smooth=0;ModelData regular=null;
        for(Path p:files) {
            byte[] bytes=Files.readAllBytes(p);StringBuilder digest=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))digest.append(String.format("%02x",b&255));String hash=digest.toString();
            // Compare scaled native decoding with the exact same decoder before our hook; no inferred coordinates.
            enabled.setBoolean(null,false);ModelData baseline=(ModelData)decode.newInstance((Object)bytes);enabled.setBoolean(null,true);
            ModelData data=(ModelData)decode.newInstance((Object)bytes);
            float scale=Float.parseFloat(bindings.getProperty("model."+hash+".scale","1"));
            require(data.getVerticesCount()==baseline.getVerticesCount(),"vertex count "+p);
            float[][] xyz={data.getVerticesX(),data.getVerticesY(),data.getVerticesZ()},original={baseline.getVerticesX(),baseline.getVerticesY(),baseline.getVerticesZ()};
            for(int axis=0;axis<3;axis++)for(int v=0;v<data.getVerticesCount();v++)require(Float.isFinite(xyz[axis][v])&&xyz[axis][v]==original[axis][v]*scale,"exact decode "+p+" vertex "+v);
            int[][] face={data.getFaceIndices1(),data.getFaceIndices2(),data.getFaceIndices3()};
            for(int f=0;f<data.getFaceCount();f++)for(int c=0;c<3;c++)require(face[c][f]>=0&&face[c][f]<data.getVerticesCount(),"face index "+p);
            var anchors=AttachmentRegistry.triangles(data);
            if(!anchors.isEmpty()) {
                emitting++;require(anchors.size()==7,"seven hem anchors "+p);
                byte[] alpha=data.getFaceTransparencies();for(var a:anchors)require(a.faceId>=782&&a.faceId<=788&&(alpha[a.faceId]&255)==255,"hidden emitter "+p);
            }
            short[] textures=data.getFaceTextures();int count=0,id=-1;
            if(textures!=null)for(int f=0;f<data.getFaceCount();f++)if(textures[f]>=0){count++;if(id==-1)id=textures[f];require(textures[f]==id,"single trim material "+p);require(data.getFaceColors()[f]==127,"native textured color "+p);}
            if(count>0 && (id==40||id==59)) {require(count==70||count==254,"single shell panel coverage "+p+" "+count);textured++;}
            Model lighting=data.light(64,850,-30,-50,-30);
            int modelId=Integer.parseInt(p.getParent().getFileName().toString());
            if(modelId==62100||modelId==62103||modelId>=62200&&(modelId-62200)%6==0){folded++;require(data.getVerticesCount()<400&&data.getFaceCount()<600,"folded inventory mesh "+p);}
            if(modelId==62101||modelId==62102||modelId==62104||modelId==62105||modelId>=62200&&((modelId-62200)%6==1||(modelId-62200)%6==2)) {
                smooth++;
                require(data.getFaceCount()>=792&&data.getFaceCount()<1577,"authored shell with optional native quiver "+p);
                for(int f=0;f<792;f++)if(data.getFaceTransparencies()[f]==0) {
                    require(lighting.getFaceColors3()[f]>=0,"smooth authored cloth "+p+" "+f);
                }
                Set<String> triangles=new HashSet<>();
                for(int f=0;f<data.getFaceCount();f++)if(data.getFaceTransparencies()[f]==0) {
                    int[] vertices={face[0][f],face[1][f],face[2][f]};Arrays.sort(vertices);
                    require(triangles.add(Arrays.toString(vertices)),"no reversed duplicate triangle "+p+" "+f);
                }
                for(int f=782;f<=788;f++)require(lighting.getFaceColors3()[f]==-2,"hidden emitter ignores animated alpha "+p);
            }
            if(p.getParent().getFileName().toString().equals("62101")) regular=data;
        }
        require(files.size()==96,"96 asset models");require(folded==16,"16 original folded inventory meshes "+folded);require(textured==6,"six Fire/Infernal assets "+textured);require(regular!=null,"regular preview mesh");
        Object parts=Array.newInstance(dataType,2);Array.set(parts,0,regular);Array.set(parts,1,regular);
        Constructor<?> merge=dataType.getDeclaredConstructor(parts.getClass(),int.class);merge.setAccessible(true);
        ModelData merged=(ModelData)merge.newInstance(parts,2);
        require(AttachmentRegistry.triangles(merged).size()==14,"merged emitters");
        List<AttachmentRegistry.Triangle> source=AttachmentRegistry.triangles(regular),combined=AttachmentRegistry.triangles(merged);
        for(int n=0;n<14;n++)require(Arrays.equals(source.get(n%7).xyz,combined.get(n).xyz),"merge precision "+n);
        ModelData copy=merged.shallowCopy();require(AttachmentRegistry.triangles(copy).size()==14,"copy emitters");
        Model lit=copy.light(64,850,-30,-50,-30);require(AttachmentRegistry.triangles(lit).size()==14,"light emitters");
        Model animated=(Model)lit.getClass().getMethod("ac",boolean.class).invoke(lit,true);
        require(AttachmentRegistry.triangles(animated).size()==14,"native shared sequence inheritance");
        var snapshot=AttachmentRegistry.triangles(animated).get(0);float saved=snapshot.xyz[0];
        for(int v=0;v<animated.getVerticesCount();v++) animated.getVerticesX()[v]+=17;
        require(snapshot.xyz[0]==saved,"shared sequence immutable emitter snapshot");
        require(Math.abs(AttachmentRegistry.triangles(animated).get(0).xyz[0]-saved-17)<.001,"emitter follows native shared vertices");
        AttachmentRegistry.normalizeAvatarPriorities(lit);
        byte[] before=lit.getFaceBias(),priorities=lit.getFaceRenderPriorities().clone();
        require(before==null,"native cape has no synthetic face bias");
        Set<Byte> groups=new HashSet<>();for(byte group:priorities)groups.add(group);
        require(groups.equals(Set.of((byte)4,(byte)5,(byte)10)),"authored native-compatible priority groups "+groups);
        AttachmentRegistry.normalizeAvatarPriorities(lit);
        require(lit.getFaceBias()==null&&Arrays.equals(priorities,lit.getFaceRenderPriorities()),"native priority and bias preserved");
        // This reproduced the owner's HD failure: animation changes alpha, then
        // a new assembled avatar is normalized. Topology comes from SHA-bound
        // metadata, never by recounting opaque faces or guessing emitter offsets.
        Model relit=merged.light(64,850,-30,-50,-30);
        relit.getFaceTransparencies()[782]=0;relit.getFaceTransparencies()[788]=(byte)128;
        relit.getFaceTransparencies()[4]=(byte)128;
        AttachmentRegistry.normalizeAvatarPriorities(relit);
        require(relit.getFaceBias()==null&&Arrays.equals(priorities,relit.getFaceRenderPriorities()),"animated alpha preserves native ordering");
        ModelData hoodModel=(ModelData)decode.newInstance((Object)Files.readAllBytes(raw.resolve("cache/live/7/62107/0.dat")));
        NativeParticleCheck.run(regular.light(64,850,-30,-50,-30),hoodModel.light());
        Model preview=regular.light(64,850,-30,-50,-30);CapePreviewRaster raster=new CapePreviewRaster(180,240);int[] first=null;
        for(int n=0;n<20;n++){raster.model(preview,false);int[] pixels=((DataBufferInt)raster.image.getRaster().getDataBuffer()).getData();if(first==null)first=pixels.clone();else require(Arrays.equals(first,pixels),"stable upright preview");}
        require(Arrays.stream(first).anyMatch(x->x!=0),"visible preview");
        if(args.length>1) {
            Path images=Path.of(args[1]);Files.createDirectories(images);
            javax.imageio.ImageIO.write(raster.image,"png",images.resolve("cape-back.png").toFile());
            raster.model(preview,true);
            javax.imageio.ImageIO.write(raster.image,"png",images.resolve("cape-front.png").toFile());
            // Label native source palette regions, retaining actual geometry/culling.
            Model labelled=regular.light(64,850,-30,-50,-30);
            int[] palette={65214,65200,65186,62995};int[] colours={960,22464,43584,127};
            for(int f=0;f<regular.getFaceCount();f++)for(int p=0;p<4;p++)if((regular.getFaceColors()[f]&65535)==palette[p]) {
                labelled.getFaceColors1()[f]=colours[p];labelled.getFaceColors2()[f]=colours[p];labelled.getFaceColors3()[f]=colours[p];
            }
            raster.model(labelled,false);javax.imageio.ImageIO.write(raster.image,"png",images.resolve("cape-regions.png").toFile());
            NativeIconCheck.run(raw,images.resolve("inventory"));
            NativeWornCheck.run(raw,images.resolve("worn"));
        }
        // Actual native ItemStats conversion, including magic damage's tenths and no second overlay.
        for(int id=63000;id<=63143;id++)if(CapeItemStats.custom(id)) {
            final int objId=id;
            ItemComposition item=(ItemComposition)Proxy.newProxyInstance(NativeAssetCheck.class.getClassLoader(),new Class<?>[]{ItemComposition.class},(proxy,method,a)->{
                if(method.getName().equals("getId"))return objId;
                if(method.getName().equals("getIntValue"))return (int)a[0]==299?20:(int)a[0]==11?6:2;
                throw new UnsupportedOperationException(method.getName());
            });
            var stats=CapeItemStats.fromCache(item).getEquipment();require(stats.getMdmg()==2f&&stats.getPrayer()==6,"native item stats "+id);
            boolean hood=id==63002||id==63003||id>=63100&&(id-63100)%2==1;require(stats.getSlot()==(hood?0:1),"item slot "+id);
        }
        System.out.println("PASS_NATIVE_VISUAL_ASSETS models="+files.size()+" folded="+folded+" smooth="+smooth+" emitting="+emitting+" textured="+textured+" exactDecode=true mergeCopyLight=true sharedSequence=true animatedAlpha=true nativePriorities=true generatedReverseFaces=0 stablePreviewFrames=20 itemStats=48");
    }
    private static void require(boolean test,String detail){if(!test)throw new AssertionError(detail);}
}
