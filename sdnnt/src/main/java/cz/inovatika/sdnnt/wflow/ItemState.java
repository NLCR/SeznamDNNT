package cz.inovatika.sdnnt.wflow;

import java.util.List;

/** All posibilities of catalog item state*/
public enum ItemState {

    A(true),
    PA(false),
    V(false),
    VS(false),
    VN(false),
    NZ(true),
    N(true);


    protected boolean licenseFinalState;
    ItemState(boolean licenseFinalState) {
        this.licenseFinalState = licenseFinalState;
    }


    public boolean isLicenseFinalState() {
        return this.licenseFinalState;
    }

    public static final boolean allFinalStates(List<String> states) {
        for (int i = 0; i < states.size(); i++) {
            if (!ItemState.valueOf(states.get(i)).isLicenseFinalState()) return false;

        }
        return true;
    }
}
