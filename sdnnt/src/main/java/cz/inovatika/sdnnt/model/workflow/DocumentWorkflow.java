package cz.inovatika.sdnnt.model.workflow;



import cz.inovatika.sdnnt.indexer.models.MarcRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//import static cz.inovatika.sdnnt.model.PublicItemState.*;
import static cz.inovatika.sdnnt.model.CuratorItemState.*;

/* Workflow aplikovano na dokumentu */
public enum DocumentWorkflow {

    // NZN, NPA
    // VN VNZ VNL


    /** Navrh na vyrazeni  ze seznamu dntstav A*/
    VVS{
        @Override
        public void change(MarcRecord mr, String user, StateChangedInformn function) {
            if (mr.dntstav == null) {
                mr.setStav( N.name(), user);
                function.changed(mr, mr.identifier, new ArrayList<>(), mr.dntstav);
            } else if (mr.dntstav.contains(A.name())) {
                mr.setStav(N.name(), user);
                function.changed(mr, mr.identifier, Arrays.asList(A.name()), mr.dntstav);
            } else if (mr.dntstav.contains(PA.name())) {
                mr.setStav(N.name(), user);
                function.changed(mr, mr.identifier, Arrays.asList(PA.name()), mr.dntstav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.dntstav));
            }
        }

        @Override
        public void reduce(MarcRecord mr, String user, StateChangedInformn function) {
            if (mr.dntstav == null) {
                mr.enhanceState(Arrays.asList(A.name(), NZ.name()), user);
                function.changed(mr, mr.identifier, new ArrayList<>(), mr.dntstav);
            } else if (mr.dntstav.contains(A.name())) {
                mr.enhanceState(Arrays.asList(NZ.name()), user);
                function.changed(mr, mr.identifier, Arrays.asList(A.name()), mr.dntstav);
            } else if (mr.dntstav.contains(PA.name())) {
                mr.setStav(Arrays.asList(A.name(), NZ.name()), user);
                function.changed(mr, mr.identifier, Arrays.asList(PA.name()), mr.dntstav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.dntstav));
            }
        }
    },

    VVN {
        @Override
        public void reduce(MarcRecord mr, String user, StateChangedInformn function) {
            if (mr.dntstav == null) {
                mr.enhanceState(Arrays.asList(A.name(), NZ.name()), user);
                function.changed(mr, mr.identifier, new ArrayList<>(), mr.dntstav);
            } else if (mr.dntstav.contains(A.name())) {
                mr.enhanceState(Arrays.asList(NZ.name()), user);
                function.changed(mr, mr.identifier, Arrays.asList(NZ.name()), mr.dntstav);
            } else if (mr.dntstav.contains(PA.name())) {
                mr.setStav(Arrays.asList(A.name(), NZ.name()), user);
                function.changed(mr, mr.identifier, Arrays.asList(PA.name()), mr.dntstav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.dntstav));
            }
        }

        @Override
        public void change(MarcRecord mr, String user, StateChangedInformn function) {
            if (mr.dntstav != null && mr.dntstav.contains(PA.name()))  {
                mr.setStav(N.name(), user);
                function.changed(mr, mr.identifier, Arrays.asList(PA.name()), mr.dntstav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.dntstav));
            }
        }
    },
    NZN {
        @Override
        public void change(MarcRecord mr, String user, StateChangedInformn function) {
            if (mr.dntstav == null) {
                mr.setStav(PA.name(), user);
                function.changed(mr, mr.identifier, new ArrayList<>(), mr.dntstav);
            } else if (mr.dntstav.contains(N.name()) ) {
                List<String> oldStav = mr.dntstav;
                mr.setStav(PA.name(), user);
                function.changed(mr, mr.identifier, oldStav, mr.dntstav);
            } else {
                LOGGER.log(Level.WARNING, String.format("Illegal state %s", mr.dntstav));
            }
        }

        @Override
        public void reduce(MarcRecord marcRecord, String user, StateChangedInformn function) {
            LOGGER.log(Level.WARNING, "Unsupported");
        }
    };



    public abstract  void change(MarcRecord marcRecord, String user, StateChangedInformn function);

    public abstract void reduce(MarcRecord marcRecord, String user, StateChangedInformn function);

    private static Logger LOGGER = Logger.getLogger(DocumentWorkflow.class.getName());

    @FunctionalInterface
    public interface StateChangedInformn {
        void changed(MarcRecord indexingRecord, String ident, List<String> oldStates, List<String> newStates);
    }
}
