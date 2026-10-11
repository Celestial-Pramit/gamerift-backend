package com.gamerift.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JsonTextTest {

    @Test
    void escapesQuotesBackslashesAndNewLines() {
        assertEquals("a\\\"b\\\\c\\nd", JsonText.escape("a\"b\\c\nd"));
    }

    @Test
    void nullBecomesEmptyText() {
        assertEquals("", JsonText.escape(null));
    }
}
