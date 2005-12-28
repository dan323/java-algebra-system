#
# read output of jas runnings and prepare input for gnuplot
#

import re;
import os;

exam = "kat_7";
runs = "t16";

bname = exam + "_" + runs;
fname = bname + ".out";
oname = bname + ".po";

f=open(fname,"r");
print f;
o=open(oname,"w");
print o;

res = {};

for line in f:
    if line.find("executed in") > 0:
       print line,
       if line.find("sequential") >= 0:
          s = re.search(".*in (\d+)",line);
          if s != None:
             t = ('0', s.group(1));
             res[ int( t[0] ) ] = float( t[1] );
       else:
          s = re.search("(\d+).*in (\d+)",line);
          if s != None:
             t = s.groups();
             t0 = int( t[0] );
             if res.has_key( t0 ):
                 res[ t0 ] = ( res[ t0 ] + float(t[1]) )/2.0;
             else:
                 res[ t0 ] = float( t[1] );
#       print "t = ", t, "\n";

print "\nres = ", res;

ks = res.keys();
#print "ks = ", ks;

ks.sort();
#print "ks = ", ks;

s1 = ks[0];
st = res[ s1 ];

#print "s1 = ", s1;
#print "st = ", st;

speed = {};

for k in ks:
    speed[ k ] = st / res[ k ]; 

#print "speed = ", speed;

print;

o.write("\n#threads time speedup\n");
print "#threads time speedup";
for k in ks:
    o.write(str(k) + " " + str(res[k]) + " " + str(speed[k]) +"\n");
    print(str(k) + " " + str(res[k]) + " " + str(speed[k]));

f.close();
o.close();


#---------------------------------------
pscript = """
set grid 
set term %s
set output "%s.ps"
set title "GBs of Katsuras example on compute" 
set time
set xlabel "#CPUs" 

set multiplot

set size 0.75,0.5
set origin 0,0.5
set ylabel "milliseconds" 
# smooth acsplines
plot "%s.po" title '%s computing time' with linespoints, \
     "%s.po" using 1:( %s/$1 ) title '%s ideal' with linespoints 

set size 0.75,0.5
set origin 0,0
set ylabel "speedup" 
# smooth bezier
plot  "%s.po" using 1:3 title '%s speedup' with linespoints, \
      "%s.po" using 1:1 title '%s ideal' with linespoints 

unset multiplot
pause mouse
""";
#---------------------------------------

psname = bname + ".ps";
pname = "plotin.gp"
p=open(pname,"w");
print p;

pscript = pscript %       ("x11",bname,bname,exam,bname,str(res[0]),exam,bname,exam,bname,exam);
#pscript = pscript % ("postscript",bname,bname,exam,bname,str(res[0]),exam,bname,exam,bname,exam);

p.write(pscript);
p.close();

os.system("gnuplot plotin.gp");

cmd = "ps2pdf " + psname;
print "cmd: " + cmd;
#os.system(cmd);
