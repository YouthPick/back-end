package com.bop.youthpick.user.entity;

import com.bop.youthpick.global.entity.BaseEntity;
import com.bop.youthpick.policy.entity.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 온보딩 프로필 (users와 1:1). 맞춤정책 점수계산(REC 6축)의 입력값. */
@Entity
@Table(
        name = "user_profiles",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_user_profiles_user", columnNames = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "birth_year", nullable = false)
    private Integer birthYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_code", nullable = false)
    private Region region;

    @Column(name = "employment_status", length = 16, nullable = false)
    private String employmentStatus;

    @Column(name = "education_level", length = 16, nullable = false)
    private String educationLevel;

    @Column(name = "marital_status", length = 16)
    private String maritalStatus;

    @Column(length = 255)
    private String major;

    @Column(name = "special_condition", length = 500)
    private String specialCondition;

    @Column private Integer income;

    @Column(length = 500)
    private String categories;

    @Column(length = 700)
    private String keywords;

    @Column(length = 20, nullable = false)
    private String status = "COMPLETED";

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static UserProfile create(
            User user,
            Region region,
            Integer birthYear,
            String employmentStatus,
            String educationLevel,
            String maritalStatus,
            String major,
            String specialCondition,
            Integer income,
            String categories,
            String keywords) {
        UserProfile profile = new UserProfile();
        profile.user = user;
        profile.region = region;
        profile.birthYear = birthYear;
        profile.employmentStatus = employmentStatus;
        profile.educationLevel = educationLevel;
        profile.maritalStatus = maritalStatus;
        profile.major = major;
        profile.specialCondition = specialCondition;
        profile.income = income;
        profile.categories = categories;
        profile.keywords = keywords;
        return profile;
    }

    public void update(
            Region region,
            Integer birthYear,
            String employmentStatus,
            String educationLevel,
            String maritalStatus,
            String major,
            String specialCondition,
            Integer income,
            String categories,
            String keywords) {
        this.region = region;
        this.birthYear = birthYear;
        this.employmentStatus = employmentStatus;
        this.educationLevel = educationLevel;
        this.maritalStatus = maritalStatus;
        this.major = major;
        this.specialCondition = specialCondition;
        this.income = income;
        this.categories = categories;
        this.keywords = keywords;
    }
}
