package cz.inovatika.sdnnt.utils;

import org.json.JSONObject;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TestKeys {

    public static final String EN = "{ \"sigla\": {  \"BOA001\" : \"Moravian Library in Brno\",\n" +
            "    \"BOE801\" : \"Museum of Brno Region\",\n" +
            "    \"ABA000\" : \"National Library of the Czech Republic\",\n" +
            "    \"ABA001\" : \"National Library of the Czech Republic - Library Collection and Services\",\n" +
            "    \"ABA004\" : \"National Library of the Czech Republic - Slavonic Library\",\n" +
            "    \"BVE301\" : \"Regional Museum in Mikulov\",\n" +
            "    \"CBA001\" : \"Research Library of South Bohemia in Ceske Budejovice\",\n" +
            "    \"OLA001\" : \"Research Library in Olomouc\",\n" +
            "    \"HKA001\" : \"Research Library in Hradec Králové\",\n" +
            "    \"ULG001\" : \"The North Bohemian Research Library in Ústí nad Labem\",\n" +
            "    \"ABA007\" : \"Library of the ASCR\",\n" +
            "    \"KVG001\" : \"Regional Library Karlovy Vary\",\n" +
            "    \"ABA013\" : \"National Library of Technology\",\n" +
            "    \"ABE310\" : \"Museum of Decorative Arts in Praguee\",\n" +
            "    \"ROE301\" : \"Dr. Bohuslav Horak Museum\",\n" +
            "    \"ABD103\" : \"Charles University in Prague - Faculty of Social Sciences\",\n" +
            "    \"ABG001\" : \"Municipal Library of Prague\",\n" +
            "    \"LIA001\" : \"Research Library in Liberec\",\n" +
            "    \"KLG001\" : \"Central Bohemian Research Libraryě\",\n" +
            "    \"ZLG001\" : \"František Bartoš Regional Library in Zlin\",\n" +
            "    \"ABA009\" : \"Library of Antonin Svehla\",\n" +
            "    \"BOE950\" : \"Library of the Benedictine Abbey Rajhrad\",\n" +
            "    \"UOG505\" : \"Municipal Library Ceska Trebova\",\n" +
            "    \"KTG503\" : \"Municipal Library in Horazdovice\",\n" +
            "    \"ABE308\" : \"Náprstek’s Muzeum of Asian, African and American Cultures\",\n" +
            "    \"ABA010\" : \"National Museum\",\n" +
            "    \"ABE045\" : \"Military History Institute in Prague\",\n" +
            "    \"ABC135\" : \"The National Film Archive Library\",\n" +
            "    \"ABE323\" : \"Jewish Museum in Prague\",\n" +
            "    \"PNA001\" : \"Education and Research Library of Pilsener Region\",\n" +
            "    \"OSA001\" : \"Moravian-Silesian Research Library in Ostrava\",\n" +
            "    \"OSE309\" : \"Archives of the Town Ostrava\",\n" +
            "    \"BOD006\" : \"Mendel University in Brno\",\n" +
            "    \"ABD001\" : \"Charles University in Prague - Faculty of Arts Library\",\n" +
            "    \"ABA006\" : \"University of Economics, Prague\",\n" +
            "    \"ABA008\" : \"National Medical Library\",\n" +
            "    \"ABE343\" : \"National Archive\",\n" +
            "    \"ABE459\" : \"Royal Canonry of Premonstratensians at Strahov\",\n" +
            "    \"HOE802\" : \"Masaryk Museum in Hodonin\",\n" +
            "    \"ABA011\" : \"Parliamentary library\",\n" +
            "    \"BOA002\" : \"Moravian Library in Brno - Pedagogická knihovna\",\n" +
            "    \"ABB045\" : \"Institute of Ethnology of the AS CR\",\n" +
            "    \"ABE050\" : \"Ministry of the Environment of the Czech Republic\",\n" +
            "    \"ABD005\" : \"Charles University - Faculty of Education\",\n" +
            "    \"HBG001\" : \"Regional Library of Highlands\",\n" +
            "    \"KOE801\" : \"Regional Museum in Kolin\",\n" +
            "    \"OPE301\" : \"Library Silesian Museum\",\n" +
            "    \"HKE302\" : \"Museum of East Bohemia in Hradec Kralove\",\n" +
            "    \"HKG001\" : \"Municipal Library of Hradec Kralove\",\n" +
            "    \"JCG001\" : \"Municipal Library in Jicin\",\n" +
            "    \"ABD025\" : \"University of Chemistry and Technology, Prague\",\n" +
            "    \"KME301\" : \"Museum of Kromeriz Region\",\n" +
            "    \"ZLE302\" : \"Museum of Southeastern Moravia of Zlin\",\n" +
            "    \"BOB007\" : \"Institute of History of the Czech Academy of Sciences\",\n" +
            "    \"BOE310\" : \"Moravian museum\",\n" +
            "    \"ABD020\" : \"Academy of Performing Arts in Prague\",\n" +
            "    \"ABE135\" : \"The Ministry of Labour and Social Affairs\",\n" +
            "    \"ABE345\" : \"National Museum - History muzeum\",\n" +
            "    \"ABE370\" : \"National Museum - Czech Museum of Music\",\n" +
            "    \"ABE336\" : \"Library of useum of Czech Literature\",\n" +
            "    \"ABD024\" : \"Academy of Arts, Architecture and Design in Prague\",\n" +
            "    \"ABD134\" : \"Charles University - First Faculty of Medicine\",\n" +
            "    \"PNE303\" : \"Museum of West Bohemia in Pilsen\",\n" +
            "    \"OPD001\" : \"Silesian university in Opava\",\n" +
            "    \"JHE301\" : \"Museum of Jindrichuv Hradec Region\",\n" +
            "    \"JCE301\" : \"Regional Museum and Gallery in Jicin\",\n" +
            "    \"NAE802\" : \"Town Museum Jaromer\",\n" +
            "    \"TUE301\" : \"Museum Podkrkonosi in Trutnov\",\n" +
            "    \"ABD065\" : \"Charles University - Faculty of Science - Map Collection\",\n" +
            "    \"ABC039\" : \"Research Institute of Geodesy, Topography and Cartography\",\n" +
            "    \"ABE400\" : \"National Gallery in Prague\",\n" +
            "    \"CHE302\" : \"Museum Cheb\",\n" +
            "    \"ABE190\" : \"Gender Studies\",\n" +
            "    \"ABE311\" : \"National Technical Museum\",\n" +
            "    \"BOE303\" : \"Moravian Gallery in Brno\",\n" +
            "    \"KVE303\" : \"Museum of Karlovy Vary Region\",\n" +
            "    \"ABA012\" : \"National Pedagogical museum and Library of J. A. Comenius\",\n" +
            "    \"ABB005\" : \"Jan Kmenta CERGE-EI Library\",\n" +
            "    \"BOD022\" : \"Masaryk University - Faculty of Economics and Administration\",\n" +
            "    \"ABB030\" : \"Oriental Institute of the AS CR\",\n" +
            "    \"TUE801\" : \"Krkonose Museum\",\n" +
            "    \"OSG002\" : \"Ostrava City Library\",\n" +
            "    \"OSD001\" : \"University of Ostrava\",\n" +
            "    \"KAG001\" : \"Regional Library Karviná\",\n" +
            "    \"KAE801\" : \"Těšín Regional Museum\",\n" +
            "    \"OSE304\" : \"Museum of Ostrava\",\n" +
            "    \"BRE302\" : \"Museum in Bruntal\",\n" +
            "    \"FME301\" : \"Beskydy Museum Frýdek-Místek\",\n" +
            "    \"OSE306\" : \"Gallery of Fine Arts in Ostrava\",\n" +
            "    \"KAE950\" : \"Library of the Silesian Diacony\",\n" +
            "    \"NJE303\" : \"Museum of Novy Jicin Region\",\n" +
            "    \"OPE801\" : \"The Hlučín Area Museum\",\n" +
            "    \"KAG502\" : \"Municipal Library in Orlova\",\n" +
            "    \"SVE951\" : \"Library of the Franciscan Monastery in Dačice\",\n" +
            "    \"HOE801\" : \"National Institute of Folk Culture\",\n" +
            "    \"SME801\" : \"Museum of the Bohemian Paradise\",\n" +
            "    \"ABB019\" : \"Institute of Sociology of the Czech Academy of Sciences\",\n" +
            "    \"ABB085\" : \"Masaryk Institute and Archives of the AS CR\",\n" +
            "    \"ABB083\" : \"Institute of Contemporary History, Czech Academy of Sciences\",\n" +
            "    \"ABE461\" : \"Jewish community in Prague\",\n" +
            "    \"KME450\" : \"Olomouc Museum of Art - Kromeriz Archdiocesan Museum\"\n" +
            "  }}";

    public static final String CS ="{\"sigla\": {\n" +
            "    \"BOA001\" : \"Moravská zemská knihovna v Brně\",\n" +
            "    \"BOE801\" : \"Muzeum Brněnska\",\n" +
            "    \"ABA000\" : \"Národní knihovna České republiky\",\n" +
            "    \"ABA001\" : \"Národní knihovna České republiky - Knihovní fondy a služby\",\n" +
            "    \"ABA004\" : \"Národní knihovna České republiky - Slovanská knihovna\",\n" +
            "    \"BVE301\" : \"Regionální muzeum v Mikulově\",\n" +
            "    \"CBA001\" : \"Jihočeská vědecká knihovna v Českých Budějovicích\",\n" +
            "    \"OLA001\" : \"Vědecká knihovna v Olomouci\",\n" +
            "    \"HKA001\" : \"Studijní a vědecká knihovna v Hradci Králové\",\n" +
            "    \"ULG001\" : \"Severočeská vědecká knihovna v Ústí nad Labem\",\n" +
            "    \"ABA007\" : \"Knihovna AV ČR\",\n" +
            "    \"KVG001\" : \"Krajská knihovna Karlovy Vary\",\n" +
            "    \"ABA013\" : \"Národní technická knihovna\",\n" +
            "    \"ABE310\" : \"Uměleckoprůmyslové museum v Praze\",\n" +
            "    \"ROE301\" : \"Muzeum Dr. Bohuslava Horáka v Rokycanech\",\n" +
            "    \"ABD103\" : \"Univerzita Karlova v Praze - Fakulta sociálních věd\",\n" +
            "    \"ABG001\" : \"Městská knihovna v Praze\",\n" +
            "    \"LIA001\" : \"Krajská vědecká knihovna v Liberci\",\n" +
            "    \"KLG001\" : \"Středočeská vědecká knihovna v Kladně\",\n" +
            "    \"ZLG001\" : \"Krajská knihovna Františka Bartoše ve Zlíně\",\n" +
            "    \"ABA009\" : \"Knihovna Antonína Švehly\",\n" +
            "    \"BOE950\" : \"Benediktinské opatství Rajhrad\",\n" +
            "    \"UOG505\" : \"Městská knihovna Česká Třebová\",\n" +
            "    \"KTG503\" : \"Městská knihovna Horažďovice\",\n" +
            "    \"ABE308\" : \"Náprstkovo muzeum asijských, afrických a amerických kultur \",\n" +
            "    \"ABA010\" : \"Národní muzeum\",\n" +
            "    \"ABE045\" : \"Vojenský historický ústav Praha\",\n" +
            "    \"ABC135\" : \"Knihovna Národního filmového archivu\",\n" +
            "    \"ABE323\" : \"Židovské muzeum v Praze\",\n" +
            "    \"PNA001\" : \"Studijní a vědecká knihovna Plzeňského kraje\",\n" +
            "    \"OSA001\" : \"Moravskoslezská vědecká knihovna v Ostravě\",\n" +
            "    \"OSE309\" : \"Archiv města Ostravy\",\n" +
            "    \"BOD006\" : \"Mendelova univerzita v Brně\",\n" +
            "    \"ABD001\" : \"Univerzita Karlova v Praze - Filozofická fakulta\",\n" +
            "    \"ABA006\" : \"Vysoká škola ekonomická v Praze\",\n" +
            "    \"ABA008\" : \"Národní lékařská knihovna\",\n" +
            "    \"ABE343\" : \"Národní archiv\",\n" +
            "    \"ABE459\" : \"Královská kanonie premonstrátů na Strahově\",\n" +
            "    \"HOE802\" : \"Masarykovo muzeum v Hodoníně\",\n" +
            "    \"ABA011\" : \"Parlamentní knihovna\",\n" +
            "    \"BOA002\" : \"Moravská zemská knihovna - Pedagogická knihovna\",\n" +
            "    \"ABB045\" : \"Etnologický ústav AV ČR\",\n" +
            "    \"ABE050\" : \"Ministerstvo životního prostředí ČR\",\n" +
            "    \"ABD005\" : \"Univerzita Karlova - Pedagogická fakulta\",\n" +
            "    \"HBG001\" : \"Krajská knihovna Vysočiny\",\n" +
            "    \"KOE801\" : \"Regionální muzeum v Kolíně\",\n" +
            "    \"OPE301\" : \"Knihovna Slezského zemského muzea\",\n" +
            "    \"HKE302\" : \"Muzeum východních Čech v Hradci Králové\",\n" +
            "    \"HKG001\" : \"Knihovna města Hradce Králové\",\n" +
            "    \"JCG001\" : \"Knihovna Václava Čtvrtka\",\n" +
            "    \"ABD025\" : \"Vysoká škola chemicko-technologická v Praze\",\n" +
            "    \"KME301\" : \"Muzeum Kroměřížska\",\n" +
            "    \"ZLE302\" : \"Muzeum jihovýchodní Moravy ve Zlíně\",\n" +
            "    \"BOB007\" : \"Historický ústav AV ČR\",\n" +
            "    \"BOE310\" : \"Moravské zemské muzeum\",\n" +
            "    \"ABD020\" : \"Akademie múzických umění v Praze\",\n" +
            "    \"ABE135\" : \"Ministerstvo práce a sociálních věcí ČR\",\n" +
            "    \"ABE345\" : \"Národní muzeum - Historické muzeum\",\n" +
            "    \"ABE370\" : \"Národní muzeum - České muzeum hudby\",\n" +
            "    \"ABE336\" : \"Knihovna Památníku národního písemnictví\",\n" +
            "    \"ABD024\" : \"Vysoká škola uměleckoprůmyslová v Praze\",\n" +
            "    \"ABD134\" : \"Univerzita Karlova v Praze - 1. lékařská fakulta\",\n" +
            "    \"PNE303\" : \"Západočeské muzeum v Plzni\",\n" +
            "    \"OPD001\" : \"Slezská univerzita v Opavě\",\n" +
            "    \"JHE301\" : \"Muzeum Jindřichohradecka\",\n" +
            "    \"JCE301\" : \"Regionální muzeum a galerie v Jičíně - Muzeum hry\",\n" +
            "    \"NAE802\" : \"Městské muzeum v Jaroměři\",\n" +
            "    \"TUE301\" : \"Muzeum Podkrkonoší v Trutnově\",\n" +
            "    \"ABD065\" : \"Univerzita Karlova - Přírodovědecká fakulta - Mapová sbírka\",\n" +
            "    \"ABC039\" : \"Výzkumný ústav geodetický, topografický a kartografický, v.v.i.\",\n" +
            "    \"ABE400\" : \"Národní galerie v Praze\",\n" +
            "    \"CHE302\" : \"Muzeum Cheb\",\n" +
            "    \"ABE190\" : \"Gender Studies\",\n" +
            "    \"ABE311\" : \"Národní technické muzeum\",\n" +
            "    \"BOE303\" : \"Moravská galerie v Brně\",\n" +
            "    \"KVE303\" : \"Muzeum Karlovy Vary\",\n" +
            "    \"ABA012\" : \"Národní pedagogické muzeum a knihovna J. A. Komenského\",\n" +
            "    \"ABB005\" : \"CERGE-EI knihovna Jana Kmenty\",\n" +
            "    \"BOD022\" : \"Masarykova univerzita - Ekonomicko-správní fakulta\",\n" +
            "    \"ABB030\" : \"Orientální ústav AV ČR\",\n" +
            "    \"TUE801\" : \"Krkonošské muzeum\",\n" +
            "    \"OSG002\" : \"Knihovna města Ostravy\",\n" +
            "    \"OSD001\" : \"Ostravská univerzita\",\n" +
            "    \"KAG001\" : \"Regionální knihovna Karviná\",\n" +
            "    \"KAE801\" : \"Muzeum Těšínska\",\n" +
            "    \"OSE304\" : \"Ostravské muzeum\",\n" +
            "    \"BRE302\" : \"Muzeum v Bruntále\",\n" +
            "    \"FME301\" : \"Muzeum Beskyd Frýdek-Místek\",\n" +
            "    \"OSE306\" : \"Galerie výtvarného umění v Ostravě\",\n" +
            "    \"KAE950\" : \"Knihovna Slezské diakonie\",\n" +
            "    \"NJE303\" : \"Muzeum Novojičínska\",\n" +
            "    \"OPE801\" : \"Muzeum Hlučínska\",\n" +
            "    \"KAG502\" : \"Městská knihovna Orlová\",\n" +
            "    \"SVE951\" : \"Knihovna kláštera františkánů v Dačicích\",\n" +
            "    \"HOE801\" : \"Národní ústav lidové kultury\",\n" +
            "    \"SME801\" : \"Muzeum Českého ráje v Turnově\",\n" +
            "    \"ABB019\" : \"Sociologický ústav AV ČR\",\n" +
            "    \"ABB085\" : \"Masarykův ústav a Archiv AV ČR\",\n" +
            "    \"ABB083\" : \"Ústav pro soudobé dějiny AV ČR\",\n" +
            "    \"ABE461\" : \"Židovská obec v Praze\",\n" +
            "    \"KME450\" : \"Muzeum umění Olomouc - Arcidiecézní muzeum Kroměříž\"\n" +
            "  }}";


    public static void main(String[] args) {
        JSONObject csObject = new JSONObject(CS);
        List<String> keys = new ArrayList<>(csObject.getJSONObject("sigla").keySet());
        Collator instance = Collator.getInstance();
        keys.sort(instance);
        System.out.println("Printing CS object ----- ");
        printObject(csObject, keys, "sigla");

        System.out.println("Printing EN object ----- ");
        JSONObject enObject = new JSONObject(EN);
        printObject(enObject, keys, "sigla");

    }

    private static void printObject(JSONObject object, List<String> keys, String skey) {
        keys.stream().forEach(
                key-> {
                    System.out.println(String.format("\"%s\":\"%s\",", key, object.getJSONObject(skey).get(key)));
                }
        );
    }
}
