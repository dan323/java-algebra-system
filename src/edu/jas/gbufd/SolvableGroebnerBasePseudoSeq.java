/*
 * $Id$
 */

package edu.jas.gbufd;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import edu.jas.gb.SolvableGroebnerBaseAbstract;
import edu.jas.gb.SolvableExtendedGB;
import edu.jas.gb.Pair;
import edu.jas.gb.PairList;
import edu.jas.gb.OrderedPairlist;
import edu.jas.poly.GenPolynomial;
//import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.PolynomialList;
import edu.jas.poly.GenSolvablePolynomial;
import edu.jas.poly.GenSolvablePolynomialRing;
import edu.jas.structure.GcdRingElem;
import edu.jas.structure.RingFactory;
import edu.jas.ufd.GCDFactory;
import edu.jas.ufd.GreatestCommonDivisorAbstract;


/**
 * Solvable Groebner Base with pseudo reduction sequential algorithm. Implements
 * coefficient fraction free Groebner bases.
 * @param <C> coefficient type
 * @author Heinz Kredel
 * 
 * @see edu.jas.application.GBAlgorithmBuilder
 * @see edu.jas.gbufd.GBFactory
 */

public class SolvableGroebnerBasePseudoSeq<C extends GcdRingElem<C>> 
             extends SolvableGroebnerBaseAbstract<C> {


    private static final Logger logger = Logger.getLogger(SolvableGroebnerBasePseudoSeq.class);


    private final boolean debug = logger.isDebugEnabled();


    /**
     * Greatest common divisor engine for coefficient content and primitive
     * parts.
     */
    protected final GreatestCommonDivisorAbstract<C> engine;


    /**
     * Pseudo reduction engine.
     */
    protected final SolvablePseudoReduction<C> red;


    /**
     * Coefficient ring factory.
     */
    protected final RingFactory<C> cofac;


    /**
     * Constructor.
     * @param rf coefficient ring factory.
     */
    public SolvableGroebnerBasePseudoSeq(RingFactory<C> rf) {
        this(new SolvablePseudoReductionSeq<C>(), rf, new OrderedPairlist<C>());
    }


    /**
     * Constructor.
     * @param rf coefficient ring factory.
     * @param pl pair selection strategy
     */
    public SolvableGroebnerBasePseudoSeq(RingFactory<C> rf, PairList<C> pl) {
        this(new SolvablePseudoReductionSeq<C>(), rf, pl);
    }


    /**
     * Constructor.
     * @param red pseudo reduction engine. <b>Note:</b> red must be an instance
     *            of PseudoReductionSeq.
     * @param rf coefficient ring factory. 
     * @param pl pair selection strategy
     */
    public SolvableGroebnerBasePseudoSeq(SolvablePseudoReduction<C> red, 
                   RingFactory<C> rf, PairList<C> pl) {
        super(red,pl);
        this.red = red;
        cofac = rf;
        engine = GCDFactory.<C> getImplementation(rf);
        //not used: engine = (GreatestCommonDivisorAbstract<C>)GCDFactory.<C>getProxy( rf );
    }


    /**
     * Left Groebner base using pairlist class.
     * @param modv module variable number.
     * @param F polynomial list.
     * @return GB(F) a Groebner base of F.
     */
    public List<GenSolvablePolynomial<C>> leftGB(int modv, List<GenSolvablePolynomial<C>> F) {
        List<GenSolvablePolynomial<C>> G = normalizeZerosOnes(F);
        G = PolynomialList.<C> castToSolvableList( engine.basePrimitivePart(PolynomialList.<C> castToList(G)) );
        if ( G.size() <= 1 ) {
            return G;
        }
        GenSolvablePolynomialRing<C> ring = G.get(0).ring;
        if ( ring.coFac.isField() ) { // TODO remove
            throw new IllegalArgumentException("coefficients from a field");
        }
        PairList<C> pairlist = strategy.create( modv, ring ); 
        pairlist.put( PolynomialList.<C> castToList(G) );

        Pair<C> pair;
        GenSolvablePolynomial<C> pi, pj, S, H;
        while (pairlist.hasNext()) {
            pair = pairlist.removeNext();
            if (pair == null)
                continue;

            pi = (GenSolvablePolynomial<C>) pair.pi;
            pj = (GenSolvablePolynomial<C>) pair.pj;
            if (debug) {
                logger.debug("pi    = " + pi);
                logger.debug("pj    = " + pj);
            }

            S = red.leftSPolynomial(pi, pj);
            if (S.isZERO()) {
                pair.setZero();
                continue;
            }
            if (debug) {
                logger.debug("ht(S) = " + S.leadingExpVector());
            }

            H = red.leftNormalform(G, S);
            if (H.isZERO()) {
                pair.setZero();
                continue;
            }
            if (debug) {
                logger.debug("ht(H) = " + H.leadingExpVector());
            }
            H = (GenSolvablePolynomial<C>) engine.basePrimitivePart(H); 
            H = (GenSolvablePolynomial<C>) H.abs();
            if (H.isConstant()) {
                G.clear();
                G.add(H);
                return G; // since no threads are activated
            }
            if (logger.isDebugEnabled()) {
                logger.debug("H = " + H);
            }
            if (H.length() > 0) {
                //l++;
                G.add(H);
                pairlist.put(H);
            }
        }
        logger.debug("#sequential list = " + G.size());
        G = leftMinimalGB(G);
        logger.info("" + pairlist);
        return G;
    }


    /**
     * Minimal ordered Solvable Groebner basis.
     * @param Gp a Solvable Groebner base.
     * @return a reduced Solvable Groebner base of Gp.
     */
    @Override
    public List<GenSolvablePolynomial<C>> leftMinimalGB(List<GenSolvablePolynomial<C>> Gp) {
        List<GenSolvablePolynomial<C>> G = normalizeZerosOnes(Gp);
        if (G.size() <= 1) {
            return G;
        }
        // remove top reducible polynomials
        GenSolvablePolynomial<C> a;
        List<GenSolvablePolynomial<C>> F = new ArrayList<GenSolvablePolynomial<C>>(G.size());
        List<GenPolynomial<C>> Gc, Fc;
        while (G.size() > 0) {
            a = G.remove(0);
            if (red.isTopReducible(G, a) || red.isTopReducible(F, a)) {
                // drop polynomial 
                if (debug) {
                    System.out.println("dropped " + a);
                    List<GenSolvablePolynomial<C>> ff;
                    ff = new ArrayList<GenSolvablePolynomial<C>>(G);
                    ff.addAll(F);
                    a = red.leftNormalform(ff, a);
                    if (!a.isZERO()) {
                        System.out.println("error, nf(a) " + a);
                    }
                }
            } else {
                F.add(a);
            }
        }
        G = F;
        if (G.size() <= 1) {
            return G;
        }
        Collections.reverse(G); // important for lex GB
        // reduce remaining polynomials
        int len = G.size();
        int i = 0;
        while (i < len) {
            a = G.remove(0);
            //System.out.println("doing " + a.length());
            a = red.leftNormalform(G, a);
            a = (GenSolvablePolynomial<C>) engine.basePrimitivePart(a); //a.monic(); not possible
            a = (GenSolvablePolynomial<C>) a.abs();
            //a = red.normalform( F, a );
            G.add(a); // adds as last
            i++;
        }
        return G;
    }


    /**
     * Twosided Solvable Groebner base using pairlist class.
     * @param modv number of module variables.
     * @param Fp solvable polynomial list.
     * @return tsGB(Fp) a twosided Groebner base of Fp.
     */
    @SuppressWarnings("unchecked")
    public List<GenSolvablePolynomial<C>> twosidedGB(int modv, List<GenSolvablePolynomial<C>> Fp) {
        if (Fp == null || Fp.size() == 0) { // 0 not 1
            return new ArrayList<GenSolvablePolynomial<C>>();
        }
        GenSolvablePolynomialRing<C> fac = Fp.get(0).ring; // assert != null
        //List<GenSolvablePolynomial<C>> X = generateUnivar( modv, Fp );
        List<GenSolvablePolynomial<C>> X = fac.univariateList(modv);
        //System.out.println("X univ = " + X);
        List<GenSolvablePolynomial<C>> F = new ArrayList<GenSolvablePolynomial<C>>(Fp.size() * (1 + X.size()));
        F.addAll(Fp);
        GenSolvablePolynomial<C> p, x, q;
        for (int i = 0; i < Fp.size(); i++) {
            p = Fp.get(i);
            for (int j = 0; j < X.size(); j++) {
                x = X.get(j);
                q = p.multiply(x);
                q = sred.leftNormalform(F, q);
                if (!q.isZERO()) {
                    F.add(q);
                }
            }
        }
        //System.out.println("F generated = " + F);
        List<GenSolvablePolynomial<C>> G = new ArrayList<GenSolvablePolynomial<C>>();
        PairList<C> pairlist = null;
        int l = F.size();
        ListIterator<GenSolvablePolynomial<C>> it = F.listIterator();
        while (it.hasNext()) {
            p = it.next();
            if (p.length() > 0) {
                p = p.monic();
                if (p.isONE()) {
                    G.clear();
                    G.add(p);
                    return G; // since no threads are activated
                }
                G.add(p);
                if (pairlist == null) {
                    // pairlist = new OrderedPairlist<C>( modv, p.ring );
                    pairlist = strategy.create(modv, p.ring);
                    if (!p.ring.coFac.isField()) {
                        //throw new IllegalArgumentException("coefficients not from a field");
                        logger.warn("coefficients not from a field " + p.ring.coFac);
                    }
                }
                // putOne not required
                pairlist.put(p);
            } else {
                l--;
            }
        }
        //System.out.println("G to check = " + G);
        if (l <= 1) { // 1 ok
            return G; // since no threads are activated
        }

        Pair<C> pair;
        GenSolvablePolynomial<C> pi, pj, S, H;
        while (pairlist.hasNext()) {
            pair = pairlist.removeNext();
            if (pair == null) {
                continue;
            }

            pi = (GenSolvablePolynomial<C>) pair.pi;
            pj = (GenSolvablePolynomial<C>) pair.pj;
            if (debug) {
                logger.debug("pi    = " + pi);
                logger.debug("pj    = " + pj);
            }

            S = sred.leftSPolynomial(pi, pj);
            if (S.isZERO()) {
                pair.setZero();
                continue;
            }
            if (debug) {
                logger.debug("ht(S) = " + S.leadingExpVector());
            }

            H = sred.leftNormalform(G, S);
            if (H.isZERO()) {
                pair.setZero();
                continue;
            }
            if (debug) {
                logger.debug("ht(H) = " + H.leadingExpVector());
            }

            H = H.monic();
            if (H.isONE()) {
                G.clear();
                G.add(H);
                return G; // since no threads are activated
            }
            if (debug) {
                logger.debug("H = " + H);
            }
            if (H.length() > 0) {
                l++;
                G.add(H);
                pairlist.put(H);
                for (int j = 0; j < X.size(); j++) {
                    l++;
                    x = X.get(j);
                    p = H.multiply(x);
                    p = sred.leftNormalform(G, p);
                    if (!p.isZERO()) {
                        p = p.monic();
                        if (p.isONE()) {
                            G.clear();
                            G.add(p);
                            return G; // since no threads are activated
                        }
                        G.add(p);
                        pairlist.put(p);
                    }
                }
            }
        }
        logger.debug("#sequential list = " + G.size());
        G = leftMinimalGB(G);
        logger.info("" + pairlist);
        return G;
    }


    /**
     * Solvable Extended Groebner base using critical pair class.
     * @param modv module variable number.
     * @param F solvable polynomial list.
     * @return a container for an extended left Groebner base of F.
     * <b>Note: </b> not implemented;
     */
    //@SuppressWarnings("unchecked")
    public SolvableExtendedGB<C> extLeftGB(int modv, List<GenSolvablePolynomial<C>> F) {
	throw new UnsupportedOperationException(); // TODO
    }

}