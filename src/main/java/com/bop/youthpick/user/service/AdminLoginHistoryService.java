package com.bop.youthpick.user.service;

import com.bop.youthpick.user.dto.LoginHistoryResponse;
import com.bop.youthpick.user.repository.AdminLoginHistorySpecifications;
import com.bop.youthpick.user.repository.LoginHistoryRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminLoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    @Transactional(readOnly = true)
    public Page<LoginHistoryResponse> search(
            Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return loginHistoryRepository
                .findAll(
                        AdminLoginHistorySpecifications.filter(userId, startDate, endDate),
                        pageable)
                .map(LoginHistoryResponse::from);
    }
}
