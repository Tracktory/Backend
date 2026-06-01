package com.hansung.tracktory.domain.catalog.loader;

import com.hansung.tracktory.domain.catalog.curriculum.entity.PrerequisiteStrength;
import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectPrerequisite;
import com.hansung.tracktory.domain.catalog.curriculum.entity.TrackSubject;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectPrerequisiteRepository;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.catalog.curriculum.repository.TrackSubjectRepository;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.CourseRow;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.catalog.organization.repository.TrackRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 과목·트랙↔과목 연결·과목 선수 관계를 적재한다. 과목은 code, 트랙 연결은 (트랙, 과목), 선수 관계는 (과목, 선수 과목) 기준 멱등. 강의계획서에
 * 본문(description)이 없어 빈 문자열로 적재하며, 이후 인덱싱 단계에서 보강한다.
 *
 * <p>선수 관계는 모든 과목 행의 과목(Subject)을 먼저 만든 뒤 2차 패스로 적재한다. 선수 코드가 과목보다 뒤에 정의되는 전방 참조를 1차 패스 완료로 보장하기
 * 위함이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurriculumSeeder {

  private final SubjectRepository subjectRepository;
  private final TrackSubjectRepository trackSubjectRepository;
  private final TrackRepository trackRepository;
  private final SubjectPrerequisiteRepository subjectPrerequisiteRepository;

  @Transactional
  public void seed(List<CourseRow> courses) {
    // 1차 패스: 과목 + 트랙↔과목 연결 적재
    int skippedTrackRefs = 0;
    for (CourseRow row : courses) {
      Subject subject = resolveSubject(row);
      for (String trackCode : row.trackCodes()) {
        Optional<Track> track = trackRepository.findByCode(trackCode);
        if (track.isEmpty()) {
          skippedTrackRefs++; // tracks.yaml 에 없는 트랙 참조는 건너뛴다(데이터 정합은 적재 후 검증)
          continue;
        }
        if (!trackSubjectRepository.existsByTrackAndSubject(track.get(), subject)) {
          trackSubjectRepository.save(
              TrackSubject.builder()
                  .track(track.get())
                  .subject(subject)
                  .type(row.type())
                  .stage(row.stage())
                  .build());
        }
      }
    }
    if (skippedTrackRefs > 0) {
      log.warn("tracks.yaml 에 없는 트랙을 참조하는 과목-트랙 연결 {}건을 건너뛰었습니다", skippedTrackRefs);
    }

    // 2차 패스: 과목 선수 관계 적재 (1차 패스로 모든 과목이 존재함을 전제)
    seedPrerequisites(courses);
  }

  /**
   * 과목 선수 관계를 적재한다. 선수 코드를 과목으로 해석할 수 없으면(다른 학교/폐강 등) 건너뛴다. 강의계획서 데이터가 선수 강도(필수/권장)를 구분하지 않으므로 모두
   * {@link PrerequisiteStrength#REQUIRED} 로 적재한다. (과목, 선수 과목) 기준 멱등.
   */
  private void seedPrerequisites(List<CourseRow> courses) {
    int savedPrereqs = 0;
    int skippedPrereqRefs = 0;
    for (CourseRow row : courses) {
      if (row.prereqCodes().isEmpty()) {
        continue;
      }
      Subject subject = resolveSubject(row);
      for (String prereqCode : row.prereqCodes()) {
        Optional<Subject> prerequisite = subjectRepository.findByCode(prereqCode);
        if (prerequisite.isEmpty()) {
          skippedPrereqRefs++; // courses.yaml 에 없는 과목을 가리키는 선수 참조는 건너뛴다
          continue;
        }
        if (!subjectPrerequisiteRepository.existsBySubjectAndPrerequisite(
            subject, prerequisite.get())) {
          subjectPrerequisiteRepository.save(
              SubjectPrerequisite.builder()
                  .subject(subject)
                  .prerequisite(prerequisite.get())
                  .strength(PrerequisiteStrength.REQUIRED)
                  .build());
          savedPrereqs++;
        }
      }
    }
    log.info("과목 선수 관계 {}건을 적재했습니다", savedPrereqs);
    if (skippedPrereqRefs > 0) {
      log.warn("courses.yaml 에 없는 과목을 참조하는 선수 관계 {}건을 건너뛰었습니다", skippedPrereqRefs);
    }
  }

  private Subject resolveSubject(CourseRow row) {
    return subjectRepository
        .findByCode(row.code())
        .orElseGet(
            () ->
                subjectRepository.save(
                    Subject.builder()
                        .code(row.code())
                        .name(row.name())
                        .description("")
                        .credit(row.credit())
                        .semester(row.semester())
                        .build()));
  }
}
