package cz.inovatika.sdnnt.wflow;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cz.inovatika.sdnnt.wflow.ItemState.*;

/** Workflow pro jednotlive typy navrhu */
public enum NavrhWorklflow {

    /** Navrh na vyrazeni  ze seznamu stav A*/
    VVS{
        @Override
        public void change(MarcRecord mr, String user, StateChanged function) {
            if (mr.stav == null) {
                mr.setStav( VS.name(), user);
                function.changed(mr, mr.identifier, new ArrayList<>(), mr.stav);
            } else if (mr.stav.contains(A.name())) {
                mr.setStav(VS.name(), user);
                function.changed(mr, mr.identifier, Arrays.asList(A.name()), mr.stav);
            } else if (mr.stav.contains(PA.name())) {
                mr.setStav(VN.name(), user);
                function.changed(mr, mr.identifier, Arrays.asList(PA.name()), mr.stav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.stav));
            }
        }

        @Override
        public void reduce(MarcRecord mr, String user, StateChanged function) {
            if (mr.stav == null) {
                mr.enhanceState(Arrays.asList(A.name(), NZ.name()), user);
                function.changed(mr, mr.identifier, new ArrayList<>(), mr.stav);
            } else if (mr.stav.contains(A.name())) {
                mr.enhanceState(Arrays.asList(NZ.name()), user);
                function.changed(mr, mr.identifier, Arrays.asList(A.name()), mr.stav);
            } else if (mr.stav.contains(PA.name())) {
                mr.setStav(Arrays.asList(A.name(), NZ.name()), user);
                function.changed(mr, mr.identifier, Arrays.asList(PA.name()), mr.stav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.stav));
            }
        }
    },

    VVN {
        @Override
        public void reduce(MarcRecord mr, String user, StateChanged function) {
            if (mr.stav == null) {
                mr.enhanceState(Arrays.asList(A.name(), NZ.name()), user);
                function.changed(mr, mr.identifier, new ArrayList<>(), mr.stav);
            } else if (mr.stav.contains(A.name())) {
                mr.enhanceState(Arrays.asList(NZ.name()), user);
                function.changed(mr, mr.identifier, Arrays.asList(NZ.name()), mr.stav);
            } else if (mr.stav.contains(PA.name())) {
                mr.setStav(Arrays.asList(A.name(), NZ.name()), user);
                function.changed(mr, mr.identifier, Arrays.asList(PA.name()), mr.stav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.stav));
            }
        }

        @Override
        public void change(MarcRecord mr, String user, StateChanged function) {
            if (mr.stav != null && mr.stav.contains(PA.name()))  {
                mr.setStav(VN.name(), user);
                function.changed(mr, mr.identifier, Arrays.asList(PA.name()), mr.stav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.stav));
            }
        }
    },
    NZN {
        @Override
        public void change(MarcRecord mr, String user,StateChanged function) {
            if (mr.stav == null) {
                mr.setStav(PA.name(), user);
                function.changed(mr, mr.identifier, new ArrayList<>(), mr.stav);
            } else if (mr.stav.contains(N.name()) || mr.stav.contains(VS.name()) || mr.stav.contains(VN.name())) {
                List<String> oldStav = mr.stav;
                mr.setStav(PA.name(), user);
                function.changed(mr, mr.identifier, oldStav, mr.stav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.stav));
            }
        }

        @Override
        public void reduce(MarcRecord marcRecord, String user, StateChanged function) {
            LOGGER.log(Level.WARNING, "Unsupported");
        }
    };



    public abstract  void change(MarcRecord marcRecord, String user, StateChanged function);

    public abstract void reduce(MarcRecord marcRecord, String user, StateChanged function);

    private static Logger LOGGER = Logger.getLogger(NavrhWorklflow.class.getName());

    @FunctionalInterface
    public interface StateChanged {
        void changed(MarcRecord indexingRecord, String ident, List<String> oldStates, List<String> newStates);
    }

}
