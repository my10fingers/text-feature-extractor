package com.rothem.tree.textfeature.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KeywordExtractionResult {
    private List<String> nouns;
    private Map<String, List<String>> regex;
}
