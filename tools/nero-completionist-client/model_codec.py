"""Bounded OSRS new-format mesh codec, derived from the owner's Nero v0.7 converter.

Supports legacy vertex labels and native type-0 texture planes. Other encodings
are rejected; this tool does not open a cache or attach to a client.
"""
import struct


class Reader:
    def __init__(self,data): self.data,self.pos=data,0
    def read(self,n):
        if n<0 or self.pos+n>len(self.data): raise ValueError('Truncated mesh')
        value=self.data[self.pos:self.pos+n];self.pos+=n;return value
    def u8(self): return self.read(1)[0]
    def u16(self): return struct.unpack('>H',self.read(2))[0]
    def smart(self): return self.u8()-64 if self.data[self.pos]<128 else self.u16()-49152
    def done(self):
        if self.pos!=len(self.data): raise ValueError('Unconsumed mesh section')


def decode(data):
    if data[-2:]!=b'\xff\xff': raise ValueError('Expected OSRS new-format mesh')
    footer=Reader(data[-23:]);nv,nf,nt=footer.u16(),footer.u16(),footer.u8()
    modes,priority,alpha,groups,materials,bones=[footer.u8() for _ in range(6)]
    xs,ys,zs,ids,uvs=[footer.u16() for _ in range(5)];footer.u16();footer.done()
    if any(flag not in (0,1) for flag in [modes,alpha,groups,materials,bones]):raise ValueError('Unsupported mesh encoding')
    r=Reader(data[:-23]);types=list(r.read(nt))
    if any(types):raise ValueError('Only native planar texture mapping is supported')
    vf=r.read(nv);face_modes=list(r.read(nf)) if modes else None;tt=r.read(nf)
    priorities=list(r.read(nf)) if priority==255 else None
    skins=list(r.read(nf)) if groups else None;bones=list(r.read(nv)) if bones else None
    alphas=list(r.read(nf)) if alpha else None;triangles=Reader(r.read(ids))
    textures=[r.u16()-1 for _ in range(nf)] if materials else [-1]*nf
    uv=Reader(r.read(uvs));colors=[r.u16() for _ in range(nf)]
    coordinates=[Reader(r.read(n)) for n in [xs,ys,zs]];vertices=[];previous=[0,0,0]
    for flag in vf:
        if flag&~7:raise ValueError('Unsupported vertex encoding')
        previous=[previous[a]+(coordinates[a].smart() if flag&(1<<a) else 0) for a in range(3)];vertices.append(previous)
    for stream in coordinates:stream.done()
    faces=[];a=b=c=last=0
    for mode in tt:
        if mode==1:a=last+triangles.smart();b=a+triangles.smart();c=b+triangles.smart()
        elif mode==2:b,c=c,last+triangles.smart()
        elif mode==3:a,c=c,last+triangles.smart()
        elif mode==4:a,b=b,a;c=last+triangles.smart()
        else:raise ValueError('Unsupported triangle mode')
        last=c
        if min(a,b,c)<0 or max(a,b,c)>=nv:raise ValueError('Invalid triangle index')
        faces.append([a,b,c])
    triangles.done();coords=[uv.u8()-1 if t>=0 and nt else -1 for t in textures];uv.done()
    planes=[[r.u16(),r.u16(),r.u16()] for _ in range(nt)];trailer=r.read(len(r.data)-r.pos)
    return dict(vertices=vertices,faces=faces,colors=colors,faceModes=face_modes,priority=priority,
                facePriorities=priorities,faceSkins=skins,vertexSkins=bones,alphas=alphas,
                sourceMaterials=textures,sourceTextureCoords=coords,planes=planes,trailer=trailer)


def smart(value):
    if -64<=value<64:return bytes([value+64])
    if -16384<=value<16384:return struct.pack('>H',value+49152)
    raise ValueError('Mesh smart outside range')


def encode(mesh):
    vertices,faces=mesh['vertices'],mesh['faces'];nv,nf,nt=len(vertices),len(faces),len(mesh.get('planes',[]))
    if nv>65535 or nf>65535 or nt>255:raise ValueError('Mesh limit exceeded')
    flags=bytearray();xyz=[bytearray() for _ in range(3)];previous=[0,0,0]
    for vertex in vertices:
        flag=0
        for a in range(3):
            delta=vertex[a]-previous[a]
            if delta:flag|=1<<a;xyz[a].extend(smart(delta))
        flags.append(flag);previous=vertex
    indices=bytearray();last=0
    for a,b,c in faces:indices.extend(smart(a-last)+smart(b-a)+smart(c-b));last=c
    modes=bytes(mesh.get('faceModes') or []);priorities=bytes(mesh.get('facePriorities') or [])
    skins=bytes(mesh.get('faceSkins') or []);bones=bytes(mesh.get('vertexSkins') or []);alphas=bytes(mesh.get('alphas') or [])
    textured=any(t>=0 for t in mesh['sourceMaterials'])
    materials=b''.join(struct.pack('>H',t+1) for t in mesh['sourceMaterials']) if textured else b''
    uv=bytes(mesh['sourceTextureCoords'][f]+1 for f,t in enumerate(mesh['sourceMaterials']) if t>=0) if textured and nt else b''
    colors=b''.join(struct.pack('>H',c) for c in mesh['colors'])
    planes=b''.join(struct.pack('>HHH',*plane) for plane in mesh.get('planes',[]))
    trailer=mesh.get('trailer',b'\0')
    payload=b''.join([bytes(nt),flags,modes,bytes([1])*nf,priorities,skins,bones,alphas,indices,materials,uv,colors,*xyz,planes,trailer])
    footer=struct.pack('>HHBBBBBBBHHHHH',nv,nf,nt,bool(modes),mesh['priority'],bool(alphas),bool(skins),bool(materials),bool(bones),*(len(v) for v in xyz),len(indices),len(uv))
    return payload+footer+b'\xff\xff'
