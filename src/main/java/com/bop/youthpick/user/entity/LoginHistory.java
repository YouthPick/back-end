package com.bop.youthpick.user.entity;

import com.bop.youthpick.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** OAuth 로그인 성공 이력. 관리자 조회 전용 — 실무적으로 불변 로그이며 갱신되지 않는다. */
@Entity
@Table(
        name = "login_histories",
        indexes = {
            @Index(name = "idx_login_histories_user", columnList = "user_id"),
            @Index(name = "idx_login_histories_created", columnList = "created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static LoginHistory create(User user) {
        LoginHistory history = new LoginHistory();
        history.user = user;
        return history;
    }
}
