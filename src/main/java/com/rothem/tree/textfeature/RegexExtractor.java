package com.rothem.tree.textfeature;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexExtractor {

    private static final Map<RegexExtractorKey, Pattern> PATTERNS = new LinkedHashMap<>();

    static {
        PATTERNS.put(RegexExtractorKey.SHORT_DATE_6, Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)"));
        PATTERNS.put(RegexExtractorKey.URL, Pattern.compile(
                "(?:(?:https?|ftp)://[\\w\\-.\\u3131-\\uD79D@]+(?:/[\\w\\-./?&=#%:+~\\u3131-\\uD79D@]*)?)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        PATTERNS.put(RegexExtractorKey.EMAIL, Pattern.compile(
                "(?<![/\\w])[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(?![/\\w])"));
        PATTERNS.put(RegexExtractorKey.DATE, Pattern.compile(
                "(?<!\\d)((?:19|20)\\d{2})[./-](0?[1-9]|1[0-2])[./-](0?[1-9]|[12][0-9]|3[01])(?!\\d)"
        ));
        PATTERNS.put(RegexExtractorKey.PHONE_KR, Pattern.compile(
                "(?<!\\d)(?:0\\d{1,2}[-\\s]?\\d{3,4}[-\\s]?\\d{4})(?!\\d)"
        ));
        PATTERNS.put(RegexExtractorKey.PHONE_INTL, Pattern.compile(
                "(?<!\\d)(?:\\+\\d{1,3}[-\\s]?(?:\\d{1,4}[-\\s]?){2,4}\\d{2,4})(?!\\d)"
        ));
        PATTERNS.put(RegexExtractorKey.ACCOUNT, Pattern.compile(
                "(?<!\\d)(?:\\d{2,4}[-\\s‒]?){2,3}\\d{5,6}(?!\\d)"));
        PATTERNS.put(RegexExtractorKey.NUMBER, Pattern.compile(
                "(?<!\\d)((?:\\d{1,3}(?:,\\d{3})+)(?:\\.\\d+)?|\\d+\\.\\d+|\\d+)(?!\\d)"));
    }

    public static Map<String, List<String>> extractRegexMatches(String text) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        List<int[]> occupiedSpans = new ArrayList<>();

        for (RegexExtractorKey key : PATTERNS.keySet()) {
            Pattern pattern = PATTERNS.get(key);
            Matcher matcher = pattern.matcher(text);
            List<String> matches = new ArrayList<>();

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                String matched = matcher.group();

                if (key == RegexExtractorKey.SHORT_DATE_6 && !isValidShortDate6(matched)) continue;
                if (key == RegexExtractorKey.DATE && !isValidDate(matched)) continue;

                if (Set.of(RegexExtractorKey.NUMBER, RegexExtractorKey.PHONE_KR, RegexExtractorKey.PHONE_INTL, RegexExtractorKey.ACCOUNT).contains(key)) {
                    if (isInside(start, end, occupiedSpans)) continue;
                }

                matches.add(matched);
                occupiedSpans.add(new int[]{start, end});
            }

            result.put(key.keyName(), matches);
        }

        return result;
    }

    public static List<int[]> getOccupiedSpans(String text) {
        List<int[]> spans = new ArrayList<>();
        for (RegexExtractorKey key : PATTERNS.keySet()) {
            if (key == RegexExtractorKey.NUMBER) continue;
            Matcher matcher = PATTERNS.get(key).matcher(text);
            while (matcher.find()) {
                spans.add(new int[]{matcher.start(), matcher.end()});
            }
        }
        return spans;
    }

    private static boolean isInside(int start, int end, List<int[]> spans) {
        for (int[] span : spans) {
            if (start >= span[0] && end <= span[1]) return true;
        }
        return false;
    }

    private static boolean isValidShortDate6(String yymmdd) {
        if (!yymmdd.matches("\\d{6}")) return false;
        try {
            int yy = Integer.parseInt(yymmdd.substring(0, 2));
            int mm = Integer.parseInt(yymmdd.substring(2, 4));
            int dd = Integer.parseInt(yymmdd.substring(4, 6));

            int fullYear = (yy >= 0 && yy <= 49) ? 2000 + yy : 1900 + yy;
            LocalDate.of(fullYear, mm, dd);
            return true;
        } catch (DateTimeException | NumberFormatException e) {
            return false;
        }
    }

    private static boolean isValidDate(String yyyymmdd) {
        try {
            LocalDate.parse(yyyymmdd.replaceAll("[./-]", "-"), DateTimeFormatter.ofPattern("yyyy-M-d"));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
