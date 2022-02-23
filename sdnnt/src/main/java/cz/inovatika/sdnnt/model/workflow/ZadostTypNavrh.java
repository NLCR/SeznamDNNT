package cz.inovatika.sdnnt.model.workflow;

import java.util.Arrays;
import java.util.Optional;

/**
 * Type of request
 */
public enum ZadostTypNavrh {

    VN, VNZ, VNL, NZN, PXN;

    public static ZadostTypNavrh find(String type) {
        if (type != null) {
            String tType = type.toUpperCase();
            ZadostTypNavrh[] values = values();
            Optional<String> any = Arrays.stream(values).map(ZadostTypNavrh::name).filter(t -> t.equals(tType)).findAny();
            if (any.isPresent()) {
                return ZadostTypNavrh.valueOf(any.get());
            } else {
                return null;
            }
        } else return null;
    }

}
