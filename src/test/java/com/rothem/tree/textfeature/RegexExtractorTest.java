package com.rothem.tree.textfeature;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class RegexExtractorTest {

    @Test
    public void testValidDateExtraction() {
        String text = "회의는 2024/11/27에 열렸습니다.";
        Map<String, List<String>> result = RegexExtractor.extractRegexMatches(text);

        assertThat(result.get(RegexExtractorKey.DATE.keyName())).contains("2024/11/27");
        assertThat(result.get(RegexExtractorKey.SHORT_DATE_6.keyName())).isEmpty();
    }

    @Test
    public void testShortDate6Extraction() {
        String text = "파일명은 미래보고서_991231.pptx입니다.";
        Map<String, List<String>> result = RegexExtractor.extractRegexMatches(text);

        assertThat(result.get(RegexExtractorKey.SHORT_DATE_6.keyName())).contains("991231");
        assertThat(result.get(RegexExtractorKey.DATE.keyName())).isEmpty();
    }

    @Test
    public void testInvalidShortDate6NotMatched() {
        String text = "파일명은 미래보고서_991332.pptx입니다.";
        Map<String, List<String>> result = RegexExtractor.extractRegexMatches(text);

        assertThat(result.get(RegexExtractorKey.SHORT_DATE_6.keyName())).isEmpty();
        assertThat(result.get(RegexExtractorKey.NUMBER.keyName())).contains("991332");
    }

    @Test
    public void testEmailAndUrl() {
        String text = "문의는 contact@example.com 또는 https://example.com 참조";
        Map<String, List<String>> result = RegexExtractor.extractRegexMatches(text);

        assertThat(result.get(RegexExtractorKey.EMAIL.keyName())).contains("contact@example.com");
        assertThat(result.get(RegexExtractorKey.URL.keyName())).contains("https://example.com");
    }

    @Test
    public void testPhoneFilterWithOverlap() {
        String text = "전화번호는 010-1234-5678이고, 계좌는 020-111-222333 입니다.";
        Map<String, List<String>> result = RegexExtractor.extractRegexMatches(text);

        assertThat(result.get(RegexExtractorKey.PHONE_KR.keyName())).contains("010-1234-5678");
        assertThat(result.get(RegexExtractorKey.ACCOUNT.keyName())).contains("020-111-222333");
        assertThat(result.get(RegexExtractorKey.NUMBER.keyName())).doesNotContain("5678");
    }
}
