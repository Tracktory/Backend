package com.hansung.tracktory.domain.profile.entity;

import com.hansung.tracktory.domain.catalog.classification.entity.CompanyType;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자 ↔ 희망 회사 유형. */
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "user_company_type",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "company_type_id"}))
public class UserCompanyType extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_type_id", nullable = false)
  private CompanyType companyType;

  // ------------------------------ 메서드 ------------------------------
  public static UserCompanyType of(User user, CompanyType companyType) {
    return UserCompanyType.builder().user(user).companyType(companyType).build();
  }
}
