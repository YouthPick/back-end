package com.bop.youthpick.admin.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bop.youthpick.user.entity.LoginHistory;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.LoginHistoryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminLoginHistoryServiceTest {

    @Mock private LoginHistoryRepository loginHistoryRepository;

    private AdminLoginHistoryService adminLoginHistoryService;

    @BeforeEach
    void setUp() {
        adminLoginHistoryService = new AdminLoginHistoryService(loginHistoryRepository);
    }

    @Test
    void 목록_조회는_필터를_Specification으로_넘겨_페이지를_반환한다() {
        LoginHistory history = mock(LoginHistory.class);
        User user = mock(User.class);
        when(history.getId()).thenReturn(1L);
        when(history.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(10L);
        Page<LoginHistory> page = new PageImpl<>(List.of(history));
        when(loginHistoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<?> result = adminLoginHistoryService.search(10L, null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
    }
}
