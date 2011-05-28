/*
 * $Id$
 */

package edu.jas.application;


import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.BasicConfigurator;

import edu.jas.arith.BigDecimal;
import edu.jas.arith.BigRational;
import edu.jas.kern.ComputerThreads;
import edu.jas.poly.Complex;
import edu.jas.poly.ComplexRing;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.TermOrder;
import edu.jas.root.ComplexRoots;
import edu.jas.root.ComplexRootsSturm;
import edu.jas.root.Rectangle;
import edu.jas.structure.Power;
import edu.jas.ufd.Squarefree;
import edu.jas.ufd.SquarefreeFactory;


/**
 * RootUtil tests with JUnit.
 * @author Heinz Kredel.
 */

public class ComplexRootTest extends TestCase {


    /**
     * main.
     */
    public static void main(String[] args) {
        BasicConfigurator.configure();
        junit.textui.TestRunner.run(suite());
        ComputerThreads.terminate();
    }


    /**
     * Constructs a <CODE>ComplexRootTest</CODE> object.
     * @param name String.
     */
    public ComplexRootTest(String name) {
        super(name);
    }


    /**
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(ComplexRootTest.class);
        return suite;
    }


    TermOrder to = new TermOrder(TermOrder.INVLEX);


    GenPolynomialRing<Complex<BigRational>> dfac;


    ComplexRing<BigRational> cfac;


    BigRational eps;


    Complex<BigRational> ceps;


    GenPolynomial<Complex<BigRational>> a;


    GenPolynomial<Complex<BigRational>> b;


    GenPolynomial<Complex<BigRational>> c;


    GenPolynomial<Complex<BigRational>> d;


    GenPolynomial<Complex<BigRational>> e;


    int rl = 1;


    int kl = 3;


    int ll = 3;


    int el = 5;


    float q = 0.7f;


    @Override
    protected void setUp() {
        a = b = c = d = e = null;
        cfac = new ComplexRing<BigRational>(new BigRational(1));
        String[] vars = new String[] { "z" };
        dfac = new GenPolynomialRing<Complex<BigRational>>(cfac, rl, to, vars);
        eps = Power.positivePower(new BigRational(1L, 10L), BigDecimal.DEFAULT_PRECISION);
        ceps = new Complex<BigRational>(cfac, eps);
    }


    @Override
    protected void tearDown() {
        a = b = c = d = e = null;
        dfac = null;
        cfac = null;
        eps = null;
    }


    /**
     * Test complex roots, imaginary.
     */
    public void xtestComplexRootsImag() {
        //Complex<BigRational> I = cfac.getIMAG(); 
        //a = dfac.parse("z^3 - i2");
        //a = dfac.random(ll+1).monic();
        //a = dfac.parse("z^7 - 2 z");
        a = dfac.parse("z^6 - i2");
        //System.out.println("a = " + a);

        List<Complex<RealAlgebraicNumber<BigRational>>> roots;
        roots = RootFactory.<BigRational> complexAlgebraicNumbersComplex(a);
        //System.out.println("a = " + a);
        //System.out.println("roots = " + roots);
        assertTrue("#roots == deg(a) ", roots.size() == a.degree(0));
        for (Complex<RealAlgebraicNumber<BigRational>> root : roots) {
            //System.out.println("root = " + root.getRe().decimalMagnitude() + " + " + root.getIm().decimalMagnitude() + " i");
        }
    }


    /*
     * Test complex roots, random polynomial.
     */
    public void xtestComplexRootsRand() {
        //Complex<BigRational> I = cfac.getIMAG(); 
        a = dfac.random(ll + 1).monic();
        if (a.isZERO() || a.isONE()) {
            a = dfac.parse("z^6 - i2");
        }
        Squarefree<Complex<BigRational>> sqf = SquarefreeFactory
                        .<Complex<BigRational>> getImplementation(cfac);
        a = sqf.squarefreePart(a);
        //System.out.println("a = " + a);
        List<Complex<RealAlgebraicNumber<BigRational>>> roots;
        roots = RootFactory.<BigRational> complexAlgebraicNumbersComplex(a);
        //System.out.println("a = " + a);
        //System.out.println("roots = " + roots);
        assertTrue("#roots == deg(a): " + (roots.size() - a.degree(0)) + ", a = " + a,
                        roots.size() == a.degree(0));
        for (Complex<RealAlgebraicNumber<BigRational>> root : roots) {
            //System.out.println("root = " + root.getRe().decimalMagnitude() + " + " + root.getIm().decimalMagnitude() + " i");
        }
    }


    /**
     * Test polynomial with complex roots.
     */
    public void testPolynomialComplexRoots() {
        a = dfac.parse("z^3 - 2");
        //System.out.println("a = " + a);
        List<Complex<RealAlgebraicNumber<BigRational>>> roots = RootFactory
                        .<BigRational> complexAlgebraicNumbersComplex(a);
        //System.out.println("a = " + a);
        //System.out.println("roots = " + roots);
        assertTrue("#roots == deg(a) ", roots.size() == a.degree(0));
        for (Complex<RealAlgebraicNumber<BigRational>> car : roots) {
            //System.out.println("car = " + car);
            assertTrue("f(r) == 0: " + car, RootFactory.<BigRational> isRoot(a,car));
        }
        Complex<RealAlgebraicNumber<BigRational>> root = roots.get(2); // 0,1,2)
        //System.out.println("a = " + a);
        //System.out.println("root = " + root.getRe().decimalMagnitude() + " + "
        //                + root.getIm().decimalMagnitude() + " i");
        //System.out.println("root = " + root.getRe() + " + " + root.getIm() + " i");
        String vre = root.getRe().toString().replace("{", "").replace("}", "").trim();
        String vim = root.getIm().toString().replace("{", "").replace("}", "").trim();
        //System.out.println("vre = " + vre);
        //System.out.println("vim = " + vim);
        String IM = root.ring.getIMAG().toString().replace("{", "").replace("}", "").replace(" ", "").trim();
        System.out.println("IM  = " + IM);

        GenPolynomialRing<Complex<RealAlgebraicNumber<BigRational>>> cring 
            = new GenPolynomialRing<Complex<RealAlgebraicNumber<BigRational>>>(root.ring, to, new String[] { "t" });
        List<GenPolynomial<Complex<RealAlgebraicNumber<BigRational>>>> gens = cring.generators();
        //System.out.println("gens  = " + gens);

        GenPolynomial<Complex<RealAlgebraicNumber<BigRational>>> cpol;
        //cpol = cring.random(1, 4, 4, q);

        //cpol = cring.univariate(0,3L).subtract(cring.fromInteger(2L));
        //cpol = cring.univariate(0,3L).subtract(gens.get(2));
        //cpol = cring.univariate(0,5L).subtract(cring.univariate(0,2L).multiply(root));
        //cpol = cring.univariate(0,4L).subtract(root);
        //cpol = cring.univariate(0,4L).subtract(root.multiply(root));
        cpol = cring.univariate(0,3L).subtract(cring.univariate(0,1L).multiply(root).sum(root.multiply(root)));
        String vpol = vre + " + " + IM + " " + vim;
        //String vpol = " 3 + " + IM + " * 3 ";
        //String vpol = " 3i3 ";
        //String vpol = IM + " " + vim;
        //String vpol = " 2 ";// + vre; // + " " + IM;
        //String vpol = vre; // + " " + IM;
        //System.out.println("vpol = " + vpol);
        //cpol = cring.univariate(0, 3L).subtract(cring.parse(vpol));
        cpol = cpol.monic();
        System.out.println("cpol = " + cpol);
        long d = cpol.degree(0);
        Squarefree<Complex<RealAlgebraicNumber<BigRational>>> sen 
            = SquarefreeFactory.<Complex<RealAlgebraicNumber<BigRational>>> getImplementation(root.ring);
        cpol = sen.squarefreePart(cpol);
        if ( cpol.degree(0) < d ) {
            System.out.println("cpol = " + cpol);
        }
        // new version with recursion: with real factorization
        List<Complex<RealAlgebraicNumber<RealAlgebraicNumber<BigRational>>>> croots = RootFactory
                        .<RealAlgebraicNumber<BigRational>> complexAlgebraicNumbersComplex(cpol);
        System.out.println("\na = " + a.toScript());
        System.out.println("root = " + root.getRe().decimalMagnitude() + " + "
                       + root.getIm().decimalMagnitude() + " i");
        System.out.println("root = " + root.getRe().toScript() + " + (" + root.getIm().toScript() + ") i");
        System.out.println("cpol = " + cpol.toScript() + ", " + cpol);
        //System.out.println("croots = " + croots);
        for (Complex<RealAlgebraicNumber<RealAlgebraicNumber<BigRational>>> croot : croots) {
            //System.out.println("croot = " + croot);
            System.out.println("croot = " + croot.getRe().toScript() + " + ( " + croot.getIm().toScript() + ") i");
            //System.out.println("croot = " + croot.getRe().decimalMagnitude() + " + "
            //                + croot.getIm().decimalMagnitude() + " i");
            assertTrue("f(r) == 0: " + croot, RootFactory.<RealAlgebraicNumber<BigRational>> isRoot(cpol,croot));
        }
        assertTrue("#croots == deg(cpol) " + croots.size() + " != " + cpol.degree(0), croots.size() == cpol.degree(0));


        // existing version with winding number and recursion: but only one step
        List<edu.jas.root.ComplexAlgebraicNumber<RealAlgebraicNumber<BigRational>>> coroots;
        coroots = edu.jas.root.RootFactory
                        .<RealAlgebraicNumber<BigRational>> complexAlgebraicNumbersComplex(cpol);
        System.out.println("\ncpol = " + cpol);
        for (edu.jas.root.ComplexAlgebraicNumber<RealAlgebraicNumber<BigRational>> cr2 : coroots) {
            System.out.println("r2.ring = " + cr2.ring); //magnitude());
            assertTrue("f(r) == 0: " + cr2, edu.jas.root.RootFactory.<RealAlgebraicNumber<BigRational>> isRootComplex(cpol,cr2));
        }

        // decimal for comparison
        for (Complex<RealAlgebraicNumber<RealAlgebraicNumber<BigRational>>> croot : croots) {
            System.out.println("croot = " + croot.getRe().decimalMagnitude() + " + "
                            + croot.getIm().decimalMagnitude() + " i");
        }
        for (edu.jas.root.ComplexAlgebraicNumber<RealAlgebraicNumber<BigRational>> cr2 : coroots) {
            System.out.println("r2.dec  = " + cr2.decimalMagnitude());
        }
        assertTrue("#coroots == deg(cpol) ", coroots.size() == cpol.degree(0));
    }

}