package cz.inovatika.sdnnt.model.workflow;

import java.util.Arrays;
import java.util.Optional;

public enum ZadostType {

    VN, VNZ, VNL, NZN;

    public static ZadostType find(String type) {
        if (type != null) {
            String tType = type.toUpperCase();
            ZadostType[] values = values();
            Optional<String> any = Arrays.stream(values).map(ZadostType::name).filter(t -> t.equals(tType)).findAny();
            if (any.isPresent()) {
                return ZadostType.valueOf(any.get());
            } else {
                return null;
            }
        } else return null;
    }
}
