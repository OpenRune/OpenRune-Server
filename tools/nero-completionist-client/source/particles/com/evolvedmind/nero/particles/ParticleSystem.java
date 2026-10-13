package com.evolvedmind.nero.particles;
import java.util.*;

/** Bounded deterministic preview simulator. Source time units/effectors/materials remain unverified. */
public final class ParticleSystem {
 public static final int LIMIT=4096;
 private final Random random;
 private final List<Particle> particles=new ArrayList<>();
 private final Map<Integer,float[]> previous=new HashMap<>();
 private final Map<Integer,Double> credit=new HashMap<>();
 public ParticleSystem(long seed){random=new Random(seed);}
 public List<Particle> particles(){return Collections.unmodifiableList(particles);}
 public void clear(){particles.clear();previous.clear();credit.clear();}

 public void step(List<Emitter> emitters,double units) {
  if(!Double.isFinite(units)||units<=0||units>4)throw new IllegalArgumentException("Invalid particle timestep");
  for(Iterator<Particle> i=particles.iterator();i.hasNext();) {
   Particle p=i.next();p.age+=units;if(p.age>=p.life){i.remove();continue;}
   double f=p.age/p.life;
   if(p.def.globalDirectionRaw!=null) {
    int[] v=p.def.globalDirectionRaw;
    if(p.def.globalConstantSpeed){p.x+=v[0]*units/16384.0;p.y+=v[1]*units/16384.0;p.z+=v[2]*units/16384.0;}
    else {p.dx+=v[0]*units/32767.0;p.dy+=v[1]*units/32767.0;p.dz+=v[2]*units/32767.0;
     while(Math.max(Math.abs(p.dx),Math.max(Math.abs(p.dy),Math.abs(p.dz)))>1){p.dx/=2;p.dy/=2;p.dz/=2;p.directionScale*=2;}}
   }
   double speed=p.speed+(p.def.endSpeed/262144.0/4-p.speed)*Math.min(1,f/(p.def.speedChange/100.0));
   speed*=p.directionScale;
   p.x+=p.dx*speed*units;p.y+=p.dy*speed*units;p.z+=p.dz*speed*units;
   p.radius=p.startRadius+(p.def.endSize/16384.0/4-p.startRadius)*f;
   p.colour=colour(p.startColour,p.def.fadeColour,f);
  }
  Set<Integer> seen=new HashSet<>();
  for(Emitter e:emitters) {
   if(!seen.add(e.face))throw new IllegalArgumentException("Duplicate emitter face");
   float[] old=previous.get(e.face);
   if(old==null || distance(old,e.xyz)>256)old=e.xyz;
   double accumulated=credit.getOrDefault(e.face,0.0)+(e.def.minRate+random.nextDouble()*(e.def.maxRate-e.def.minRate))*units/64;
   int spawn=(int)accumulated;credit.put(e.face,accumulated-spawn);
   for(int n=0;n<spawn && particles.size()<LIMIT;n++) {
    double u=random.nextDouble(),v=random.nextDouble();if(u+v>1){u=1-u;v=1-v;}
    double a=1-u-v,t=(n+random.nextDouble())/Math.max(1,spawn);
    double[] xyz=new double[3];
    for(int axis=0;axis<3;axis++) {
     double then=old[axis]*a+old[axis+3]*u+old[axis+6]*v;
     double now=e.xyz[axis]*a+e.xyz[axis+3]*u+e.xyz[axis+6]*v;
     xyz[axis]=then+(now-then)*t;
    }
    double[] direction=direction(e.xyz,e.def);
    double size=(e.def.minSize+random.nextDouble()*(e.def.maxSize-e.def.minSize))/16384.0/4;
    double speed=(e.def.minSpeed+random.nextDouble()*(e.def.maxSpeed-e.def.minSpeed))/262144.0/4;
    double life=e.def.minLife+random.nextDouble()*(e.def.maxLife-e.def.minLife);
    double rgbVariance=random.nextDouble(),alphaVariance=random.nextDouble();
    int start=variance(e.def.minColour,e.def.maxColour,rgbVariance,alphaVariance);
    particles.add(new Particle(xyz,direction,size,speed,life,start,e.def));
   }
   previous.put(e.face,e.xyz.clone());
  }
  previous.keySet().retainAll(seen);credit.keySet().retainAll(seen);
 }

 private double[] direction(float[] p,Definition d) {
  double ax=p[3]-p[0],ay=p[4]-p[1],az=p[5]-p[2],bx=p[6]-p[0],by=p[7]-p[1],bz=p[8]-p[2];
  double x=ay*bz-az*by,y=az*bx-ax*bz,z=ax*by-ay*bx;
  double length=Math.sqrt(x*x+y*y+z*z);if(length<1e-9)return new double[]{0,-1,0};
  if(d.angles==null)return new double[]{x/length,y/length,z/length};
  int yaw=(int)(Math.atan2(z,x)*2607.5945876176133),pitch=(int)(Math.atan2(y,Math.sqrt(x*x+z*z))*2607.5945876176133);
  int hr=(d.angles[1]-d.angles[0])*8,vr=(d.angles[3]-d.angles[2])*8;
  int h=(yaw+d.angles[0]*8-(hr>>1)+(int)(hr*random.nextDouble()))&16383;
  int v=(pitch+d.angles[2]*8-(vr>>1)+(int)(vr*random.nextDouble()))&8191;
  double ha=h*Math.PI/8192,va=v*Math.PI/8192;
  return new double[]{Math.cos(ha)*Math.sin(va),-Math.cos(va),Math.sin(ha)*Math.sin(va)};
 }
 private static double distance(float[] a,float[] b){double sum=0;for(int i=0;i<3;i++)sum+=(a[i]-b[i])*(a[i]-b[i]);return Math.sqrt(sum);}
 private static int variance(int a,int b,double rgb,double alpha){int result=0;for(int shift=0;shift<=24;shift+=8){double f=shift==24?alpha:rgb;result|=((int)(((a>>>shift)&255)+(((b>>>shift)&255)-((a>>>shift)&255))*f))<<shift;}return result;}
 private static int colour(int a,int b,double f){int c=0;for(int shift=0;shift<=24;shift+=8)c|=((int)Math.round(((a>>>shift)&255)+(((b>>>shift)&255)-((a>>>shift)&255))*f))<<shift;return c;}

 public static final class Definition {
  public int minRate,maxRate,minSize,maxSize,endSize,minSpeed,maxSpeed,endSpeed,minLife,maxLife,minColour,maxColour,fadeColour,speedChange;
  public int[] angles;
  public int[] globalDirectionRaw;
  public boolean globalConstantSpeed;
  public boolean fixedStyle;
  public void validate() {
   if(minRate<0||maxRate<minRate||maxRate>1024||minLife<=0||maxLife<minLife||maxLife>1000||speedChange<=0)
    throw new IllegalArgumentException("Unsupported particle definition");
  }
 }
 public static final class Emitter {
  public final int face;public final float[] xyz;public final Definition def;
  public Emitter(int face,float[] xyz,Definition def){if(xyz.length!=9)throw new IllegalArgumentException("Invalid triangle");for(float n:xyz)if(!Float.isFinite(n))throw new IllegalArgumentException("Invalid position");this.face=face;this.xyz=xyz.clone();this.def=def;}
 }
 public static final class Particle {
  public double x,y,z,radius,age;public int colour;
  public double dx,dy,dz,directionScale=1;
  public final double speed,life,startRadius;public final int startColour;public final Definition def;
  Particle(double[] xyz,double[] dir,double size,double speed,double life,int colour,Definition def){x=xyz[0];y=xyz[1];z=xyz[2];dx=dir[0];dy=dir[1];dz=dir[2];radius=startRadius=size;this.speed=speed;this.life=life;this.colour=startColour=colour;this.def=def;}
 }
}
