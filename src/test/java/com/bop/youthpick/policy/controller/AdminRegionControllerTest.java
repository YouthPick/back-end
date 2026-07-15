package com.bop.youthpick.policy.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.policy.dto.RegionResponse;
import com.bop.youthpick.policy.service.AdminRegionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminRegionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminRegionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminRegionService adminRegionService;

    @Test
    void 지역_목록은_200과_전체_목록을_반환한다() throws Exception {
        when(adminRegionService.findAll())
                .thenReturn(List.of(new RegionResponse("11110", "서울특별시", "강남구")));

        mockMvc.perform(get("/api/v1/admin/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].regionCode").value("11110"))
                .andExpect(jsonPath("$.data[0].provinceName").value("서울특별시"))
                .andExpect(jsonPath("$.data[0].districtName").value("강남구"))
                .andExpect(jsonPath("$.meta").doesNotExist());
    }
}
