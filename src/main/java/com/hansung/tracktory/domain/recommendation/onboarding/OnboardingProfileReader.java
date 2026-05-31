package com.hansung.tracktory.domain.recommendation.onboarding;

import java.util.Optional;

/**
 * 추천 생성을 위해 사용자의 온보딩 프로필을 조회하는 포트.
 *
 * <p>온보딩 영속 도메인이 준비되기 전까지 추천 도메인은 본 포트로만 온보딩 데이터를 읽는다. 온보딩 정보가 없으면 {@link Optional#empty()} 를 반환하며,
 * 호출 측은 이를 온보딩 미완료로 처리한다.
 */
public interface OnboardingProfileReader {

  Optional<OnboardingProfileSnapshot> read(Long userId);
}
