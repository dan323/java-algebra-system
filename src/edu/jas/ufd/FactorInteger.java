/*
 * $Id$
 */

package edu.jas.ufd;


import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;

import org.apache.log4j.Logger;

import edu.jas.arith.BigInteger;
import edu.jas.arith.ModIntegerRing;
import edu.jas.arith.ModLongRing;
import edu.jas.arith.Modular;
import edu.jas.arith.ModularRingFactory;
import edu.jas.arith.PrimeList;
import edu.jas.poly.ExpVector;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.PolyUtil;
import edu.jas.structure.GcdRingElem;
import edu.jas.structure.Power;
import edu.jas.structure.RingElem;
import edu.jas.structure.RingFactory;
import edu.jas.util.KsubSet;


/**
 * Integer coefficients factorization algorithms. This class implements
 * factorization methods for polynomials over integers.
 * @author Heinz Kredel
 */

public class FactorInteger<MOD extends GcdRingElem<MOD> & Modular> extends FactorAbstract<BigInteger> {


    private static final Logger logger = Logger.getLogger(FactorInteger.class);


    private final boolean debug = true || logger.isDebugEnabled();


    /**
     * Factorization engine for modular base coefficients.
     */
    protected final FactorAbstract<MOD> mfactor;


    /**
     * Gcd engine for modular base coefficients.
     */
    protected final GreatestCommonDivisorAbstract<MOD> mengine;


    /**
     * No argument constructor.
     */
    public FactorInteger() {
        this(BigInteger.ONE);
    }


    /**
     * Constructor.
     * @param cfac coefficient ring factory.
     */
    public FactorInteger(RingFactory<BigInteger> cfac) {
        super(cfac);
        ModularRingFactory<MOD> mcofac = (ModularRingFactory<MOD>) (Object) new ModLongRing(13, true); // hack
        mfactor = FactorFactory.getImplementation(mcofac); //new FactorModular(mcofac);
        mengine = GCDFactory.getImplementation(mcofac);
        //mengine = GCDFactory.getProxy(mcofac);
    }


    /**
     * GenPolynomial base factorization of a squarefree polynomial.
     * @param P squarefree and primitive! GenPolynomial.
     * @return [p_1,...,p_k] with P = prod_{i=1, ..., k} p_i.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<GenPolynomial<BigInteger>> baseFactorsSquarefree(GenPolynomial<BigInteger> P) {
        if (P == null) {
            throw new IllegalArgumentException(this.getClass().getName() + " P == null");
        }
        List<GenPolynomial<BigInteger>> factors = new ArrayList<GenPolynomial<BigInteger>>();
        if (P.isZERO()) {
            return factors;
        }
        if (P.isONE()) {
            factors.add(P);
            return factors;
        }
        GenPolynomialRing<BigInteger> pfac = P.ring;
        if (pfac.nvar > 1) {
            throw new IllegalArgumentException(this.getClass().getName() + " only for univariate polynomials");
        }
        if (P.degree(0) <= 1L) {
            factors.add(P);
            return factors;
        }
        // compute norm
        BigInteger an = P.maxNorm();
        BigInteger ac = P.leadingBaseCoefficient();
        //compute factor coefficient bounds
        ExpVector degv = P.degreeVector();
        int degi = (int) P.degree(0);
        BigInteger M = an.multiply(PolyUtil.factorBound(degv));
        M = M.multiply(ac.abs().multiply(ac.fromInteger(8)));
        //System.out.println("M = " + M);
        //M = M.multiply(M); // test

        //initialize prime list and degree vector
        PrimeList primes = new PrimeList(PrimeList.Range.small);
        int pn = 30; //primes.size();
        ModularRingFactory<MOD> cofac = null;
        GenPolynomial<MOD> am = null;
        GenPolynomialRing<MOD> mfac = null;
        final int TT = 5; // 7
        List<GenPolynomial<MOD>>[] modfac = new List[TT];
        List<GenPolynomial<BigInteger>>[] intfac = new List[TT];
        BigInteger[] plist = new BigInteger[TT];
        List<GenPolynomial<MOD>> mlist = null;
        List<GenPolynomial<BigInteger>> ilist = null;
        int i = 0;
        if (debug) {
            logger.debug("an  = " + an);
            logger.debug("ac  = " + ac);
            logger.debug("M   = " + M);
            logger.info("degv = " + degv);
        }
        Iterator<java.math.BigInteger> pit = primes.iterator();
        pit.next(); // skip p = 2
        pit.next(); // skip p = 3
        MOD nf = null;
        for (int k = 0; k < TT; k++) {
            if (k == TT - 1) { // -2
                primes = new PrimeList(PrimeList.Range.medium);
                pit = primes.iterator();
            }
            if (k == TT + 1) { // -1
                primes = new PrimeList(PrimeList.Range.large);
                pit = primes.iterator();
            }
            while (pit.hasNext()) {
                java.math.BigInteger p = pit.next();
                //System.out.println("next run ++++++++++++++++++++++++++++++++++");
                if (++i >= pn) {
                    logger.error("prime list exhausted, pn = " + pn);
                    throw new ArithmeticException("prime list exhausted");
                }
                if (ModLongRing.MAX_LONG.compareTo(p) > 0) {
                    cofac = (ModularRingFactory) new ModLongRing(p, true);
                } else {
                    cofac = (ModularRingFactory) new ModIntegerRing(p, true);
                }
                logger.info("prime = " + cofac);
                nf = cofac.fromInteger(ac.getVal());
                if (nf.isZERO()) {
                    logger.info("unlucky prime (nf) = " + p);
                    //System.out.println("unlucky prime (nf) = " + p);
                    continue;
                }
                // initialize polynomial factory and map polynomial
                mfac = new GenPolynomialRing<MOD>(cofac, pfac);
                am = PolyUtil.<MOD> fromIntegerCoefficients(mfac, P);
                if (!am.degreeVector().equals(degv)) { // allways true
                    logger.info("unlucky prime (deg) = " + p);
                    //System.out.println("unlucky prime (deg) = " + p);
                    continue;
                }
                GenPolynomial<MOD> ap = PolyUtil.<MOD> baseDeriviative(am);
                if (ap.isZERO()) {
                    logger.info("unlucky prime (a')= " + p);
                    //System.out.println("unlucky prime (a')= " + p);
                    continue;
                }
                GenPolynomial<MOD> g = mengine.baseGcd(am, ap);
                if (g.isONE()) {
                    logger.info("**lucky prime = " + p);
                    //System.out.println("**lucky prime = " + p);
                    break;
                }
            }
            // now am is squarefree mod p, make monic and factor mod p
            if (!nf.isONE()) {
                //System.out.println("nf = " + nf);
                am = am.divide(nf); // make monic
            }
            mlist = mfactor.baseFactorsSquarefree(am);
            if (logger.isInfoEnabled()) {
                logger.info("modlist  = " + mlist);
            }
            if (mlist.size() <= 1) {
                factors.add(P);
                return factors;
            }
            if (!nf.isONE()) {
                GenPolynomial<MOD> mp = mfac.getONE(); //mlist.get(0);
                //System.out.println("mp = " + mp);
                mp = mp.multiply(nf);
                //System.out.println("mp = " + mp);
                mlist.add(0, mp); // set(0,mp);
            }
            modfac[k] = mlist;
            plist[k] = cofac.getIntegerModul(); // p
        }

        // search shortest factor list
        int min = Integer.MAX_VALUE;
        BitSet AD = null;
        for (int k = 0; k < TT; k++) {
            List<ExpVector> ev = PolyUtil.<MOD> leadingExpVector(modfac[k]);
            BitSet D = factorDegrees(ev, degi);
            if (AD == null) {
                AD = D;
            } else {
                AD.and(D);
            }
            int s = modfac[k].size();
            logger.info("mod(" + plist[k] + ") #s = " + s + ", D = " + D /*+ ", lt = " + ev*/);
            //System.out.println("mod s = " + s);
            if (s < min) {
                min = s;
                mlist = modfac[k];
            }
        }
        logger.info("min = " + min + ", AD = " + AD);
        if (mlist.size() <= 1) {
            logger.info("mlist.size() = 1");
            factors.add(P);
            return factors;
        }
        if (AD.cardinality() <= 2) { // only one possible factor
            logger.info("degree set cardinality = " + AD.cardinality());
            factors.add(P);
            return factors;
        }

        boolean allLists = false; //true; //false;
        if (allLists) {
            // try each factor list
            for (int k = 0; k < TT; k++) {
                mlist = modfac[k];
                if (debug) {
                    logger.info("lifting from " + mlist);
                }
                if (false && P.leadingBaseCoefficient().isONE()) {
                    factors = searchFactorsMonic(P, M, mlist, AD); // does now work in all cases
                    if (factors.size() == 1) {
                        factors = searchFactorsNonMonic(P, M, mlist, AD);
                    }
                } else {
                    factors = searchFactorsNonMonic(P, M, mlist, AD);
                }
                intfac[k] = factors;
            }
        } else {
            // try only shortest factor list
            if (debug) {
                logger.info("lifting shortest from " + mlist);
            }
            if (true && P.leadingBaseCoefficient().isONE()) {
                long t = System.currentTimeMillis();
                try {
                    mlist = PolyUtil.<MOD> monic(mlist);
                    factors = searchFactorsMonic(P, M, mlist, AD); // does now work in all cases
                    t = System.currentTimeMillis() - t;
                    //System.out.println("monic time = " + t);
                    if (false && debug) {
                        t = System.currentTimeMillis();
                        List<GenPolynomial<BigInteger>> fnm = searchFactorsNonMonic(P, M, mlist, AD);
                        t = System.currentTimeMillis() - t;
                        System.out.println("non monic time = " + t);
                        if (debug) {
                            if (!factors.equals(fnm)) {
                                System.out.println("monic factors     = " + factors);
                                System.out.println("non monic factors = " + fnm);
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    t = System.currentTimeMillis();
                    factors = searchFactorsNonMonic(P, M, mlist, AD);
                    t = System.currentTimeMillis() - t;
                    //System.out.println("only non monic time = " + t);
                }
            } else {
                long t = System.currentTimeMillis();
                factors = searchFactorsNonMonic(P, M, mlist, AD);
                t = System.currentTimeMillis() - t;
                //System.out.println("non monic time = " + t);
            }
            return factors;
        }

        // search longest factor list
        int max = 0;
        for (int k = 0; k < TT; k++) {
            int s = intfac[k].size();
            logger.info("int s = " + s);
            //System.out.println("int s = " + s);
            if (s > max) {
                max = s;
                ilist = intfac[k];
            }
        }
        factors = ilist;
        return factors;
    }


    /**
     * BitSet for factor degree list.
     * @param E exponent vector list.
     * @return b_0,...,b_k} a BitSet of possible factor degrees.
     */
    public BitSet factorDegrees(List<ExpVector> E, int deg) {
        BitSet D = new BitSet(deg + 1);
        D.set(0); // constant factor
        for (ExpVector e : E) {
            int i = (int) e.getVal(0);
            BitSet s = new BitSet(deg + 1);
            for (int k = 0; k < deg + 1 - i; k++) { // shift by i places
                s.set(i + k, D.get(k));
            }
            //System.out.println("s = " + s);
            D.or(s);
            //System.out.println("D = " + D);
        }
        return D;
    }


    /**
     * Sum of all degrees.
     * @param L univariate polynomial list.
     * @return sum deg(p) for p in L.
     */
    public static <C extends RingElem<C>> long degreeSum(List<GenPolynomial<C>> L) {
        long s = 0L;
        for (GenPolynomial<C> p : L) {
            ExpVector e = p.leadingExpVector();
            long d = e.getVal(0);
            s += d;
        }
        return s;
    }


    /**
     * Factor search with modular Hensel lifting algorithm. Let p =
     * f_i.ring.coFac.modul() i = 0, ..., n-1 and assume C == prod_{0,...,n-1}
     * f_i mod p with ggt(f_i,f_j) == 1 mod p for i != j
     * @param C GenPolynomial.
     * @param M bound on the coefficients of g_i as factors of C.
     * @param F = [f_0,...,f_{n-1}] List&lt;GenPolynomial&gt;.
     * @param D bit set of possible factor degrees.
     * @return [g_0,...,g_{n-1}] = lift(C,F), with C = prod_{0,...,n-1} g_i mod
     *         p**e. <b>Note:</b> does not work in all cases.
     */
    List<GenPolynomial<BigInteger>> searchFactorsMonic(GenPolynomial<BigInteger> C, BigInteger M,
                    List<GenPolynomial<MOD>> F, BitSet D) {
        //System.out.println("*** monic factor combination ***");
        if (C == null || C.isZERO() || F == null || F.size() == 0) {
            throw new IllegalArgumentException("C must be nonzero and F must be nonempty");
        }
        GenPolynomialRing<BigInteger> pfac = C.ring;
        if (pfac.nvar != 1) { // todo assert
            throw new IllegalArgumentException("polynomial ring not univariate");
        }
        List<GenPolynomial<BigInteger>> factors = new ArrayList<GenPolynomial<BigInteger>>(F.size());
        List<GenPolynomial<MOD>> mlist = F;
        List<GenPolynomial<MOD>> lift;

        //MOD nf = null;
        GenPolynomial<MOD> ct = mlist.get(0);
        if (ct.isConstant()) {
            //nf = ct.leadingBaseCoefficient();
            mlist.remove(ct);
            //System.out.println("=== nf = " + nf);
            if (mlist.size() <= 1) {
                factors.add(C);
                return factors;
            }
        } else {
            //nf = ct.ring.coFac.getONE();
        }
        //System.out.println("modlist  = " + mlist); // includes not ldcf
        ModularRingFactory<MOD> mcfac = (ModularRingFactory<MOD>) ct.ring.coFac;
        BigInteger m = mcfac.getIntegerModul();
        long k = 1;
        BigInteger pi = m;
        while (pi.compareTo(M) < 0) {
            k++;
            pi = pi.multiply(m);
        }
        logger.info("p^k = " + m + "^" + k);
        GenPolynomial<BigInteger> PP = C, P = C;
        // lift via Hensel
        try {
            lift = HenselUtil.<MOD> liftHenselMonic(PP, mlist, k);
            //System.out.println("lift = " + lift);
        } catch (NoLiftingException e) {
            throw new RuntimeException(e);
        }
        if (logger.isInfoEnabled()) {
            logger.info("lifted modlist = " + lift);
        }
        GenPolynomialRing<MOD> mpfac = lift.get(0).ring;

        // combine trial factors
        int dl = (lift.size() + 1) / 2;
        //System.out.println("dl = " + dl); 
        GenPolynomial<BigInteger> u = PP;
        long deg = (u.degree(0) + 1L) / 2L;
        //System.out.println("deg = " + deg); 
        //BigInteger ldcf = u.leadingBaseCoefficient();
        //System.out.println("ldcf = " + ldcf); 
        for (int j = 1; j <= dl; j++) {
            //System.out.println("j = " + j + ", dl = " + dl + ", lift = " + lift); 
            KsubSet<GenPolynomial<MOD>> ps = new KsubSet<GenPolynomial<MOD>>(lift, j);
            for (List<GenPolynomial<MOD>> flist : ps) {
                //System.out.println("degreeSum = " + degreeSum(flist));
                if (!D.get((int) FactorInteger.<MOD> degreeSum(flist))) {
                    logger.info("skipped by degree set " + D + ", deg = " + degreeSum(flist));
                    continue;
                }
                GenPolynomial<MOD> mtrial = mpfac.getONE();
                for (int kk = 0; kk < flist.size(); kk++) {
                    GenPolynomial<MOD> fk = flist.get(kk);
                    mtrial = mtrial.multiply(fk);
                }
                //System.out.println("+flist = " + flist + ", mtrial = " + mtrial);
                if (mtrial.degree(0) > deg) { // this test is sometimes wrong
                    logger.info("degree " +  mtrial.degree(0) + " > deg " + deg);
                    //continue;
                }
                //System.out.println("+flist    = " + flist);
                GenPolynomial<BigInteger> trial = PolyUtil.integerFromModularCoefficients(pfac, mtrial);
                //System.out.println("+trial = " + trial);
                //trial = engine.basePrimitivePart( trial.multiply(ldcf) );
                trial = engine.basePrimitivePart(trial);
                //System.out.println("pp(trial)= " + trial);
                if (PolyUtil.<BigInteger> basePseudoRemainder(u, trial).isZERO()) {
                    logger.info("successful trial = " + trial);
                    //System.out.println("trial    = " + trial);
                    //System.out.println("flist    = " + flist);
                    //trial = engine.basePrimitivePart(trial);
                    //System.out.println("pp(trial)= " + trial);
                    factors.add(trial);
                    u = PolyUtil.<BigInteger> basePseudoDivide(u, trial); //u.divide( trial );
                    //System.out.println("u        = " + u);
                    if (lift.removeAll(flist)) {
                        logger.info("new lift= " + lift);
                        dl = (lift.size() + 1) / 2;
                        //System.out.println("dl = " + dl); 
                        j = 0; // since j++
                        break;
                    }
                    logger.error("error removing flist from lift = " + lift);
                }
            }
        }
        if (!u.isONE() && !u.equals(P)) {
            logger.info("rest u = " + u);
            //System.out.println("rest u = " + u);
            factors.add(u);
        }
        if (factors.size() == 0) {
            logger.info("irred u = " + u);
            //System.out.println("irred u = " + u);
            factors.add(PP);
        }
        return factors;
    }


    /**
     * Factor search with modular Hensel lifting algorithm. Let p =
     * f_i.ring.coFac.modul() i = 0, ..., n-1 and assume C == prod_{0,...,n-1}
     * f_i mod p with ggt(f_i,f_j) == 1 mod p for i != j
     * @param C GenPolynomial.
     * @param M bound on the coefficients of g_i as factors of C.
     * @param F = [f_0,...,f_{n-1}] List&lt;GenPolynomial&gt;.
     * @param D bit set of possible factor degrees.
     * @return [g_0,...,g_{n-1}] = lift(C,F), with C = prod_{0,...,n-1} g_i mod
     *         p**e.
     */
    List<GenPolynomial<BigInteger>> searchFactorsNonMonic(GenPolynomial<BigInteger> C, BigInteger M,
                    List<GenPolynomial<MOD>> F, BitSet D) {
        //System.out.println("*** non monic factor combination ***");
        if (C == null || C.isZERO() || F == null || F.size() == 0) {
            throw new IllegalArgumentException("C must be nonzero and F must be nonempty");
        }
        GenPolynomialRing<BigInteger> pfac = C.ring;
        if (pfac.nvar != 1) { // todo assert
            throw new IllegalArgumentException("polynomial ring not univariate");
        }
        List<GenPolynomial<BigInteger>> factors = new ArrayList<GenPolynomial<BigInteger>>(F.size());
        List<GenPolynomial<MOD>> mlist = F;

        MOD nf = null;
        GenPolynomial<MOD> ct = mlist.get(0);
        if (ct.isConstant()) {
            nf = ct.leadingBaseCoefficient();
            mlist.remove(ct);
            //System.out.println("=== nf   = " + nf);
            //System.out.println("=== ldcf = " + C.leadingBaseCoefficient());
            if (mlist.size() <= 1) {
                factors.add(C);
                return factors;
            }
        } else {
            nf = ct.ring.coFac.getONE();
        }
        //System.out.println("modlist  = " + mlist); // includes not ldcf
        GenPolynomialRing<MOD> mfac = ct.ring;
        GenPolynomial<MOD> Pm = PolyUtil.<MOD> fromIntegerCoefficients(mfac, C);
        GenPolynomial<BigInteger> PP = C, P = C;

        // combine trial factors
        int dl = (mlist.size() + 1) / 2;
        GenPolynomial<BigInteger> u = PP;
        long deg = (u.degree(0) + 1L) / 2L;
        GenPolynomial<MOD> um = Pm;
        //BigInteger ldcf = u.leadingBaseCoefficient();
        //System.out.println("ldcf = " + ldcf); 
        HenselApprox<MOD> ilist = null;
        for (int j = 1; j <= dl; j++) {
            //System.out.println("j = " + j + ", dl = " + dl + ", ilist = " + ilist); 
            KsubSet<GenPolynomial<MOD>> ps = new KsubSet<GenPolynomial<MOD>>(mlist, j);
            for (List<GenPolynomial<MOD>> flist : ps) {
                //System.out.println("degreeSum = " + degreeSum(flist));
                if (!D.get((int) FactorInteger.<MOD> degreeSum(flist))) {
                    logger.info("skipped by degree set " + D + ", deg = " + degreeSum(flist));
                    continue;
                }
                GenPolynomial<MOD> trial = mfac.getONE().multiply(nf);
                for (int kk = 0; kk < flist.size(); kk++) {
                    GenPolynomial<MOD> fk = flist.get(kk);
                    trial = trial.multiply(fk);
                }
                if (trial.degree(0) > deg) { // this test is sometimes wrong
                    logger.info("degree > deg " + deg + ", degree = " + trial.degree(0));
                    //continue;
                }
                GenPolynomial<MOD> cofactor = um.divide(trial);
                //System.out.println("trial    = " + trial);
                //System.out.println("cofactor = " + cofactor);

                // lift via Hensel
                try {
                    // ilist = HenselUtil.liftHenselQuadraticFac(PP, M, trial, cofactor);
                    ilist = HenselUtil.<MOD> liftHenselQuadratic(PP, M, trial, cofactor);
                    //ilist = HenselUtil.<MOD> liftHensel(PP, M, trial, cofactor);
                } catch (NoLiftingException e) {
                    // no liftable factors
                    if ( /*debug*/logger.isDebugEnabled()) {
                        logger.info("no liftable factors " + e);
                        e.printStackTrace();
                    }
                    continue;
                }
                GenPolynomial<BigInteger> itrial = ilist.A;
                GenPolynomial<BigInteger> icofactor = ilist.B;
                if (logger.isDebugEnabled()) {
                    logger.info("       modlist = " + trial + ", cofactor " + cofactor);
                    logger.info("lifted intlist = " + itrial + ", cofactor " + icofactor);
                }
                //System.out.println("lifted intlist = " + itrial + ", cofactor " + icofactor); 

                itrial = engine.basePrimitivePart(itrial);
                //System.out.println("pp(trial)= " + itrial);
                if (PolyUtil.<BigInteger> basePseudoRemainder(u, itrial).isZERO()) {
                    logger.info("successful trial = " + itrial);
                    //System.out.println("trial    = " + itrial);
                    //System.out.println("cofactor = " + icofactor);
                    //System.out.println("flist    = " + flist);
                    //itrial = engine.basePrimitivePart(itrial);
                    //System.out.println("pp(itrial)= " + itrial);
                    factors.add(itrial);
                    //u = PolyUtil.<BigInteger> basePseudoDivide(u, itrial); //u.divide( trial );
                    u = icofactor;
                    PP = u; // fixed finally on 2009-05-03
                    um = cofactor;
                    //System.out.println("u        = " + u);
                    //System.out.println("um       = " + um);
                    if (mlist.removeAll(flist)) {
                        logger.info("new mlist= " + mlist);
                        dl = (mlist.size() + 1) / 2;
                        j = 0; // since j++
                        break;
                    }
                    logger.error("error removing flist from ilist = " + mlist);
                }
            }
        }
        if (!u.isONE() && !u.equals(P)) {
            logger.info("rest u = " + u);
            //System.out.println("rest u = " + u);
            factors.add(u);
        }
        if (factors.size() == 0) {
            logger.info("irred u = " + u);
            //System.out.println("irred u = " + u);
            factors.add(PP);
        }
        return factors;
    }


    /**
     * GenPolynomial factorization of a multivariate squarefree polynomial, using Hensel lifting.
     * @param P squarefree and primitive! (respectively monic) multivariate GenPolynomial over the integers.
     * @return [p_1,...,p_k] with P = prod_{i=1,...,r} p_i.
     */
    public List<GenPolynomial<BigInteger>> factorsSquarefreeHensel(GenPolynomial<BigInteger> P) {
        if (P == null) {
            throw new IllegalArgumentException(this.getClass().getName() + " P != null");
        }
        GenPolynomialRing<BigInteger> pfac = P.ring;
        if (pfac.nvar == 1) {
            return baseFactorsSquarefree(P);
        }
        List<GenPolynomial<BigInteger>> factors = new ArrayList<GenPolynomial<BigInteger>>();
        if (P.isZERO()) {
            return factors;
        }
        if (P.degreeVector().totalDeg() <= 1L) {
            factors.add(P);
            return factors;
        }
        GenPolynomialRing<GenPolynomial<BigInteger>> rfac = pfac.recursive(1);
        System.out.println("pfac = " + pfac.toScript());
        System.out.println("rfac = " + rfac.toScript());
        GenPolynomial<GenPolynomial<BigInteger>> Pr = PolyUtil.<BigInteger> recursive(rfac,P);
        GenPolynomialRing<BigInteger> cfac = (GenPolynomialRing<BigInteger>) rfac.coFac;
        System.out.println("cfac = " + cfac.toScript());

        GenPolynomial<BigInteger> ps = Pr.leadingBaseCoefficient(); 
        GenPolynomial<BigInteger> pd = P; 
        System.out.println("ps = " + ps);
        System.out.println("pd = " + pd);
        // ldcf(pd)
        BigInteger ac = pd.leadingBaseCoefficient();
        SortedMap<GenPolynomial<BigInteger>,Long> ldfacs = factors(ps);
        System.out.println("ldfacs = " + ldfacs);

        //initialize prime list
        PrimeList primes = new PrimeList(PrimeList.Range.medium); // PrimeList.Range.medium);
        Iterator<java.math.BigInteger> primeIter = primes.iterator();
        int pn = 50; //primes.size();

        //for ( int i = 0; i < 11; i++ ) { // meta loop
        for ( int i = 0; i < 1; i++ ) { // meta loop
            System.out.println("======================================================= run " 
                               + pfac.nvar + ", " + i);
            java.math.BigInteger p = null; //new java.math.BigInteger("19"); //primes.next();
            // 5 small, 5 medium and 1 large size primes
            if ( i == 0 ) { // medium size
                primes = new PrimeList(PrimeList.Range.medium);
                primeIter = primes.iterator();
            }
            if ( i == 5 ) { // small size
                primes = new PrimeList(PrimeList.Range.small);
                primeIter = primes.iterator();
                p = primeIter.next(); // 2
                p = primeIter.next(); // 3
                p = primeIter.next(); // 5
                p = primeIter.next(); // 7
            }
            if ( i == 10 ) { // large size
                primes = new PrimeList(PrimeList.Range.large);
                primeIter = primes.iterator();
            }
            ModularRingFactory<MOD> cofac = null;
            int pi = 0;
            while ( pi < pn && primeIter.hasNext() && pi == 0) {
                p = primeIter.next();
                p = new java.math.BigInteger("19"); // test
                logger.info("prime = " + p);
                // initialize coefficient factory and map normalization factor and polynomials
                ModularRingFactory<MOD> cf = null;
                if (ModLongRing.MAX_LONG.compareTo(p) > 0) {
                    cf = (ModularRingFactory) new ModLongRing(p, true);
                } else {
                    cf = (ModularRingFactory) new ModIntegerRing(p, true);
                }
                MOD nf = cf.fromInteger(ac.getVal());
                if (nf.isZERO()) {
                    continue;
                }
                nf = cf.fromInteger(pd.leadingBaseCoefficient().getVal());
                if (nf.isZERO()) {
                    continue;
                }
                nf = cf.fromInteger(ps.leadingBaseCoefficient().getVal());
                if (nf.isZERO()) {
                    continue;
                }
                cofac = cf;
                break;
            }
            if ( cofac == null ) { // no lucky prime found
                throw new RuntimeException("giving up on Hensel preparation");
            }
            logger.info("lucky prime = " + cofac.getIntegerModul());

            List<MOD> V = new ArrayList<MOD>(1);
            GenPolynomialRing<MOD> mfac = new GenPolynomialRing<MOD>(cofac, pfac);
            GenPolynomialRing<MOD> mcfac = new GenPolynomialRing<MOD>(cofac, cfac);
            System.out.println("mfac  = " + mfac.toScript());
            System.out.println("mcfac = " + mcfac.toScript());
            GenPolynomial<MOD> Pm = PolyUtil.<MOD> fromIntegerCoefficients(mfac, pd);
            System.out.println("Pm = " + Pm);

            // search evaluation point and evaluate
            GenPolynomialRing<MOD> ckfac = mfac;
            GenPolynomial<MOD> pe = Pm;
            GenPolynomial<MOD> pep;
            for ( int j = pfac.nvar; j > 1; j-- ) {
                // evaluation to univariate case
                long degp = pe.degree(ckfac.nvar-2);
                ckfac = ckfac.contract(1);
                long vi = 1L; //(long)(pfac.nvar-j); // 1L; 0 not so good for small p
                if ( p.longValue() > 1000L ) {
                    //vi = (long)j+1L;
                    vi = 0L;
                }
                // search small evaluation point
                while( true ) { 
                    MOD vp = cofac.fromInteger(vi++);
                    //System.out.println("vp = " + vp);
                    if ( vp.isZERO() && vi != 1L ) { // all elements of Z_p exhausted
                        pe = null;
                        break;
                    }
                    pep = PolyUtil.<MOD> evaluateMain(ckfac,pe,vp);
                    //System.out.println("pep = " + pep);

                    // check lucky evaluation point 
                    MOD pl = pep.leadingBaseCoefficient();
                    if (pl.isZERO()) { // nearly non sense
                        continue;
                    }
                    if (degp != pep.degree(ckfac.nvar-1)) {
                        System.out.println("deg(pe) = " + degp + ", deg(pep) = " + pep.degree(ckfac.nvar-1));
                        continue;
                    }
                    // check squarefree
                    if ( !mfactor.isSquarefree(pep) ) {
                        System.out.println("not squarefeee = " + vp);
                        continue;
                    }
                    V.add(vp);
                    pe = pep;
                    break;
                }
                if ( pe == null ) {
                    break;
                }
            }
            if ( pe == null ) {
                continue;
            }
            logger.info("evaluation points  = " + V);
            System.out.println("pe = " + pe);

            // recursion base:
            SortedMap<GenPolynomial<MOD>,Long> Ce = mfactor.baseFactors(pe);
            if ( Ce.size() <= 1 ) {
                factors.add(P);
                return factors;
            }
            //System.out.println("Ce = " + Ce);

            // double check 
            //if ( mfactor.factorsDegree(Ce) != P.degree() ) {
            //    continue;
            //}
            boolean sqf = true;
            for ( Long ll : Ce.values() ) {
		if ( ll > 1L ) {
                    sqf = false;
                    break;
                }
            }
            if ( ! sqf ) {
                logger.info("base factors = " + Ce);
                continue;
            }
            // now the factorization is known to be squarefree
            List<GenPolynomial<MOD>> F = new ArrayList<GenPolynomial<MOD>>( Ce.keySet() );
            logger.info("base factors squarefree = " + F);

            // norm
            BigInteger an = pd.maxNorm();
            BigInteger mn = an.multiply(ac.abs()).multiply(new BigInteger(2L));

            long k = Power.logarithm(new BigInteger(p),mn) + 1L;
            System.out.println("mn = " + mn);
            System.out.println("k = " + k);
        
            GenPolynomialRing<GenPolynomial<BigInteger>> rnfac = pfac.recursive(pfac.nvar-1);
            GenPolynomial<GenPolynomial<BigInteger>> pr = PolyUtil.<BigInteger>recursive(rnfac,pd);
            GenPolynomial<GenPolynomial<BigInteger>> prr = PolyUtil.<BigInteger>switchVariables(pr);
            GenPolynomial<BigInteger> lprr = prr.leadingBaseCoefficient();
            System.out.println("prr      = " + prr);
            System.out.println("lprr      = " + lprr);

            SortedMap<GenPolynomial<BigInteger>,Long> lfactors = factors(lprr);
            System.out.println("lfactors = " + lfactors);

            List<GenPolynomial<BigInteger>> lf = new ArrayList<GenPolynomial<BigInteger>>();
            for ( GenPolynomial<MOD> pp : F) {
		lf.add( lprr.ring.getONE() ); // ps
            }
            lf.set(1, lfactors.firstKey() ); // ps
            System.out.println("lf = " + lf);

            List<GenPolynomial<MOD>> mlift;
            try {
                mlift = HenselMultUtil.<MOD> liftHenselFull(pd,F,V,k,lf);
                logger.info("mlift = " + mlift);
            } catch ( NoLiftingException nle ) {
                //System.out.println("exception : " + nle);
                continue;  
            } catch ( ArithmeticException ae ) {
                //System.out.println("exception : " + ae);
                continue;  
            }
            if ( mlift.size() <= 1 ) { // irreducible mod I, p^k
                factors.add(P);
                return factors;
	    }

            // convert factors Ci from Z_{p^k}[y0,...,yr] to Z[y0,...,yr]
            //for ( GenPolynomial<MOD> Cm : mlift ) {
            //     GenPolynomial<BigInteger> ci = PolyUtil.integerFromModularCoefficients( pfac, Cm );
            //     factors.add(ci);
	    //}
            //if ( isFactorization(P,factors) ) {
            //    logger.info("test factors = " + factors);
            //    return factors; // done
            //}
            //factors.clear();

            // combine trial factors
            mfac = mlift.get(0).ring;
            int dl = (mlift.size() + 1) / 2;
            GenPolynomial<BigInteger> u = P;
            long deg = (u.degree() + 1L) / 2L;
            GenPolynomial<MOD> um = Pm;
            GenPolynomial<BigInteger> ui = pd;
            for (int j = 1; j <= dl; j++) {
                System.out.println("j = " + j + ", dl = " + dl + ", mlift = " + mlift); 
                KsubSet<GenPolynomial<MOD>> subs = new KsubSet<GenPolynomial<MOD>>(mlift, j);
                for (List<GenPolynomial<MOD>> flist : subs) {
                    //System.out.println("degreeSum = " + degreeSum(flist));
                    GenPolynomial<MOD> mtrial = mfac.getONE(); // .multiply(nf); // == 1, since primitive
                    for (int kk = 0; kk < flist.size(); kk++) {
                        GenPolynomial<MOD> fk = flist.get(kk);
                        mtrial = mtrial.multiply(fk);
                    }
                    if (mtrial.degree() > deg) { // this test is sometimes wrong
                        logger.info("degree > deg " + deg + ", degree = " + mtrial.degree());
                        //continue;
                    }
                    GenPolynomial<MOD> cofactor = um.divide(mtrial);
                    GenPolynomial<BigInteger> trial = PolyUtil.integerFromModularCoefficients(pfac, mtrial);
                    GenPolynomial<BigInteger> cotrial = PolyUtil.integerFromModularCoefficients(pfac, cofactor);
                    System.out.println("trial    = " + trial);
                    System.out.println("cotrial  = " + cotrial);
                    if (trial.multiply(cotrial).equals(ui) ) {
                        factors.add(trial);
                        ui = PolyUtil.<BigInteger> basePseudoDivide(ui, trial); //u.divide( trial );
                        //System.out.println("ui        = " + ui);
                        if (mlift.removeAll(flist)) {
                            logger.info("new mlift= " + mlift);
                            //System.out.println("dl = " + dl); 
                            if ( mlift.size() > 1 ) {
                                dl = (mlift.size() + 1) / 2;
                                j = 0; // since j++
                                break;
                            } else {
                                logger.info("last ui = " + ui);
                                factors.add(ui);
                                return factors;
                            }
                        }
                        logger.error("error removing flist from mlift = " + mlift);
                    }
                }
            }
            System.out.println("end combine");
            if (!ui.isONE() && !ui.equals(pd)) {
               logger.info("rest ui = " + ui);
               //System.out.println("rest ui = " + ui);
               factors.add(ui);
            }
            if ( factors.size() > 0 ) {
                System.out.println("size > 0: " + factors);
                return factors;
	    }
            // no factors found, next meta loop
            factors.clear();
            //break; // or repeat??
        } // end for meta loop

        if (factors.size() == 0) {
            logger.info("irred P = " + P);
            factors.add(P);
        }
        return factors;
    }

}
