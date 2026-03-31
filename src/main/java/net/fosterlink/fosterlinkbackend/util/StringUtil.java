package net.fosterlink.fosterlinkbackend.util;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

public class StringUtil {

    public static String cleanString(String danger) {
        // Escape HTML entities first, then convert newlines to <br> so Jsoup
        // (which parses as HTML and would otherwise collapse whitespace) preserves them.
        // Safelist.basic() permits <br>, so they survive the clean pass.
        String escaped = StringEscapeUtils.escapeHtml4(danger);
        String withBreaks = escaped.replace("\r\n", "<br>").replace("\n", "<br>").replace("\r", "<br>");
        // Disable pretty-printing so Jsoup does not inject extra newlines around <br> tags.
        Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);
        String cleaned = Jsoup.clean(withBreaks, "", Safelist.basic(), outputSettings);
        // Restore newlines for plain-text / markdown storage.
        return cleaned.replaceAll("(?i)<br\\s*/?>", "\n");
    }

}
