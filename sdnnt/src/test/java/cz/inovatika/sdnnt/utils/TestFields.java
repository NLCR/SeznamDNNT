package cz.inovatika.sdnnt.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TestFields {

    public static void main(String[] args) {
        String fields = "marc_040$,marc_956u,marc_956x,marc_956y,marc_9109,marc_700x,marc_700w,marc_700u,marc_700t,marc_700s,marc_700r,marc_700q,marc_700p,marc_700o,marc_700n,marc_700m,marc_700l,marc_700k,marc_700j,marc_700i,marc_700h,marc_700g,marc_700f,marc_700e,marc_700d,marc_700c,marc_700b,marc_700a,marc_2601,marc_040b,marc_040c,marc_040d,marc_040e,marc_040a,marc_9569,marc_0409,marc_260g,marc_260f,marc_992a,marc_992b,marc_260c,marc_260b,marc_260e,marc_260a,marc_992p,marc_992s,marc_3382,marc_2606,marc_2603,marc_2643,marc_020q,marc_910a,marc_910b,marc_910c,marc_990a,marc_910k,marc_910l,marc_020a,marc_910o,marc_250b,marc_910p,marc_250a,marc_020c,marc_910q,marc_910r,marc_910s,marc_910u,marc_044a,marc_1309,marc_910w,marc_910x,marc_910z,marc_1307,marc_911a,marc_911d,marc_991a,marc_911p,marc_911r,marc_911s,marc_911u,marc_338b,marc_260x,marc_2503,marc_338a,marc_2506,marc_240p,marc_240s,marc_240r,marc_240m,marc_240l,marc_240o,marc_240n,marc_240h,marc_022l,marc_240k,marc_022m,marc_240d,marc_240g,marc_240f,marc_240a,marc_264c,marc_264b,marc_264a,marc_022a,marc_130t,marc_7309,marc_130p,marc_130s,marc_7307,marc_130r,marc_130m,marc_130l,marc_130o,marc_130n,marc_130k,marc_130d,marc_130g,marc_130f,marc_130a,marc_020z,marc_2646,marc_2407,marc_730x,marc_730t,marc_730s,marc_730r,marc_730p,marc_730o,marc_730n,marc_730m,marc_730l,marc_730k,marc_730i,marc_730h,marc_730g,marc_730f,marc_035a,marc_730d,marc_022z,marc_730a,marc_240t,marc_022y,marc_264x,marc_2466,marc_2463,marc_7117,marc_2465,marc_7116,marc_7114,marc_2460,marc_245I,marc_1007,marc_1009,marc_1004,marc_2456,marc_1006,marc_7109,marc_2452,marc_1000,marc_7107,marc_7106,marc_7105,marc_1001,marc_7104,marc_7103,marc_7102,marc_100$,marc_856q,marc_856u,marc_243a,marc_856y,marc_856z,marc_998a,marc_711x,marc_711u,marc_246t,marc_711t,marc_246n,marc_246p,marc_711q,marc_711p,marc_711n,marc_246g,marc_246f,marc_711k,marc_246i,marc_246h,marc_711h,marc_246c,marc_246b,marc_711g,marc_711f,marc_711e,marc_711d,marc_711c,marc_246a,marc_711b,marc_711a,marc_015z,marc_100w,marc_100t,marc_100u,marc_710x,marc_7009,marc_100p,marc_7007,marc_710u,marc_7006,marc_100q,marc_245s,marc_710t,marc_7005,marc_245n,marc_100l,marc_7004,marc_710s,marc_7003,marc_710r,marc_245p,marc_100n,marc_7002,marc_710p,marc_7001,marc_100g,marc_710n,marc_100j,marc_245k,marc_710l,marc_100d,marc_245f,marc_710k,marc_100c,marc_245e,marc_245h,marc_100f,marc_245g,marc_100e,marc_245b,marc_8563,marc_710g,marc_245a,marc_8564,marc_710f,marc_100b,marc_245d,marc_710e,marc_245c,marc_100a,marc_710d,marc_710c,marc_700$,marc_710b,marc_710a,marc_015a";
        String[] split = fields.split(",");
        List<String> collect = Arrays.stream(split).collect(Collectors.toList());
        Collections.sort(collect);
        collect.stream().forEach(it -> {
            System.out.println(it);
        });
    }
}
