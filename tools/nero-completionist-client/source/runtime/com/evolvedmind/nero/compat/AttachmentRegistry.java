package com.evolvedmind.nero.compat;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.security.MessageDigest;

/** Keeps emitter face IDs separate from game model data, including shared animation topology. */
public final class AttachmentRegistry {
    private static final Map<String,List<Emitter>> templates = new HashMap<>();
    private static final Map<int[],Binding> bindings = new WeakHashMap<>();
    private static final Map<String,List<PaletteFace>> paletteTemplates = new HashMap<>();
    private static final Map<String,Float> scales = new HashMap<>();
    private static final Map<int[],Boolean> precise = new WeakHashMap<>();
    private static final Map<int[],PaletteBinding> palettes = new WeakHashMap<>();
    private static boolean enabled;

    public static synchronized void configure(String path) throws Exception {
        templates.clear();
        bindings.clear();
        paletteTemplates.clear();palettes.clear();
        scales.clear();
        precise.clear();
        enabled = path != null && !path.isEmpty();
        if (!enabled) return;
        Properties profile = new Properties();
        try (var input = Files.newInputStream(Path.of(path))) { profile.load(input); }
        if (!"1".equals(profile.getProperty("schemaVersion"))) throw new IllegalArgumentException("Unknown bindings schema");
        for (String key : profile.stringPropertyNames()) {
            if(key.startsWith("model.") && key.endsWith(".scale")) {
                String hash=key.substring(6,key.length()-6);
                float scale=Float.parseFloat(profile.getProperty(key));
                if(!hash.matches("[a-f0-9]{64}") || scale!=.25f)throw new IllegalArgumentException("Invalid precision model scale");
                scales.put(hash,scale);continue;
            }
            if(key.startsWith("model.") && key.endsWith(".palette")) {
                String hash=key.substring(6,key.length()-8);
                if(!hash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid palette hash");
                List<PaletteFace> faces=new ArrayList<>();
                for(String token:profile.getProperty(key).split(",")) {
                    if(token.isEmpty())continue;
                    String[] pair=token.split(":");int face=Integer.parseInt(pair[0]),channel=Integer.parseInt(pair[1]);
                    if(face<0||face>65535||channel<0||channel>3||faces.size()>65535)throw new IllegalArgumentException("Invalid palette binding");
                    faces.add(new PaletteFace(face,channel));
                }
                paletteTemplates.put(hash,List.copyOf(faces));continue;
            }
            if (!key.startsWith("model.") || !key.endsWith(".emitters")) continue;
            String hash = key.substring(6,key.length()-9);
            if (!hash.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid model hash");
            List<Emitter> emitters = new ArrayList<>();
            for (String token : profile.getProperty(key).split(",")) {
                if (token.isEmpty()) continue;
                String[] pair = token.split(":");
                if (pair.length != 2) throw new IllegalArgumentException("Invalid emitter binding");
                int definition = Integer.parseInt(pair[0]),face = Integer.parseInt(pair[1]);
                if (definition < 0 || definition > 65535 || face < 0 || face > 65535)
                    throw new IllegalArgumentException("Emitter binding outside range");
                emitters.add(new Emitter(definition,face));
            }
            if (emitters.size() > 255 || templates.size() > 4096) throw new IllegalArgumentException("Bindings limit exceeded");
            templates.put(hash,Collections.unmodifiableList(emitters));
        }
    }

    public static void decoded(Object model,byte[] data) {
        if (!enabled) return;
        try {
            StringBuilder hash = new StringBuilder();
            for (byte value : MessageDigest.getInstance("SHA-256").digest(data)) hash.append(String.format("%02x",value & 255));
            Float scale=scales.get(hash.toString());
            if(scale!=null) {
                // A paired update can replace an already-qualified test-cache
                // model without rewriting an active server's cache. Unknown
                // source hashes never enter this branch. Both ends are SHA-bound.
                Properties profile=new Properties();
                Path bindingFile=Path.of(System.getProperty("nero.compat.bindings"));
                try(var in=Files.newInputStream(bindingFile)){profile.load(in);}
                String replacement=profile.getProperty("model."+hash+".replacement");
                if(replacement!=null) {
                    if(!replacement.matches("[a-f0-9]{64}")||replacement.equals(hash.toString())||profile.containsKey("model."+replacement+".replacement"))throw new IllegalArgumentException("Invalid model replacement");
                    byte[] bytes=Files.readAllBytes(bindingFile.resolveSibling("models").resolve(replacement+".dat"));
                    StringBuilder digest=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(bytes))digest.append(String.format("%02x",value&255));
                    if(!digest.toString().equals(replacement)||!scales.containsKey(replacement))throw new IllegalArgumentException("Unqualified replacement asset");
                    var ctor=model.getClass().getDeclaredConstructor(byte[].class);ctor.setAccessible(true);Object updated=ctor.newInstance((Object)bytes);
                    for(var field:model.getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())){field.setAccessible(true);field.set(model,field.get(updated));}
                    return;
                }
                for(String axis:new String[]{"X","Y","Z"}) {
                    float[] vertices=(float[])model.getClass().getMethod("getVertices"+axis).invoke(model);
                    for(int i=0;i<vertices.length;i++)vertices[i]*=scale;
                }
                synchronized(AttachmentRegistry.class){precise.put(indices(model,1),true);}
            }
            List<Emitter> emitters = templates.get(hash.toString());
            if (emitters != null && !emitters.isEmpty()) register(model,emitters);
            List<PaletteFace> palette=paletteTemplates.get(hash.toString());
            if(palette!=null && !palette.isEmpty())registerPalette(model,palette);
        } catch (Throwable error) { failed(error); }
    }

    /** A cross-revision cape and OSRS body must use one opaque-face depth order.
     * Applying a group to the cape alone leaves the torso in a later painter layer.
     * Only a bound, assembled player model is affected; source/cache models stay intact.
     */
    public static void normalizeAvatarPriorities(Object model) throws Exception {
        if(model==null || find(model).isEmpty() || faceCount(model)<=792)return;
        Class<?> type=model.getClass();
        byte[] priorities=(byte[])type.getMethod("getFaceRenderPriorities").invoke(model);
        if(priorities==null)return;
        byte[] alpha=(byte[])type.getMethod("getFaceTransparencies").invoke(model);
        int count=faceCount(model);
        if(priorities.length<count || (alpha!=null && alpha.length<count))throw new IllegalArgumentException("Invalid avatar face arrays");
        for(int i=0;i<count;i++)if(alpha==null || alpha[i]==0)priorities[i]=0;
        // The source uses priority 5 for details laid over priority 4 cloth.
        // The zone renderer uses native face bias instead of painter priorities.
        // Restore only those layers in SHA-bound Completionist topology; retain
        // all ordinary equipment bias and exclude the hidden emitter triangles.
        byte[] bias=(byte[])type.getMethod("getFaceBias").invoke(model);
        if(bias==null)bias=new byte[count];else if(bias.length<count)throw new IllegalArgumentException("Invalid face bias");
        boolean changed=false;
        for(Emitter emitter:find(model)) {
            int start=emitter.faceId-782;if(start<0||start+1577>count||bias[start+782]==126)continue;
            // Only the first of the seven consecutive emitter faces identifies
            // the start. The others are already marked by this iteration.
            if(alpha==null||alpha[start+782]!=(byte)255||alpha[start+788]!=(byte)255)continue;
            byte[] layers=java.util.Base64.getDecoder().decode("AAABAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQMDAwMDAwMDAwMDAwMDAwMDAwICAgICAwMDAwMDAwMDAwMDAwMDAwICAgICAgICAgICAgICAAAAAAMDAwMDAwMDAwMDAwMBAQEBAwMDAwMDAwEBAAAAAQEBAQEBAQEBAQEBAQEAAAEBAQEBAQEBAQEBAQEBAAABAQEBAQEBAQEBAQEBAQEBAQAAAAAAAAAAAAEBAQEAAQEAAAAAAAABAQEBAQAAAQEBAAABAQAAAAABAQAAAQEBAAABAQEBAQAAAAAAAQEAAAEBAAAAAQEBAQEBAQAAAAAAAAABAQEBAAAAAAAAAAAAAQEAAQAAAgIDAwEAAAEBAAAAAAAAAAABAQEAAAAAAAEBAAAAAgICAgICAgICAgIDAwICAwMCAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAAEBAAACAgMDAwMDAwMDAwMDAwMDAgICAwMDAwICAAEBAAABAQEAAAAAAgIDAwMCAwMDAwMDAwEBAQEBAAEDAwMDAwMDAwMDAwICAwMDAwEBAQEBAQEBAAABAwMCAAABAQECAgIDAwMDAQEBAQEBAQEAAAABAQAAAQEBAQMDAgICAgMDAgIAAAECAQEAAAABAQEAAAAAAgICAwMDAgAAAAAAAAAAAAIDAwMDAwICAgIDAQEBAwMDAwMDAwMDAwICAgICAgICAgMDAwMDAwMDAwAAAgICAgICAwMDAwMDAwMDAgICAwMDAgICAgICAgICAQEDAwMBAQMDAQEBAQAAAAEBAQEAAAMDAwMDAwMDAwMDAwMDAwEDAwMDAwMDAwMDAwEBAQAAAAAAAAAAAQEBAQAAAgIAAAAAAAAAAAAAAAAAAQEBAQAAAQEBAAAAAAAAAAABAQEBAAEBAQEBAQEBAQEBAQEAAAEBAQEBAQEBAAABAQEBAQEBAQEBAQEBAQEBAQAAAAAAAAAAAAABAQEBAgICAgACAgEAAAAAAQEAAQEBAQAAAQEAAAAAAQEBAQECAgIAAAACAgAAAgICAAAAAAAAAAAAAAAA");
            int reverse=start+792;
            for(int f=0;f<792;f++) {
                bias[start+f]=layers[f];
                if(alpha[start+f]!=(byte)255)bias[reverse++]=layers[f];
            }
            if(reverse!=start+1577)throw new IllegalArgumentException("Unexpected Completionist reverse topology");
            bias[start+782]=126;changed=true;
        }
        if(changed){java.lang.reflect.Field field=type.getDeclaredField("cl");field.setAccessible(true);field.set(model,bias);}

    }

    public static void merged(Object model,Object parts,int count) {
        if (!enabled) return;
        try {
            if (count < 0 || count > Array.getLength(parts)) throw new IllegalArgumentException("Invalid merge count");
            preserveMergePrecision(model,parts,count);
            List<Emitter> result = new ArrayList<>();
            List<PaletteFace> palette=new ArrayList<>();
            int offset = 0;
            for (int i = 0; i < count; i++) {
                Object part = Array.get(parts,i);
                if (part == null) continue;
                for (Emitter emitter : find(part)) result.add(new Emitter(emitter.definitionId,emitter.faceId+offset));
                for(PaletteFace face:palette(part))palette.add(new PaletteFace(face.faceId+offset,face.channel));
                offset = Math.addExact(offset,faceCount(part));
            }
            if (!result.isEmpty()) {
                if (faceCount(model) != offset) throw new IllegalArgumentException("Merge face count mismatch");
                register(model,result);
            }
            if(!palette.isEmpty())registerPalette(model,palette);
        } catch (Throwable error) { failed(error); }
    }

    public static void inherited(Object result,Object source) {
        if (!enabled || result == null || result == source) return;
        try {
            List<Emitter> emitters = find(source);
            List<PaletteFace> palette=palette(source);
            if (emitters.isEmpty() && palette.isEmpty()) return;
            if (faceCount(source) != faceCount(result)) return;
            for (int i = 1; i <= 3; i++) {
                if (!Arrays.equals(indices(source,i),indices(result,i))) return;
            }
            if(!emitters.isEmpty())register(result,emitters);
            if(!palette.isEmpty())registerPalette(result,palette);
            synchronized(AttachmentRegistry.class){if(precise.containsKey(indices(source,1)))precise.put(indices(result,1),true);}
        } catch (Throwable error) { failed(error); }
    }

    /** Native ModelData deduplication truncates float coordinates to integers.
     * Keep its ordinary equipment/textures intact and append exact vertices only
     * for our SHA-bound, untextured precision parts, redirecting their own faces.
     */
    private static synchronized void preserveMergePrecision(Object model,Object parts,int count)throws Exception {
        if(!model.getClass().getName().equals("er"))return;
        Class<?> type=model.getClass();int base=(Integer)type.getMethod("getVerticesCount").invoke(model),extra=0;
        for(int i=0;i<count;i++){Object part=Array.get(parts,i);if(part!=null&&precise.containsKey(indices(part,1)))extra=Math.addExact(extra,(Integer)type.getMethod("getVerticesCount").invoke(part));}
        if(extra==0)return;if(base+extra>65535)throw new IllegalArgumentException("Precision merge too large");
        float[][] xyz=new float[3][];
        for(int a=0;a<3;a++)xyz[a]=Arrays.copyOf((float[])type.getMethod("getVertices"+"XYZ".charAt(a)).invoke(model),base+extra);
        java.lang.reflect.Field skinField=type.getDeclaredField("bk");skinField.setAccessible(true);
        int[] oldSkins=(int[])skinField.get(model),skins=oldSkins==null?new int[base+extra]:Arrays.copyOf(oldSkins,base+extra);
        if(oldSkins==null)Arrays.fill(skins,255);
        int[][] corners={indices(model,1),indices(model,2),indices(model,3)};int vertexOffset=base,faceOffset=0;
        for(int i=0;i<count;i++) {
            Object part=Array.get(parts,i);if(part==null)continue;int faces=faceCount(part);
            if(precise.containsKey(indices(part,1))) {
                int vertices=(Integer)type.getMethod("getVerticesCount").invoke(part);
                for(int a=0;a<3;a++){float[] source=(float[])type.getMethod("getVertices"+"XYZ".charAt(a)).invoke(part);System.arraycopy(source,0,xyz[a],vertexOffset,vertices);int[] sourceFaces=indices(part,a+1);for(int f=0;f<faces;f++)corners[a][faceOffset+f]=vertexOffset+sourceFaces[f];}
                int[] sourceSkins=(int[])skinField.get(part);
                if(sourceSkins!=null)System.arraycopy(sourceSkins,0,skins,vertexOffset,vertices);
                else {
                    java.lang.reflect.Field groupsField=type.getDeclaredField("ba");groupsField.setAccessible(true);int[][] groups=(int[][])groupsField.get(part);
                    Arrays.fill(skins,vertexOffset,vertexOffset+vertices,255);
                    if(groups!=null)for(int g=0;g<groups.length;g++)if(groups[g]!=null)for(int v:groups[g])skins[vertexOffset+v]=g;
                }
                // Our precision assets use legacy vertex labels, not Animaya weights.
                for(String name:new String[]{"bg","bj"}){java.lang.reflect.Field field=type.getDeclaredField(name);field.setAccessible(true);if(field.get(part)!=null)throw new IllegalArgumentException("Weighted precision part unsupported");}
                vertexOffset+=vertices;
            }
            faceOffset+=faces;
        }
        for(int a=0;a<3;a++){java.lang.reflect.Field field=type.getDeclaredField(new String[]{"jc","mp","sk"}[a]);field.setAccessible(true);field.set(model,xyz[a]);}
        skinField.set(model,skins);java.lang.reflect.Field size=type.getDeclaredField("aa");size.setAccessible(true);size.setInt(model,base+extra);
        // Preserve ordinary parts' skeletal arrays while extending the unused
        // appended legacy entries. Their bone animation remains in vertex labels.
        for(String name:new String[]{"bg","bj"}){java.lang.reflect.Field field=type.getDeclaredField(name);field.setAccessible(true);int[][] data=(int[][])field.get(model);if(data!=null)field.set(model,Arrays.copyOf(data,base+extra));}
        precise.put(indices(model,1),true);
    }

    public static synchronized List<Emitter> find(Object model) throws Exception {
        if (!enabled || model == null || bindings.isEmpty()) return Collections.emptyList();
        Binding binding = bindings.get(indices(model,1));
        if (binding == null || binding.second.get() != indices(model,2) || binding.third.get() != indices(model,3))
            return Collections.emptyList();
        return binding.emitters;
    }

    public static synchronized void register(Object model,List<Emitter> emitters) throws Exception {
        int count = faceCount(model);
        for (Emitter emitter : emitters) if (emitter.faceId < 0 || emitter.faceId >= count)
            throw new IllegalArgumentException("Emitter face outside model");
        if (bindings.size() > 4096) throw new IllegalStateException("Live model bindings limit exceeded");
        bindings.put(indices(model,1),new Binding(indices(model,2),indices(model,3),List.copyOf(emitters)));
    }

    public static synchronized List<PaletteFace> palette(Object model)throws Exception {
        if(!enabled || model==null)return Collections.emptyList();
        PaletteBinding binding=palettes.get(indices(model,1));
        if(binding==null || binding.second.get()!=indices(model,2) || binding.third.get()!=indices(model,3))return Collections.emptyList();
        return binding.faces;
    }

    private static synchronized void registerPalette(Object model,List<PaletteFace> faces)throws Exception {
        int count=faceCount(model);
        for(PaletteFace face:faces)if(face.faceId>=count)throw new IllegalArgumentException("Palette face outside model");
        if(palettes.size()>4096)throw new IllegalStateException("Live palette limit exceeded");
        palettes.put(indices(model,1),new PaletteBinding(indices(model,2),indices(model,3),faces));
    }

    public static final class PaletteFace {
        public final int faceId,channel;
        public PaletteFace(int face,int channel){this.faceId=face;this.channel=channel;}
    }

    private static final class PaletteBinding {
        final WeakReference<int[]> second,third;final List<PaletteFace> faces;
        PaletteBinding(int[] second,int[] third,List<PaletteFace> faces){this.second=new WeakReference<>(second);this.third=new WeakReference<>(third);this.faces=List.copyOf(faces);}
    }

    public static List<Anchor> anchors(Object model) {
        try {
            List<Emitter> emitters = find(model);
            if (emitters.isEmpty()) return Collections.emptyList();
            Class<?> type = model.getClass();
            float[] x = (float[]) type.getMethod("getVerticesX").invoke(model);
            float[] y = (float[]) type.getMethod("getVerticesY").invoke(model);
            float[] z = (float[]) type.getMethod("getVerticesZ").invoke(model);
            int[][] faces = {indices(model,1),indices(model,2),indices(model,3)};
            List<Anchor> result = new ArrayList<>();
            for (Emitter emitter : emitters) {
                int a = faces[0][emitter.faceId],b = faces[1][emitter.faceId],c = faces[2][emitter.faceId];
                result.add(new Anchor(emitter.definitionId,emitter.faceId,
                    (x[a]+x[b]+x[c])/3f,(y[a]+y[b]+y[c])/3f,(z[a]+z[b]+z[c])/3f));
            }
            return result;
        } catch (Throwable error) { failed(error); return Collections.emptyList(); }
    }

    /** Snapshot all three animated vertices immediately; native shared models are reused later. */
    public static List<Triangle> triangles(Object model) throws Exception {
        List<Emitter> emitters=find(model);
        if(emitters.isEmpty()) return Collections.emptyList();
        Class<?> type=model.getClass();
        float[] x=(float[])type.getMethod("getVerticesX").invoke(model);
        float[] y=(float[])type.getMethod("getVerticesY").invoke(model);
        float[] z=(float[])type.getMethod("getVerticesZ").invoke(model);
        int[][] faces={indices(model,1),indices(model,2),indices(model,3)};
        List<Triangle> result=new ArrayList<>();
        for(Emitter emitter:emitters) {
            float[] xyz=new float[9];
            for(int corner=0;corner<3;corner++) {
                int vertex=faces[corner][emitter.faceId];
                xyz[corner*3]=x[vertex];xyz[corner*3+1]=y[vertex];xyz[corner*3+2]=z[vertex];
            }
            result.add(new Triangle(emitter.definitionId,emitter.faceId,xyz));
        }
        return result;
    }

    public static final class Triangle {
        public final int definitionId,faceId;
        public final float[] xyz;
        public Triangle(int definitionId,int faceId,float[] xyz) {
            if(xyz.length!=9)throw new IllegalArgumentException("Triangle must have three vertices");
            this.definitionId=definitionId;this.faceId=faceId;this.xyz=xyz.clone();
        }
    }

    private static int faceCount(Object model) throws Exception {
        return (Integer) model.getClass().getMethod("getFaceCount").invoke(model);
    }

    private static int[] indices(Object model,int corner) throws Exception {
        return (int[]) model.getClass().getMethod("getFaceIndices"+corner).invoke(model);
    }

    private static synchronized void failed(Throwable error) {
        enabled = false;
        bindings.clear();
        palettes.clear();
        ModelProbe.event("attachments-disabled",error.getClass().getSimpleName());
    }

    public static final class Emitter {
        public final int definitionId,faceId;
        public Emitter(int definitionId,int faceId) { this.definitionId=definitionId; this.faceId=faceId; }
    }

    public static final class Anchor {
        public final int definitionId,faceId;
        public final float x,y,z;
        public Anchor(int definitionId,int faceId,float x,float y,float z) {
            this.definitionId=definitionId; this.faceId=faceId; this.x=x; this.y=y; this.z=z;
        }
    }

    private static final class Binding {
        final WeakReference<int[]> second,third;
        final List<Emitter> emitters;
        Binding(int[] second,int[] third,List<Emitter> emitters) {
            this.second=new WeakReference<>(second); this.third=new WeakReference<>(third); this.emitters=emitters;
        }
    }
}
