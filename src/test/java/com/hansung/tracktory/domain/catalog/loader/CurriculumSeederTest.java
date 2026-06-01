package com.hansung.tracktory.domain.catalog.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.hansung.tracktory.domain.catalog.curriculum.entity.PrerequisiteStrength;
import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectPrerequisite;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectSemester;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectStage;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectType;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectPrerequisiteRepository;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.catalog.curriculum.repository.TrackSubjectRepository;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.CourseRow;
import com.hansung.tracktory.domain.catalog.organization.repository.TrackRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 선수 관계 적재(2차 패스)의 동작·멱등·미해결 코드 처리를 검증한다. Spring 컨텍스트·DB 없이 Mockito 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class CurriculumSeederTest {

  @Mock private SubjectRepository subjectRepository;
  @Mock private TrackSubjectRepository trackSubjectRepository;
  @Mock private TrackRepository trackRepository;
  @Mock private SubjectPrerequisiteRepository subjectPrerequisiteRepository;

  private CurriculumSeeder seeder;

  /** code → Subject 인메모리 저장소. findByCode/save 를 실제 적재처럼 흉내내 2차 패스의 선수 해석을 검증한다. */
  private final Map<String, Subject> subjectStore = new HashMap<>();

  @BeforeEach
  void setUp() {
    seeder =
        new CurriculumSeeder(
            subjectRepository,
            trackSubjectRepository,
            trackRepository,
            subjectPrerequisiteRepository);
    subjectStore.clear();
    given(subjectRepository.findByCode(any()))
        .willAnswer(inv -> Optional.ofNullable(subjectStore.get(inv.getArgument(0))));
    given(subjectRepository.save(any(Subject.class)))
        .willAnswer(
            inv -> {
              Subject s = inv.getArgument(0);
              subjectStore.put(s.getCode(), s);
              return s;
            });
  }

  private CourseRow course(String code, List<String> prereqCodes) {
    // 트랙 연결은 본 테스트 관심 밖이라 빈 목록 — 1차 패스가 Subject 만 만들도록 한다.
    return new CourseRow(
        code,
        code + "과목",
        new BigDecimal("3.0"),
        SubjectSemester.BOTH,
        SubjectType.ELECTIVE,
        SubjectStage.CORE,
        List.of(),
        prereqCodes);
  }

  @Test
  void seed_선수코드가있으면_선수관계_REQUIRED로_생성() {
    List<CourseRow> courses =
        List.of(course("A", List.of("B", "C")), course("B", List.of()), course("C", List.of()));
    given(subjectPrerequisiteRepository.existsBySubjectAndPrerequisite(any(), any()))
        .willReturn(false);

    seeder.seed(courses);

    ArgumentCaptor<SubjectPrerequisite> captor = ArgumentCaptor.forClass(SubjectPrerequisite.class);
    verify(subjectPrerequisiteRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues())
        .allSatisfy(
            sp -> {
              assertThat(sp.getSubject().getCode()).isEqualTo("A");
              assertThat(sp.getStrength()).isEqualTo(PrerequisiteStrength.REQUIRED);
            });
    assertThat(captor.getAllValues())
        .extracting(sp -> sp.getPrerequisite().getCode())
        .containsExactlyInAnyOrder("B", "C");
  }

  @Test
  void seed_이미존재하는_선수관계는_중복적재하지않음_멱등() {
    List<CourseRow> courses = List.of(course("A", List.of("B")), course("B", List.of()));
    // 이미 (A, B) 선수 관계가 존재한다고 가정.
    given(subjectPrerequisiteRepository.existsBySubjectAndPrerequisite(any(), any()))
        .willReturn(true);

    seeder.seed(courses);

    verify(subjectPrerequisiteRepository, never()).save(any());
  }

  @Test
  void seed_과목으로_해석안되는_선수코드는_건너뛴다_에러아님() {
    // "X" 는 courses 에 없어 Subject 로 해석되지 않는다 — 건너뛰되 예외는 던지지 않는다.
    // findByCode("X") 가 empty 라 exists 검사까지 도달하지 않으므로 해당 stub 은 두지 않는다(strict stubbing).
    List<CourseRow> courses = List.of(course("A", List.of("X")));

    seeder.seed(courses);

    verify(subjectPrerequisiteRepository, never()).save(any());
  }

  @Test
  void seed_상호순환_선수관계는_각방향을_적재하고_무한루프없음() {
    // 강의계획서 데이터에 A↔B 상호 선수 쌍이 실제로 존재한다. 적재는 단방향 한 건씩
    // 두 행으로 처리되며, 다운스트림 조회가 1-hop 평탄 lookup 이라 순환이 문제되지 않음을 박제.
    List<CourseRow> courses = List.of(course("A", List.of("B")), course("B", List.of("A")));
    given(subjectPrerequisiteRepository.existsBySubjectAndPrerequisite(any(), any()))
        .willReturn(false);

    seeder.seed(courses);

    ArgumentCaptor<SubjectPrerequisite> captor = ArgumentCaptor.forClass(SubjectPrerequisite.class);
    verify(subjectPrerequisiteRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues())
        .extracting(sp -> sp.getSubject().getCode() + "->" + sp.getPrerequisite().getCode())
        .containsExactlyInAnyOrder("A->B", "B->A");
  }
}
