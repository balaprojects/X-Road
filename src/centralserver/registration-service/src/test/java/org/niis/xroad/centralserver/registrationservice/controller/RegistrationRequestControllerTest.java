/**
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
package org.niis.xroad.centralserver.registrationservice.controller;

import ee.ria.xroad.common.OcspTestUtils;
import ee.ria.xroad.common.SystemProperties;
import ee.ria.xroad.common.TestCertUtil;
import ee.ria.xroad.common.conf.globalconf.GlobalConf;
import ee.ria.xroad.common.identifier.SecurityServerId;

import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.niis.xroad.centralserver.registrationservice.service.AdminApiService;
import org.niis.xroad.centralserver.registrationservice.testutil.TestAuthCertRegRequest;
import org.niis.xroad.centralserver.registrationservice.testutil.TestAuthRegRequestBuilder;
import org.niis.xroad.centralserver.registrationservice.testutil.TestGlobalConf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RegistrationRequestControllerTest {
    public static final String CONTENT_TYPE = "multipart/related; boundary=partboundary";
    private static KeyPair ownerKeyPair;
    private static KeyPair authKeyPair;

    private static MessageFactory factory;
    private final SecurityServerId.Conf serverId =
            SecurityServerId.Conf.create(GlobalConf.getInstanceIdentifier(), "CLASS", "MEMBER", "SS1");

    @Autowired
    RegistrationRequestController controller;

    @TestConfiguration
    static class TestConfig {
        @Bean
        AdminApiService adminApiServiceImpl() {
            return (serverId, address, certificate) -> 0;
        }
    }

    @BeforeClass
    public static void setup() throws Exception {
        var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        authKeyPair = keyPairGenerator.generateKeyPair();
        ownerKeyPair = keyPairGenerator.generateKeyPair();
        factory = MessageFactory.newInstance();

        System.setProperty(SystemProperties.CONFIGURATION_PATH, "build/resources/test/testconf");
        GlobalConf.reload(new TestGlobalConf());
    }

    @Test
    public void shouldFailIfAuthSignatureIsInvalid() throws Exception {
        var authCert = TestCertUtil.generateAuthCert(authKeyPair.getPublic());
        var ownerCert = TestCertUtil.generateSignCert(ownerKeyPair.getPublic(), serverId.getOwner());

        var ownerOcsp = OcspTestUtils.createOCSPResponse(ownerCert,
                TestCertUtil.getCaCert(),
                TestCertUtil.getOcspSigner().certChain[0],
                TestCertUtil.getOcspSigner().key,
                CertificateStatus.GOOD);

        var builder = new TestAuthRegRequestBuilder(serverId.getOwner(), serverId.getOwner());
        var req = builder.buildAuthCertRegRequest(serverId, "ss1.example.org", authCert);

        var envelope = new TestAuthCertRegRequest(authCert,
                ownerCert.getEncoded(),
                ownerOcsp.getEncoded(),
                req,
                ownerKeyPair.getPrivate(),
                ownerKeyPair.getPrivate());

        var is = envelope.getRequestContent();
        var result = controller.register(envelope.getRequestContentType(), is);

        assertTrue(result.getStatusCode().is5xxServerError());
        assertFault(result.getBody(), "InvalidSignatureValue");
    }

    @Test
    public void shouldFailIfOwnerSignatureIsInvalid() throws Exception {
        var authCert = TestCertUtil.generateAuthCert(authKeyPair.getPublic());
        var ownerCert = TestCertUtil.generateSignCert(ownerKeyPair.getPublic(), serverId.getOwner());

        var ownerOcsp = OcspTestUtils.createOCSPResponse(ownerCert,
                TestCertUtil.getCaCert(),
                TestCertUtil.getOcspSigner().certChain[0],
                TestCertUtil.getOcspSigner().key,
                CertificateStatus.GOOD);

        var builder = new TestAuthRegRequestBuilder(serverId.getOwner(), serverId.getOwner());
        var req = builder.buildAuthCertRegRequest(serverId, "ss1.example.org", authCert);

        var envelope = new TestAuthCertRegRequest(authCert,
                ownerCert.getEncoded(),
                ownerOcsp.getEncoded(),
                req,
                authKeyPair.getPrivate(),
                authKeyPair.getPrivate());

        var is = envelope.getRequestContent();
        var result = controller.register(envelope.getRequestContentType(), is);

        assertTrue(result.getStatusCode().is5xxServerError());
        assertFault(result.getBody(), "InvalidSignatureValue");
    }

    @Test
    public void shouldFailIfOwnerCertIsRevoked() throws Exception {
        var authCert = TestCertUtil.generateAuthCert(authKeyPair.getPublic());
        var ownerCert = TestCertUtil.generateSignCert(ownerKeyPair.getPublic(), serverId.getOwner());

        var ownerOcsp = OcspTestUtils.createOCSPResponse(ownerCert,
                TestCertUtil.getCaCert(),
                TestCertUtil.getOcspSigner().certChain[0],
                TestCertUtil.getOcspSigner().key,
                new RevokedStatus(Date.from(Instant.now().minusSeconds(3600)), CRLReason.unspecified));

        var builder = new TestAuthRegRequestBuilder(serverId.getOwner(), serverId.getOwner());
        var req = builder.buildAuthCertRegRequest(serverId, "ss1.example.org", authCert);

        var envelope = new TestAuthCertRegRequest(authCert,
                ownerCert.getEncoded(),
                ownerOcsp.getEncoded(),
                req,
                authKeyPair.getPrivate(),
                authKeyPair.getPrivate());

        var is = envelope.getRequestContent();
        var result = controller.register(envelope.getRequestContentType(), is);

        assertTrue(result.getStatusCode().is5xxServerError());
        assertFault(result.getBody(), "CertValidation");
    }

    @Test
    public void shouldFailIfAuthCertIsInvalid() throws Exception {
        var authCert = TestCertUtil.generateAuthCert(authKeyPair.getPublic());
        var ownerCert = TestCertUtil.generateSignCert(ownerKeyPair.getPublic(), serverId.getOwner());

        var ownerOcsp = OcspTestUtils.createOCSPResponse(ownerCert,
                TestCertUtil.getCaCert(),
                TestCertUtil.getOcspSigner().certChain[0],
                TestCertUtil.getOcspSigner().key,
                CertificateStatus.GOOD);

        var builder = new TestAuthRegRequestBuilder(serverId.getOwner(), GlobalConf.getManagementRequestService());
        var req = builder.buildAuthCertRegRequest(serverId, "ss1.example.org", new byte[authCert.length]);

        var envelope = new TestAuthCertRegRequest(authCert,
                ownerCert.getEncoded(),
                ownerOcsp.getEncoded(),
                req,
                authKeyPair.getPrivate(),
                ownerKeyPair.getPrivate());

        var is = envelope.getRequestContent();
        var result = controller.register(envelope.getRequestContentType(), is);

        assertTrue(result.getStatusCode().is5xxServerError());
        assertFault(result.getBody(), "CertValidation");
    }

    @Test
    public void shouldFailIfEmptyRequest() throws SOAPException, IOException {
        var result = controller.register(CONTENT_TYPE, new ByteArrayInputStream(new byte[0]));
        assertTrue(result.getStatusCode().is5xxServerError());
        assertFault(result.getBody(), "InvalidRequest");
    }

    @Test
    public void shouldFailIfWrongInstanceId() throws Exception {
        var sid = SecurityServerId.Conf.create(GlobalConf.getInstanceIdentifier() + "-X", "CLASS", "MEMBER", "SS1");
        var result = register(sid, "ss1.example.org");
        assertTrue(result.getStatusCode().is5xxServerError());
        assertFault(result.getBody(), "InvalidRequest");
    }

    @Test
    public void shouldFailIfInvalidServerId() throws Exception {
        var sid = SecurityServerId.Conf.create(GlobalConf.getInstanceIdentifier(), "CLASS", "MEM BER", "S:;S1");
        var result = register(sid, "ss1.example.org");
        assertTrue(result.getStatusCode().is5xxServerError());
        assertFault(result.getBody(), "InvalidClientIdentifier");
    }

    @Test
    public void shouldFailIfInvalidServerAddress() throws Exception {
        var result = register(serverId, String.format("%s.invalid", "a".repeat(64)));
        assertTrue(result.getStatusCode().is5xxServerError());
        assertFault(result.getBody(), "InvalidRequest");
    }

    @Test
    public void shouldFailIfInvalidCertificate() throws Exception {
        var result = registerWithInvalidCerts(serverId);
        assertTrue(result.getStatusCode().is5xxServerError());
        assertFault(result.getBody(), "IncorrectCertificate");
    }

    private ResponseEntity<String> register(SecurityServerId.Conf sid, String address) throws Exception {
        var authCert = TestCertUtil.generateAuthCert(authKeyPair.getPublic());
        var ownerCert = TestCertUtil.generateSignCert(ownerKeyPair.getPublic(), sid.getOwner());
        var ownerOcsp = OcspTestUtils.createOCSPResponse(ownerCert,
                TestCertUtil.getCaCert(),
                TestCertUtil.getOcspSigner().certChain[0],
                TestCertUtil.getOcspSigner().key,
                CertificateStatus.GOOD);

        var builder = new TestAuthRegRequestBuilder(sid.getOwner(), GlobalConf.getManagementRequestService());
        var req = builder.buildAuthCertRegRequest(sid, address, authCert);

        var envelope = new TestAuthCertRegRequest(authCert,
                ownerCert.getEncoded(),
                ownerOcsp.getEncoded(),
                req,
                authKeyPair.getPrivate(),
                ownerKeyPair.getPrivate());

        var is = envelope.getRequestContent();
        return controller.register(envelope.getRequestContentType(), is);
    }

    private ResponseEntity<String> registerWithInvalidCerts(SecurityServerId.Conf sid)
            throws Exception {

        var mockData = new byte[1024];
        new Random().nextBytes(mockData);

        var builder = new TestAuthRegRequestBuilder(sid.getOwner(), GlobalConf.getManagementRequestService());
        var req = builder.buildAuthCertRegRequest(sid, "ss1.example.org", mockData);

        var envelope = new TestAuthCertRegRequest(mockData,
                mockData,
                mockData,
                req,
                authKeyPair.getPrivate(),
                ownerKeyPair.getPrivate());

        var is = envelope.getRequestContent();
        return controller.register(envelope.getRequestContentType(), is);
    }

    private static void assertFault(String message, String code) throws SOAPException, IOException {
        var msg = factory.createMessage(null, new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)));
        assertEquals(code, msg.getSOAPBody().getFault().getFaultCode());
    }
}
