package com.hansung.tracktory.domain.recommendation.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectSemester;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.profile.dto.CompletedSubjectAddRequest;
import com.hansung.tracktory.domain.profile.service.CompletedSubjectService;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationTriggerSource;
import com.hansung.tracktory.domain.recommendation.repository.RecommendationRepository;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이수 과목 변경 → 추천 무효화 이벤트 배선 통합 테스트.
 *
 * <p>단위 테스트는 발행과 수신을 분리 검증하므로, 실제 Spring 이벤트 배선(서비스 발행 → 리스너 수신 → 같은 트랜잭션에서 DB 반영)은 본 테스트로만 보장된다.
 * 실제 DB 를 사용하므로 {@code @Tag("integration")} 으로 분리한다.
 */
@SpringBootTest
@Transactional
@Tag("integration")
class CompletedSubjectInvalidationIntegrationTest {

  @Autowired private CompletedSubjectService completedSubjectService;
  @Autowired private UserRepository userRepository;
  @Autowired private SubjectRepository subjectRepository;
  @Autowired private RecommendationRepository recommendationRepository;

  @Test
  void addingCompletedSubject_supersedesActiveRecommendation() {
    User user =
        userRepository.save(
            User.builder().email("inv-test@hansung.ac.kr").passwordHash("x").build());
    subjectRepository.save(
        Subject.builder()
            .code("INV0001")
            .name("무효화통합테스트과목")
            .description("통합 테스트용 합성 과목")
            .credit(new BigDecimal("3.0"))
            .semester(SubjectSemester.FIRST)
            .build());
    Recommendation active =
        recommendationRepository.save(
            Recommendation.builder()
                .user(user)
                .status(RecommendationStatus.ACTIVE)
                .triggerSource(RecommendationTriggerSource.MANUAL)
                .build());

    completedSubjectService.add(user.getId(), addRequest("무효화통합테스트과목", 1, 1));

    Recommendation reloaded = recommendationRepository.findById(active.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(RecommendationStatus.SUPERSEDED);
  }

  private static CompletedSubjectAddRequest addRequest(
      String subjectName, Integer year, Integer semester) {
    CompletedSubjectAddRequest request = new CompletedSubjectAddRequest();
    ReflectionTestUtils.setField(request, "subjectName", subjectName);
    ReflectionTestUtils.setField(request, "year", year);
    ReflectionTestUtils.setField(request, "semester", semester);
    return request;
  }
}
