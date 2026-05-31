package com.hansung.tracktory.domain.recommendation.onboarding;

import com.hansung.tracktory.domain.catalog.classification.entity.CompanyType;
import com.hansung.tracktory.domain.catalog.classification.entity.DevField;
import com.hansung.tracktory.domain.catalog.classification.entity.Interest;
import com.hansung.tracktory.domain.catalog.classification.entity.WorkValue;
import com.hansung.tracktory.domain.catalog.classification.repository.CompanyTypeRepository;
import com.hansung.tracktory.domain.catalog.classification.repository.DevFieldRepository;
import com.hansung.tracktory.domain.catalog.classification.repository.InterestRepository;
import com.hansung.tracktory.domain.catalog.classification.repository.WorkValueRepository;
import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.catalog.organization.entity.Department;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.catalog.organization.repository.TrackRepository;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot.CompletedCourse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 온보딩 영속 도메인이 준비되기 전까지 사용하는 임시 시드 구현.
 *
 * <p>적재된 카탈로그 기준 데이터에서 그럴듯한 프로필 한 건을 합성한다. 반환하는 모든 분류·트랙·과목 코드는 실제 카탈로그에 존재하므로 추천 파이프라인의 코드 → 카탈로그
 * 매핑이 성립한다. 트랙 두 개를 합성하지 못할 만큼 카탈로그가 비어 있으면 {@link Optional#empty()} 를 반환해 온보딩 미완료로 처리되게 한다. 온보딩
 * 도메인이 완성되면 본 구현을 영속 기반 reader 로 교체한다.
 */
@Component
@RequiredArgsConstructor
public class SeedOnboardingProfileReader implements OnboardingProfileReader {

  private static final int SEED_ADMISSION_YEAR = 2023;
  private static final int SEED_CURRENT_SEMESTER = 3;

  // 이수 과목은 현재 학기(3학기) 이전 학기에만 놓여야 한다. completed=true 인 과목이 현재 학기 이후로
  // 배치되면 (a) 같은 (year,semester) 학기 카드가 로드맵에서 중복되고 (b) completed=true 인데 timing 이
  // FUTURE 가 되는 모순이 발생한다. 1학년 1·2학기 두 칸만 사용한다.
  private static final int[][] COMPLETED_SCHEDULE = {{1, 1}, {1, 2}};

  // MVP 타겟(컴퓨터공학부 빅데이터)을 mirror 한 데모 프로필. 모두 카탈로그에 존재하는 코드이며,
  // 1트랙(빅데이터트랙)이 주전공 소속이라 home 단과대/학부가 자연히 IT공과대학/컴퓨터공학부로 잡힌다.
  // 코드가 누락된 환경에서는 카탈로그 선두 항목으로 graceful fallback 한다.
  private static final List<String> SEED_TRACK_CODES = List.of("빅데이터트랙", "AIㆍ소프트웨어학과");
  private static final List<String> SEED_INTEREST_CODES = List.of("IT/인터넷", "연구개발/설계");
  private static final List<String> SEED_DEV_FIELD_CODES = List.of("데이터");
  private static final List<String> SEED_COMPANY_TYPE_CODES = List.of("대기업");
  private static final List<String> SEED_WORK_VALUE_CODES = List.of("성장성");

  // 이수 과목은 컴퓨터공학부 기초 과목으로 합성한다(COMPLETED_SCHEDULE 와 순서·개수 정합).
  // 카탈로그 선두 4개 과목은 패션(계약학과) 소속이라 데모 프로필의 학과와 무관하므로 코드로 직접 지정한다.
  private static final List<String> SEED_COMPLETED_COURSE_CODES = List.of("CTE0001", "W080002");

  private final TrackRepository trackRepository;
  private final SubjectRepository subjectRepository;
  private final InterestRepository interestRepository;
  private final DevFieldRepository devFieldRepository;
  private final CompanyTypeRepository companyTypeRepository;
  private final WorkValueRepository workValueRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<OnboardingProfileSnapshot> read(Long userId) {
    List<Track> tracks = resolveTracks();
    List<Interest> interests =
        resolveByCodes(
            SEED_INTEREST_CODES,
            interestRepository::findByCode,
            () -> interestRepository.findAll(PageRequest.of(0, 2)).getContent());
    List<DevField> devFields =
        resolveByCodes(
            SEED_DEV_FIELD_CODES,
            devFieldRepository::findByCode,
            () -> devFieldRepository.findAll(PageRequest.of(0, 1)).getContent());
    List<CompanyType> companyTypes =
        resolveByCodes(
            SEED_COMPANY_TYPE_CODES,
            companyTypeRepository::findByCode,
            () -> companyTypeRepository.findAll(PageRequest.of(0, 1)).getContent());
    List<WorkValue> workValues =
        resolveByCodes(
            SEED_WORK_VALUE_CODES,
            workValueRepository::findByCode,
            () -> workValueRepository.findAll(PageRequest.of(0, 1)).getContent());
    List<Subject> subjects =
        resolveByCodes(
            SEED_COMPLETED_COURSE_CODES,
            subjectRepository::findByCode,
            () -> subjectRepository.findAll(PageRequest.of(0, 2)).getContent());

    // AI 중계 서버는 정확히 0개 또는 2개 트랙을 요구한다. 시드는 2개 모드를 합성한다.
    if (tracks.size() < 2 || interests.isEmpty() || devFields.isEmpty() || companyTypes.isEmpty()) {
      return Optional.empty();
    }

    Department homeDepartment = tracks.get(0).getDepartment();
    OnboardingProfileSnapshot snapshot =
        new OnboardingProfileSnapshot(
            userId,
            SEED_ADMISSION_YEAR,
            homeDepartment.getCollege().getName(),
            homeDepartment.getName(),
            SEED_CURRENT_SEMESTER,
            tracks.stream().map(Track::getCode).toList(),
            interests.stream().map(Interest::getCode).toList(),
            devFields.stream().map(DevField::getCode).toList(),
            workValues.stream().map(WorkValue::getCode).toList(),
            companyTypes.stream().map(CompanyType::getCode).toList(),
            List.of(),
            toCompletedCourses(subjects));
    return Optional.of(snapshot);
  }

  private List<Track> resolveTracks() {
    List<Track> resolved = new ArrayList<>();
    for (String code : SEED_TRACK_CODES) {
      trackRepository.findByCode(code).ifPresent(resolved::add);
    }
    if (resolved.size() >= 2) {
      return resolved.subList(0, 2);
    }
    return trackRepository.findAll(PageRequest.of(0, 2)).getContent();
  }

  private <T> List<T> resolveByCodes(
      List<String> codes, Function<String, Optional<T>> finder, Supplier<List<T>> fallback) {
    List<T> resolved = new ArrayList<>();
    for (String code : codes) {
      finder.apply(code).ifPresent(resolved::add);
    }
    return resolved.isEmpty() ? fallback.get() : resolved;
  }

  private List<CompletedCourse> toCompletedCourses(List<Subject> subjects) {
    List<CompletedCourse> completed = new ArrayList<>();
    for (int i = 0; i < subjects.size() && i < COMPLETED_SCHEDULE.length; i++) {
      int[] slot = COMPLETED_SCHEDULE[i];
      completed.add(new CompletedCourse(subjects.get(i).getCode(), slot[0], slot[1]));
    }
    return completed;
  }
}
