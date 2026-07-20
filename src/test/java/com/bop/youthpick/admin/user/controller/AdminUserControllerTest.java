package com.bop.youthpick.admin.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.admin.user.dto.AdminUserProfileResponse;
import com.bop.youthpick.admin.user.dto.AdminUserResponse;
import com.bop.youthpick.admin.user.service.AdminUserService;
import com.bop.youthpick.user.entity.Role;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminUserService adminUserService;

    private static final AdminUserResponse USER_RESPONSE =
            new AdminUserResponse(1L, "kakao", "kakao-1", Role.USER, LocalDateTime.now(), null);

    @Test
    void 목록_조회는_200과_페이지_데이터를_반환한다() throws Exception {
        Page<AdminUserResponse> page = new PageImpl<>(List.of(USER_RESPONSE));
        when(adminUserService.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].provider").value("kakao"))
                .andExpect(jsonPath("$.meta.totalCount").value(1));
    }

    @Test
    void role_필터가_허용값이_아니면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").param("role", "SUPERADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 프로필이_있으면_200과_프로필을_반환한다() throws Exception {
        AdminUserProfileResponse profile =
                new AdminUserProfileResponse(
                        1L,
                        2000,
                        "EMPLOYED",
                        "UNIVERSITY",
                        "SINGLE",
                        List.of("COMPUTER_SCIENCE"),
                        List.of("LOW_INCOME"),
                        3000,
                        List.of("취업"),
                        List.of("청년"),
                        "COMPLETED",
                        "서울특별시 강남구");
        when(adminUserService.getProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/admin/users/{userId}/profile", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.merryStatus").value("SINGLE"))
                .andExpect(jsonPath("$.data.major[0]").value("COMPUTER_SCIENCE"))
                .andExpect(jsonPath("$.data.specialCondition[0]").value("LOW_INCOME"))
                .andExpect(jsonPath("$.data.income").value(3000))
                .andExpect(jsonPath("$.data.regionLabel").value("서울특별시 강남구"));
    }

    @Test
    void 프로필이_없으면_200과_null_데이터를_반환한다() throws Exception {
        when(adminUserService.getProfile(1L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/admin/users/{userId}/profile", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void role_변경이_유효하면_200과_변경된_사용자를_반환한다() throws Exception {
        AdminUserResponse updated =
                new AdminUserResponse(
                        1L, "kakao", "kakao-1", Role.ADMIN, LocalDateTime.now(), null);
        when(adminUserService.updateRole(eq(1L), eq("ADMIN"))).thenReturn(updated);

        mockMvc.perform(
                        patch("/api/v1/admin/users/{userId}/role", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void role_변경_대상이_없으면_404와_U001을_반환한다() throws Exception {
        when(adminUserService.updateRole(eq(1L), anyString()))
                .thenThrow(new UserException(UserError.USER_NOT_FOUND));

        mockMvc.perform(
                        patch("/api/v1/admin/users/{userId}/role", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("U001"));
    }

    @Test
    void role_값이_허용범위가_아니면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/admin/users/{userId}/role", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"SUPERADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 탈퇴_처리하면_200과_deletedAt이_채워진_사용자를_반환한다() throws Exception {
        AdminUserResponse deleted =
                new AdminUserResponse(
                        1L,
                        "kakao",
                        "kakao-1",
                        Role.USER,
                        LocalDateTime.now(),
                        LocalDateTime.now());
        when(adminUserService.softDelete(1L)).thenReturn(deleted);

        mockMvc.perform(delete("/api/v1/admin/users/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedAt").exists());
    }

    @Test
    void 탈퇴_대상이_없으면_404와_U001을_반환한다() throws Exception {
        when(adminUserService.softDelete(1L))
                .thenThrow(new UserException(UserError.USER_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/admin/users/{userId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("U001"));
    }
}
