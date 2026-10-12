"""Build the paired sources into a new runtime, retaining client fingerprints."""
import argparse,hashlib,json,os,re,subprocess,zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parent


def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def jar(path,entries,manifest):
    with zipfile.ZipFile(path,'w',zipfile.ZIP_DEFLATED) as archive:
        for name,data in [('META-INF/MANIFEST.MF',manifest),*sorted(entries.items())]:
            info=zipfile.ZipInfo(name,(2026,10,12,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;archive.writestr(info,data)


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--java-home',type=Path,required=True);parser.add_argument('--bootstrap-runtime',default='runtime-v011-final');parser.add_argument('--libraries',type=Path,default=Path.home()/'.runelite/repository2');args=parser.parse_args()
    name=(ROOT/'runtime-current.txt').read_text().strip()
    if not re.fullmatch(r'runtime-[a-zA-Z0-9_-]+',name) or name==args.bootstrap_runtime:raise ValueError('Build into a new dedicated runtime')
    target=ROOT/name;meta=json.loads((ROOT/'qualified-client.json').read_text(encoding='utf-8'))
    for p,h in meta['clientLibraries'].items():
        if digest(args.libraries/p)!=h:raise ValueError('Changed client dependency '+p)
    for p,h in {**meta.get('buildBootstrapHashes',{}),**meta['bundleHashes']}.items():
        if p in ['runtime-current.txt','client-plugin/Nero-Particle-Preview.jar'] or p.startswith(name+'/'):continue
        if digest(ROOT/p)!=h:raise ValueError('Changed qualified bundle input '+p)
    # Refuse overwriting a runtime potentially used by an earlier package.
    if (target/'nero-compatibility-runtime.jar').exists():raise ValueError('Choose a fresh runtime name; existing binaries are preserved')
    build=ROOT.parents[2]/'cape-client-build'/name;asm=build/'asm';asm.mkdir(parents=True,exist_ok=True)
    bootstrap=ROOT/args.bootstrap_runtime
    with zipfile.ZipFile(bootstrap/'nero-compatibility-runtime.jar') as archive:
        manifest=archive.read('META-INF/MANIFEST.MF')
        for n in archive.namelist():
            if n.startswith('org/objectweb/asm/') and n.endswith('.class'):
                p=asm/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(archive.read(n))
    javac=args.java_home/'bin/javac.exe';runtime_classes=build/'runtime-classes';runtime_classes.mkdir(exist_ok=True)
    subprocess.run([str(javac),'--release','11','-encoding','UTF-8','-cp',str(asm),'-d',str(runtime_classes),*map(str,sorted((ROOT/'source/runtime').rglob('*.java')))],check=True)
    classes={p.relative_to(runtime_classes).as_posix():p.read_bytes() for p in runtime_classes.rglob('*.class')}
    asm_classes={p.relative_to(asm).as_posix():p.read_bytes() for p in asm.rglob('*.class')}
    jar(target/'nero-compatibility-runtime.jar',{**classes,**asm_classes,'META-INF/ASM-LICENSE.txt':(target/'ASM-LICENSE.txt').read_bytes()},manifest)
    bridge={n:d for n,d in classes.items() if n.rsplit('/',1)[-1]=='ModelProbe.class' or n.rsplit('/',1)[-1].startswith(('AttachmentRegistry','NativeDepthSurface','ParticleRuntime'))}
    jar(target/'nero-compatibility-bridge.jar',bridge,b'Manifest-Version: 1.0\n\n')
    plugin_classes=build/'plugin-classes';plugin_classes.mkdir(exist_ok=True)
    cp=os.pathsep.join(map(str,[target/'nero-compatibility-runtime.jar',*[args.libraries/n for n in meta['classpath']]]))
    subprocess.run([str(javac),'--release','11','-encoding','UTF-8','-cp',cp,'-d',str(plugin_classes),*map(str,sorted((ROOT/'source/particles').rglob('*.java')))],check=True)
    plugin={p.relative_to(plugin_classes).as_posix():p.read_bytes() for p in plugin_classes.rglob('*.class')}
    jar(ROOT/'client-plugin/Nero-Particle-Preview.jar',plugin,b'Manifest-Version: 1.0\nImplementation-Version: 0.14-avatar-depth\n\n')
    for p in [ROOT/'runtime-current.txt',ROOT/'client-plugin/Nero-Particle-Preview.jar',*target.rglob('*')]:
        if p.is_file():meta['bundleHashes'][p.relative_to(ROOT).as_posix()]=digest(p)
    meta['packageVersion']='Nero completionist v0.14 avatar occlusion correction'
    (ROOT/'qualified-client.json').write_text(json.dumps(meta,indent=2)+'\n',encoding='utf-8',newline='\n')
    (build/'source-proof.json').write_text(json.dumps({'runtimeClasses':len(classes),'pluginClasses':len(plugin),'sources':{p.relative_to(ROOT).as_posix():digest(p) for p in (ROOT/'source').rglob('*.java')},'clientFingerprintsUnchanged':True},indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({'runtime':name,'runtimeClasses':len(classes),'pluginClasses':len(plugin),'clientFingerprintsUnchanged':True}))


if __name__=='__main__':main()
