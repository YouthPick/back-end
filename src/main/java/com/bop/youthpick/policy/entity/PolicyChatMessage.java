package com.bop.youthpick.policy.entity;

import com.bop.youthpick.global.entity.BaseEntity;
import com.bop.youthpick.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "policy_chat_messages",
        indexes = {
            @Index(name = "idx_policy_chat_messages_policy_cursor", columnList = "policy_id, id"),
            @Index(name = "idx_policy_chat_messages_user", columnList = "user_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 1000, nullable = false)
    private String content;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static PolicyChatMessage create(Policy policy, User user, String content) {
        PolicyChatMessage message = new PolicyChatMessage();
        message.policy = policy;
        message.user = user;
        message.content = content;
        return message;
    }
}
