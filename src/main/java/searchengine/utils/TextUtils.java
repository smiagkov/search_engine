package searchengine.utils;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Component
public class TextUtils {
    public String[] getWordsFromText(String text) {
        Pattern pattern = Pattern.compile("[а-я]+", Pattern.UNICODE_CHARACTER_CLASS
                | Pattern.CASE_INSENSITIVE);
        return pattern.matcher(text).results()
                .map(MatchResult::group)
                .map(String::trim)
                .toArray(String[]::new);
    }

    public String removeHtmlTags(String text) {
        return Jsoup.parse(text).text();
    }
}
