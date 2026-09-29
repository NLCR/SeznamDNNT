package cz.inovatika.sdnnt.services.kraminstances;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONObject;
import org.junit.Test;

public class CheckKrameriusConfigurationTest {

    @Test
    public void resolvesConfiguredApiPointForNdkUuidLinkMatchedBySlashPattern() {
        CheckKrameriusConfiguration configuration = CheckKrameriusConfiguration.initConfiguration(new JSONObject()
                .put("urls", new JSONObject()
                        .put("https?://ndk.cz/.*", new JSONObject()
                                .put("api", "https://api.kramerius7.nkp.cz/search")
                                .put("client", "https://ndk.cz/uuid/{0}")
                                .put("acronym", "nkp")
                                .put("sigla", "ABA000")
                                .put("version", "V7")
                                .put("skip", false))));

        assertEquals(
                "https://api.kramerius7.nkp.cz/search",
                configuration.resolveApiPoint("https://ndk.cz/uuid/uuid:329273f3-84ad-49ca-ad1a-3acf357f22ee"));
    }

    @Test
    public void findsConfigurationByResolvedApiPoint() {
        CheckKrameriusConfiguration configuration = CheckKrameriusConfiguration.initConfiguration(new JSONObject()
                .put("urls", new JSONObject()
                        .put("https?://ndk.cz/.*", new JSONObject()
                                .put("api", "https://api.kramerius7.nkp.cz/search")
                                .put("client", "https://ndk.cz/uuid/{0}")
                                .put("acronym", "nkp")
                                .put("sigla", "ABA000")
                                .put("version", "V7")
                                .put("skip", false))));

        String apiPoint = configuration.resolveApiPoint(
                "https://ndk.cz/uuid/uuid:329273f3-84ad-49ca-ad1a-3acf357f22ee");
        InstanceConfiguration instance = configuration.findByApiPoint(apiPoint);

        assertEquals("nkp", instance.getAcronym());
        assertEquals(InstanceConfiguration.KramVersion.V7, instance.getVersion());
    }

    @Test
    public void keepsFallbackSearchUrlForUnknownUuidLink() {
        CheckKrameriusConfiguration configuration = CheckKrameriusConfiguration.initConfiguration(new JSONObject()
                .put("urls", new JSONObject()));

        assertEquals(
                "https://unknown.example/search",
                configuration.resolveApiPoint("https://unknown.example/uuid/uuid:329273f3-84ad-49ca-ad1a-3acf357f22ee"));
    }

    @Test
    public void returnsNullForUnsupportedLinkShape() {
        CheckKrameriusConfiguration configuration = CheckKrameriusConfiguration.initConfiguration(new JSONObject()
                .put("urls", new JSONObject()));

        assertNull(configuration.resolveApiPoint("https://unknown.example/title/123"));
    }
}
