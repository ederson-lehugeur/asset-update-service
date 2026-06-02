package com.invest.domain.entities.enumerator;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum IndicatorType {
    PRICE("PRICE"),
    DIVIDEND_YIELD("DIVIDEND_YIELD"),
    PVP("PVP"),
    PL("PL"),
    ROE("ROE");

    private final String code;

    private static final Map<String, IndicatorType> CODE_MAP =
            Arrays.stream(values())
                  .collect(Collectors.toMap(IndicatorType::code, Function.identity()));

    IndicatorType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<IndicatorType> fromCode(String code) {
        return Optional.ofNullable(CODE_MAP.get(code));
    }
}
