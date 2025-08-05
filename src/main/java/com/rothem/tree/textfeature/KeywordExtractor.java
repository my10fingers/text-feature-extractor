package com.rothem.tree.textfeature;

import com.rothem.tree.textfeature.data.KeywordExtractionResult;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.Token;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeywordExtractor {
    private static final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

    private static final Set<String> EN_STOPWORDS = Set.of(
            "a","about","above","after","again","against","all","am","an","and","any","are","aren't",
            "as","at","be","because","been","before","being","below","between","both","but","by",
            "can't","cannot","could","couldn't","did","didn't","do","does","doesn't","doing","don't",
            "down","during","each","few","for","from","further","had","hadn't","has","hasn't","have",
            "haven't","having","he","he'd","he'll","he's","her","here","here's","hers","herself",
            "him","himself","his","how","how's","i","i'd","i'll","i'm","i've","if","in","into","is",
            "isn't","it","it's","its","itself","let's","me","more","most","mustn't","my","myself",
            "no","nor","not","of","off","on","once","only","or","other","ought","our","ours",
            "ourselves","out","over","own","same","shan't","she","she'd","she'll","she's","should",
            "shouldn't","so","some","such","than","that","that's","the","their","theirs","them",
            "themselves","then","there","there's","these","they","they'd","they'll","they're",
            "they've","this","those","through","to","too","under","until","up","very","was","wasn't",
            "we","we'd","we'll","we're","we've","were","weren't","what","what's","when","when's",
            "where","where's","which","while","who","who's","whom","why","why's","will","with",
            "won't","would","wouldn't","you","you'd","you'll","you're","you've","your","yours",
            "yourself","yourselves"
    );

    public static List<String> extractUniqueKeywords(String text) {
        var result = extractKeywords(text);

        Set<String> uniqueKeywords = new LinkedHashSet<>();

        if (result.getNouns() != null) {
            uniqueKeywords.addAll(result.getNouns());
        }

        if (result.getRegex() != null) {
            for (List<String> values : result.getRegex().values()) {
                uniqueKeywords.addAll(values);
            }
        }

        return new ArrayList<>(uniqueKeywords);
    }

    public static KeywordExtractionResult extractKeywords(String text) {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, List<String>> regexMatches = RegexExtractor.extractRegexMatches(text);
        List<int[]> occupiedSpans = RegexExtractor.getOccupiedSpans(text);

        List<String> nouns = new ArrayList<>();
        Set<String> used = new HashSet<>();

        List<Token> tokens = komoran.analyze(text).getTokenList();

        for (Token token : tokens) {
            String word = token.getMorph();
            String tag = token.getPos();
            int start = token.getBeginIndex();

            if (!isValidNounTag(tag)) continue;
            if (used.contains(word + ":" + start)) continue;
            if (isInOccupied(start, occupiedSpans)) continue;
            if (!isMeaningfulWord(word)) continue;

            nouns.add(word);
            used.add(word + ":" + start);
        }

        return new KeywordExtractionResult(nouns, regexMatches);
    }

    public static List<String> splitFilenameToTokens(String filename) {
        Set<String> result = new LinkedHashSet<>();

        String cleaned = Normalizer.normalize(filename, Normalizer.Form.NFC);

        cleaned = cleaned
                .replaceAll("[_\\-\\[\\]\\{\\}\\(\\)<>~!@#$%^&*+=|;:'\",.?/`]", " ")
                .replaceAll("[“”‘’]", " ")
                .replaceAll("[^\\w\\uAC00-\\uD7A3\\u1100-\\u11FF\\u3130-\\u318F\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        for (String token : cleaned.split("\\s+")) {
            if (token.isEmpty()) continue;
            result.add(token);

            Matcher m = Pattern.compile("[가-힣]+|[a-zA-Z]+|[0-9]+").matcher(token);
            List<String> subTokens = new ArrayList<>();
            int blocks = 0, hangul = 0, latin = 0, digit = 0;

            while (m.find()) {
                String sub = m.group();
                blocks++;
                if (sub.matches("[가-힣]+")) hangul++;
                else if (sub.matches("[a-zA-Z]+")) latin++;
                else if (sub.matches("[0-9]+")) digit++;
                subTokens.add(sub);
            }

            if (blocks > 1 && hangul > 0 && (latin > 0 || digit > 0)) {
                for (String sub : subTokens) {
                    result.add(sub);
                }
            }
        }
        return new ArrayList<>(result);
    }

    private static boolean isValidNounTag(String tag) {
        return Set.of("NNG", "NNP", "NP", "NR", "SH", "SL", "NA").contains(tag);
    }

    private static boolean isMeaningfulWord(String word) {
        if (word.matches("\\d{2,}")) return false; // 숫자만 두 자리 이상
        if (word.matches("[a-zA-Z'\\-\\.]+")) {
            return !EN_STOPWORDS.contains(word.toLowerCase());
        }
        return true;
    }

    private static boolean isInOccupied(int pos, List<int[]> spans) {
        for (int[] span : spans) {
            if (pos >= span[0] && pos < span[1]) return true;
        }
        return false;
    }
}
