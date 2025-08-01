package searchengine.services.implementation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageIndexing {

    private static final String REGEX = "(https?://[.\\w]*)/";

    public String getSiteUrl(String url) {
        Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(url);

        return matcher.find() ? matcher.group(1) : "";
    }



}
