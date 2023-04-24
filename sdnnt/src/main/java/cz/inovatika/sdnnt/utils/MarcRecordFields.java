package cz.inovatika.sdnnt.utils;

/** Marc record fields */
public class MarcRecordFields {
    
    // aplikacni pole 
    // historie verejneho stavu 
    public static final String HISTORIE_STAVU_FIELD = "historie_stavu";
    // analyzovane pole; pouze pro hledani 
    public static final String HISTORIE_STAVU_FIELD_CUT = "historie_stavu_cut";
    // hlavni pole pro verejny stav 
    public static final String DNTSTAV_FIELD = "dntstav";
    // zaznamenana posledni zmena stavu 
    public static final String DATUM_STAVU_FIELD = "datum_stavu";
    // kuratorsky stav 
    public static final String KURATORSTAV_FIELD = "kuratorstav";
    // datum kuratorskeho stavu 
    public static final String DATUM_KURATOR_STAV_FIELD = "datum_kurator_stav";
    // historie kuratorskeho stavu 
    public static final String HISTORIE_KURATORSTAVU_FIELD = "historie_kurator_stavu";
    // analyzovane pole; pouze pro hledani  
    public static final String HISTORIE_KURATORSTAVU_FIELD_CUT = "historie_kurator_stavu_cut";
    // historie granulovaneho stavu
    public static final String HISTORIE_GRANULOVANEHOSTAVU_FIELD = "historie_granulovaneho_stavu";
    // pridelena licence 
    public static final String LICENSE_FIELD = "license";
    // historie licenci - deprecated 
    @Deprecated
    public static final String LICENSE_HISTORY_FIELD = "license_history";
    
    // Granularita 
    public static final String GRANULARITY_FIELD = "granularity";
    
    // Master links; linky ktere zbyly po presunuti casti z hlavnich linku do granularity
    public static final String MASTERLINKS_FIELD = "masterlinks";
    
    // Priznak urcuje, zda vsechny master links jsou potlaceny - vsechny linky z 856u a 911u jsou casti granularity 
    public static final String MASTERLINKS_DISABLED_FIELD = "masterlinks_disabled";
    
    // identifikator 
    public static final String IDENTIFIER_FIELD = "identifier";

    // 
    public static final String DATESTAMP_FIELD = "datestamp";
    
    // specifikace OAI setu 
    public static final String SET_SPEC_FIELD = "setSpec";
    
    // Leader pole 
    public static final String LEADER_FIELD = "leader";
    
    // Raw zaznam z OAI 
    public static final String RAW_FIELD = "raw";
    
    
    public static final String RECORD_STATUS_FIELD = "record_status";
    public static final String TYPE_OF_RESOURCE_FIELD = "type_of_resource";
    public static final String ITEM_TYPE_FIELD = "item_type";

    
    // context information
    public static final String CTX_FIELD = "ctx";

    public static final String LEGACY_STAV_FIELD = "dntstav";

    // institutions, sigla
    public static final String SIGLA_FIELD = "sigla";
    public static final String MARC_910_A = "marc_910a";
    public static final String MARC_040_A = "marc_040a";

    // links
    public static final String MARC_911_U = "marc_911u";
    public static final String MARC_956_U = "marc_956u";
    public static final String MARC_856_U = "marc_856u";

    public static final String NAZEV_FIELD = "nazev";
    public static final String AUTHOR_FIELD = "author";
    public static final String FMT_FIELD = "fmt";

    // Additional author field
    public static final String MARC_700_a = "marc_700a";

    // publisher fields
    public static final String NAKLADATEL_FIELD = "nakladatel";
    public static final String MARC_264_B = "marc_264b";
    public static final String MARC_260_B = "marc_260b";

    // identifiers
    //isbn
    public static final String MARC_020_A = "marc_020a";
    public static final String MARC_902_A = "marc_902a";

    public static final String MARC_035_A = "marc_035a";
    
    //marc_902a
    //issn
    public static final String MARC_022_A = "marc_022a";

    //ccnb
    public static final String MARC_015_A = "marc_015a";

    // canceled ccnb
    public static final String MARC_015_Z = "marc_015z";

    //rocniky
    public static final String MARC_2643 = "marc_2643";

    // odkaz do SKC 
    public static final String MARC_996_A = "marc_996a";
    
    // priznaky; verejne v dk
    public static final String FLAG_PUBLIC_IN_DL= "flag_public_in_dl";

    // rozparsovany rok vydani
    public static final String YEAR_OF_PUBLICATION="rokvydani";
    public static final String YEAR_OF_PUBLICATION_1="date1_int";
    public static final String YEAR_OF_PUBLICATION_2="date2_int";

    // CCNB pole
    public static final String ID_CCNB_FIELD = "id_ccnb";
    // 
    public static final String ID_OAIIDENT = "id_oaiident";
    public static final String ID_SDNNT = "id_sdnnt";
    public static final String ID_PID="id_pid";
    
    public static final String ID_ISSN="id_issn";
    public static final String ID_ISBN="id_isbn";
    public static final String ID_ISMN="id_ismn";
    
    
    // sigla of digital libraries
    public static final String DIGITAL_LIBRARIES = "digital_libraries";
    
    //TODO: Remove - not used now
    public static final String ALTERNATIVE_ALEPH_LINK = "alternative_aleph_link";
    
    // followers
    public static final String FOLLOWERS = "followers";
    
    private MarcRecordFields() {}
}
