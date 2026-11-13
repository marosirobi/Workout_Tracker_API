package com.marosirobi.workouttrackerapi.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LogUtilsTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(nullValues = "null",quoteCharacter = '\'', textBlock = """
            user@gmail.com,   us***@gmail.com
            ab@x.hu,          a***@x.hu
            ' user@gmail.com ', us***@gmail.com
            no-at-sign,       ***
            @gmail.com,       ***
            null,             -
            """)
    void maskEmail(String input, String expected) {
        assertThat(LogUtils.maskEmail(input)).isEqualTo(expected);
    }

    @Test
    void maskEmail_removesLineBreaks() {
        assertThat(LogUtils.maskEmail("us\ner@gmail.com")).isEqualTo("us***@gmail.com");
    }
}
