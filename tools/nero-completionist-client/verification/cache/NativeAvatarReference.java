import dev.openrune.filesystem.Cache;
import java.nio.file.*;
import java.util.*;
public final class NativeAvatarReference {
 public static void main(String[] args)throws Exception {
  Path source=Path.of(args[0]).toAbsolutePath().normalize(),out=Path.of(args[1]).toAbsolutePath().normalize();
  if(out.startsWith(source))throw new IllegalArgumentException("Export outside the source cache");
  Cache cache=Cache.Companion.load(source);Files.createDirectories(out);Set<Integer> selected=new HashSet<>();
  try {for(int id=0;id<300;id++) {
   byte[] bytes=cache.data(2,3,id,null);if(bytes==null)continue;
   // Native rev240 uses 32-bit model-ID opcodes 5 and 70..79. This bounded
   // read-only exporter supports those without changing the server codec.
   var input=new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes));int part=-1;boolean nonSelectable=false;List<Integer> models=new ArrayList<>();
   while(true){int op=input.readUnsignedByte();if(op==0)break;
    if(op==1)part=input.readUnsignedByte();else if(op==2){int n=input.readUnsignedByte();for(int i=0;i<n;i++)models.add(input.readUnsignedShort());}
    else if(op==5){int n=input.readUnsignedByte();for(int i=0;i<n;i++)models.add(input.readInt());} else if(op==3)nonSelectable=true;else if(op==40||op==41){int n=input.readUnsignedByte();for(int i=0;i<n;i++){input.readUnsignedShort();input.readUnsignedShort();}}
    else if(op>=60&&op<70)input.readUnsignedShort();else if(op>=70&&op<80)input.readInt();else throw new IllegalStateException("Unknown native kit opcode "+op);
   }
   if(part<0||part>13||nonSelectable||!selected.add(part))continue;
   System.out.println("kit="+id+" part="+part+" models="+models);
   for(int model:models)Files.write(out.resolve("kit-"+part+"-"+model+".dat"),cache.data(7,model,0,null));
  }}finally{cache.close();}
 }
}
