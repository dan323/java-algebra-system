/*
 * $Id$
 */

package edu.jas.root;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;

import edu.jas.arith.BigDecimal;
import edu.jas.arith.BigRational;
import edu.jas.arith.Roots;
import edu.jas.poly.ExpVector;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.PolyUtil;
import edu.jas.structure.Complex;
import edu.jas.structure.ComplexRing;
import edu.jas.structure.RingElem;
import edu.jas.structure.RingFactory;
import edu.jas.structure.UnaryFunctor;
import edu.jas.ufd.Squarefree;
import edu.jas.ufd.SquarefreeFactory;


/**
 * Complex roots abstract class.
 * @param <C> coefficient type.
 * @author Heinz Kredel
 */
public abstract class ComplexRootAbstract<C extends RingElem<C>> implements ComplexRoots<C> {


    private static final Logger logger = Logger.getLogger(ComplexRootAbstract.class);


    private static boolean debug = true || logger.isDebugEnabled();


    /**
     * Root bound. With f(M) * f(-M) != 0.
     * @param f univariate polynomial.
     * @return M such that -M &lt; root(f) &lt; M.
     */
    public Complex<C> rootBound(GenPolynomial<Complex<C>> f) {
        if (f == null) {
            return null;
        }
        RingFactory<Complex<C>> cfac = f.ring.coFac;
        Complex<C> M = cfac.getONE();
        if (f.isZERO() || f.isConstant()) {
            return M;
        }
        Complex<C> a = f.leadingBaseCoefficient().norm();
        for (Complex<C> c : f.getMap().values()) {
            Complex<C> d = c.norm().divide(a);
            if (M.compareTo(d) < 0) {
                M = d;
            }
        }
        M = M.sum(f.ring.coFac.getONE());
        //System.out.println("M = " + M);
        return M;
    }


    /**
     * Real part.
     * @param fac result polynomial factory.
     * @param A polynomial with BigComplex coefficients to be converted.
     * @return polynomial with real part of the coefficients.
     */
    public//static <C extends RingElem<C>>
    GenPolynomial<C> realPart(GenPolynomialRing<C> fac, GenPolynomial<Complex<C>> A) {
        return PolyUtil.<Complex<C>, C> map(fac, A, new RealPart<C>());
    }


    /**
     * Imaginary part.
     * @param fac result polynomial factory.
     * @param A polynomial with BigComplex coefficients to be converted.
     * @return polynomial with imaginary part of coefficients.
     */
    public//static <C extends RingElem<C>>
    GenPolynomial<C> imaginaryPart(GenPolynomialRing<C> fac, GenPolynomial<Complex<C>> A) {
        return PolyUtil.<Complex<C>, C> map(fac, A, new ImagPart<C>());
    }


    /**
     * Taylor series for polynomial.
     * @param f univariate polynomial.
     * @param a expansion point.
     * @return Taylor series (a polynomial) of f at a.
     */
    public static <C extends RingElem<C>> GenPolynomial<C> seriesOfTaylor(GenPolynomial<C> f, C a) {
        if (f == null) {
            return null;
        }
        GenPolynomialRing<C> fac = f.ring;
        if (fac.nvar > 1) {
            throw new RuntimeException("only for univariate polynomials");
        }
        if (f.isZERO() || f.isConstant()) {
            return f;
        }
        GenPolynomial<C> s = fac.getZERO();
        C fa = PolyUtil.<C> evaluateMain(fac.coFac, f, a);
        s = s.sum(fa);
        //System.out.println("s = " + s);
        long n = 1;
        long i = 0;
        GenPolynomial<C> g = PolyUtil.<C> baseDeriviative(f);
        GenPolynomial<C> p = fac.getONE();
        //GenPolynomial<C> xa = fac.univariate(0,1).subtract(a);
        //System.out.println("xa = " + xa);
        while (!g.isZERO()) {
            i++;
            n *= i;
            //p = p.multiply(xa);
            //System.out.println("p = " + p);
            fa = PolyUtil.<C> evaluateMain(fac.coFac, g, a);
            GenPolynomial<C> q = fac.univariate(0, i); //p;
            q = q.multiply(fa);
            q = q.divide(fac.fromInteger(n));
            //System.out.println("q = " + q);
            s = s.sum(q);
            g = PolyUtil.<C> baseDeriviative(g);
        }
        //System.out.println("s = " + s);
        return s;
    }


    /**
     * Substitute linear polynomial to polynomial.
     * @param f univariate polynomial.
     * @param a constant coefficient of substituent.
     * @param b linear coefficient of substituent.
     * @return f(a+b*z).
     */
    public static <C extends RingElem<C>> GenPolynomial<C> substituteLinear(GenPolynomial<C> f, C a, C b) {
        if (f == null) {
            return null;
        }
        GenPolynomialRing<C> fac = f.ring;
        if (fac.nvar > 1) {
            throw new RuntimeException("only for univariate polynomials");
        }
        if (f.isZERO() || f.isConstant()) {
            return f;
        }
        GenPolynomial<C> z = fac.univariate(0, 1L).multiply(b);
        // assert decending exponents, i.e. compatible term order
        Map<ExpVector, C> val = f.getMap();
        GenPolynomial<C> s = null;
        long el1 = -1; // undefined
        long el2 = -1;
        for (ExpVector e : val.keySet()) {
            el2 = e.getVal(0);
            if (s == null /*el1 < 0*/) { // first turn
                s = fac.getZERO().sum(val.get(e));
            } else {
                for (long i = el2; i < el1; i++) {
                    s = s.multiply(z);
                }
                s = s.sum(val.get(e));
            }
            el1 = el2;
        }
        for (long i = 0; i < el2; i++) {
            s = s.multiply(z);
        }
        //System.out.println("s = " + s);
        return s;
    }


    /**
     * Complex root count of complex polynomial on rectangle.
     * @param rect rectangle.
     * @param a univariate complex polynomial.
     * @return root count of a in rectangle.
     */
    public abstract long complexRootCount(Rectangle<C> rect, GenPolynomial<Complex<C>> a);


    /**
     * List of complex roots of complex polynomial a on rectangle.
     * @param rect rectangle.
     * @param a univariate squarefree complex polynomial.
     * @return list of complex roots.
     */
    public abstract List<Rectangle<C>> complexRoots(Rectangle<C> rect, GenPolynomial<Complex<C>> a);


    /**
     * List of complex roots of complex polynomial.
     * @param a univariate complex polynomial.
     * @return list of complex roots.
     */
    @SuppressWarnings("unchecked")
    public List<Rectangle<C>> complexRoots(GenPolynomial<Complex<C>> a) {
        ComplexRing<C> cr = (ComplexRing<C>) a.ring.coFac;
        Squarefree<Complex<C>> engine = SquarefreeFactory.<Complex<C>> getImplementation(cr);
        SortedMap<GenPolynomial<Complex<C>>, Long> sa = engine.squarefreeFactors(a);
        List<Rectangle<C>> roots = new ArrayList<Rectangle<C>>();
        for (GenPolynomial<Complex<C>> p : sa.keySet()) {
            Complex<C> Mb = rootBound(p);
            C M = Mb.getRe();
            C M1 = M.sum(M.factory().fromInteger(1));
            //System.out.println("M = " + M);
            if (debug) {
                logger.info("rootBound = " + M);
            }
            Complex<C>[] corner = (Complex<C>[]) new Complex[4];
            corner[0] = new Complex<C>(cr, M1.negate(), M); // nw
            corner[1] = new Complex<C>(cr, M1.negate(), M1.negate()); // sw
            corner[2] = new Complex<C>(cr, M, M1.negate()); // se
            corner[3] = new Complex<C>(cr, M, M); // ne
            Rectangle<C> rect = new Rectangle<C>(corner);

            List<Rectangle<C>> rs = complexRoots(rect, p);
            long e = sa.get(p);
            for (int i = 0; i < e; i++) {
                roots.addAll(rs);
            }
        }
        return roots;
    }


    /**
     * Complex root refinement of complex polynomial a on rectangle.
     * @param rect rectangle containing exactly one complex root.
     * @param a univariate squarefree complex polynomial.
     * @param len rational length for refinement.
     * @return refined complex root.
     */
    public Rectangle<C> complexRootRefinement(Rectangle<C> rect, GenPolynomial<Complex<C>> a, BigRational len) {
        ComplexRing<C> cr = (ComplexRing<C>) a.ring.coFac;
        Rectangle<C> root = rect;
        long w;
        if (debug) {
            w = complexRootCount(root, a);
            if (w < 1) {
                System.out.println("#root = " + w);
                System.out.println("root = " + root);
                throw new RuntimeException("no initial isolating rectangle " + rect);
            }
        }
        Complex<C> eps = cr.fromInteger(1);
        eps = eps.divide(cr.fromInteger(1000)); // 1/1000
        //System.out.println("eps = " + eps);

        while (root.rationalLength().compareTo(len) > 0) {

            //System.out.println("------------------------------------"); 
            //System.out.println("root = " + root + ", len = " + new BigDecimal(root.rationalLength())); 
            Complex<C> delta = root.corners[3].subtract(root.corners[1]);
            delta = delta.divide(cr.fromInteger(2));
            //System.out.println("delta = " + delta); 
            Complex<C> center = root.corners[1].sum(delta);
            while (center.isZERO() || center.getRe().isZERO() || center.getIm().isZERO()) {
                delta = delta.sum(delta.multiply(eps)); // distort
                //System.out.println("delta = " + delta); 
                center = root.corners[1].sum(delta);
                eps = eps.sum(eps.multiply(cr.getIMAG()));
            }
            //System.out.println("center = " + center); 
            if (debug) {
                logger.info("new center = " + center);
            }
            Complex<C>[] cp = Arrays.<Complex<C>> copyOf(root.corners, 4);
            // cp[0] fix
            cp[1] = new Complex<C>(cr, cp[1].getRe(), center.getIm());
            cp[2] = center;
            cp[3] = new Complex<C>(cr, center.getRe(), cp[3].getIm());
            Rectangle<C> nw = new Rectangle<C>(cp);
            //System.out.println("nw = " + nw); 
            w = complexRootCount(nw, a);
            //System.out.println("#nwr = " + w); 
            if (w == 1) {
                root = nw;
                continue;
            }

            cp = Arrays.<Complex<C>> copyOf(root.corners, 4);
            cp[0] = new Complex<C>(cr, cp[0].getRe(), center.getIm());
            // cp[1] fix
            cp[2] = new Complex<C>(cr, center.getRe(), cp[2].getIm());
            cp[3] = center;
            Rectangle<C> sw = new Rectangle<C>(cp);
            //System.out.println("sw = " + sw); 
            w = complexRootCount(sw, a);
            //System.out.println("#swr = " + w); 
            if (w == 1) {
                root = sw;
                continue;
            }

            cp = Arrays.<Complex<C>> copyOf(root.corners, 4);
            cp[0] = center;
            cp[1] = new Complex<C>(cr, center.getRe(), cp[1].getIm());
            // cp[2] fix
            cp[3] = new Complex<C>(cr, cp[3].getRe(), center.getIm());
            Rectangle<C> se = new Rectangle<C>(cp);
            //System.out.println("se = " + se); 
            w = complexRootCount(se, a);
            //System.out.println("#ser = " + w); 
            if (w == 1) {
                root = se;
                continue;
            }

            cp = Arrays.<Complex<C>> copyOf(root.corners, 4);
            cp[0] = new Complex<C>(cr, center.getRe(), cp[0].getIm());
            cp[1] = center;
            cp[2] = new Complex<C>(cr, cp[2].getRe(), center.getIm());
            // cp[3] fix
            Rectangle<C> ne = new Rectangle<C>(cp);
            //System.out.println("ne = " + ne); 
            w = complexRootCount(ne, a);
            //System.out.println("#ner = " + w); 
            if (w == 1) {
                root = ne;
                continue;
            }
            if (debug) {
                w = complexRootCount(root, a);
                System.out.println("#root = " + w);
                System.out.println("root = " + root);
            }
            throw new RuntimeException("no isolating rectangle " + rect);
        }
        return root;
    }


    /**
     * Distance.
     * @param a complex number.
     * @param b complex number.
     * @return |a-b|.
     */
    public C distance(Complex<C> a, Complex<C> b) {
        Complex<C> d = a.subtract(b);
        C r = d.norm().getRe();
        String s = r.toString();
        BigRational rs = new BigRational(s);
        //System.out.println("s  = " + s);
        BigDecimal rd = new BigDecimal(rs);
        rd = Roots.sqrt(rd);
        //System.out.println("rd = " + rd);
        r = a.ring.ring.parse(rd.toString());
        //System.out.println("rd = " + rd + ", r  = " + r);
        return r;
    }

}


/**
 * Real part functor.
 */
class RealPart<C extends RingElem<C>> implements UnaryFunctor<Complex<C>, C> {


    public C eval(Complex<C> c) {
        if (c == null) {
            return null;
        } else {
            return c.getRe();
        }
    }
}


/**
 * Imaginary part functor.
 */
class ImagPart<C extends RingElem<C>> implements UnaryFunctor<Complex<C>, C> {


    public C eval(Complex<C> c) {
        if (c == null) {
            return null;
        } else {
            return c.getIm();
        }
    }
}