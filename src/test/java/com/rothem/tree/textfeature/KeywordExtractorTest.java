package com.rothem.tree.textfeature;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KeywordExtractorTest {
    @Test
    public void testNounExtractionWithRegex() {
        String input = "파일명은 미래보고서_991231.pptx이고, 회의는 2024/11/27(화요일)에 열렸습니다.";
        Map<String, Object> result = KeywordExtractor.extractKeywords(input);

        @SuppressWarnings("unchecked")
        List<String> nouns = (List<String>) result.get("nouns");
        @SuppressWarnings("unchecked")
        Map<String, List<String>> regex = (Map<String, List<String>>) result.get("regex");

        assertThat(nouns).contains("파일", "미래", "보고서", "회의");
        assertThat(regex.get("short_date_6")).contains("991231");
        assertThat(regex.get("date")).contains("2024/11/27");
    }
}
