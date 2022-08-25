/**
 * The MIT License
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.centralserver.restapi.openapi;

import org.junit.jupiter.api.Test;
import org.niis.xroad.centralserver.openapi.model.GlobalGroupCodeAndDescriptionDto;
import org.niis.xroad.centralserver.openapi.model.GlobalGroupResourceDto;
import org.niis.xroad.centralserver.restapi.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

class GlobalGroupApiTest extends AbstractApiRestTemplateTestContext {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void addGlobalGroup() {
        TestUtils.addApiKeyAuthorizationHeader(restTemplate);
        var entity = prepareAddGlobalGroupRequest("code");

        ResponseEntity<GlobalGroupResourceDto> response =
                restTemplate.postForEntity("/api/v1/global-groups", entity, GlobalGroupResourceDto.class);
        assertNotNull(response);
        assertEquals(CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertAddedGlobalGroup(response.getBody());
    }

    @Test
    void addGlobalGroupWhenGlobalGroupExists() {
        TestUtils.addApiKeyAuthorizationHeader(restTemplate);
        var entity = prepareAddGlobalGroupRequest("CODE_1");

        ResponseEntity<GlobalGroupResourceDto> response =
                restTemplate.postForEntity("/api/v1/global-groups", entity, GlobalGroupResourceDto.class);
        assertNotNull(response);
        assertEquals(CONFLICT, response.getStatusCode());
    }

    @Test
    void findGlobalGroups() {
        TestUtils.addApiKeyAuthorizationHeader(restTemplate);
        ResponseEntity<GlobalGroupResourceDto[]> response = restTemplate.getForEntity(
                "/api/v1/global-groups",
                GlobalGroupResourceDto[].class);
        assertNotNull(response);
        assertEquals(OK, response.getStatusCode());
        assertThat(Objects.requireNonNull(response.getBody()).length).isGreaterThanOrEqualTo(1);
        GlobalGroupResourceDto expectedGroup = Arrays.stream(response.getBody())
                .filter(ent -> 1000001 == ent.getId())
                .findFirst()
                .orElse(null);
        assertNotNull(expectedGroup);
        assertGlobalGroup(expectedGroup);
    }

    @Test
    void findGlobalGroupsWithContainsMember() {
        TestUtils.addApiKeyAuthorizationHeader(restTemplate);
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("containsMember", "TEST:GOV:M1:SS1");
        ResponseEntity<GlobalGroupResourceDto[]> response = restTemplate.getForEntity(
                "/api/v1/global-groups?contains_member={containsMember}",
                GlobalGroupResourceDto[].class,
                uriVariables);
        assertNotNull(response);
        assertEquals(OK, response.getStatusCode());
        assertEquals(1, Objects.requireNonNull(response.getBody()).length);
        assertGlobalGroup(response.getBody()[0]);
    }

    @Test
    void getGlobalGroups() {
        TestUtils.addApiKeyAuthorizationHeader(restTemplate);
        ResponseEntity<GlobalGroupResourceDto> response = restTemplate.getForEntity(
                "/api/v1/global-groups/1000001",
                GlobalGroupResourceDto.class);
        assertNotNull(response.getBody());
        assertEquals(OK, response.getStatusCode());
        assertGlobalGroup(response.getBody());
    }

    @Test
    void deleteGlobalGroup() {
        TestUtils.addApiKeyAuthorizationHeader(restTemplate);
        ResponseEntity<GlobalGroupResourceDto> existingGlobalGroup =
                restTemplate.getForEntity("/api/v1/global-groups/1000002", GlobalGroupResourceDto.class);
        assertNotNull(existingGlobalGroup.getBody());
        assertEquals(OK, existingGlobalGroup.getStatusCode());
        assertEquals(1000002, existingGlobalGroup.getBody().getId());

        restTemplate.delete("/api/v1/global-groups/1000002");

        ResponseEntity<GlobalGroupResourceDto> deleteGlobalGroup =
                restTemplate.getForEntity("/api/v1/global-groups/1000002", GlobalGroupResourceDto.class);

        assertNotNull(deleteGlobalGroup.getBody());
        assertEquals(NOT_FOUND, deleteGlobalGroup.getStatusCode());
    }

    @Test
    void updateGlobalGroupDescription() {
        TestUtils.addApiKeyAuthorizationHeader(restTemplate);
        GlobalGroupResourceDto updatedGlobalGroup = restTemplate.patchForObject("/api/v1/global-groups/1000002",
                Collections.singletonMap("description", "New description"), GlobalGroupResourceDto.class);
        assertEquals("New description", updatedGlobalGroup.getDescription());
    }

    private HttpEntity<GlobalGroupCodeAndDescriptionDto> prepareAddGlobalGroupRequest(String code) {
        GlobalGroupCodeAndDescriptionDto request = new GlobalGroupCodeAndDescriptionDto();
        request.code(code);
        request.description("description");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(request, headers);
    }

    private void assertGlobalGroup(GlobalGroupResourceDto globalGroup) {
        assertEquals(1000001, globalGroup.getId());
        assertEquals("CODE_1", globalGroup.getCode());
        assertEquals("First global group", globalGroup.getDescription());
        assertEquals(1, globalGroup.getMemberCount());
        assertEquals(1, globalGroup.getMembers().size());
        assertEquals("1000001", globalGroup.getMembers().stream().iterator().next().getId());
        assertEquals("TEST:GOV:M1:SS1", globalGroup.getMembers().stream().iterator().next().getName());
        assertNotNull(globalGroup.getMembers().stream().iterator().next().getCreatedAt());
        assertNotNull(globalGroup.getCreatedAt());
        assertNotNull(globalGroup.getUpdatedAt());
    }

    private void assertAddedGlobalGroup(GlobalGroupResourceDto globalGroup) {
        assertNotNull(globalGroup.getId());
        assertEquals("code", globalGroup.getCode());
        assertEquals("description", globalGroup.getDescription());
        assertEquals(0, globalGroup.getMemberCount());
        assertEquals(0, globalGroup.getMembers().size());
        assertNotNull(globalGroup.getCreatedAt());
        assertNotNull(globalGroup.getUpdatedAt());
    }
}
