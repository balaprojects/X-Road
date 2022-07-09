/**
 * The MIT License
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
package ee.ria.xroad.proxy.util;

import ee.ria.xroad.common.identifier.CentralServiceId;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.SecurityServerId;
import ee.ria.xroad.common.identifier.ServiceId;
import ee.ria.xroad.common.identifier.XRoadId;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Test to verify MessageProcessorBase behavior
 */
public class IdentifierValidatorTest {

    @Test
    public void testCheckIdentifierValid() {
        final ServiceId valid = ServiceId.Conf.create("TEST", "CLASS", "CO DE", null, "SERVICE");
        assertTrue(MessageProcessorBase.checkIdentifier(valid));
    }

    @Test
    public void testCheckIdentifierInvalid() {
        final XRoadId[] cases = {
                ClientId.Conf.create("TEST", "CLASS", "CO\tDE"),
                SecurityServerId.Conf.create("TEST", "CLASS", "MEMBER", "SER:VER"),
                ServiceId.Conf.create("TE/ST", "CLASS", "MEMBER", "SYSTEM", "SERVICE"),
                ServiceId.Conf.create("TEST", "CLA;SS", "MEMBER", "SYSTEM", "SERVICE"),
                ServiceId.Conf.create("TEST", "CLASS", "MEM\\BER", "SYSTEM", "SERVICE"),
                ServiceId.Conf.create("TEST", "CLASS", "MEMBER", "SYS%TEM", "SERVICE"),
                ServiceId.Conf.create("TEST", "CLASS", "MEMBER", "SYSTEM", "SERVICE\u200b"),
                CentralServiceId.Conf.create("TEST", "SERVICE;"),
                CentralServiceId.Conf.create("TE\ufeffST", "SERVICE")
        };
        for (XRoadId id : cases) {
            assertFalse(MessageProcessorBase.checkIdentifier(id));
        }

    }

}
