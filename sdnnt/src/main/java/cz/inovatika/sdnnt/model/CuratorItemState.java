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

    // vyrazeno  N
    N{
        @Override
        public PublicItemState getPublicItemState() {
            return PublicItemState.N;
        }
    },
    // vyrazeno/nezarazeno ale ceka na zarazeni  N
    NPA {
        @Override
        public PublicItemState getPublicItemState() {
            return PublicItemState.N;
        }
    },
    // ceka na zarazeni  PA
    PA {
        @Override
        public PublicItemState getPublicItemState() {
            return PublicItemState.PA;
        }
    },
    //zarazeno  A
    A {
        @Override
        public PublicItemState getPublicItemState() {
            return PublicItemState.A;
        }
    },

    // zarazeno  A
    NL {
        @Override
        public PublicItemState getPublicItemState() {
            return PublicItemState.A;
        }
    },

    NLX {
        @Override
        public PublicItemState getPublicItemState() {
            return PublicItemState.A;
        }
    },

    NZ {
        @Override
        public PublicItemState getPublicItemState() {
            return PublicItemState.N;
        }
    },

    PX {
        @Override
        public PublicItemState getPublicItemState() {
            return null;
        }
    },
    X {
        @Override
        public PublicItemState getPublicItemState() {
            return null;
        }
    }; // neni zadny stav

    public abstract PublicItemState getPublicItemState();
}
