package searchengine.dto.indexing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageUrlConverter {

    private static final String REGEX = "(https?://[.\\w]*)/";

    public static String getSiteUrlFromPageUrl(String url) {
        Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(url);

        return matcher.find() ? matcher.group(1) : "";
    }


}
