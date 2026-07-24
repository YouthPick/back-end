package com.bop.youthpick.user.entity;

import com.bop.youthpick.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 서비스 회원 (소셜 로그인 전용). 식별은 (provider, providerId) — email은 카카오 미제공/미보유 가능이라 NULL 허용. */
@Entity
@Table(
        name = "users",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_users_provider",
                        columnNames = {"provider", "provider_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false)
    private String provider;

    @Column(name = "provider_id", length = 255, nullable = false)
    private String providerId;

    @Column(length = 255)
    private String email;

    @Column(length = 100)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role = Role.USER;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static User createSocialUser(
            String provider, String providerId, String email, String nickname) {
        User user = new User();
        user.provider = provider;
        user.providerId = providerId;
        user.email = email;
        user.nickname = nickname;
        return user;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
