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
 * Represents curator states enhanced public item states
 */
public enum CuratorItemState {

    
    
    // Vyrazeno
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
            if (owner.getPublicState() != null)  return PublicItemState.N;
            else return null;
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
    //Omezeni na terminal - ocekavana produkce
    NLX {
        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            PublicItemState currentPublicState = owner.getPublicState();
            return currentPublicState != null ? currentPublicState :  PublicItemState.A;
        }
    },
    // Omezeni na terminal - ocekavana produkce
    NZ {
        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            return PublicItemState.N;
        }
    },
    // Pravdepodobne volne dilo
    PX {
        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            if (owner != null) {
                return owner.getPublicState();
            } else {
                return null;
            }
        }
    },
    // Volne dilo
    X {
        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            return PublicItemState.X;
        }
    },
    // Pravdepodobna duplicita
    DX {

        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            if (owner != null) {
                return owner.getPublicState();
            } else {
                return null;
            }
        }
        
    },
    
    // Realna duplicita
    D {

        @Override
        public PublicItemState getPublicItemState(WorkflowOwner owner) {
            // TODO Auto-generated method stub
            return PublicItemState.D;
        }
        
    };
    
    /**
     * Curator and public state association
     * @param owner Given owner
     * @return Appropriate public state
     */
    public abstract PublicItemState getPublicItemState(WorkflowOwner owner);
}
