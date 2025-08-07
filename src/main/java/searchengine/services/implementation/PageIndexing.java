package searchengine.services.implementation;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageIndexing {

    private static final String REGEX = "(https?://[.\\w]*)/";

    public static String getSiteUrl(String url) {
        Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(url);

        return matcher.find() ? matcher.group(1) : "";
    }

    public static String getDecodedUrl(String url) {
        return URLDecoder.decode(url, StandardCharsets.UTF_8);

    }
}
