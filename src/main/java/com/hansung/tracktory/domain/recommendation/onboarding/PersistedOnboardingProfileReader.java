package com.hansung.tracktory.domain.recommendation.onboarding;

import com.hansung.tracktory.domain.catalog.organization.entity.Department;
import com.hansung.tracktory.domain.profile.entity.UserCompletedSubject;
import com.hansung.tracktory.domain.profile.entity.UserProfile;
import com.hansung.tracktory.domain.profile.repository.UserCompanyTypeRepository;
import com.hansung.tracktory.domain.profile.repository.UserCompletedSubjectRepository;
import com.hansung.tracktory.domain.profile.repository.UserDevFieldRepository;
import com.hansung.tracktory.domain.profile.repository.UserInterestRepository;
import com.hansung.tracktory.domain.profile.repository.UserProfileRepository;
import com.hansung.tracktory.domain.profile.repository.UserTrackRepository;
import com.hansung.tracktory.domain.profile.repository.UserWorkValueRepository;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot.CompletedCourse;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저장된 온보딩 프로필을 추천 입력 스냅샷으로 변환한다.
 *
 * <p>추천 도메인은 본 reader 를 통해서만 프로필 도메인의 영속 모델을 읽는다. 트랙을 아직 선택하지 않은 사용자도 온보딩 프로필이 있으면 빈 트랙 목록을 그대로 AI
 * 입력으로 전달해, "선택 없음" 과 "선택 있음" 을 구분한다.
 */
@Component
@RequiredArgsConstructor
public class PersistedOnboardingProfileReader implements OnboardingProfileReader {

  private static final int DEFAULT_ADMISSION_YEAR_UNTIL_PROFILE_SCHEMA_SUPPORTS_IT = 2023;

  private final UserProfileRepository userProfileRepository;
  private final UserTrackRepository userTrackRepository;
  private final UserInterestRepository userInterestRepository;
  private final UserDevFieldRepository userDevFieldRepository;
  private final UserWorkValueRepository userWorkValueRepository;
  private final UserCompanyTypeRepository userCompanyTypeRepository;
  private final UserCompletedSubjectRepository userCompletedSubjectRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<OnboardingProfileSnapshot> read(Long userId) {
    return userProfileRepository.findByUserId(userId).map(profile -> toSnapshot(userId, profile));
  }

  private OnboardingProfileSnapshot toSnapshot(Long userId, UserProfile profile) {
    Department department = profile.getDepartment();
    return new OnboardingProfileSnapshot(
        userId,
        DEFAULT_ADMISSION_YEAR_UNTIL_PROFILE_SCHEMA_SUPPORTS_IT,
        department.getCollege().getName(),
        department.getName(),
        currentSemester(profile.getCurrentYear()),
        trackCodes(userId),
        interestCodes(userId),
        devFieldCodes(userId),
        workValueCodes(userId),
        companyTypeCodes(userId),
        List.of(),
        completedCourses(userId));
  }

  private Integer currentSemester(Integer currentYear) {
    if (currentYear == null) {
      return null;
    }
    return Math.max(1, currentYear * 2 - 1);
  }

  private List<String> trackCodes(Long userId) {
    return userTrackRepository.findByUserIdOrderByTrackOrderAsc(userId).stream()
        .map(userTrack -> userTrack.getTrack().getCode())
        .toList();
  }

  private List<String> interestCodes(Long userId) {
    return userInterestRepository.findByUserIdOrderByIdAsc(userId).stream()
        .map(userInterest -> userInterest.getInterest().getCode())
        .toList();
  }

  private List<String> devFieldCodes(Long userId) {
    return userDevFieldRepository.findByUserIdOrderByIdAsc(userId).stream()
        .map(userDevField -> userDevField.getDevField().getCode())
        .toList();
  }

  private List<String> workValueCodes(Long userId) {
    return userWorkValueRepository.findByUserIdOrderByIdAsc(userId).stream()
        .map(userWorkValue -> userWorkValue.getWorkValue().getCode())
        .toList();
  }

  private List<String> companyTypeCodes(Long userId) {
    return userCompanyTypeRepository.findByUserIdOrderByIdAsc(userId).stream()
        .map(userCompanyType -> userCompanyType.getCompanyType().getCode())
        .toList();
  }

  private List<CompletedCourse> completedCourses(Long userId) {
    return userCompletedSubjectRepository.findByUserIdOrderByYearAscSemesterAsc(userId).stream()
        .map(this::completedCourse)
        .toList();
  }

  private CompletedCourse completedCourse(UserCompletedSubject completedSubject) {
    return new CompletedCourse(
        completedSubject.getSubject().getCode(),
        completedSubject.getYear(),
        completedSubject.getSemester());
  }
}
