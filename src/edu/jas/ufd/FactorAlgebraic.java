
/*
 * $Id$
 */

package edu.jas.ufd;


import java.util.List;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.jas.structure.Power;
import edu.jas.structure.GcdRingElem;

import edu.jas.arith.BigInteger;
import edu.jas.arith.BigRational;
import edu.jas.arith.ModInteger;
import edu.jas.arith.ModIntegerRing;
import edu.jas.arith.PrimeList;

import edu.jas.poly.AlgebraicNumber;
import edu.jas.poly.AlgebraicNumberRing;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.ExpVector;
import edu.jas.poly.PolyUtil;
import edu.jas.poly.Monomial;

import edu.jas.util.PowerSet;
import edu.jas.util.KsubSet;


/**
 * Algebraic number coefficients factorization algorithms.
 * @author Heinz Kredel
 */

public class FactorAlgebraic <C extends GcdRingElem<C>> 
       extends FactorAbstract<AlgebraicNumber<C>> {


    private static final Logger logger = Logger.getLogger(FactorAlgebraic.class);
    private boolean debug = true || logger.isInfoEnabled();


    /**
     * Factorization engine for base coefficients.
     */
    protected final FactorAbstract<C> factorCoeff;


    /**
     * No argument constructor.
     * <b>Note:</b> can't use this constructor.
     */
    protected FactorAlgebraic() {
        throw new IllegalArgumentException("don't use this constructor");
    }


    /**
     * Constructor.
     * @parm factorCoeff factorization engine for base coefficients.
     */
    public FactorAlgebraic(FactorAbstract<C> factorCoeff) {
        this.factorCoeff = factorCoeff;
    }


    /**
     * GenPolynomial base factorization of a squarefree polynomial.
     * @param P squarefree and primitive! GenPolynomial<AlgebraicNumber<C>>.
     * @return [p_1,...,p_k] with P = prod_{i=1, ..., k} p_i.
     */
    public List<GenPolynomial<AlgebraicNumber<C>>> 
      baseFactorsSquarefree(GenPolynomial<AlgebraicNumber<C>> P) {
        if ( P == null ) {
            throw new RuntimeException(this.getClass().getName() + " P == null");
        }
        List<GenPolynomial<AlgebraicNumber<C>>> factors = new ArrayList<GenPolynomial<AlgebraicNumber<C>>>();
        if ( P.isZERO() ) {
            return factors;
        }
        if ( P.isONE() ) {
            factors.add(P);
            return factors;
        }
        System.out.println("\nP = " + P);
        GenPolynomialRing<AlgebraicNumber<C>> pfac = P.ring; // Q(alpha)[x]
        if ( pfac.nvar > 1 ) {
            throw new RuntimeException(this.getClass().getName()
                    + " only for univariate polynomials");
        }
        AlgebraicNumberRing<C> afac = (AlgebraicNumberRing<C>)pfac.coFac;

        GreatestCommonDivisorSubres<C> engine 
            = new GreatestCommonDivisorSubres<C>( /*cfac.coFac*/ );
              // = (GreatestCommonDivisorAbstract<C>)GCDFactory.<C>getImplementation( cfac.coFac );
        GreatestCommonDivisor<AlgebraicNumber<C>> aengine 
            = GCDFactory.<AlgebraicNumber<C>>getProxy( afac );
              //= new GreatestCommonDivisorSubres<AlgebraicNumber<C>>( /*cfac.coFac*/ );

        GenPolynomial<C> agen = afac.modul;
        GenPolynomialRing<C> cfac = afac.ring;
        GenPolynomialRing<GenPolynomial<C>> rfac = new GenPolynomialRing<GenPolynomial<C>>(cfac,pfac);
        // Q[alpha][x] = Q[X][alpha]
        //GenPolynomialRing<C> dfac = cfac.extend(1);

        // transform minimal polynomial to bi-variate polynomial
        GenPolynomial<GenPolynomial<C>> Ac = rfac.getONE().clone(); 
        //Ac = Ac.multiply(agen); // to lower variable 
        GenPolynomial<C> f = cfac.getONE();
        for ( Monomial<C> m : agen ) {
            C c = m.c;
            GenPolynomial<C> ac = f.multiply(c); // to upper variable
            Ac.doPutToMap(m.e,ac);
        }
        System.out.println("Ac = " + Ac);

        // search squarefree resultant
        long k = 1;
        GenPolynomial<C> res = null;
        GenPolynomial<GenPolynomial<C>> Pc; // Q[X][alpha] or Q[x,alpha]
        GenPolynomial<GenPolynomial<C>> kc;
        GenPolynomial<GenPolynomial<C>> fx;
        GenPolynomial<GenPolynomial<C>> fy;
        while ( true ) {
            if ( k > 4 ) {
                k = -1;
                //break;
            }
            // transform to bi-variate polynomial, switching varaible sequence
            Pc = PolyUfdUtil.<C>  fromAlgebraicCoefficients( rfac, P, k );
            //System.out.println("Pc = " + Pc);
            Pc = PolyUtil.<C>monic(Pc);
            System.out.println("\nPc = " + Pc);
            //System.out.println("Ac = " + Ac);

            GenPolynomial<GenPolynomial<C>> Rc = engine.recursiveResultant(Pc,Ac);
            System.out.println("Rc = " + Rc);
            res = Rc.leadingBaseCoefficient();
            System.out.println("res = " + res);
            if ( res.isZERO() || res.isConstant() ) {
                k++;
                continue;
            }

            boolean sqf = factorCoeff.isSquarefree(res);
            System.out.println("sqf = " + sqf + "\n");
            if ( sqf ) {
                break;
            }
            if ( k < 0 ) {
                break;
            }
            k++;
        }
        // Res is now squarefree, so we can factor it
        //SortedMap<GenPolynomial<C>,Long> nfacs = factorCoeff.baseFactors( res );
        List<GenPolynomial<C>> nfacs = factorCoeff.baseFactorsSquarefree( res );
        System.out.println("\nnfacs = " + nfacs); // Q[X]
        if ( !factorCoeff.isFactorization( res, nfacs ) ) {
           System.out.println("isFactorization = false"); 
        }
        if ( nfacs.size() == 1 ) {
            factors.add(P);
            return factors;
        }

        // compute gcds of factors with polynomial in Q(alpha)[X]
        GenPolynomial<AlgebraicNumber<C>> Pp = P;
        System.out.println("Pp = " + Pp);
        GenPolynomial<AlgebraicNumber<C>> Ni;
        for ( GenPolynomial<C> nfi : nfacs ) { // .keySet()
             System.out.println("nfi = " + nfi);
             Ni = PolyUfdUtil.<C> convertToAlgebraicCoefficients(pfac,nfi,k);
             System.out.println("Ni = " + Ni);

             // compute gcds of factors with polynomial
             GenPolynomial<AlgebraicNumber<C>> pni = aengine.gcd(Ni,Pp);
             System.out.println("gcd(Ni,P) = " + pni);

             if ( !pni.isONE() ) {
                factors.add( pni );
                Pp = Pp.divide( pni );
             } else {
                GenPolynomial<AlgebraicNumber<C>> qni = Pp.divide(Ni);
                GenPolynomial<AlgebraicNumber<C>> rni = Pp.remainder(Ni);
                System.out.println("div qni = " + qni);
                System.out.println("div rni = " + rni);
             }
        }
        if ( ! Pp.isZERO() && ! Pp.isONE() ) { // hack to pretend factorization
            factors.add( Pp );
        }
        System.out.println("factors = " + factors);
        return factors;
    }

}
