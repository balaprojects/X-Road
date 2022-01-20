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
package org.niis.xroad.centralserver.restapi.openapi;

import lombok.RequiredArgsConstructor;
import org.niis.xroad.centralserver.openapi.MemberClassesApi;
import org.niis.xroad.centralserver.openapi.model.MemberClass;
import org.niis.xroad.centralserver.restapi.dto.MemberClassDto;
import org.niis.xroad.centralserver.restapi.service.MemberClassService;
import org.niis.xroad.restapi.config.audit.AuditDataHelper;
import org.niis.xroad.restapi.config.audit.AuditEventMethod;
import org.niis.xroad.restapi.config.audit.RestApiAuditEvent;
import org.niis.xroad.restapi.config.audit.RestApiAuditProperty;
import org.niis.xroad.restapi.openapi.ControllerUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping(ControllerUtil.API_V1_PREFIX)
@PreAuthorize("hasAuthority('VIEW_MEMBER_CLASSES')")
@RequiredArgsConstructor
public class MemberClassController implements MemberClassesApi {

    private final MemberClassService service;
    private final AuditDataHelper auditData;

    @Override
    @PreAuthorize("hasAuthority('ADD_MEMBER_CLASS')")
    @AuditEventMethod(event = RestApiAuditEvent.ADD_MEMBER_CLASS)
    public ResponseEntity<MemberClass> addMemberClass(MemberClass memberClass) {
        auditData.put(RestApiAuditProperty.MEMBER_CLASS_CODE, memberClass.getCode());
        var result = service.add(new MemberClassDto(memberClass.getCode(), memberClass.getDescription()));
        return ResponseEntity.ok(convert(result));
    }

    @Override
    @PreAuthorize("hasAuthority('DELETE_MEMBER_CLASS')")
    @AuditEventMethod(event = RestApiAuditEvent.DELETE_MEMBER_CLASS)
    public ResponseEntity<Void> deleteMemberClass(String code) {
        auditData.put(RestApiAuditProperty.MEMBER_CLASS_CODE, code);
        service.delete(code);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_MEMBER_CLASSES')")
    public ResponseEntity<Set<MemberClass>> getMemberClasses() {
        var memberClasses = service.list();
        return ResponseEntity.ok(convert(memberClasses));
    }

    @Override
    @PreAuthorize("hasAuthority('EDIT_MEMBER_CLASS')")
    @AuditEventMethod(event = RestApiAuditEvent.EDIT_MEMBER_CLASS)
    public ResponseEntity<MemberClass> updateMemberClassDescription(String code, MemberClass memberClass) {
        auditData.put(RestApiAuditProperty.MEMBER_CLASS_CODE, code);
        var result = service.update(new MemberClassDto(code, memberClass.getDescription()));
        return ResponseEntity.ok(convert(result));
    }

    private static MemberClass convert(MemberClassDto dto) {
        var result = new MemberClass();
        result.setCode(dto.getCode());
        result.setDescription(dto.getDescription());
        return result;
    }

    private static Set<MemberClass> convert(Collection<MemberClassDto> dtos) {
        var result = new HashSet<MemberClass>(dtos.size());
        for (var dto : dtos) {
            result.add(convert(dto));
        }
        return result;
    }
}
