package com.rothem.tree.textfeature;

import java.util.List;
import java.util.Map;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
    public void testUserDictionaryFileIsApplied() {
        String customWord = "테스트용단어";
        String userDicPath = Paths.get("src", "test", "resources", "user_test.dic").toString();

        KeywordExtractor.setUserDictionaryPath(userDicPath);

        String input = "이 문장은 테스트용단어 를 포함합니다.";
        var result = KeywordExtractor.extractKeywords(input);

        assertThat(result.getNouns()).contains(customWord, "테스트용", "단어");
    }

    @Test
    public void testUserDictionaryWordIsIncluded() {
        String customWord = "하이퍼그로스플랜";
        KeywordExtractor.addUserDictionary(List.of(customWord));

        String input = "우리 팀은 하이퍼그로스플랜을 준비 중입니다.";
        var result = KeywordExtractor.extractKeywords(input);

        assertThat(result.getNouns()).contains(customWord);
    }

    @ParameterizedTest()
    @ValueSource(strings = {
            "EML 저장용 첨부 (제목은 MSG).eml",
            "파일명은 미래보고서_991231.pptx이고, 회의는 2024/11/27(화요일)에 열렸습니다.",
            "200622 주요 현안 법무RM팀 v2.pptx",
            "주요 현안 200622 법무RM팀 v2.pptx",
            "주요 현안 법무RM팀",
            "2012년 상반기 관리등급 결정 계획서 pptx",
            "우리나라는 좋은 나라입니다. 대한민국 만세입니다."
    }
    )
    public void testExtractionWithRegexAndPrint(String input) {
//        String input = "200622_주요 현안_법무RM팀-v2.pptx".replace('_', ' ');
        var result = KeywordExtractor.extractKeywords(input);

        List<String> nouns = result.getNouns();
        Map<String, List<String>> regex = result.getRegex();

//        assertTrue(nouns.size() > 0);
        System.out.println(input + ": " + nouns);
        System.out.println(regex);
    }

    @Test
    public void testSplitFilenameToTokens() {
        String input = "200622_주요 현안_[우리집]_{가출한}_법무RM팀-v2.pptx";
        var result = KeywordExtractor.splitFilenameToTokens(input);

        assertNotNull(result);
        assertFalse(result.isEmpty(), "추출된 키워드가 있어야 합니다.");
        System.out.println("추출된 키워드: " + result);
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

    @Test
    public void testExtractKeywordsForPrint() {
        String input = """
                AI 기반 불복 조서 자동 작성 시스템 (R5)
                프로젝트 제안서 요약
                
                
                1. 프로젝트 개요
                오프라인 기반 AI 불복 조서 자동 작성 시스템
                기존 조서 + 판례 + 법령을 AI로 분석 및 생성
                RAG 구조 기반 생성, API 없이 오프라인 구축
                
                
                2. 주요 기능
                문서 수집 및 파싱 (OCR, HWP/PDF 지원)
                지식 벡터화 및 검색 (FAISS, KoBERT 등)
                AI 기반 논리 문장 생성 (KoGPT, LLaMA2-Ko)
                DB 설계 및 메타정보/임베딩 저장
                하이브리드 UI 및 사내 인프라 구성
                
                
                3. 기대 효과 및 추진 전략
                작성 시간 80% 절감, 판례 인용 정확도 향상
                단계적 추진: PoC(6개월) → 정식 구축(12개월)
                
                
                4. PoC 구성 요소 및 산출물
                정제된 텍스트 Corpus + Vector DB 구축
                생성 모델 샘플 및 UI 프로토타입
                서버 구성 문서, 기술 가이드, EXE 샘플
                
                
                5. 예산 재산정 (정식 구축)
                인건비: 15~18억 / 라벨링: 5~7억
                AI 인프라: 7~10억 / 보안시스템: 2억
                총합: 약 36.5~46.5억 (최대 50억까지 확장 가능)
                요구 사항의 구체화에 따라 200% 이상 차이 발생
                
                
                6. 투자 시나리오별 예산
                기초 PoC: 5~7억 (기술 검증)
                실사용 베타: 20~25억 (내부 적용 가능 수준)
                정식 구축: 35~50억 (망분리 포함)
                상품화/기업화: 100억 이상 (SaaS 확장)
                요구 사항의 구체화에 따라 200% 이상 차이 발생
                
                
                7. 인력 구성
                AI 리서처 2, NLP 2, 인프라 1, 라벨러 5, 분석가 4 등
                총 20~22명 (12~15개월 기준)
                
                
                8. 오픈소스 모델 비교
                KoGPT (Apache 2.0): 가볍고 추천
                LLaMA2-Ko: 고성능, 비상업적 사용 가능
                BGE-Ko: 판례 검색용 임베딩에 적합
                
                """;
        var result = KeywordExtractor.extractKeywords(input);

        assertNotNull(result);
        List<String> nouns = result.getNouns();
        Map<String, List<String>> regex = result.getRegex();

//        assertTrue(nouns.size() > 0);
        System.out.println(input + ": " + nouns);
        System.out.println(regex);
    }
}
