package com.evolvedmind.nero.particles;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.Model;

/** A private UI surface: never redirects or changes the game's rasterizer. */
final class CapePreviewRaster {
 final BufferedImage image;
 private final BufferedImage samples;
 private final int width,height;
 private final int[] pixels;
 private final double[] depth;
 private double scale,centerY;
 CapePreviewRaster(int width,int height) {
  image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
  this.width=width*3;this.height=height*3;samples=new BufferedImage(this.width,this.height,BufferedImage.TYPE_INT_ARGB);
  pixels=((DataBufferInt)samples.getRaster().getDataBuffer()).getData();depth=new double[this.width*this.height];
 }
 void model(Model model,boolean hood) {
  Arrays.fill(pixels,0);Arrays.fill(depth,Double.POSITIVE_INFINITY);
  float[] x=model.getVerticesX(),y=model.getVerticesY(),z=model.getVerticesZ();
  double minY=Double.POSITIVE_INFINITY,maxY=-Double.POSITIVE_INFINITY,maxX=1;
  for(int i=0;i<model.getVerticesCount();i++){minY=Math.min(minY,y[i]);maxY=Math.max(maxY,y[i]);maxX=Math.max(maxX,Math.abs(x[i]));}
  scale=Math.min((width-72)/(maxX*2),(height-84)/Math.max(1,maxY-minY));centerY=(minY+maxY)/2;
  int[] a=model.getFaceIndices1(),b=model.getFaceIndices2(),c=model.getFaceIndices3();
  int[] ca=model.getFaceColors1(),cb=model.getFaceColors2(),cc=model.getFaceColors3();byte[] alpha=model.getFaceTransparencies();
  for(int f=0;f<model.getFaceCount();f++) {
   if(cc[f]==-2||(alpha!=null&&(alpha[f]&255)==255))continue;
   int ia=a[f],ib=b[f],ic=c[f];int colorA=rgb(ca[f]),colorB=rgb(cc[f]==-1?ca[f]:cb[f]),colorC=rgb(cc[f]==-1?ca[f]:cc[f]);
   // View the decorated back of the cape. For the hood, view its face opening.
   double side=hood?-1:1;
   triangle(px(x[ia]*side),py(y[ia]),-z[ia]*side,px(x[ib]*side),py(y[ib]),-z[ib]*side,px(x[ic]*side),py(y[ic]),-z[ic]*side,colorA,colorB,colorC);
  }
  resolve();
 }
 double px(double x){return width/2.0+x*scale;}
 double py(double y){return height/2.0+(y-centerY)*scale;}
 void particles(List<ParticleSystem.Particle> particles) {
  for(var p:particles) {
   double x=px(p.x),y=py(p.y),radius=Math.min(192,Math.max(1.5,p.radius*scale));
   int minX=Math.max(0,(int)Math.floor(x-radius)),maxX=Math.min(width-1,(int)Math.ceil(x+radius));
   int minY=Math.max(0,(int)Math.floor(y-radius)),maxY=Math.min(height-1,(int)Math.ceil(y+radius));
   for(int yy=minY;yy<=maxY;yy++)for(int xx=minX;xx<=maxX;xx++) {
    int index=yy*width+xx;if(-p.z>depth[index]+.5)continue;
    double d=((xx+.5-x)*(xx+.5-x)+(yy+.5-y)*(yy+.5-y))/(radius*radius);if(d>=1)continue;
    int opacity=(int)((p.colour>>>24)*(1-d)*(1-d));if(opacity==0)continue;
    pixels[index]=blend(pixels[index],p.colour,opacity);
   }
  }
  resolve();
 }
 private void resolve() {
  java.awt.Graphics2D graphics=image.createGraphics();
  try {graphics.setComposite(java.awt.AlphaComposite.Src);graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);graphics.drawImage(samples,0,0,image.getWidth(),image.getHeight(),null);} finally {graphics.dispose();}
 }
 private void triangle(double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,int ac,int bc,int cc) {
  // Use the authored face winding, as the native cape rasterizer does. The
  // garment already contains its lining; no generated reversed skin is needed.
  double area=(bx-ax)*(cy-ay)-(by-ay)*(cx-ax);if(area<=.0001)return;
  int left=Math.max(0,(int)Math.floor(Math.min(ax,Math.min(bx,cx)))),right=Math.min(width-1,(int)Math.ceil(Math.max(ax,Math.max(bx,cx))));
  int top=Math.max(0,(int)Math.floor(Math.min(ay,Math.min(by,cy)))),bottom=Math.min(height-1,(int)Math.ceil(Math.max(ay,Math.max(by,cy))));
  for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++) {
   double u=((bx-x-.5)*(cy-y-.5)-(by-y-.5)*(cx-x-.5))/area;
   double v=((cx-x-.5)*(ay-y-.5)-(cy-y-.5)*(ax-x-.5))/area,w=1-u-v;
   if(u<0||v<0||w<0)continue;double z=az*u+bz*v+cz*w;int index=y*width+x;
   if(z>=depth[index])continue;depth[index]=z;int color=0;
   for(int shift=0;shift<=16;shift+=8)color|=((int)(((ac>>shift)&255)*u+((bc>>shift)&255)*v+((cc>>shift)&255)*w))<<shift;
   pixels[index]=0xff000000|color;
  }
 }
 private static int blend(int base,int color,int alpha) {
  int baseAlpha=base>>>24,outAlpha=alpha+baseAlpha*(255-alpha)/255,result=outAlpha<<24;
  for(int shift=0;shift<=16;shift+=8){int source=color>>>shift&255,dest=base>>>shift&255;int premult=source*alpha+dest*baseAlpha*(255-alpha)/255;result|=(premult/Math.max(1,outAlpha))<<shift;}
  return result;
 }
 static int rgb(int hsl) {
  double h=((hsl>>10)&63)/64.0+1.0/128,s=((hsl>>7)&7)/8.0+1.0/16,l=(hsl&127)/128.0;
  double q=l<.5?l*(1+s):l+s-l*s,p=2*l-q;int rgb=0;
  for(double t:new double[]{h+1.0/3,h,h-1.0/3}){t=(t+1)%1;double value=t<1.0/6?p+(q-p)*6*t:t<.5?q:t<2.0/3?p+(q-p)*(2.0/3-t)*6:p;rgb=(rgb<<8)|(int)(Math.pow(Math.max(0,value),.8)*255);}
  return rgb;
 }
}
