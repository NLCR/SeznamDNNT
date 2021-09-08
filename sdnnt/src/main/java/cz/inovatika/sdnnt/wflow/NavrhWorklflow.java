package cz.inovatika.sdnnt.wflow;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public enum NavrhWorklflow {


    VVS{
        @Override
        public void change(MarcRecord mr, String user, StateChanged function) {
            if (mr.stav == null) {
                mr.setStav("VS", user);
                function.changed(mr, mr.identifier, new ArrayList<>(), mr.stav);
            } else if (mr.stav.contains("A")) {
                mr.setStav("VS", user);
                function.changed(mr, mr.identifier, Arrays.asList("A"), mr.stav);
            } else if (mr.stav.contains("PA")) {
                mr.setStav("VN", user);
                function.changed(mr, mr.identifier, Arrays.asList("PA"), mr.stav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.stav));
            }
        }

        @Override
        public void reduce(MarcRecord mr, String user, StateChanged function) {
            if (mr.stav == null) {
                mr.enhanceState(Arrays.asList("A", "NZ"), user);
                function.changed(mr, mr.identifier, new ArrayList<>(), mr.stav);
            } else if (mr.stav.contains("A")) {
                mr.enhanceState(Arrays.asList("NZ"), user);
                function.changed(mr, mr.identifier, Arrays.asList("A"), mr.stav);
            } else if (mr.stav.contains("PA")) {
                mr.setStav(Arrays.asList("A", "NZ"), user);
                function.changed(mr, mr.identifier, Arrays.asList("PA"), mr.stav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.stav));
            }
        }
    },
    VVN {
        @Override
        public void reduce(MarcRecord mr, String user, StateChanged function) {
            if (mr.stav == null) {
                mr.enhanceState(Arrays.asList("A", "NZ"), user);
                function.changed(mr, mr.identifier, new ArrayList<>(), mr.stav);
            } else if (mr.stav.contains("A")) {
                mr.enhanceState(Arrays.asList("NZ"), user);
                function.changed(mr, mr.identifier, Arrays.asList("NZ"), mr.stav);
            } else if (mr.stav.contains("PA")) {
                mr.setStav(Arrays.asList("A", "NZ"), user);
                function.changed(mr, mr.identifier, Arrays.asList("PA"), mr.stav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.stav));
            }
        }

        @Override
        public void change(MarcRecord mr, String user, StateChanged function) {
            if (mr.stav != null && mr.stav.contains("PA"))  {
                mr.setStav("VN", user);
                function.changed(mr, mr.identifier, Arrays.asList("PA"), mr.stav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.stav));
            }
        }
    },
    NZN {
        @Override
        public void change(MarcRecord mr, String user,StateChanged function) {
            if (mr.stav == null) {
                mr.setStav("PA", user);
                function.changed(mr, mr.identifier, new ArrayList<>(), mr.stav);
            } else if (mr.stav.contains("N") || mr.stav.contains("VS") || mr.stav.contains("VN")) {
                List<String> oldStav = mr.stav;
                mr.setStav("PA", user);
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
