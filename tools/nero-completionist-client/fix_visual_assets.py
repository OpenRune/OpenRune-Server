"""Build reviewed cape meshes from an immutable v0.11 baseline, never a live cache."""
import argparse,copy,hashlib,json,struct
from pathlib import Path
from model_codec import decode,encode

ROOT=Path(__file__).resolve().parent
RAW=ROOT.parents[1]/'content/other/nero-completionist/pack/src/main/resources/pack/raw'
PALETTE=[65214,65200,65186,62995]


def inventory_channel(colour):
    if colour in PALETTE:return PALETTE.index(colour)
    # The original folded mesh bakes several luminances of channel 3.
    if colour&65408==PALETTE[3]&65408:return 3
    return None


def inventory_colour(colour,palette):
    channel=inventory_channel(colour)
    if channel is None:return colour
    source=PALETTE[channel];target=palette[channel]
    if target==source:return colour
    light=max(2,min(126,round((colour&127)*(target&127)/max(1,source&127))))
    return (target&65408)|light


def original_shell(mesh):
    """Keep the authored outer cloth, lining and trim exactly once.

    The source is already sided, like native max cape 29616/29624. The v0.11
    conversion appended 785 reversed faces over its existing lining/ornament;
    v0.12 displaced them, but still drew the same garment twice. Preserve the
    first 792 source faces (including seven emitters) and any native quiver.
    """
    keep=list(range(792))+list(range(1577,len(mesh['faces'])))
    remap={old:new for new,old in enumerate(keep)}
    for key in ['faces','colors','faceModes','facePriorities','faceSkins','alphas','sourceMaterials','sourceTextureCoords']:
        if mesh.get(key) is not None:mesh[key]=[mesh[key][f] for f in keep]
    mesh['facePriorities'][:792]=json.loads((ROOT/'assets/original-worn-priorities.json').read_text(encoding='utf-8'))
    mesh['faceModes']=[2 if a==255 else 0 for a in mesh['alphas']]
    used=sorted({v for face in mesh['faces'] for v in face})
    vertices={v:i for i,v in enumerate(used)}
    mesh['faces']=[[vertices[v] for v in face] for face in mesh['faces']]
    mesh['vertices']=[mesh['vertices'][v] for v in used]
    if mesh['vertexSkins'] is not None:mesh['vertexSkins']=[mesh['vertexSkins'][v] for v in used]
    mesh['planes']=[]
    return remap


def original_hood(mesh):
    """The worn source includes its own head; retain that head exactly once.

    All 564 male / 675 female authored faces were followed by coincident,
    flat-shaded reversed copies. They fight the original hood and face in both
    renderers. Hair and jaw are already masked by the native equipment slots.
    """
    count=len(mesh['faces'])//2
    if count not in (564,675) or any(mesh['faces'][f+count]!=list(reversed(mesh['faces'][f])) for f in range(count)):
        raise ValueError('Unexpected worn hood topology')
    for key in ['faces','colors','faceModes','facePriorities','faceSkins','alphas','sourceMaterials','sourceTextureCoords']:
        if mesh.get(key) is not None:mesh[key]=mesh[key][:count]
    return {f:f for f in range(count)}


def quiver_clearance(mesh):
    # Native quiver geometry is a separate part after the 792 authored cloth
    # faces. Move only that part eight native units towards the outside/back.
    cloth={v for face in mesh['faces'][:792] for v in face}
    quiver={v for face in mesh['faces'][792:] for v in face}
    if len(mesh['faces'])!=1136 or cloth&quiver:raise ValueError('Unexpected quiver attachment')
    for v in quiver:mesh['vertices'][v][2]+=32


def texture_panels(mesh,front_count,channels,texture):
    # The reference textures the broad cloth panels; raised edges and the
    # central embroidered pattern retain their family colours.
    nf=len(mesh['faces']);mesh['sourceMaterials']=[-1]*nf;mesh['sourceTextureCoords']=[-1]*nf
    mask=set(channels)
    for f in range(front_count):
        if f in mask:mesh['sourceMaterials'][f]=texture;mesh['sourceTextureCoords'][f]=0
    used={v for f in mask for v in mesh['faces'][f]}
    lo=[min(mesh['vertices'][v][i] for v in used) for i in range(3)]
    hi=[max(mesh['vertices'][v][i] for v in used) for i in range(3)]
    base=len(mesh['vertices']);z=round((lo[2]+hi[2])/2)
    mesh['vertices'].extend([[lo[0],lo[1],z],[hi[0],lo[1],z],[lo[0],hi[1],z]])
    if mesh['vertexSkins'] is not None:mesh['vertexSkins'].extend([255]*3)
    mesh['planes']=[[base,base+1,base+2]]
    for f,t in enumerate(mesh['sourceMaterials']):
        if t>=0:mesh['colors'][f]=127


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--baseline',type=Path,required=True);parser.add_argument('--runtime',default='runtime-v012-visual');args=parser.parse_args()
    source=args.baseline.resolve(strict=True)
    if source==RAW.resolve() or '.data' in source.parts:raise ValueError('Use immutable source assets outside the checkout/cache')
    manifest=json.loads((source/'cache-manifest.json').read_text(encoding='utf-8'))
    for row in manifest['writes']:
        if hashlib.sha256((source/row['file']).read_bytes()).hexdigest()!=row['sha256']:raise ValueError('Changed baseline '+row['key'])
    variants=json.loads((source/'variants.json').read_text(encoding='utf-8'))
    inventory=json.loads((ROOT/'assets/original-inventory.source.json').read_text(encoding='utf-8'))
    male=decode((source/'cache/live/7/62101/0.dat').read_bytes())
    cloth=[f for f,c in enumerate(male['colors'][:792]) if c in (PALETTE[1],PALETTE[3])]
    models={62100:(None,PALETTE),62103:(None,[65214,65200,65186,62995])}
    for row in variants:models.setdefault(row['modelIds'][0],(row['key'],row['colours']))
    runtime=ROOT/args.runtime;runtime.mkdir(exist_ok=True)
    previous=ROOT/'runtime-v011-final'
    for f in ['definitions.json','runtime-240.properties','ASM-LICENSE.txt']:
        if not (runtime/f).exists():(runtime/f).write_bytes((previous/f).read_bytes())
    old_bindings={}
    for line in (previous/'bindings.properties').read_text(encoding='utf-8').splitlines():
        if '=' in line:key,value=line.split('=',1);old_bindings[key]=value
    new=['schemaVersion=1'];report=[]
    for p in sorted((source/'cache/live/7').glob('*/0.dat')):
        model_id=int(p.parent.name);old=p.read_bytes();old_hash=hashlib.sha256(old).hexdigest();mesh=decode(old);face_remap=None;opaque_depth=False
        if model_id in models:
            key,colours=models[model_id]
            mesh=copy.deepcopy(json.loads((ROOT/'assets/trimmed-inventory.source.json').read_text(encoding='utf-8')) if model_id==62103 else inventory);nf=len(mesh['faces'])
            mesh.update(planes=[],trailer=b'\0',sourceMaterials=[-1]*nf,sourceTextureCoords=[-1]*nf)
            mesh['priority']=255;mesh['alphas']=[0]*nf
            mesh['faceModes']=[0]*nf;mesh['faceSkins']=None
            channels=[f for f,c in enumerate(mesh['colors']) if inventory_channel(c) in (1,3)]
            mesh['colors']=[inventory_colour(c,colours) for c in mesh['colors']]
            if key in ('fire','infernal'):texture_panels(mesh,nf,channels,40 if key=='fire' else 59)
        elif len(mesh['faces'])>=1577:
            face_remap=original_shell(mesh)
            opaque_depth=True
            key=next((r['key'] for r in variants if model_id in r['modelIds'][1:3]),None)
            if key=='dizana':quiver_clearance(mesh)
            if key in ('fire','infernal'):texture_panels(mesh,792,cloth,40 if key=='fire' else 59)
        elif len(mesh['faces']) in (1128,1350):
            face_remap=original_hood(mesh)
            opaque_depth=True
        data=encode(mesh);digest=hashlib.sha256(data).hexdigest()
        (RAW/p.relative_to(source)).write_bytes(data)
        new.append(f'model.{digest}.scale=0.25')
        if opaque_depth:new.append(f'model.{digest}.opaqueDepth=true')
        # One opaque depth order for the assembled avatar; no synthetic face
        # bias and no mutation of cached/native equipment priority arrays.
        for suffix in ['emitters','palette']:
            value=old_bindings.get(f'model.{old_hash}.{suffix}')
            if value and face_remap is not None and suffix=='palette':
                value=','.join(f'{face_remap[int(face)]}:{channel}' for face,channel in (token.split(':') for token in value.split(',')) if int(face) in face_remap)
            if model_id in models and suffix=='palette':
                value=','.join(f'{f}:{inventory_channel(c)}' for f,c in enumerate(mesh['colors']) if inventory_channel(c) is not None) if model_id==62100 else None
            if value:new.append(f'model.{digest}.{suffix}={value}')
        report.append(dict(model=model_id,vertices=len(mesh['vertices']),faces=len(mesh['faces']),textured=sum(t>=0 for t in mesh['sourceMaterials']),sha256=digest))
    # Restore the original folded icon's exact projection on every cape variant.
    # Server definitions retain their gameplay fields; client opcodes are native rev240.
    ids=[63000,63001]+[r['capeId'] for r in variants]
    projection=b''.join(bytes([op])+struct.pack('>H',value) for op,value in [(4,1316),(5,252),(6,1020),(7,65535),(8,24),(95,0)])+b'\0'
    for item in ids:
        p=source/f'cache/live/2/10/{item}.dat';data=p.read_bytes()
        if data[-1]!=0:raise ValueError('Invalid client item trailer')
        (RAW/p.relative_to(source)).write_bytes(data[:-1]+projection)
    for row in manifest['writes']:row['sha256']=hashlib.sha256((RAW/row['file']).read_bytes()).hexdigest()
    (RAW/'cache-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8',newline='\n')
    (runtime/'bindings.properties').write_text('\n'.join(new)+'\n',encoding='utf-8',newline='\n')
    (ROOT/'runtime-current.txt').write_text(args.runtime+'\n',encoding='utf-8',newline='\n')
    (ROOT/'assets/visual-assets.json').write_text(json.dumps({'revision':240,'inventoryProjection':{'zoom':1316,'pitch':252,'yaw':1020,'x':-1,'y':24},'models':report},indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({'models':len(report),'foldedInventoryModels':len(models),'textured':[(r['model'],r['textured']) for r in report if r['textured']],'runtime':args.runtime}))


if __name__=='__main__':main()
