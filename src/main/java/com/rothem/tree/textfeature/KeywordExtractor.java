package com.rothem.tree.textfeature;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.Token;

import java.util.*;

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

    public static Map<String, Object> extractKeywords(String text) {
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

        result.put("nouns", nouns);
        result.put("regex", regexMatches);
        return result;
    }

    private static boolean isValidNounTag(String tag) {
        return Set.of("NNG", "NNP", "NP", "NR", "SH", "SL").contains(tag);
    }

    private static boolean isMeaningfulWord(String word) {
        if (word.matches("\\d{2,}")) return false; // 숫자만 두 자리 이상은 무시
        if (word.matches("[a-zA-Z]{2,}")) {
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
