package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.PolicyApplicationChecklist;
import java.time.LocalDateTime;

/**
 * PolicyApplicationChecklistController의 add()/getByApplication()/update()가 공통으로 쓰는 응답 DTO. 필드명은
 * message지만 엔티티에서 실제로 읽어오는 건 {@link PolicyApplicationChecklist#getContent()}다 — 요청 DTO들과 이름을 맞추려고
 * API 표면에서만 message로 부르는 것뿐, 엔티티/DB 컬럼명(content)과는 다르다.
 */
public record PolicyApplicationChecklistResponse(
        Long id,
        Long policyApplicationId,
        String message,
        boolean checked,
        LocalDateTime createdAt) {

    /**
     * PolicyApplicationChecklistController.add()/update()와
     * PolicyApplicationChecklistService.getByApplication()(Page.map)에서 호출된다.
     */
    public static PolicyApplicationChecklistResponse from(PolicyApplicationChecklist checklist) {
        return new PolicyApplicationChecklistResponse(
                checklist.getId(),
                checklist.getApplication().getId(),
                checklist.getContent(),
                checklist.isChecked(),
                checklist.getCreatedAt());
    }
}
