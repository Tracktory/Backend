package com.hansung.tracktory.domain.profile.event;

/**
 * 사용자의 이수 과목이 추가·삭제되어 학습 이력이 바뀌었음을 알리는 도메인 이벤트.
 *
 * <p>발행 트랜잭션 안에서 동기로 처리되어, 직전에 저장된 추천을 무효화하는 데 쓰인다. 프로필 도메인이 추천 도메인을 직접 의존하지 않도록 이벤트로 분리한다.
 *
 * @param userId 학습 이력이 바뀐 사용자 식별자
 */
public record CompletedSubjectsChangedEvent(Long userId) {}
