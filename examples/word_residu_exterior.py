#
# jruby examples for jas.
# $Id$
#

#from java.lang import System

from jas import WordRing, WordPolyRing, WordPolyIdeal
from jas import terminate, startLog
from jas import QQ, ZZ, GF, ZM, WRC

# exterior calculus example
# Hartley and Tuckey, 1993,
# GB in Clifford and Grassmann algebras

r = WordPolyRing(QQ(), "a,b,c,f,g,h,u,v,w,x,y,z");
print "WordPolyRing: " + str(r);
print;

one,a,b,c,f,g,h,u,v,w,x,y,z = r.gens();
print "a = %s" % a;
print "z = %s" % z;
print;

# exterior algebra relations
rs = [
  b * a   - a * b ,
  c * a   - a * c ,
  f * a   - a * f ,
  g * a   - a * g ,
  h * a   - a * h ,
  u * a   - a * u ,
  v * a   - a * v ,
  w * a   - a * w ,
  x * a   - a * x ,
  y * a   - a * y ,
  z * a   - a * z ,

  c * b   - b * c ,
  f * b   - b * f ,
  g * b   - b * g ,
  h * b   - b * h ,
  u * b   - b * u ,
  v * b   - b * v ,
  w * b   - b * w ,
  x * b   - b * x ,
  y * b   - b * y ,
  z * b   - b * z ,

  f * c   - c * f ,
  g * c   - c * g ,
  h * c   - c * h ,
  u * c   - c * u ,
  v * c   - c * v ,
  w * c   - c * w ,
  x * c   - c * x ,
  y * c   - c * y ,
  z * c   - c * z ,

  g * f   - f * g ,
  h * f   - f * h ,
  u * f   - f * u ,
  v * f   - f * v ,
  w * f   - f * w ,
  x * f   - f * x ,
  y * f   - f * y ,
  z * f   - f * z ,

  h * g   - g * h ,
  u * g   - g * u ,
  v * g   - g * v ,
  w * g   - g * w ,
  x * g   - g * x ,
  y * g   - g * y ,
  z * g   - g * z ,

  u * h   - h * u ,
  v * h   - h * v ,
  w * h   - h * w ,
  x * h   - h * x ,
  y * h   - h * y ,
  z * h   - h * z ,

  v * u   - u * v ,
  w * u   - u * w ,
  x * u   - u * x ,
  y * u   - u * y ,
  z * u   - u * z ,

  w * v   - v * w ,
  x * v   - v * x ,
  y * v   - v * y ,
  z * v   - v * z ,

  x * w   - w * x ,
  y * w   - w * y ,
  z * w   - w * z ,

  y * x   - x * y ,
  z * x   - x * z ,

  z * y   - y * z ,

  a * a ,
  b * b ,
  c * c ,
  f * f ,
  g * g ,
  h * h ,
  u * u ,
  v * v ,
  w * w ,
  x * x ,
  y * y ,
  z * z 
];


fi = r.ideal("", rs); 
print "WordPolyIdeal: " + str(fi);
print;


ff = [
 ( a*b + c*f + g*h ),
 ( u*v + w*x + y*z ),
 ( a*v + w*x + y*z )
];


r1 = WRC(fi,ff[0]);
print "r1: " + str(r1);
r2 = WRC(fi,ff[1]);
print "r2: " + str(r2);
r3 = WRC(fi,ff[2]);
print "r3: " + str(r3);
print;


r4 = r1*r2 + r3;
print "r4 = r1*r2 + r3: " + str(r4);
r5 = r2*r1 + r3;
print "r5 = r2*r1 + r3: " + str(r5);
print;

print "r4 == r5: " + str(r4 == r5);
print;

r6 = r4**3;
print "r6 = r4**3: " + str(r6);
print;


r7 = r6/r4;
print "r7 = r6 / r4: " + str(r7);
r8 = r6 % r4;
print "r8 = r6 % r4: " + str(r8);
print;

