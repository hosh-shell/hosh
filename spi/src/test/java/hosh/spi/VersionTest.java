/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Davide Angelocola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package hosh.spi;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import static org.assertj.core.api.Assertions.*;

class VersionTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "v1.1.0", // git describe with tag
            "v0.1.14", // git describe with tag
            "v0.1.14-89-ge38fe101", // git describe without tag
            "v0.1.14-89-ge38fe101-dirty", // git describe without tag and with uncommitted changes
    })
    void validVersion(String candidate) {
        Version result = new Version(candidate);

        assertThat(result.hoshVersion())
                .isEqualTo(Values.ofText("hosh " + candidate))
        ;
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "1.0.1",    // missing starting "v" and too
            "0.1.14",   // missing starting "v"
            "v1",       // too few components
            "v1.2",     // too few components
            "v1.2.3.4", // too many components
    })
    void invalidVersion(String candidate) {
        var result = assertThatThrownBy(() -> new Version(candidate));

        result.isInstanceOf(IllegalArgumentException.class);
    }

}