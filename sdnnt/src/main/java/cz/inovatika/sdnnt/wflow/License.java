package cz.inovatika.sdnnt.wflow;

import java.util.List;

/** Represents licenses */
public enum License {

    dnntt {
        @Override
        public boolean acceptCurrentStatesSetting(List<String> states) {
            return states.contains(ItemState.NZ.name());
        }
    },
    dnnto {
        @Override
        public boolean acceptCurrentStatesSetting(List<String> states) {
            return states.contains(ItemState.A.name()) && !states.contains(ItemState.NZ.name());
        }
    };

    public abstract  boolean acceptCurrentStatesSetting(List<String> states);

    public static License findLincese(List<String> states) {
        for (int i = 0; i < values().length; i++) {
            if (values()[i].acceptCurrentStatesSetting(states)) return values()[i];
        }
        return null;
    }

}
