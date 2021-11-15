package cz.inovatika.sdnnt.model;

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

/**
 * Represents currator states enhanced public item states
 */
public enum CuratorItemState {


    N, // vyrazeno  N
    NPA, // vyrazeno/nezarazeno ale ceka na zarazeni  N

    PA, // ceka na zarazeni  PA

    A, //zarazeno  A

    NL, // zarazeno  A
    NZ, // zarazeno A

    PX, // neni zadny stav
    X; // neni zadny stav
}
