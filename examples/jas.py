#
# jython interface to jas.
# $Id$
#

from java.lang import System
from java.io import StringReader

from edu.jas.structure   import *
from edu.jas.arith       import *
from edu.jas.poly        import *
from edu.jas.ring        import *
from edu.jas.module      import *
from edu.jas.vector      import *
from edu.jas.application import *
from edu.jas.util        import *
from edu.jas             import *
from edu                 import *
#PrettyPrint.setInternal();

from org.apache.log4j import BasicConfigurator;

def startLog():
    BasicConfigurator.configure();


class Ring:

    def __init__(self,ringstr="",ring=None):
        if ring == None:
           sr = StringReader( ringstr );
           tok = GenPolynomialTokenizer(sr);
           self.pset = tok.nextPolynomialSet();
           self.ring = self.pset.ring;
        else:
           self.ring = ring;

    def __str__(self):
        return str(self.ring);

    def ideal(self,ringstr="",list=None):
        return Ideal(self,ringstr,list);


class Ideal:

    def __init__(self,ring,ringstr="",list=None):
        self.ring = ring;
        if list == None:
           sr = StringReader( ringstr );
           tok = GenPolynomialTokenizer(ring.pset.ring,sr);
           self.list = tok.nextPolynomialList();
        else:
           self.list = list;
        self.pset = OrderedPolynomialList(ring.ring,self.list);

    def __str__(self):
        return str(self.pset);

    def GB(self):
        s = self.pset;
        F = s.list;
        t = System.currentTimeMillis();
        G = GroebnerBaseSeq().GB(F);
        t = System.currentTimeMillis() - t;
        print "sequential executed in %s ms" % t; 
        return Ideal(self.ring,"",G);

    def parGB(self,th):
        s = self.pset;
        F = s.list;
        bbpar = GroebnerBaseSeqPairParallel(th);
        t = System.currentTimeMillis();
        G = bbpar.GB(F);
        t = System.currentTimeMillis() - t;
        bbpar.terminate();
        print "parallel %s executed in %s ms" % (th, t); 
        return Ideal(self.ring,"",G);

    def parOldGB(self,th):
        s = self.pset;
        F = s.list;
        bbpar = GroebnerBaseParallel(th);
        t = System.currentTimeMillis();
        G = bbpar.GB(F);
        t = System.currentTimeMillis() - t;
        bbpar.terminate();
        print "parallel-old %s executed in %s ms" % (th, t); 
        return Ideal(self.ring,"",G);

    def distGB(self,th=2,machine="examples/machines.localhost",port=7114):
        s = self.pset;
        F = s.list;
        t = System.currentTimeMillis();
        # G = GroebnerBaseDistributed.Server(F,th);
        G = GBDist(th,machine,port).execute(F);
        t = System.currentTimeMillis() - t;
        print "distributed %s executed in %s ms" % (th,t); 
        return Ideal(self.ring,"",G);

    def distClient(self,port=8114):
        s = self.pset;
        es = ExecutableServer( port );
        es.init();
        return None;

    def NF(self,reducer):
        s = self.pset;
        F = s.list;
        G = reducer.list;
        t = System.currentTimeMillis();
        N = ReductionSeq().normalform(G,F);
        t = System.currentTimeMillis() - t;
        print "sequential executed in %s ms" % t; 
        return Ideal(self.ring,"",N);

    def intersect(self,ring):
        s = self.pset;
        N = jas.ring.Ideal(s).intersect(ring.ring);
        return Ideal(self.ring,"",N.getList());


class SolvableRing:

    def __init__(self,ringstr="",ring=None):
        if ring == None:
           sr = StringReader( ringstr );
           tok = GenPolynomialTokenizer(sr);
           self.pset = tok.nextSolvablePolynomialSet();
           self.ring = self.pset.ring;
        else:
           self.ring = ring;
        if not self.ring.isAssociative():
           print "warning: ring is not associative";

    def __str__(self):
        return str(self.ring);

    def ideal(self,ringstr="",list=None):
        return SolvableIdeal(self,ringstr,list);


class SolvableIdeal:

    def __init__(self,ring,ringstr="",list=None):
        self.ring = ring;
        if list == None:
           sr = StringReader( ringstr );
           tok = GenPolynomialTokenizer(ring.ring,sr);
           self.list = tok.nextSolvablePolynomialList();
        else:
           self.list = list;
        self.pset = OrderedPolynomialList(ring.ring,self.list);

    def __str__(self):
        return str(self.pset);

    def leftGB(self):
        s = self.pset;
        F = s.list;
        t = System.currentTimeMillis();
        G = SolvableGroebnerBaseSeq().leftGB(F);
        t = System.currentTimeMillis() - t;
        print "executed in %s ms" % t; 
        return SolvableIdeal(self.ring,"",G);

    def twosidedGB(self):
        s = self.pset;
        F = s.list;
        t = System.currentTimeMillis();
        G = SolvableGroebnerBaseSeq().twosidedGB(F);
        t = System.currentTimeMillis() - t;
        print "executed in %s ms" % t; 
        return SolvableIdeal(self.ring,"",G);

    def rightGB(self):
        s = self.pset;
        F = s.list;
        t = System.currentTimeMillis();
        G = SolvableGroebnerBaseSeq().rightGB(F);
        t = System.currentTimeMillis() - t;
        print "executed in %s ms" % t; 
        return SolvableIdeal(self.ring,"",G);

    def intersect(self,ring):
        s = self.pset;
        N = jas.ring.Ideal(s).intersect(ring.ring);
        return SolvableIdeal(self.ring,"",N.getList());

    def parLeftGB(self,th):
        s = self.pset;
        F = s.list;
        bbpar = SolvableGroebnerBaseParallel(th);
        t = System.currentTimeMillis();
        G = bbpar.leftGB(F);
        t = System.currentTimeMillis() - t;
        bbpar.terminate();
        print "parallel %s executed in %s ms" % (th, t); 
        return Ideal(self.ring,"",G);

    def parTwosidedGB(self,th):
        s = self.pset;
        F = s.list;
        bbpar = SolvableGroebnerBaseParallel(th);
        t = System.currentTimeMillis();
        G = bbpar.twosidedGB(F);
        t = System.currentTimeMillis() - t;
        bbpar.terminate();
        print "parallel %s executed in %s ms" % (th, t); 
        return Ideal(self.ring,"",G);


class Module:

    def __init__(self,modstr="",ring=None):
        if ring == None:
           sr = StringReader( modstr );
           tok = GenPolynomialTokenizer(sr);
           self.mset = tok.nextSubModuleSet();
        else:
           self.mset = ModuleList(ring,None);
        self.ring = self.mset.ring;

    def __str__(self):
        return str(self.mset);

    def submodul(self,modstr="",list=None):
        return Submodule(self,modstr,list);


class SubModule:

    def __init__(self,module,modstr="",list=None):
        self.module = module;
        if list == None:
           sr = StringReader( modstr );
           tok = GenPolynomialTokenizer(module.ring,sr);
           self.list = tok.nextSubModuleList();
        else:
           self.list = list;
        self.mset = OrderedModuleList(module.ring,self.list);
        self.cols = self.mset.cols;
        self.rows = self.mset.rows;
        #print "cols = %s" % self.cols;
        #self.pset = self.mset.getPolynomialList();

    def __str__(self):
        return str(self.mset); # + "\n\n" + str(self.pset);

    def GB(self):
        t = System.currentTimeMillis();
        G = ModGroebnerBaseAbstract().GB(self.mset);
        t = System.currentTimeMillis() - t;
        print "executed in %s ms" % t; 
        return SubModule(self.module,"",G.list);



class SolvableModule:

    def __init__(self,modstr="",ring=None):
        if ring == None:
           sr = StringReader( modstr );
           tok = GenPolynomialTokenizer(sr);
           self.mset = tok.nextSolvableSubModuleSet();
        else:
           self.mset = ModuleList(ring,None);
        self.ring = self.mset.ring;

    def __str__(self):
        return str(self.mset);

    def solvsubmodul(self,modstr="",list=None):
        return Submodule(self,modstr,list);


class SolvableSubModule:

    def __init__(self,module,modstr="",list=None):
        self.module = module;
        if list == None:
           sr = StringReader( modstr );
           tok = GenPolynomialTokenizer(module.ring,sr);
           self.list = tok.nextSolvableSubModuleList();
        else:
           self.list = list;
        self.mset = OrderedModuleList(module.ring,self.list);
        self.cols = self.mset.cols;
        self.rows = self.mset.rows;

    def __str__(self):
        return str(self.mset); # + "\n\n" + str(self.pset);

    def leftGB(self):
        t = System.currentTimeMillis();
        G = ModSolvableGroebnerBaseAbstract().leftGB(self.mset);
        t = System.currentTimeMillis() - t;
        print "executed in %s ms" % t; 
        return SolvableSubModule(self.module,"",G.list);

    def twosidedGB(self):
        t = System.currentTimeMillis();
        G = ModSolvableGroebnerBaseAbstract().twosidedGB(self.mset);
        t = System.currentTimeMillis() - t;
        print "executed in %s ms" % t; 
        return SolvableSubModule(self.module,"",G.list);

    def rightGB(self):
        t = System.currentTimeMillis();
        G = ModSolvableGroebnerBaseAbstract().rightGB(self.mset);
        t = System.currentTimeMillis() - t;
        print "executed in %s ms" % t; 
        return SolvableSubModule(self.module,"",G.list);
