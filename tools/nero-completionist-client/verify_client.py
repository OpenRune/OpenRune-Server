"""Offline native decoder, particle attachment, preview and stats checks; no game/cache mutation."""
import argparse,json,os,subprocess
from pathlib import Path
from client_profile import ROOT,selected_runtime,require_hash

parser=argparse.ArgumentParser()
parser.add_argument("--java",type=Path,required=True)
parser.add_argument("--assets",type=Path,required=True)
parser.add_argument("--output",type=Path,required=True)
parser.add_argument("--libraries",type=Path,default=Path.home()/".runelite/repository2")
args=parser.parse_args()
meta=json.loads((ROOT/"qualified-client.json").read_text())
for name,h in meta["bundleHashes"].items(): require_hash(ROOT/name,h)
for name,h in meta["clientLibraries"].items(): require_hash(args.libraries/name,h)
runtime=selected_runtime();output=args.output.resolve();output.mkdir(parents=True,exist_ok=True)
classes=output/"classes";classes.mkdir(exist_ok=True)
classpath=os.pathsep.join(map(str,[runtime/"nero-compatibility-runtime.jar",ROOT/"client-plugin/Nero-Particle-Preview.jar",*[args.libraries/n for n in meta["classpath"]]]))
subprocess.run([str(args.java.with_name("javac.exe")),"--release","11","-encoding","UTF-8","-cp",classpath,"-d",str(classes),*map(str,sorted((ROOT/"verification").glob("*.java")))],check=True)
command=[str(args.java),"-Xverify:all","-Djava.awt.headless=true",f"-javaagent:{runtime/'nero-compatibility-runtime.jar'}",f"-Dnero.compat.profile={runtime/'runtime-240.properties'}",f"-Dnero.compat.bindings={runtime/'bindings.properties'}",f"-Dnero.compat.log={output/'native-runtime.jsonl'}","-Dnero.compat.enabled=true","-cp",str(classes)+os.pathsep+classpath,"com.evolvedmind.nero.particles.NativeAssetCheck",str(args.assets.resolve()),str(output/'images')]
result=subprocess.run(command,capture_output=True,text=True)
(output/"native-check.log").write_text(result.stdout+result.stderr,encoding="utf-8",newline="\n")
print(result.stdout[-2500:]);print(result.stderr[-2500:])
if result.returncode: raise RuntimeError("Native check failed; see "+str(output/"native-check.log"))
(output/"result.json").write_text(json.dumps({"passed":True,"revision":240,"models":96,"native":True,"output":result.stdout.splitlines()},indent=2),encoding="utf-8",newline="\n")
