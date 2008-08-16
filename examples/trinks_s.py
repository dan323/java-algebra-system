#
# jython examples for jas.
# $Id$
#

import sys;

from jas import Ring
from jas import Ideal
from jas import startLog
from jas import terminate


# trinks 6/7 example

#r = Ring( "Mod 19 (B,S,T,Z,P,W) L" );
#r = Ring( "Mod 1152921504606846883 (B,S,T,Z,P,W) L" ); # 2^60-93
#r = Ring( "Quat(B,S,T,Z,P,W) L" );
#r = Ring( "Z(B,S,T,Z,P,W) L" );
#r = Ring( "C(B,S,T,Z,P,W) L" );
r = Ring( "Z(B,S,T,Z,P,W) L" );
print "Ring: " + str(r);
print;

[B,S,T,Z,P,W] = r.gens();
print "r.gens() = ", [ str(f) for f in r.gens() ];
print;

f1 = 45 * P + 35 * S - 165 * B - 36;
f2 = 35 * P + 40 * Z + 25 * T - 27 * S;
f3 = 15 * W + 25 * S * P + 30 * Z - 18 * T - 165 * B**2;
f4 = - 9 * W + 15 * T * P + 20 * S * Z;
f5 = P * W + 2 * T * Z - 11 * B**3;
f6 = 99 * W - 11 *B * S + 3 * B**2;
f7 = 10000 * B**2 + 6600 * B + 2673;
F = [ f1, f2, f3, f4, f5, f6, f7 ];
#F = [ f1, f2, f3, f4, f5, f6 ]; 
#print "F = ", [ str(f) for f in F ];

I = r.ideal( "", list=F );
print "Ideal: " + str(I);
print;

#sys.exit();

rg = I.GB();
print "seq Output:", rg;
print;

terminate();
sys.exit();

ps = """
( 
 ( 45 P + 35 S - 165 B - 36 ), 
 ( 35 P + 40 Z + 25 T - 27 S ), 
 ( 15 W + 25 S P + 30 Z - 18 T - 165 B**2 ), 
 ( - 9 W + 15 T P + 20 S Z ), 
 ( P W + 2 T Z - 11 B**3 ), 
 ( 99 W - 11 B S + 3 B**2 ),
 ( 10000 B**2 + 6600 B + 2673 )
) 
""";

# ( 10000 B**2 + 6600 B + 2673 )
# ( B**2 + 33/50 B + 2673/10000 )

#f = Ideal( r, ps );
#print "Ideal: " + str(f);
#print;

f = r.ideal( ps );
print "Ideal: " + str(f);
print;

#startLog();

rg = f.GB();
#print "seq Output:", rg;
#print;

terminate();
sys.exit();

