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

import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;

/**
 * Represents currator states enhanced public item states
 */
public enum CuratorItemState {

    N{
        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            return PublicItemState.N;
        }
    },
    // vyrazeno/nezarazeno ale ceka na zarazeni  N
    NPA {
        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            return PublicItemState.N;
        }
    },
    // ceka na zarazeni  PA
    PA {
        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            return PublicItemState.PA;
        }
    },
    //zarazeno  A
    A {
        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            return PublicItemState.A;
        }
    },

    // zarazeno  A
    NL {
        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            return PublicItemState.NL;
        }
    },

    NLX {
        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            PublicItemState currentPublicState = owner.getPublicState();
            return currentPublicState != null ? currentPublicState :  PublicItemState.A;
        }
    },

    NZ {
        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            return PublicItemState.N;
        }
    },

    PX {
        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            return owner.getPublicState();
        }
    },
    X {
        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            return PublicItemState.X;
        }
    };


    public abstract PublicItemState getPublicItemState(WorkflowOwner owner);
}
