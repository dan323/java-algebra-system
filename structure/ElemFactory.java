/*
 * $Id$
 */

package edu.jas.structure;

import java.math.BigInteger;
import java.io.Reader;
import java.io.Serializable;
import java.util.Random;

/**
 * ElemFactory interface for use with the polynomial classes.
 * 
 * @author Heinz Kredel
 */

public interface ElemFactory<C extends Element<C>> extends Serializable {


    /**
     * Get the Element for a.
     * @param a
     * @return a: Element.
     */
    public C fromInteger(long a);


    /**
     * Get the Element for a.
     * @param a
     * @return a: Element.
     */
    public C fromInteger(BigInteger a);


    /**
     * Generate a random Element with size less equal to n.
     * @param n
     * @return a random element.
     */
    public C random(int n);


    /**
     * Generate a random Element with size less equal to n.
     * @param n
     * @param random is a source for random bits.
     * @return a random element.
     */
    public C random(int n, Random random);


    /**
     * Create a copy of Element c.
     * @param c
     * @return a copy of c.
     */
    public C copy(C c);


    /**
     * Parse from String.
     * @param s String.
     * @return a Element corresponding to s.
     */
    public C parse(String s);


    /**
     * Parse from Reader.
     * @param r Reader.
     * @return the next Element found on r.
     */
    public C parse(Reader r);

}
