package com.evolvedmind.nero.particles;
import static org.lwjgl.opengl.GL33C.*;
import org.lwjgl.opengl.GL;
import java.util.*;

/** Runs only at the SHA-qualified 117 HD ZoneRenderer scene-pass tail, before resolve/UI. */
final class HdParticleSurface {
 private Object context;private int program,vao,vbo;private int frames;
 private boolean depthChecked;
 private int shader(int type,String source) {
  int id=glCreateShader(type);glShaderSource(id,source);glCompileShader(id);
  if(glGetShaderi(id,GL_COMPILE_STATUS)==0){String log=glGetShaderInfoLog(id);glDeleteShader(id);throw new IllegalStateException(log);}
  return id;
 }
 private void initialize() {
  Object active=GL.getCapabilities();if(context==active && program!=0)return;
  // IDs from a destroyed HD context must never be deleted in a new context.
  context=active;program=vao=vbo=0;depthChecked=false;
  int vertex=shader(GL_VERTEX_SHADER,"#version 330 core\nlayout(location=0) in vec3 pos;layout(location=1) in vec2 uv;layout(location=2) in vec4 color;out vec2 disc;out vec4 tint;void main(){gl_Position=vec4(pos,1);disc=uv;tint=color;}");
  int fragment=shader(GL_FRAGMENT_SHADER,"#version 330 core\nin vec2 disc;in vec4 tint;out vec4 frag;void main(){float f=1-dot(disc,disc);if(f<=0)discard;float a=tint.a*f*f;if(a<0.0039)discard;frag=vec4(tint.rgb,a);}");
  program=glCreateProgram();glAttachShader(program,vertex);glAttachShader(program,fragment);glLinkProgram(program);glDeleteShader(vertex);glDeleteShader(fragment);
  if(glGetProgrami(program,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(program));
  vao=glGenVertexArrays();vbo=glGenBuffers();
 }
 private void depthCheck() {
  int drawFbo=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING),readFbo=glGetInteger(GL_READ_FRAMEBUFFER_BINDING),tex=glGetInteger(GL_TEXTURE_BINDING_2D),pack=glGetInteger(GL_PIXEL_PACK_BUFFER_BINDING);
  int[] viewport=new int[4];glGetIntegerv(GL_VIEWPORT,viewport);float[] clear=new float[4];glGetFloatv(GL_COLOR_CLEAR_VALUE,clear);double clearDepth=glGetDouble(GL_DEPTH_CLEAR_VALUE);
  int fbo=glGenFramebuffers(),color=glGenTextures(),z=glGenTextures();
  try {
   glBindFramebuffer(GL_FRAMEBUFFER,fbo);
   glBindTexture(GL_TEXTURE_2D,color);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,8,8,0,GL_RGBA,GL_UNSIGNED_BYTE,0L);
   glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,color,0);
   glBindTexture(GL_TEXTURE_2D,z);glTexImage2D(GL_TEXTURE_2D,0,GL_DEPTH_COMPONENT32F,8,8,0,GL_DEPTH_COMPONENT,GL_FLOAT,0L);
   glFramebufferTexture2D(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,z,0);glDrawBuffer(GL_COLOR_ATTACHMENT0);glReadBuffer(GL_COLOR_ATTACHMENT0);
   if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("HD test FBO incomplete");
   glViewport(0,0,8,8);glClearColor(0,0,0,1);glClearDepth(0);glDepthMask(true);glEnable(GL_DEPTH_TEST);glDepthFunc(GL_GEQUAL);glDisable(GL_BLEND);glDisable(GL_CULL_FACE);
   glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);quad(.5f,0,0,1);
   glBindBuffer(GL_PIXEL_PACK_BUFFER,0);
   java.nio.ByteBuffer pixels=java.nio.ByteBuffer.allocateDirect(4);
   glReadPixels(4,4,1,1,GL_RGBA,GL_UNSIGNED_BYTE,pixels);int baseline=pixels.getInt(0);
   glDepthMask(false);quad(.1f,1,0,0);pixels.clear();glReadPixels(4,4,1,1,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
   if(pixels.getInt(0)!=baseline)throw new IllegalStateException("Far HD particle drew through test wall");
   quad(.8f,1,0,0);pixels.clear();glReadPixels(4,4,1,1,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
   if(pixels.getInt(0)==baseline)throw new IllegalStateException("Near HD particle was incorrectly occluded");
   depthChecked=true;
  }finally {
   glBindFramebuffer(GL_DRAW_FRAMEBUFFER,drawFbo);glBindFramebuffer(GL_READ_FRAMEBUFFER,readFbo);glBindTexture(GL_TEXTURE_2D,tex);glBindBuffer(GL_PIXEL_PACK_BUFFER,pack);
   glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);glClearColor(clear[0],clear[1],clear[2],clear[3]);glClearDepth(clearDepth);
   glDeleteFramebuffers(fbo);glDeleteTextures(color);glDeleteTextures(z);
  }
  com.evolvedmind.nero.compat.ModelProbe.event("hd-depth-proof","PASS real OpenGL shader: behind wall hidden, in front visible; private test FBO deleted, HD scene FBO and viewport restored");
 }
 private void quad(float depth,float r,float g,float b) {
  float[] vertices=new float[54];int at=0;
  for(int corner:new int[]{0,1,2,0,2,3}) {
   vertices[at++]=(corner==0||corner==3)?-1:1;vertices[at++]=corner<2?-1:1;vertices[at++]=depth;
   vertices[at++]=0;vertices[at++]=0;vertices[at++]=r;vertices[at++]=g;vertices[at++]=b;vertices[at++]=1;
  }
  glBufferData(GL_ARRAY_BUFFER,vertices,GL_STREAM_DRAW);glDrawArrays(GL_TRIANGLES,0,6);
 }
 int draw(Object renderer,List<ParticleSystem.Particle> particles)throws Exception {
  if(!renderer.getClass().getName().equals("rs117.hd.renderer.zone.ZoneRenderer"))throw new IllegalArgumentException("Unqualified HD renderer");
  if(glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)==0)throw new IllegalStateException("HD scene framebuffer absent");
  Object camera=renderer.getClass().getField("sceneCamera").get(renderer);
  float[] matrix=(float[])camera.getClass().getMethod("getViewProjMatrix").invoke(camera);
  float[] projection=(float[])camera.getClass().getMethod("getProjectionMatrix").invoke(camera);
  float[] eye=(float[])camera.getClass().getMethod("getPosition").invoke(camera);
  List<ParticleSystem.Particle> sorted=new ArrayList<>(particles);
  sorted.sort((a,b)->Double.compare(distance(b,eye),distance(a,eye)));
  float[] vertices=new float[Math.min(sorted.size(),ParticleSystem.LIMIT)*6*9];int at=0,count=0;
  for(var p:sorted) {
   if(count==ParticleSystem.LIMIT)break;
   float x=(float)p.x,y=(float)p.y,z=(float)p.z;
   float w=matrix[3]*x+matrix[7]*y+matrix[11]*z+matrix[15];if(!Float.isFinite(w)||w<=0)continue;
   float cx=(matrix[0]*x+matrix[4]*y+matrix[8]*z+matrix[12])/w;
   float cy=(matrix[1]*x+matrix[5]*y+matrix[9]*z+matrix[13])/w;
   float cz=(matrix[2]*x+matrix[6]*y+matrix[10]*z+matrix[14])/w;
   float rx=(float)Math.abs(projection[0]*p.radius/w),ry=(float)Math.abs(projection[5]*p.radius/w);
   if(!Float.isFinite(cx+cy+cz+rx+ry)||cz < -1 || cz>1 || Math.abs(cx)>1+rx || Math.abs(cy)>1+ry)continue;
   int c=p.colour;float[] color={(c>>>16&255)/255f,(c>>>8&255)/255f,(c&255)/255f,(c>>>24)/255f};
   for(int corner:new int[]{0,1,2,0,2,3}) {
    float u=(corner==0||corner==3)?-1:1,v=corner<2?-1:1;
    vertices[at++]=cx+u*rx;vertices[at++]=cy+v*ry;vertices[at++]=cz;vertices[at++]=u;vertices[at++]=v;
    for(float channel:color)vertices[at++]=channel;
   }
   count++;
  }
  if(count==0)return 0;
  int oldProgram=glGetInteger(GL_CURRENT_PROGRAM),oldVao=glGetInteger(GL_VERTEX_ARRAY_BINDING),oldBuffer=glGetInteger(GL_ARRAY_BUFFER_BINDING);
  int oldDepth=glGetInteger(GL_DEPTH_FUNC),srcRgb=glGetInteger(GL_BLEND_SRC_RGB),dstRgb=glGetInteger(GL_BLEND_DST_RGB),srcA=glGetInteger(GL_BLEND_SRC_ALPHA),dstA=glGetInteger(GL_BLEND_DST_ALPHA);
  int eqRgb=glGetInteger(GL_BLEND_EQUATION_RGB),eqA=glGetInteger(GL_BLEND_EQUATION_ALPHA);
  boolean depth=glIsEnabled(GL_DEPTH_TEST),blend=glIsEnabled(GL_BLEND),cull=glIsEnabled(GL_CULL_FACE),mask=glGetBoolean(GL_DEPTH_WRITEMASK);
  try {
   initialize();glUseProgram(program);glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);
   for(int i=0;i<3;i++)glEnableVertexAttribArray(i);
   glVertexAttribPointer(0,3,GL_FLOAT,false,36,0L);glVertexAttribPointer(1,2,GL_FLOAT,false,36,12L);glVertexAttribPointer(2,4,GL_FLOAT,false,36,20L);
   if(!depthChecked)depthCheck();
   glBufferData(GL_ARRAY_BUFFER,Arrays.copyOf(vertices,at),GL_STREAM_DRAW);
   glEnable(GL_DEPTH_TEST);glDepthFunc(GL_GEQUAL);glDepthMask(false);glDisable(GL_CULL_FACE);
   glEnable(GL_BLEND);glBlendEquation(GL_FUNC_ADD);glBlendFuncSeparate(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA,GL_ZERO,GL_ONE);
   boolean measure=++frames%100==0 && glGetQueryi(GL_SAMPLES_PASSED,GL_CURRENT_QUERY)==0;int query=measure?glGenQueries():0;
   if(measure)glBeginQuery(GL_SAMPLES_PASSED,query);
   glDrawArrays(GL_TRIANGLES,0,count*6);
   if(measure) {
    glEndQuery(GL_SAMPLES_PASSED);int passed=glGetQueryObjecti(query,GL_QUERY_RESULT);glDeleteQueries(query);
    com.evolvedmind.nero.compat.ModelProbe.event("hd-particle-frame","particles="+particles.size()+" submitted="+count+" passedSamples="+passed+" sceneFbo="+glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)+" depth=GEQUAL depthWrite=false clip="+vertices[0]+","+vertices[1]+","+vertices[2]+" eye="+Arrays.toString(eye));
   }
  }finally {
   glUseProgram(oldProgram);glBindVertexArray(oldVao);glBindBuffer(GL_ARRAY_BUFFER,oldBuffer);
   glDepthFunc(oldDepth);glDepthMask(mask);glBlendEquationSeparate(eqRgb,eqA);glBlendFuncSeparate(srcRgb,dstRgb,srcA,dstA);
   set(GL_DEPTH_TEST,depth);set(GL_BLEND,blend);set(GL_CULL_FACE,cull);
  }
  return count;
 }
 private static void set(int cap,boolean enabled){if(enabled)glEnable(cap);else glDisable(cap);}
 private static double distance(ParticleSystem.Particle p,float[] eye){double x=p.x-eye[0],y=p.y-eye[1],z=p.z-eye[2];return x*x+y*y+z*z;}
}
