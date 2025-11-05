package net.fosterlink.fosterlinkbackend.util;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.boot.autoconfigure.web.ErrorProperties;

public class StringUtil {

    public static String cleanString(String danger) {
        return Jsoup.clean(StringEscapeUtils.escapeHtml4(danger), Safelist.basic());
    }

}
