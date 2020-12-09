package com.adtiming.om.server.cp.util;

import com.adtiming.om.pb.CrossPromotionPB;
import com.adtiming.om.server.cp.dto.AdRequest;
import com.adtiming.om.server.cp.dto.Campaign;
import com.adtiming.om.server.dto.PublisherApp;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MacroReplaceUtil {
    private static final Logger LOG = LogManager.getLogger();

    private static final LinkedHashMap<Pattern, String> REG_REPLACES;
    private static final String[] SEARCHS = {"{", /**/"}", "|", /**/"[", "]", /**/" ", "<", /**/">", "(", /**/")", "@", /**/"\"", "^", "&amp;", "\\/", "\t", "\r", "\n"};
    private static final String[] REPLACE = {"%7B", "%7D", "%7C", "%5B", "%5D", "%20", "%3C", "%3E", "%28", "%29", "%40", "%22", "%5E", "&", /* */"/", "", /**/"", ""};

    static {
        REG_REPLACES = new LinkedHashMap<>();
        REG_REPLACES.put(Pattern.compile("(?i)%(?![\\da-f]{2})"), "%25");
        REG_REPLACES.put(Pattern.compile("##([^#]+)##"), "%23%23$1%23%23");
        REG_REPLACES.put(Pattern.compile("#([^#]+)#"), "%23$1%23");
    }

    private static final String[] PS_DID = {"{device_id}", "{idfa}", "{gaid}", "%7Bdevice_id%7D", "%7Bidfa%7D", "%7Bgaid%7D"};
    private static final String[] PS_SUBID = {"{subid}", "%7Bsubid%7D"};
    private static final String[] PS_IP = {"{ip}", "%7Bip%7D"};
    private static final String[] PS_BUNDLE_ID = {"{bundle_id}", "%7Bbundle_id%7D"};
    private static final String[] PS_BUNDLE_NAME = {"{bundle_name}", "%7Bbundle_name%7D"};
    private static final String[] PS_CAMPAIGN_ID = {"{campaign_id}", "%7Bcampaign_id%7D"};
    private static final String[] PS_CAMPAIGN_NAME = {"{campaign_name}", "%7Bcampaign_name%7D"};
    private static final String[] PS_CREATIVE_ID = {"{creative_id}", "%7Bcreative_id%7D"};
    private static final String[] PS_CREATIVE_NAME = {"{creative_name}", "%7Bcreative_name%7D"};
    private static final String[] PS_LANG = {"{lang_code}", "%7Blang_code%7D"};
    private static final String[] PS_UA = {"{ua}", "%7Bua%7D"};

    public static String replaceURL(String url, AdRequest req, Campaign campaign,
                                    CrossPromotionPB.CpCreative creative) {
        PublisherApp pubApp = req.getPubApp();
        Object[] arr = {
                PS_DID, req.getDid(),
                PS_SUBID, String.format("cp_%d", pubApp.getId()),
                PS_IP, req.getIp(),
                PS_BUNDLE_ID, StringUtils.isNotBlank(pubApp.getAppId()) ? pubApp.getAppId() : req.getBundle(),
                PS_BUNDLE_NAME, pubApp.getAppName(),
                PS_CAMPAIGN_ID, String.format("cp_%d_%d", pubApp.getId(), campaign.getId()),
                PS_CAMPAIGN_NAME, campaign.getName(),
                PS_CREATIVE_ID, String.format("cp_%d_%d", req.getPubApp().getId(), creative.getId()),
                PS_CREATIVE_NAME, String.valueOf(creative.getId()),
                PS_LANG, req.getLang(),
                PS_UA, req.getUa()
        };
        for (int i = 0; i < arr.length; ++i) {
            String[] ps = (String[]) arr[i];
            String v = StringUtils.defaultString(encodeParam((String) arr[++i]));
            for (String p : ps) {
                url = StringUtils.replace(url, p, v);
            }
        }

        // 必须在最后替换
        for (Map.Entry<Pattern, String> me : REG_REPLACES.entrySet()) {
            url = me.getKey().matcher(url).replaceAll(me.getValue());
        }
        return StringUtils.replaceEach(url, SEARCHS, REPLACE);
    }

    private static String encodeParam(String param) {
        if (StringUtils.isBlank(param))
            return "";
        try {
            return URLEncoder.encode(param, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return param;
    }
}
