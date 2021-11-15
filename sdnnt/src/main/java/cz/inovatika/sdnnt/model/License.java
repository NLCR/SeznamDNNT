package cz.inovatika.sdnnt.model;

import java.util.List;

/** Represents licenses */
public enum License {

    dnntt {
        @Override
        public boolean acceptCurrentStatesSetting(List<String> states) {
            return states.contains("NZ");
        }
    },
    dnnto {
        @Override
        public boolean acceptCurrentStatesSetting(List<String> states) {
            return states.contains(PublicItemState.A.name()) && !states.contains("NZ");
        }
    };

    /** returns true if given states means that we can change licences */
    public abstract  boolean acceptCurrentStatesSetting(List<String> states);

    public static License findLincese(List<String> states) {
        for (int i = 0; i < values().length; i++) {
            if (values()[i].acceptCurrentStatesSetting(states)) return values()[i];
        }
        return null;
    }

}
