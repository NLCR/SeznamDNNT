package cz.inovatika.sdnnt.model;

import java.util.List;

/**
 * Stavy z indexu
 *  dntstav
 *          "A",513933,
 *         "NZ",42086,
 *         "PA",10120,
 *         "N",9485,
 *         "X",8643,
 *  marc_990a
 *         "A",513933,
 *         "NZ",42085,
 *         "PA",10120,
 *         "N",9485,
 *         "X",8643]},
 */

/** All posibilities of catalog item state*/
public enum PublicItemState {

    A, PA, N, X;
    /*,  NL, NZ*/;

}
