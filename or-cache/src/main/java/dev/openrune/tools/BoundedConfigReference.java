package dev.openrune.tools;
import com.displee.cache.CacheLibrary;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Keeps native file-ID metadata, including the signed -1 varp sentinel, intact. */
public final class BoundedConfigReference {
  private record Layout(int protocol, int flags, int[] archives, Map<Integer,List<int[]>> fields, int prefix, Map<Integer,int[]> counts, Map<Integer,int[]> files, Map<Integer,int[]> names) {}

  private static int smart(ByteBuffer in) {
    if (in.get(in.position()) < 0) return in.getInt() & 0x7fffffff;
    int value = Short.toUnsignedInt(in.getShort());
    return value == 32767 ? -1 : value;
  }

  private static int count(ByteBuffer in, int protocol) {
    return protocol >= 7 ? smart(in) : Short.toUnsignedInt(in.getShort());
  }

  private static Layout layout(byte[] bytes) {
    ByteBuffer in = ByteBuffer.wrap(bytes);
    int protocol = Byte.toUnsignedInt(in.get());
    if (protocol < 5 || protocol > 7) throw new IllegalStateException("Unsupported reference protocol");
    if (protocol >= 6) in.getInt();
    int flags = Byte.toUnsignedInt(in.get());
    if ((flags & ~15) != 0) throw new IllegalStateException("Unsupported reference flags");
    int[] archives = new int[count(in,protocol)];
    int id = 0;
    for (int i = 0; i < archives.length; i++) archives[i] = id += count(in,protocol);
    var fields = new LinkedHashMap<Integer,List<int[]>>();
    for (int archive : archives) fields.put(archive,new ArrayList<>());
    if ((flags & 1) != 0) in.position(in.position() + archives.length * 4);
    int prefix = in.position();
    section(in,archives,fields,4); // CRCs
    if ((flags & 8) != 0) section(in,archives,fields,4);
    if ((flags & 2) != 0) section(in,archives,fields,64);
    if ((flags & 4) != 0) section(in,archives,fields,8);
    section(in,archives,fields,4); // Revisions
    int[] files = new int[archives.length];
    var counts = new LinkedHashMap<Integer,int[]>();
    var fileIds = new LinkedHashMap<Integer,int[]>();
    var names = new LinkedHashMap<Integer,int[]>();
    for (int i = 0; i < files.length; i++) {
      int start = in.position(); files[i] = count(in,protocol);
      counts.put(archives[i],new int[]{start,in.position()-start});
    }
    for (int i = 0; i < files.length; i++) {
      int start = in.position();
      for (int j = 0; j < files[i]; j++) count(in,protocol);
      fileIds.put(archives[i],new int[]{start,in.position()-start});
    }
    if ((flags & 1) != 0) for (int i = 0; i < files.length; i++) {
      int start = in.position(); in.position(start + files[i] * 4);
      names.put(archives[i],new int[]{start,in.position()-start});
    }
    if (in.hasRemaining()) throw new IllegalStateException("Unexpected reference-table tail");
    return new Layout(protocol,flags,archives,fields,prefix,counts,fileIds,names);
  }

  private static void section(ByteBuffer in, int[] ids, Map<Integer,List<int[]>> fields, int size) {
    for (int id : ids) {
      fields.get(id).add(new int[]{in.position(),size});
      in.position(in.position()+size);
    }
  }

  static byte[] merge(byte[] original, byte[] candidate, int changedArchive) {
    Layout before = layout(original), after = layout(candidate);
    if (before.protocol != after.protocol || before.flags != after.flags
        || !Arrays.equals(before.archives,after.archives) || !before.fields.containsKey(changedArchive))
      throw new IllegalStateException("Reference structure changed");
    byte[] result = original.clone();
    if (before.protocol >= 6) System.arraycopy(candidate,1,result,1,4);
    var originalFields = before.fields.get(changedArchive);
    var candidateFields = after.fields.get(changedArchive);
    for (int i = 0; i < originalFields.size(); i++) {
      int[] to = originalFields.get(i), from = candidateFields.get(i);
      if (to[1] != from[1]) throw new IllegalStateException("Reference field size changed");
      System.arraycopy(candidate,from[0],result,to[0],to[1]);
    }
    return result;
  }

  /** Copies selected archive metadata from native builders, including deliberate new file IDs. */
  static byte[] mergeGroups(byte[] original, byte[] npcCandidate, Map<Integer,byte[]> fullSources) throws IOException {
    return mergeGroups(original,npcCandidate,fullSources,58);
  }

  static byte[] mergeGroups(byte[] original, byte[] candidate, Map<Integer,byte[]> fullSources, int changedArchive) throws IOException {
    byte[] metadata = merge(original,candidate,changedArchive);
    Layout base = layout(metadata);
    var layouts = new HashMap<Integer,Layout>();
    for (var entry : fullSources.entrySet()) {
      Layout source = layout(entry.getValue());
      if (base.protocol != source.protocol || base.flags != source.flags ||
          !Arrays.equals(base.archives,source.archives) || !base.fields.containsKey(entry.getKey()))
        throw new IllegalStateException("Reference structure changed");
      layouts.put(entry.getKey(),source);
    }
    var out = new ByteArrayOutputStream();
    out.write(metadata,0,base.prefix);
    for (int section = 0; section < base.fields.values().iterator().next().size(); section++) {
      for (int id : base.archives) {
        Layout source = layouts.getOrDefault(id,base);
        byte[] bytes = fullSources.getOrDefault(id,metadata);
        int[] range = source.fields.get(id).get(section);
        out.write(bytes,range[0],range[1]);
      }
    }
    for (int section = 0; section < ((base.flags & 1) != 0 ? 3 : 2); section++) {
      for (int id : base.archives) {
        Layout source = layouts.getOrDefault(id,base);
        byte[] bytes = fullSources.getOrDefault(id,metadata);
        int[] range = (section == 0 ? source.counts : section == 1 ? source.files : source.names).get(id);
        out.write(bytes,range[0],range[1]);
      }
    }
    byte[] result = out.toByteArray(); layout(result);
    return result;
  }

  public static int varpFileOffset(Path cache) throws IOException {
    byte[] reference = reference(cache);
    Layout index = layout(reference);
    int[] files = index.files.get(67);
    if (files == null) return 0;
    int first = smart(ByteBuffer.wrap(reference, files[0], files[1]));
    return first == -1 ? 32768 : 0;
  }

  /** Displee treats the native 0x7fff sentinel as +32767; retain the native signed file IDs. */
  public static void restoreSignedVarpSentinel(Path cache, int offset) throws IOException {
    if (offset == 0) return;
    byte[] bytes = reference(cache);
    Layout index = layout(bytes);
    int[] files = index.files.get(67);
    ByteBuffer first = ByteBuffer.wrap(bytes, files[0], files[1]);
    int start = first.position();
    int value = smart(first);
    if (value == -1 && first.position() - start == 2) return; // No-op rebuilds retain the original reference.
    if (value != 32767 || first.position() - start != 4) throw new IOException("Unexpected varp sentinel encoding");
    var output = new ByteArrayOutputStream();
    output.write(bytes,0,start);
    output.write(0x7f);output.write(0xff);
    output.write(bytes,first.position(),bytes.length-first.position());
    write(cache,output.toByteArray());
  }

  static byte[] reference(Path cache) throws IOException {
    try (RandomAccessFile index = new RandomAccessFile(cache.resolve("main_file_cache.idx255").toFile(),"r");
         RandomAccessFile data = new RandomAccessFile(cache.resolve("main_file_cache.dat2").toFile(),"r")) {
      index.seek(2 * 6);
      int length = medium(index), sector = medium(index);
      byte[] container = new byte[length];
      int offset = 0, chunk = 0;
      while (offset < length) {
        if (sector <= 0) throw new IOException("Invalid reference sector");
        data.seek(sector * 520L);
        int archive = data.readUnsignedShort(), ordinal = data.readUnsignedShort();
        sector = medium(data);
        int indexId = data.readUnsignedByte();
        if (archive != 2 || ordinal != chunk++ || indexId != 255) throw new IOException("Invalid reference header");
        int size = Math.min(512,length-offset);
        data.readFully(container,offset,size);
        offset += size;
      }
      ByteBuffer in = ByteBuffer.wrap(container);
      int compression = Byte.toUnsignedInt(in.get()), packed = in.getInt();
      if (compression == 0) return Arrays.copyOfRange(container,5,5+packed);
      if (compression != 2) throw new IOException("Unsupported reference compression");
      int unpacked = in.getInt();
      try (var gzip = new GZIPInputStream(new ByteArrayInputStream(container,9,packed))) {
        byte[] result = gzip.readAllBytes();
        if (result.length != unpacked) throw new IOException("Reference length mismatch");
        return result;
      }
    }
  }

  private static int medium(RandomAccessFile in) throws IOException {
    return (in.readUnsignedByte()<<16) | (in.readUnsignedByte()<<8) | in.readUnsignedByte();
  }

  static void preserve(Path original, Path candidate, int changedArchive) throws IOException {
    write(candidate,merge(reference(original),reference(candidate),changedArchive));
  }

  static void write(Path candidate, byte[] merged) throws IOException {
    var packed = new ByteArrayOutputStream();
    try (var gzip = new GZIPOutputStream(packed)) { gzip.write(merged); }
    byte[] compressed = packed.toByteArray();
    byte[] container = ByteBuffer.allocate(9+compressed.length)
      .put((byte)2).putInt(compressed.length).putInt(merged.length).put(compressed).array();
    try (var writer = new CacheLibrary(candidate,false,null)) {
      if (!writer.getIndex255().writeArchiveSector(2,container)) throw new IOException("Reference write failed");
    }
  }
}
