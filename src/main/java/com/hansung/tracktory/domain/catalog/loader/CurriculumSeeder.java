package com.hansung.tracktory.domain.catalog.loader;

import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.TrackSubject;
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
 * 과목과 트랙↔과목 연결을 적재한다. 과목은 code, 연결은 (트랙, 과목) 기준 멱등. 강의계획서에 본문(description)이 없어 빈 문자열로 적재하며, 이후 인덱싱
 * 단계에서 보강한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurriculumSeeder {

  private final SubjectRepository subjectRepository;
  private final TrackSubjectRepository trackSubjectRepository;
  private final TrackRepository trackRepository;

  @Transactional
  public void seed(List<CourseRow> courses) {
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
