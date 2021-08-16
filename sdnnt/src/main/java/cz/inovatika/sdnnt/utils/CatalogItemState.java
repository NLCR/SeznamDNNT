package cz.inovatika.sdnnt.utils;

import java.util.List;

/** All posibilities of catalog item state*/
public enum CatalogItemState {

    A(true),
    PA(false),
    V(false),
    VN(false),
    NZ(true);

    protected boolean licenseFinalState;

    CatalogItemState(boolean licenseFinalState) {
        this.licenseFinalState = licenseFinalState;
    }


    public boolean isLicenseFinalState() {
        return this.licenseFinalState;
    }

    public static final boolean allFinalStates(List<String> states) {
        for (int i = 0; i < states.size(); i++) {
            if (!CatalogItemState.valueOf(states.get(i)).isLicenseFinalState()) return false;

        }
        return true;
    }
}
