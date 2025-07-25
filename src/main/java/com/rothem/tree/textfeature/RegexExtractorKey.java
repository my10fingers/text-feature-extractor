package com.rothem.tree.textfeature;

public enum RegexExtractorKey {
    SHORT_DATE_6,
    URL,
    EMAIL,
    DATE,
    PHONE_KR,
    PHONE_INTL,
    ACCOUNT,
    NUMBER;

    public String keyName() {
        return this.name().toLowerCase();
    }
}
