package com.rothem.tree.textfeature;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KeywordExtractorTest {
    @Test
    public void testNounExtractionWithRegex() {
        String input = "파일명은 미래보고서_991231.pptx이고, 회의는 2024/11/27(화요일)에 열렸습니다.";
        var result = KeywordExtractor.extractKeywords(input);

        List<String> nouns = result.getNouns();
        Map<String, List<String>> regex = result.getRegex();

        assertThat(nouns).contains("파일", "미래", "보고서", "회의");
        assertThat(regex.get(RegexExtractorKey.SHORT_DATE_6.keyName())).contains("991231");
        assertThat(regex.get(RegexExtractorKey.DATE.keyName())).contains("2024/11/27");
    }

    @Test
    public void testExtractUniqueKeywordsFromComplexText() {
        String input = """
            사건 번호는 2023고합123입니다. 이 사건은 김철수가 연루된 사건입니다.
            김철수는 총 100만원을 요구했고, 해당 금액은 100만원으로 기록되었습니다.
            판결은 2024.05.01.에 선고되었고, 실제 집행일도 2024.05.01.입니다.
            이메일은 kim@example.com이며, 전화번호는 010-1234-5678입니다.
            이후 김철수는 같은 이메일 kim@example.com과 전화번호 010-1234-5678로 연락받았습니다.
        """;

        List<String> uniqueKeywords = KeywordExtractor.extractUniqueKeywords(input);

        assertThat(uniqueKeywords).contains(
                "사건", "번호", "고합", "김철수", "연루", "만", "요구", "해당", "금액", "기록", "판결",
                "선고", "실제", "집행", "일", "이메일", "전화번호", "이후", "연락",
                "kim@example.com", "2024.05.01", "010-1234-5678", "2023", "123", "100"
        );

        assertThat(uniqueKeywords).doesNotHaveDuplicates();
    }
}
