/*
 * The MIT License
 *
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.ss.test.ui.glue.mappers;

import com.codeborne.selenide.Condition;
import io.cucumber.java.ParameterType;
import lombok.Getter;

public class ParameterMappers {

    public enum SelenideValidation {
        PRESENT(Condition.visible),
        MISSING(Condition.not(Condition.visible)),
        ENABLED(Condition.enabled),
        DISABLED(Condition.disabled);

        @Getter
        private final Condition selenideCondition;

        SelenideValidation(Condition selenideCondition) {
            this.selenideCondition = selenideCondition;
        }

        public static SelenideValidation fromString(String value) {
            return valueOf(value.trim().toUpperCase());
        }
    }

    public enum SortDir {
        ASC, DESC
    }

    /**
     * Note: Annotation provides autocomplete functionality and validation.
     *
     * @param name string to convert
     */
    @ParameterType("present|missing|enabled|disabled")
    public SelenideValidation selenideValidation(String name) {
        return SelenideValidation.valueOf(name.toUpperCase());
    }

    @ParameterType("checked|unchecked")
    public Boolean checkbox(String name) {
        return "checked".equalsIgnoreCase(name);
    }

    @ParameterType("asc|desc")
    public SortDir sortDir(String name) {
        return SortDir.ASC.name().equalsIgnoreCase(name) ? SortDir.ASC : SortDir.DESC;
    }
}
