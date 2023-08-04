package cz.inovatika.sdnnt.services.utils;
import java.util.HashMap;
import java.util.Map;

public class ISO693Converter {

    private  Map<String, String> iso6932ToIso6933Map = new HashMap<>();
    private  Map<String, String> iso6933ToIso6932Map = new HashMap<>();

     {
        // Mapování kódů z ISO 693-2 na ISO 693-3
        iso6932ToIso6933Map.put("aar", "aar");
        iso6932ToIso6933Map.put("abk", "abk");
        iso6932ToIso6933Map.put("ada", "ada");
        iso6932ToIso6933Map.put("afa", "afa");
        iso6932ToIso6933Map.put("afr", "afr");
        iso6932ToIso6933Map.put("akk", "akk");
        iso6932ToIso6933Map.put("alb", "sqi");
        iso6932ToIso6933Map.put("ale", "ale");
        iso6932ToIso6933Map.put("alt", "alt");
        iso6932ToIso6933Map.put("amh", "amh");
        iso6932ToIso6933Map.put("ang", "ang");
        iso6932ToIso6933Map.put("anp", "anp");
        iso6932ToIso6933Map.put("ara", "ara");
        iso6932ToIso6933Map.put("arc", "arc");
        iso6932ToIso6933Map.put("arm", "hye");
        iso6932ToIso6933Map.put("arp", "arp");
        iso6932ToIso6933Map.put("art", "art");
        iso6932ToIso6933Map.put("asm", "asm");
        iso6932ToIso6933Map.put("ast", "ast");
        iso6932ToIso6933Map.put("aus", "aus");
        iso6932ToIso6933Map.put("ava", "ava");
        iso6932ToIso6933Map.put("ave", "ave");
        iso6932ToIso6933Map.put("aym", "aym");
        iso6932ToIso6933Map.put("aze", "aze");
        iso6932ToIso6933Map.put("bad", "bad");
        iso6932ToIso6933Map.put("bak", "bak");
        iso6932ToIso6933Map.put("bal", "bal");
        iso6932ToIso6933Map.put("bam", "bam");
        iso6932ToIso6933Map.put("ban", "ban");
        iso6932ToIso6933Map.put("baq", "eus");
        iso6932ToIso6933Map.put("bas", "bas");
        iso6932ToIso6933Map.put("bat", "bat");
        iso6932ToIso6933Map.put("bel", "bel");
        iso6932ToIso6933Map.put("ben", "ben");
        iso6932ToIso6933Map.put("ber", "ber");
        iso6932ToIso6933Map.put("bik", "bik");
        iso6932ToIso6933Map.put("bnt", "bnt");
        iso6932ToIso6933Map.put("bos", "bos");
        iso6932ToIso6933Map.put("bre", "bre");
        iso6932ToIso6933Map.put("bua", "bua");
        iso6932ToIso6933Map.put("bug", "bug");
        iso6932ToIso6933Map.put("bul", "bul");
        iso6932ToIso6933Map.put("bur", "mya");
        iso6932ToIso6933Map.put("cad", "cad");
        iso6932ToIso6933Map.put("cai", "cai");
        iso6932ToIso6933Map.put("cat", "cat");
        iso6932ToIso6933Map.put("cau", "cau");
        iso6932ToIso6933Map.put("ceb", "ceb");
        iso6932ToIso6933Map.put("cel", "cel");
        iso6932ToIso6933Map.put("cha", "cha");
        iso6932ToIso6933Map.put("che", "che");
        iso6932ToIso6933Map.put("chg", "chg");
        iso6932ToIso6933Map.put("chi", "zho");
        iso6932ToIso6933Map.put("chm", "chm");
        iso6932ToIso6933Map.put("chn", "chn");
        iso6932ToIso6933Map.put("cho", "cho");
        iso6932ToIso6933Map.put("chp", "chp");
        iso6932ToIso6933Map.put("chr", "chr");
        iso6932ToIso6933Map.put("chu", "chu");
        iso6932ToIso6933Map.put("chv", "chv");
        iso6932ToIso6933Map.put("cop", "cop");
        iso6932ToIso6933Map.put("cor", "cor");
        iso6932ToIso6933Map.put("cos", "cos");
        iso6932ToIso6933Map.put("cpe", "cpe");
        iso6932ToIso6933Map.put("cpf", "cpf");
        iso6932ToIso6933Map.put("cpp", "cpp");
        iso6932ToIso6933Map.put("cre", "cre");
        iso6932ToIso6933Map.put("crh", "crh");
        iso6932ToIso6933Map.put("csb", "csb");
        iso6932ToIso6933Map.put("cus", "cus");
        iso6932ToIso6933Map.put("cze", "ces");
        iso6932ToIso6933Map.put("dak", "dak");
        iso6932ToIso6933Map.put("dan", "dan");
        iso6932ToIso6933Map.put("dar", "dar");
        iso6932ToIso6933Map.put("del", "del");
        iso6932ToIso6933Map.put("den", "den");
        iso6932ToIso6933Map.put("din", "din");
        iso6932ToIso6933Map.put("dra", "dra");
        iso6932ToIso6933Map.put("dsb", "dsb");
        iso6932ToIso6933Map.put("dua", "dua");
        iso6932ToIso6933Map.put("dum", "dum");
        iso6932ToIso6933Map.put("dut", "nld");
        iso6932ToIso6933Map.put("efi", "efi");
        iso6932ToIso6933Map.put("egy", "egy");
        iso6932ToIso6933Map.put("elx", "elx");
        iso6932ToIso6933Map.put("eng", "eng");
        iso6932ToIso6933Map.put("enm", "enm");
        iso6932ToIso6933Map.put("epo", "epo");
        iso6932ToIso6933Map.put("est", "est");
        iso6932ToIso6933Map.put("ewe", "ewe");
        iso6932ToIso6933Map.put("fan", "fan");
        iso6932ToIso6933Map.put("fao", "fao");
        iso6932ToIso6933Map.put("fil", "fil");
        iso6932ToIso6933Map.put("fin", "fin");
        iso6932ToIso6933Map.put("fiu", "fiu");
        iso6932ToIso6933Map.put("fon", "fon");
        iso6932ToIso6933Map.put("fre", "fra");
        iso6932ToIso6933Map.put("frm", "frm");
        iso6932ToIso6933Map.put("fro", "fro");
        iso6932ToIso6933Map.put("frr", "frr");
        iso6932ToIso6933Map.put("fry", "fry");
        iso6932ToIso6933Map.put("fur", "fur");
        iso6932ToIso6933Map.put("gaa", "gaa");
        iso6932ToIso6933Map.put("gem", "gem");
        iso6932ToIso6933Map.put("geo", "kat");
        iso6932ToIso6933Map.put("ger", "deu");
        iso6932ToIso6933Map.put("gez", "gez");
        iso6932ToIso6933Map.put("gla", "gla");
        iso6932ToIso6933Map.put("gle", "gle");
        iso6932ToIso6933Map.put("glg", "glg");
        iso6932ToIso6933Map.put("glv", "glv");
        iso6932ToIso6933Map.put("gmh", "gmh");
        iso6932ToIso6933Map.put("goh", "goh");
        iso6932ToIso6933Map.put("gon", "gon");
        iso6932ToIso6933Map.put("got", "got");
        iso6932ToIso6933Map.put("grb", "grb");
        iso6932ToIso6933Map.put("grc", "grc");
        iso6932ToIso6933Map.put("gre", "ell");
        iso6932ToIso6933Map.put("grn", "grn");
        iso6932ToIso6933Map.put("gsw", "gsw");
        iso6932ToIso6933Map.put("guj", "guj");
        iso6932ToIso6933Map.put("gwi", "gwi");
        iso6932ToIso6933Map.put("hai", "hai");
        iso6932ToIso6933Map.put("hau", "hau");
        iso6932ToIso6933Map.put("haw", "haw");
        iso6932ToIso6933Map.put("heb", "heb");
        iso6932ToIso6933Map.put("her", "her");
        iso6932ToIso6933Map.put("hil", "hil");
        iso6932ToIso6933Map.put("hin", "hin");
        iso6932ToIso6933Map.put("hit", "hit");
        iso6932ToIso6933Map.put("hmn", "hmn");
        iso6932ToIso6933Map.put("hrv", "hrv");
        iso6932ToIso6933Map.put("hsb", "hsb");
        iso6932ToIso6933Map.put("hun", "hun");
        iso6932ToIso6933Map.put("iba", "iba");
        iso6932ToIso6933Map.put("ibo", "ibo");
        iso6932ToIso6933Map.put("ice", "isl");
        iso6932ToIso6933Map.put("ido", "ido");
        iso6932ToIso6933Map.put("iku", "iku");
        iso6932ToIso6933Map.put("ilo", "ilo");
        iso6932ToIso6933Map.put("ina", "ina");
        iso6932ToIso6933Map.put("inc", "inc");
        iso6932ToIso6933Map.put("ind", "ind");
        iso6932ToIso6933Map.put("ine", "ine");
        iso6932ToIso6933Map.put("ipk", "ipk");
        iso6932ToIso6933Map.put("ira", "ira");
        iso6932ToIso6933Map.put("ita", "ita");
        iso6932ToIso6933Map.put("jpn", "jpn");
        iso6932ToIso6933Map.put("kaa", "kaa");
        iso6932ToIso6933Map.put("kac", "kac");
        iso6932ToIso6933Map.put("kal", "kal");
        iso6932ToIso6933Map.put("kam", "kam");
        iso6932ToIso6933Map.put("kan", "kan");
        iso6932ToIso6933Map.put("kar", "kar");
        iso6932ToIso6933Map.put("kaw", "kaw");
        iso6932ToIso6933Map.put("kaz", "kaz");
        iso6932ToIso6933Map.put("khi", "khi");
        iso6932ToIso6933Map.put("khm", "khm");
        iso6932ToIso6933Map.put("kir", "kir");
        iso6932ToIso6933Map.put("kok", "kok");
        iso6932ToIso6933Map.put("kom", "kom");
        iso6932ToIso6933Map.put("kon", "kon");
        iso6932ToIso6933Map.put("kor", "kor");
        iso6932ToIso6933Map.put("kum", "kum");
        iso6932ToIso6933Map.put("kur", "kur");
        iso6932ToIso6933Map.put("lad", "lad");
        iso6932ToIso6933Map.put("lao", "lao");
        iso6932ToIso6933Map.put("lat", "lat");
        iso6932ToIso6933Map.put("lav", "lav");
        iso6932ToIso6933Map.put("lin", "lin");
        iso6932ToIso6933Map.put("lit", "lit");
        iso6932ToIso6933Map.put("lol", "lol");
        iso6932ToIso6933Map.put("loo", "loo");
        iso6932ToIso6933Map.put("loz", "loz");
        iso6932ToIso6933Map.put("ltz", "ltz");
        iso6932ToIso6933Map.put("lug", "lug");
        iso6932ToIso6933Map.put("mac", "mkd");
        iso6932ToIso6933Map.put("mad", "mad");
        iso6932ToIso6933Map.put("mag", "mag");
        iso6932ToIso6933Map.put("mai", "mai");
        iso6932ToIso6933Map.put("mak", "mak");
        iso6932ToIso6933Map.put("mal", "mal");
        iso6932ToIso6933Map.put("mao", "mri");
        iso6932ToIso6933Map.put("map", "map");
        iso6932ToIso6933Map.put("mar", "mar");
        iso6932ToIso6933Map.put("may", "msa");
        iso6932ToIso6933Map.put("mdr", "mdr");
        iso6932ToIso6933Map.put("mga", "mga");
        iso6932ToIso6933Map.put("mis", "mis");
        iso6932ToIso6933Map.put("mlg", "mlg");
        iso6932ToIso6933Map.put("mlt", "mlt");
        iso6932ToIso6933Map.put("mnc", "mnc");
        iso6932ToIso6933Map.put("mol", "mol");
        iso6932ToIso6933Map.put("mon", "mon");
        iso6932ToIso6933Map.put("mul", "mul");
        iso6932ToIso6933Map.put("myn", "myn");
        iso6932ToIso6933Map.put("nah", "nah");
        iso6932ToIso6933Map.put("nai", "nai");
        iso6932ToIso6933Map.put("nap", "nap");
        iso6932ToIso6933Map.put("nde", "nde");
        iso6932ToIso6933Map.put("ndo", "ndo");
        iso6932ToIso6933Map.put("nds", "nds");
        iso6932ToIso6933Map.put("nep", "nep");
        iso6932ToIso6933Map.put("new", "new");
        iso6932ToIso6933Map.put("nic", "nic");
        iso6932ToIso6933Map.put("nno", "nno");
        iso6932ToIso6933Map.put("nob", "nob");
        iso6932ToIso6933Map.put("non", "non");
        iso6932ToIso6933Map.put("nor", "nor");
        iso6932ToIso6933Map.put("nso", "nso");
        iso6932ToIso6933Map.put("nub", "nub");
        iso6932ToIso6933Map.put("nya", "nya");
        iso6932ToIso6933Map.put("oci", "oci");
        iso6932ToIso6933Map.put("oss", "oss");
        iso6932ToIso6933Map.put("ota", "ota");
        iso6932ToIso6933Map.put("oto", "oto");
        iso6932ToIso6933Map.put("paa", "paa");
        iso6932ToIso6933Map.put("pag", "pag");
        iso6932ToIso6933Map.put("pal", "pal");
        iso6932ToIso6933Map.put("pan", "pan");
        iso6932ToIso6933Map.put("per", "fas");
        iso6932ToIso6933Map.put("phi", "phi");
        iso6932ToIso6933Map.put("pli", "pli");
        iso6932ToIso6933Map.put("pol", "pol");
        iso6932ToIso6933Map.put("por", "por");
        iso6932ToIso6933Map.put("pra", "pra");
        iso6932ToIso6933Map.put("pro", "pro");
        iso6932ToIso6933Map.put("pus", "pus");
        iso6932ToIso6933Map.put("que", "que");
        iso6932ToIso6933Map.put("raj", "raj");
        iso6932ToIso6933Map.put("rap", "rap");
        iso6932ToIso6933Map.put("re ", "rej");
        iso6932ToIso6933Map.put("roa", "roa");
        iso6932ToIso6933Map.put("roh", "roh");
        iso6932ToIso6933Map.put("rom", "rom");
        iso6932ToIso6933Map.put("rum", "ron");
        iso6932ToIso6933Map.put("run", "run");
        iso6932ToIso6933Map.put("rup", "rup");
        iso6932ToIso6933Map.put("rus", "rus");
        iso6932ToIso6933Map.put("sah", "sah");
        iso6932ToIso6933Map.put("sai", "sai");
        iso6932ToIso6933Map.put("sal", "sal");
        iso6932ToIso6933Map.put("san", "san");
        iso6932ToIso6933Map.put("scc", "srp");
        iso6932ToIso6933Map.put("sco", "sco");
        iso6932ToIso6933Map.put("scr", "hrv");
        iso6932ToIso6933Map.put("sem", "sem");
        iso6932ToIso6933Map.put("sgn", "sgn");
        iso6932ToIso6933Map.put("shn", "shn");
        iso6932ToIso6933Map.put("sin", "sin");
        iso6932ToIso6933Map.put("sit", "sit");
        iso6932ToIso6933Map.put("sla", "sla");
        iso6932ToIso6933Map.put("slo", "slk");
        iso6932ToIso6933Map.put("slv", "slv");
        iso6932ToIso6933Map.put("sma", "sma");
        iso6932ToIso6933Map.put("smi", "smi");
        iso6932ToIso6933Map.put("smj", "smj");
        iso6932ToIso6933Map.put("smo", "smo");
        iso6932ToIso6933Map.put("sna", "sna");
        iso6932ToIso6933Map.put("snd", "snd");
        iso6932ToIso6933Map.put("som", "som");
        iso6932ToIso6933Map.put("sot", "sot");
        iso6932ToIso6933Map.put("spa", "spa");
        iso6932ToIso6933Map.put("srd", "srd");
        iso6932ToIso6933Map.put("srp", "srp");
        iso6932ToIso6933Map.put("ssa", "ssa");
        iso6932ToIso6933Map.put("sun", "sun");
        iso6932ToIso6933Map.put("sux", "sux");
        iso6932ToIso6933Map.put("swa", "swa");
        iso6932ToIso6933Map.put("swe", "swe");
        iso6932ToIso6933Map.put("syc", "syc");
        iso6932ToIso6933Map.put("syr", "syr");
        iso6932ToIso6933Map.put("tah", "tah");
        iso6932ToIso6933Map.put("tai", "tai");
        iso6932ToIso6933Map.put("tam", "tam");
        iso6932ToIso6933Map.put("tat", "tat");
        iso6932ToIso6933Map.put("tel", "tel");
        iso6932ToIso6933Map.put("tem", "tem");
        iso6932ToIso6933Map.put("ter", "ter");
        iso6932ToIso6933Map.put("tgk", "tgk");
        iso6932ToIso6933Map.put("tgl", "tgl");
        iso6932ToIso6933Map.put("tha", "tha");
        iso6932ToIso6933Map.put("tib", "bod");
        iso6932ToIso6933Map.put("tir", "tir");
        iso6932ToIso6933Map.put("tmh", "tmh");
        iso6932ToIso6933Map.put("ton", "ton");
        iso6932ToIso6933Map.put("tsn", "tsn");
        iso6932ToIso6933Map.put("tso", "tso");
        iso6932ToIso6933Map.put("tuk", "tuk");
        iso6932ToIso6933Map.put("tur", "tur");
        iso6932ToIso6933Map.put("tut", "tut");
        iso6932ToIso6933Map.put("tyv", "tyv");
        iso6932ToIso6933Map.put("udm", "udm");
        iso6932ToIso6933Map.put("uga", "uga");
        iso6932ToIso6933Map.put("uig", "uig");
        iso6932ToIso6933Map.put("ukr", "ukr");
        iso6932ToIso6933Map.put("und", "und");
        iso6932ToIso6933Map.put("urd", "urd");
        iso6932ToIso6933Map.put("uzb", "uzb");
        iso6932ToIso6933Map.put("vai", "vai");
        iso6932ToIso6933Map.put("ven", "ven");
        iso6932ToIso6933Map.put("vie", "vie");
        iso6932ToIso6933Map.put("vjd", "vai");
        iso6932ToIso6933Map.put("vol", "vol");
        iso6932ToIso6933Map.put("vot", "vot");
        iso6932ToIso6933Map.put("wak", "wak");
        iso6932ToIso6933Map.put("wel", "wel");
        iso6932ToIso6933Map.put("wen", "wen");
        iso6932ToIso6933Map.put("wln", "wln");
        iso6932ToIso6933Map.put("wol", "wol");
        iso6932ToIso6933Map.put("xal", "xal");
        iso6932ToIso6933Map.put("xho", "xho");
        iso6932ToIso6933Map.put("xxx", "und");
        iso6932ToIso6933Map.put("yid", "yid");
        iso6932ToIso6933Map.put("yor", "yor");
        iso6932ToIso6933Map.put("zha", "zha");
        iso6932ToIso6933Map.put("zul", "zul");
        iso6932ToIso6933Map.put("zun", "zun");
        iso6932ToIso6933Map.put("zxx", "zxx");
        iso6932ToIso6933Map.put("zza", "zza");

        // Mapování kódů z ISO 693-3 na ISO 693-2
        iso6933ToIso6932Map.put("aar", "aar");
        iso6933ToIso6932Map.put("abk", "abk");
        iso6933ToIso6932Map.put("ada", "ada");
        iso6933ToIso6932Map.put("afa", "afa");
        iso6933ToIso6932Map.put("afr", "afr");
        iso6933ToIso6932Map.put("akk", "akk");
        iso6933ToIso6932Map.put("sqi", "alb");
        iso6933ToIso6932Map.put("ale", "ale");
        iso6933ToIso6932Map.put("alt", "alt");
        iso6933ToIso6932Map.put("amh", "amh");
        iso6933ToIso6932Map.put("ang", "ang");
        iso6933ToIso6932Map.put("anp", "anp");
        iso6933ToIso6932Map.put("ara", "ara");
        iso6933ToIso6932Map.put("arc", "arc");
        iso6933ToIso6932Map.put("hye", "arm");
        iso6933ToIso6932Map.put("arp", "arp");
        iso6933ToIso6932Map.put("art", "art");
        iso6933ToIso6932Map.put("asm", "asm");
        iso6933ToIso6932Map.put("ast", "ast");
        iso6933ToIso6932Map.put("aus", "aus");
        iso6933ToIso6932Map.put("ava", "ava");
        iso6933ToIso6932Map.put("ave", "ave");
        iso6933ToIso6932Map.put("aym", "aym");
        iso6933ToIso6932Map.put("aze", "aze");
        iso6933ToIso6932Map.put("bad", "bad");
        iso6933ToIso6932Map.put("bak", "bak");
        iso6933ToIso6932Map.put("bal", "bal");
        iso6933ToIso6932Map.put("bam", "bam");
        iso6933ToIso6932Map.put("ban", "ban");
        iso6933ToIso6932Map.put("eus", "baq");
        iso6933ToIso6932Map.put("bas", "bas");
        iso6933ToIso6932Map.put("bat", "bat");
        iso6933ToIso6932Map.put("bel", "bel");
        iso6933ToIso6932Map.put("ben", "ben");
        iso6933ToIso6932Map.put("ber", "ber");
        iso6933ToIso6932Map.put("bik", "bik");
        iso6933ToIso6932Map.put("bnt", "bnt");
        iso6933ToIso6932Map.put("bos", "bos");
        iso6933ToIso6932Map.put("bre", "bre");
        iso6933ToIso6932Map.put("bua", "bua");
        iso6933ToIso6932Map.put("bug", "bug");
        iso6933ToIso6932Map.put("bul", "bul");
        iso6933ToIso6932Map.put("mya", "bur");
        iso6933ToIso6932Map.put("cad", "cad");
        iso6933ToIso6932Map.put("cai", "cai");
        iso6933ToIso6932Map.put("cat", "cat");
        iso6933ToIso6932Map.put("cau", "cau");
        iso6933ToIso6932Map.put("ceb", "ceb");
        iso6933ToIso6932Map.put("cel", "cel");
        iso6933ToIso6932Map.put("cha", "cha");
        iso6933ToIso6932Map.put("che", "che");
        iso6933ToIso6932Map.put("chg", "chg");
        iso6933ToIso6932Map.put("zho", "chi");
        iso6933ToIso6932Map.put("chm", "chm");
        iso6933ToIso6932Map.put("chn", "chn");
        iso6933ToIso6932Map.put("cho", "cho");
        iso6933ToIso6932Map.put("chp", "chp");
        iso6933ToIso6932Map.put("chr", "chr");
        iso6933ToIso6932Map.put("chu", "chu");
        iso6933ToIso6932Map.put("chv", "chv");
        iso6933ToIso6932Map.put("cop", "cop");
        iso6933ToIso6932Map.put("cor", "cor");
        iso6933ToIso6932Map.put("cos", "cos");
        iso6933ToIso6932Map.put("cpe", "cpe");
        iso6933ToIso6932Map.put("cpf", "cpf");
        iso6933ToIso6932Map.put("cpp", "cpp");
        iso6933ToIso6932Map.put("cre", "cre");
        iso6933ToIso6932Map.put("crh", "crh");
        iso6933ToIso6932Map.put("csb", "csb");
        iso6933ToIso6932Map.put("cus", "cus");
        iso6933ToIso6932Map.put("ces", "cze");
        iso6933ToIso6932Map.put("dak", "dak");
        iso6933ToIso6932Map.put("dan", "dan");
        iso6933ToIso6932Map.put("dar", "dar");
        iso6933ToIso6932Map.put("del", "del");
        iso6933ToIso6932Map.put("den", "den");
        iso6933ToIso6932Map.put("din", "din");
        iso6933ToIso6932Map.put("dra", "dra");
        iso6933ToIso6932Map.put("dsb", "dsb");
        iso6933ToIso6932Map.put("dua", "dua");
        iso6933ToIso6932Map.put("dum", "dum");
        iso6933ToIso6932Map.put("nld", "dut");
        iso6933ToIso6932Map.put("efi", "efi");
        iso6933ToIso6932Map.put("egy", "egy");
        iso6933ToIso6932Map.put("elx", "elx");
        iso6933ToIso6932Map.put("eng", "eng");
        iso6933ToIso6932Map.put("enm", "enm");
        iso6933ToIso6932Map.put("epo", "epo");
        iso6933ToIso6932Map.put("est", "est");
        iso6933ToIso6932Map.put("ewe", "ewe");
        iso6933ToIso6932Map.put("fan", "fan");
        iso6933ToIso6932Map.put("fao", "fao");
        iso6933ToIso6932Map.put("fil", "fil");
        iso6933ToIso6932Map.put("fin", "fin");
        iso6933ToIso6932Map.put("fiu", "fiu");
        iso6933ToIso6932Map.put("fon", "fon");
        iso6933ToIso6932Map.put("fra", "fre");
        iso6933ToIso6932Map.put("frm", "frm");
        iso6933ToIso6932Map.put("fro", "fro");
        iso6933ToIso6932Map.put("frr", "frr");
        iso6933ToIso6932Map.put("fry", "fry");
        iso6933ToIso6932Map.put("fur", "fur");
        iso6933ToIso6932Map.put("gaa", "gaa");
        iso6933ToIso6932Map.put("gem", "gem");
        iso6933ToIso6932Map.put("kat", "geo");
        iso6933ToIso6932Map.put("deu", "ger");
        iso6933ToIso6932Map.put("gez", "gez");
        iso6933ToIso6932Map.put("gla", "gla");
        iso6933ToIso6932Map.put("gle", "gle");
        iso6933ToIso6932Map.put("glg", "glg");
        iso6933ToIso6932Map.put("glv", "glv");
        iso6933ToIso6932Map.put("gmh", "gmh");
        iso6933ToIso6932Map.put("goh", "goh");
        iso6933ToIso6932Map.put("gon", "gon");
        iso6933ToIso6932Map.put("got", "got");
        iso6933ToIso6932Map.put("grb", "grb");
        iso6933ToIso6932Map.put("grc", "grc");
        iso6933ToIso6932Map.put("ell", "gre");
        iso6933ToIso6932Map.put("grn", "grn");
        iso6933ToIso6932Map.put("gsw", "gsw");
        iso6933ToIso6932Map.put("guj", "guj");
        iso6933ToIso6932Map.put("gwi", "gwi");
        iso6933ToIso6932Map.put("hai", "hai");
        iso6933ToIso6932Map.put("hau", "hau");
        iso6933ToIso6932Map.put("haw", "haw");
        iso6933ToIso6932Map.put("heb", "heb");
        iso6933ToIso6932Map.put("her", "her");
        iso6933ToIso6932Map.put("hil", "hil");
        iso6933ToIso6932Map.put("hin", "hin");
        iso6933ToIso6932Map.put("hit", "hit");
        iso6933ToIso6932Map.put("hmn", "hmn");
        iso6933ToIso6932Map.put("hrv", "hrv");
        iso6933ToIso6932Map.put("hsb", "hsb");
        iso6933ToIso6932Map.put("hun", "hun");
        iso6933ToIso6932Map.put("iba", "iba");
        iso6933ToIso6932Map.put("ibo", "ibo");
        iso6933ToIso6932Map.put("isl", "ice");
        iso6933ToIso6932Map.put("ido", "ido");
        iso6933ToIso6932Map.put("iku", "iku");
        iso6933ToIso6932Map.put("ilo", "ilo");
        iso6933ToIso6932Map.put("ina", "ina");
        iso6933ToIso6932Map.put("inc", "inc");
        iso6933ToIso6932Map.put("ind", "ind");
        iso6933ToIso6932Map.put("ine", "ine");
        iso6933ToIso6932Map.put("ipk", "ipk");
        iso6933ToIso6932Map.put("ira", "ira");
        iso6933ToIso6932Map.put("ita", "ita");
        iso6933ToIso6932Map.put("jpn", "jpn");
        iso6933ToIso6932Map.put("kaa", "kaa");
        iso6933ToIso6932Map.put("kac", "kac");
        iso6933ToIso6932Map.put("kal", "kal");
        iso6933ToIso6932Map.put("kam", "kam");
        iso6933ToIso6932Map.put("kan", "kan");
        iso6933ToIso6932Map.put("kar", "kar");
        iso6933ToIso6932Map.put("kaw", "kaw");
        iso6933ToIso6932Map.put("kaz", "kaz");
        iso6933ToIso6932Map.put("khi", "khi");
        iso6933ToIso6932Map.put("khm", "khm");
        iso6933ToIso6932Map.put("kir", "kir");
        iso6933ToIso6932Map.put("kok", "kok");
        iso6933ToIso6932Map.put("kom", "kom");
        iso6933ToIso6932Map.put("kon", "kon");
        iso6933ToIso6932Map.put("kor", "kor");
        iso6933ToIso6932Map.put("kum", "kum");
        iso6933ToIso6932Map.put("kur", "kur");
        iso6933ToIso6932Map.put("lad", "lad");
        iso6933ToIso6932Map.put("lao", "lao");
        iso6933ToIso6932Map.put("lat", "lat");
        iso6933ToIso6932Map.put("lav", "lav");
        iso6933ToIso6932Map.put("lin", "lin");
        iso6933ToIso6932Map.put("lit", "lit");
        iso6933ToIso6932Map.put("lol", "lol");
        iso6933ToIso6932Map.put("loo", "loo");
        iso6933ToIso6932Map.put("loz", "loz");
        iso6933ToIso6932Map.put("ltz", "ltz");
        iso6933ToIso6932Map.put("lug", "lug");
        iso6933ToIso6932Map.put("mkd", "mac");
        iso6933ToIso6932Map.put("mad", "mad");
        iso6933ToIso6932Map.put("mag", "mag");
        iso6933ToIso6932Map.put("mai", "mai");
        iso6933ToIso6932Map.put("mak", "mak");
        iso6933ToIso6932Map.put("mal", "mal");
        iso6933ToIso6932Map.put("mri", "mao");
        iso6933ToIso6932Map.put("map", "map");
        iso6933ToIso6932Map.put("mar", "mar");
        iso6933ToIso6932Map.put("msa", "may");
        iso6933ToIso6932Map.put("mdr", "mdr");
        iso6933ToIso6932Map.put("mga", "mga");
        iso6933ToIso6932Map.put("mis", "mis");
        iso6933ToIso6932Map.put("mlg", "mlg");
        iso6933ToIso6932Map.put("mlt", "mlt");
        iso6933ToIso6932Map.put("mnc", "mnc");
        iso6933ToIso6932Map.put("mol", "mol");
        iso6933ToIso6932Map.put("mon", "mon");
        iso6933ToIso6932Map.put("mul", "mul");
        iso6933ToIso6932Map.put("myn", "myn");
        iso6933ToIso6932Map.put("nah", "nah");
        iso6933ToIso6932Map.put("nai", "nai");
        iso6933ToIso6932Map.put("nap", "nap");
        iso6933ToIso6932Map.put("nde", "nde");
        iso6933ToIso6932Map.put("ndo", "ndo");
        iso6933ToIso6932Map.put("nds", "nds");
        iso6933ToIso6932Map.put("nep", "nep");
        iso6933ToIso6932Map.put("new", "new");
        iso6933ToIso6932Map.put("nic", "nic");
        iso6933ToIso6932Map.put("nno", "nno");
        iso6933ToIso6932Map.put("nob", "nob");
        iso6933ToIso6932Map.put("non", "non");
        iso6933ToIso6932Map.put("nor", "nor");
        iso6933ToIso6932Map.put("nso", "nso");
        iso6933ToIso6932Map.put("nub", "nub");
        iso6933ToIso6932Map.put("nya", "nya");
        iso6933ToIso6932Map.put("oci", "oci");
        iso6933ToIso6932Map.put("oss", "oss");
        iso6933ToIso6932Map.put("ota", "ota");
        iso6933ToIso6932Map.put("oto", "oto");
        iso6933ToIso6932Map.put("paa", "paa");
        iso6933ToIso6932Map.put("pag", "pag");
        iso6933ToIso6932Map.put("pal", "pal");
        iso6933ToIso6932Map.put("pan", "pan");
        iso6933ToIso6932Map.put("fas", "per");
        iso6933ToIso6932Map.put("phi", "phi");
        iso6933ToIso6932Map.put("pli", "pli");
        iso6933ToIso6932Map.put("pol", "pol");
        iso6933ToIso6932Map.put("por", "por");
        iso6933ToIso6932Map.put("pra", "pra");
        iso6933ToIso6932Map.put("pro", "pro");
        iso6933ToIso6932Map.put("pus", "pus");
        iso6933ToIso6932Map.put("que", "que");
        iso6933ToIso6932Map.put("raj", "raj");
        iso6933ToIso6932Map.put("rap", "rap");
        iso6933ToIso6932Map.put("rej", "re ");
        iso6933ToIso6932Map.put("roa", "roa");
        iso6933ToIso6932Map.put("roh", "roh");
        iso6933ToIso6932Map.put("rom", "rom");
        iso6933ToIso6932Map.put("ron", "rum");
        iso6933ToIso6932Map.put("run", "run");
        iso6933ToIso6932Map.put("rup", "rup");
        iso6933ToIso6932Map.put("rus", "rus");
        iso6933ToIso6932Map.put("sah", "sah");
        iso6933ToIso6932Map.put("sai", "sai");
        iso6933ToIso6932Map.put("sal", "sal");
        iso6933ToIso6932Map.put("san", "san");
        iso6933ToIso6932Map.put("srp", "scc");
        iso6933ToIso6932Map.put("sco", "sco");
        iso6933ToIso6932Map.put("hrv", "scr");
        iso6933ToIso6932Map.put("sem", "sem");
        iso6933ToIso6932Map.put("sgn", "sgn");
        iso6933ToIso6932Map.put("shn", "shn");
        iso6933ToIso6932Map.put("sin", "sin");
        iso6933ToIso6932Map.put("sit", "sit");
        iso6933ToIso6932Map.put("sla", "sla");
        iso6933ToIso6932Map.put("slk", "slo");
        iso6933ToIso6932Map.put("slv", "slv");
        iso6933ToIso6932Map.put("sma", "sma");
        iso6933ToIso6932Map.put("smi", "smi");
        iso6933ToIso6932Map.put("smj", "smj");
        iso6933ToIso6932Map.put("smo", "smo");
        iso6933ToIso6932Map.put("sna", "sna");
        iso6933ToIso6932Map.put("snd", "snd");
        iso6933ToIso6932Map.put("som", "som");
        iso6933ToIso6932Map.put("sot", "sot");
        iso6933ToIso6932Map.put("spa", "spa");
        iso6933ToIso6932Map.put("srd", "srd");
        iso6933ToIso6932Map.put("srp", "srp");
        iso6933ToIso6932Map.put("ssa", "ssa");
        iso6933ToIso6932Map.put("sun", "sun");
        iso6933ToIso6932Map.put("sux", "sux");
        iso6933ToIso6932Map.put("swa", "swa");
        iso6933ToIso6932Map.put("swe", "swe");
        iso6933ToIso6932Map.put("syc", "syc");
        iso6933ToIso6932Map.put("syr", "syr");
        iso6933ToIso6932Map.put("tah", "tah");
        iso6933ToIso6932Map.put("tai", "tai");
        iso6933ToIso6932Map.put("tam", "tam");
        iso6933ToIso6932Map.put("tat", "tat");
        iso6933ToIso6932Map.put("tel", "tel");
        iso6933ToIso6932Map.put("tem", "tem");
        iso6933ToIso6932Map.put("ter", "ter");
        iso6933ToIso6932Map.put("tgk", "tgk");
        iso6933ToIso6932Map.put("tgl", "tgl");
        iso6933ToIso6932Map.put("tha", "tha");
        iso6933ToIso6932Map.put("bod", "tib");
        iso6933ToIso6932Map.put("tir", "tir");
        iso6933ToIso6932Map.put("tmh", "tmh");
        iso6933ToIso6932Map.put("ton", "ton");
        iso6933ToIso6932Map.put("tsn", "tsn");
        iso6933ToIso6932Map.put("tso", "tso");
        iso6933ToIso6932Map.put("tuk", "tuk");
        iso6933ToIso6932Map.put("tur", "tur");
        iso6933ToIso6932Map.put("tut", "tut");
        iso6933ToIso6932Map.put("tyv", "tyv");
        iso6933ToIso6932Map.put("udm", "udm");
        iso6933ToIso6932Map.put("uga", "uga");
        iso6933ToIso6932Map.put("uig", "uig");
        iso6933ToIso6932Map.put("ukr", "ukr");
        iso6933ToIso6932Map.put("und", "und");
        iso6933ToIso6932Map.put("urd", "urd");
        iso6933ToIso6932Map.put("uzb", "uzb");
        iso6933ToIso6932Map.put("vai", "vai");
        iso6933ToIso6932Map.put("ven", "ven");
        iso6933ToIso6932Map.put("vie", "vie");
        iso6933ToIso6932Map.put("vai", "vjd");
        iso6933ToIso6932Map.put("vol", "vol");
        iso6933ToIso6932Map.put("vot", "vot");
        iso6933ToIso6932Map.put("wak", "wak");
        iso6933ToIso6932Map.put("wel", "wel");
        iso6933ToIso6932Map.put("wen", "wen");
        iso6933ToIso6932Map.put("wln", "wln");
        iso6933ToIso6932Map.put("wol", "wol");
        iso6933ToIso6932Map.put("xal", "xal");
        iso6933ToIso6932Map.put("xho", "xho");
        iso6933ToIso6932Map.put("und", "xxx");
        iso6933ToIso6932Map.put("yid", "yid");
        iso6933ToIso6932Map.put("yor", "yor");
        iso6933ToIso6932Map.put("zha", "zha");
        iso6933ToIso6932Map.put("zul", "zul");
        iso6933ToIso6932Map.put("zun", "zun");
        iso6933ToIso6932Map.put("zxx", "zxx");
        iso6933ToIso6932Map.put("zza", "zza");
        
    }

    public String convertToISO6933(String iso6932Code) {
        String iso6933Code = iso6932ToIso6933Map.get(iso6932Code);
        return iso6933Code != null ? iso6933Code : iso6932Code;
    }

    public String convertToISO6932(String iso6933Code) {
        String iso6932Code = iso6933ToIso6932Map.get(iso6933Code);
        return iso6932Code != null ? iso6932Code : iso6933Code;
    }

    public static void main(String[] args) {

        ISO693Converter converter = new ISO693Converter();
        
        String iso6932Code = "cze-";
        String iso6933Code = converter.convertToISO6933(iso6932Code);
        System.out.println("ISO 693-2 to ISO 693-3: " + iso6932Code + " -> " + iso6933Code);

        String iso6933Code2 = "deu";
        String iso6932Code2 = converter.convertToISO6932(iso6933Code2);
        System.out.println("ISO 693-3 to ISO 693-2: " + iso6933Code2 + " -> " + iso6932Code2);
    }
}