package net.timzh;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhotonPawTests {

    @ParameterizedTest
    @CsvSource({
            "1, 1"
    })
    void test(int i, int j) {
        assertEquals(i, j);
    }
}
