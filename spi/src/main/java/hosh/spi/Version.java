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

/**
 * Hosh version as immutable and safe value.
 */
public class Version {

    private final String value;

    public Version(String candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("null version");
        }
        if (candidate.isBlank()) {
            throw new IllegalArgumentException("empty version");
        }
        final String[] split = candidate.split("\\.");
        if (!split[0].startsWith("v")) {
            throw new IllegalArgumentException("missing prefix: " + candidate);
        }
        if (split.length != 3) { // expecting to follow our maven conventions of major.minor.patch
            throw new IllegalArgumentException("invalid maven version: " + candidate);
        }
        // at this point candidate is fully validated
        this.value = candidate;
    }

    /**
     * A textual representation that be safely printed to screen.
     */
    public Value hoshVersion() {
        return new Values.TextValue("hosh " + value);
    }
}
