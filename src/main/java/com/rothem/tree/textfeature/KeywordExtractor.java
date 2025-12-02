package com.rothem.tree.textfeature;

import com.rothem.tree.textfeature.data.KeywordExtractionResult;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.Token;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeywordExtractor {
    private static final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
    private static final LinkedHashSet<String> userDictionary = new LinkedHashSet<>();
    private static final LinkedHashSet<String> userDictionaryWords = new LinkedHashSet<>();
    private static Path userDictionaryFile;

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

    private static final Pattern KOREAN_JOSA_SUFFIX = Pattern.compile("(.+?)(?:은|는|이|가|을|를|의|에|에서|에게|한테|으로|로|과|와|도|만|까지|부터)$");

    /**
     * Adds user-defined words into KOMORAN's dictionary.
     * Accepts plain words (automatically tagged as NNP) or already-formatted entries (e.g. "단어\tNNP").
     */
    public static synchronized void addUserDictionary(Collection<String> userWords) {
        if (userWords == null || userWords.isEmpty()) return;

        boolean updated = false;
        for (String word : userWords) {
            String normalized = normalizeUserDictionaryEntry(word);
            if (normalized != null) {
                updated |= userDictionary.add(normalized);
                String baseWord = normalized.split("\\s+")[0];
                if (!baseWord.isEmpty()) {
                    userDictionaryWords.add(baseWord);
                }
            }
        }

        if (updated) {
            applyUserDictionary();
        }
    }

    /**
     * Applies an existing KOMORAN user dictionary file.
     *
     * @param userDictionaryPath path to a KOMORAN-format user dictionary file
     */
    public static synchronized void setUserDictionaryPath(String userDictionaryPath) {
        if (userDictionaryPath == null || userDictionaryPath.isBlank()) return;
        userDictionaryWords.clear();
        try {
            for (String line : Files.readAllLines(Path.of(userDictionaryPath), StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                String baseWord = trimmed.split("\\s+")[0];
                if (!baseWord.isEmpty()) {
                    userDictionaryWords.add(baseWord);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read user dictionary file: " + userDictionaryPath, e);
        }
        komoran.setUserDic(userDictionaryPath);
    }

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
        if (text == null || text.isBlank()) {
            return new KeywordExtractionResult(Collections.emptyList(), Collections.emptyMap());
        }

        Map<String, List<String>> regexMatches = RegexExtractor.extractRegexMatches(text);
        List<int[]> occupiedSpans = RegexExtractor.getOccupiedSpans(text);

        LinkedHashSet<String> nouns = new LinkedHashSet<>();
        Set<String> used = new HashSet<>();

        List<Token> tokens = analyzeSafely(text);

        for (Token token : tokens) {
            String word = normalizeToken(token.getMorph());
            String tag = token.getPos();
            int start = token.getBeginIndex();

            if (word.isEmpty()) continue;
            if (!isValidNounTag(tag)) continue;
            if (used.contains(word + ":" + start)) continue;
            if (isInOccupied(start, occupiedSpans)) continue;
            if (!isMeaningfulWord(word)) continue;

            nouns.add(word);
            addUserDictionarySubTokens(word, nouns);
            used.add(word + ":" + start);
        }

        if (looksLikeFilename(text)) {
            for (String token : splitFilenameToTokens(text)) {
                String normalized = normalizeToken(token);
                if (!isMeaningfulFilenameToken(normalized)) continue;
                nouns.add(normalized);
            }
        }

        splitWhitespaceTokens(nouns);
        removeRedundantShortLatinTokens(nouns);

        return new KeywordExtractionResult(new ArrayList<>(nouns), regexMatches);
    }

    // KOMORAN occasionally throws NPE internally on malformed input; guard and fall back to empty.
    private static List<Token> analyzeSafely(String text) {
        try {
            var result = komoran.analyze(text);
            if (result == null || result.getTokenList() == null) return List.of();
            return result.getTokenList();
        } catch (Exception e) {
            return List.of();
        }
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

    private static String normalizeToken(String word) {
        if (word == null) return "";

        String cleaned = Normalizer.normalize(word, Normalizer.Form.NFC)
                .replaceAll("^[\\p{Punct}“”‘’]+", "")
                .replaceAll("[\\p{Punct}“”‘’]+$", "");

        if (cleaned.matches("^[가-힣]{2,}$")) {
            Matcher m = KOREAN_JOSA_SUFFIX.matcher(cleaned);
            if (m.matches() && m.group(1).length() >= 2) {
                cleaned = m.group(1);
            }
        }

        return cleaned.trim();
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

    private static boolean isMeaningfulFilenameToken(String word) {
        if (word == null || word.isEmpty()) return false;
        if (word.matches("[가-힣]")) return false; // avoid single-syllable noise like "팀"
        if (word.matches("\\d{2,}")) return true; // allow numeric tokens in filenames
        return isMeaningfulWord(word);
    }

    private static boolean looksLikeFilename(String text) {
        if (text == null) return false;
        return Pattern.compile("\\.[A-Za-z0-9]{2,6}(\\s|$)").matcher(text).find();
    }

    private static String normalizeUserDictionaryEntry(String entry) {
        if (entry == null) return null;
        String trimmed = entry.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.contains("\t") || trimmed.contains(" ")) return trimmed;
        return trimmed + "\tNNP";
    }

    private static void applyUserDictionary() {
        try {
            if (userDictionaryFile == null) {
                userDictionaryFile = Files.createTempFile("keyword-extractor-userdic", ".txt");
                userDictionaryFile.toFile().deleteOnExit();
            }
            Files.writeString(userDictionaryFile, String.join("\n", userDictionary), StandardCharsets.UTF_8);
            komoran.setUserDic(userDictionaryFile.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to apply user dictionary", e);
        }
    }

    private static void addUserDictionarySubTokens(String word, LinkedHashSet<String> nouns) {
        if (userDictionaryWords.isEmpty()) return;
        for (String base : userDictionaryWords) {
            if (base.equals(word)) continue;
            if (!word.contains(base)) continue;
            if (!isMeaningfulWord(base)) continue;
            nouns.add(base);
        }
    }

    private static void removeRedundantShortLatinTokens(LinkedHashSet<String> nouns) {
        if (nouns.isEmpty()) return;

        Set<String> longer = new HashSet<>();
        for (String n : nouns) {
            if (n.matches("[A-Za-z0-9]{2,}")) {
                longer.add(n.toLowerCase());
            }
        }

        Iterator<String> it = nouns.iterator();
        while (it.hasNext()) {
            String n = it.next();
            if (n.matches("[A-Za-z]")) {
                String lower = n.toLowerCase();
                boolean hasLonger = longer.stream().anyMatch(l -> l.startsWith(lower));
                if (hasLonger) it.remove();
            }
        }
    }

    private static void splitWhitespaceTokens(LinkedHashSet<String> nouns) {
        if (nouns.isEmpty()) return;

        LinkedHashSet<String> rebuilt = new LinkedHashSet<>();
        for (String noun : nouns) {
            if (noun.contains(" ")) {
                for (String part : noun.split("\\s+")) {
                    String normalized = normalizeToken(part);
                    if (normalized.isEmpty()) continue;
                    if (!isMeaningfulWord(normalized)) continue;
                    rebuilt.add(normalized);
                }
            } else {
                rebuilt.add(noun);
            }
        }

        nouns.clear();
        nouns.addAll(rebuilt);
    }

    private static boolean isInOccupied(int pos, List<int[]> spans) {
        for (int[] span : spans) {
            if (pos >= span[0] && pos < span[1]) return true;
        }
        return false;
    }
}
