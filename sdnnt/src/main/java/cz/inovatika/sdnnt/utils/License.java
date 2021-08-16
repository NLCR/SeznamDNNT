package cz.inovatika.sdnnt.utils;

import java.util.List;

/** Represents licenses */
public enum License {

    dnntt {
        @Override
        public boolean acceptCurrentStatesSetting(List<String> states) {
            return states.contains(CatalogItemState.NZ.name());
        }
    },
    dnnto {
        @Override
        public boolean acceptCurrentStatesSetting(List<String> states) {
            return states.contains(CatalogItemState.A.name()) && !states.contains(CatalogItemState.NZ.name());
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
