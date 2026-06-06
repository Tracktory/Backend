package com.hansung.tracktory.domain.recommendation.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hansung.tracktory.domain.catalog.classification.entity.CompanyType;
import com.hansung.tracktory.domain.catalog.classification.entity.DevField;
import com.hansung.tracktory.domain.catalog.classification.entity.Interest;
import com.hansung.tracktory.domain.catalog.classification.entity.WorkValue;
import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.organization.entity.College;
import com.hansung.tracktory.domain.catalog.organization.entity.Department;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.profile.entity.UserCompanyType;
import com.hansung.tracktory.domain.profile.entity.UserCompletedSubject;
import com.hansung.tracktory.domain.profile.entity.UserDevField;
import com.hansung.tracktory.domain.profile.entity.UserInterest;
import com.hansung.tracktory.domain.profile.entity.UserProfile;
import com.hansung.tracktory.domain.profile.entity.UserTrack;
import com.hansung.tracktory.domain.profile.entity.UserWorkValue;
import com.hansung.tracktory.domain.profile.repository.UserCompanyTypeRepository;
import com.hansung.tracktory.domain.profile.repository.UserCompletedSubjectRepository;
import com.hansung.tracktory.domain.profile.repository.UserDevFieldRepository;
import com.hansung.tracktory.domain.profile.repository.UserInterestRepository;
import com.hansung.tracktory.domain.profile.repository.UserProfileRepository;
import com.hansung.tracktory.domain.profile.repository.UserTrackRepository;
import com.hansung.tracktory.domain.profile.repository.UserWorkValueRepository;
import com.hansung.tracktory.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersistedOnboardingProfileReaderTest {

  @InjectMocks private PersistedOnboardingProfileReader reader;

  @Mock private UserProfileRepository userProfileRepository;
  @Mock private UserTrackRepository userTrackRepository;
  @Mock private UserInterestRepository userInterestRepository;
  @Mock private UserDevFieldRepository userDevFieldRepository;
  @Mock private UserWorkValueRepository userWorkValueRepository;
  @Mock private UserCompanyTypeRepository userCompanyTypeRepository;
  @Mock private UserCompletedSubjectRepository userCompletedSubjectRepository;

  @Test
  void read_notOnboarded_returnsEmptyWithoutReadingCollections() {
    given(userProfileRepository.findByUserId(1L)).willReturn(Optional.empty());

    Optional<OnboardingProfileSnapshot> result = reader.read(1L);

    assertThat(result).isEmpty();
    verify(userTrackRepository, never()).findByUserIdOrderByTrackOrderAsc(1L);
    verify(userCompletedSubjectRepository, never()).findByUserIdOrderByYearAscSemesterAsc(1L);
  }

  @Test
  void read_persistedProfile_mapsUserSpecificSelections() {
    User user = sampleUser();
    Department department = sampleDepartment();
    UserProfile profile = UserProfile.of(user, department, 3, "이재원");
    Track primary = Track.builder().code("BIGDATA").department(department).name("빅데이터트랙").build();
    Track secondary = Track.builder().code("WEB").department(department).name("웹공학트랙").build();
    Subject completed = Subject.builder().code("W080001").name("프로그래밍기초").build();

    given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));
    given(userTrackRepository.findByUserIdOrderByTrackOrderAsc(1L))
        .willReturn(List.of(UserTrack.of(user, primary, 1), UserTrack.of(user, secondary, 2)));
    given(userInterestRepository.findByUserIdOrderByIdAsc(1L))
        .willReturn(List.of(UserInterest.of(user, Interest.builder().code("IT/인터넷").build())));
    given(userDevFieldRepository.findByUserIdOrderByIdAsc(1L))
        .willReturn(List.of(UserDevField.of(user, DevField.builder().code("데이터").build())));
    given(userWorkValueRepository.findByUserIdOrderByIdAsc(1L))
        .willReturn(List.of(UserWorkValue.of(user, WorkValue.builder().code("성장성").build())));
    given(userCompanyTypeRepository.findByUserIdOrderByIdAsc(1L))
        .willReturn(List.of(UserCompanyType.of(user, CompanyType.builder().code("대기업").build())));
    given(userCompletedSubjectRepository.findByUserIdOrderByYearAscSemesterAsc(1L))
        .willReturn(List.of(UserCompletedSubject.of(user, completed, 1, 1)));

    OnboardingProfileSnapshot result = reader.read(1L).orElseThrow();

    assertThat(result.userId()).isEqualTo(1L);
    assertThat(result.admissionYear()).isEqualTo(2023);
    assertThat(result.collegeName()).isEqualTo("IT공과대학");
    assertThat(result.departmentName()).isEqualTo("컴퓨터공학부");
    assertThat(result.currentSemester()).isEqualTo(5);
    assertThat(result.trackCodes()).containsExactly("BIGDATA", "WEB");
    assertThat(result.interestCodes()).containsExactly("IT/인터넷");
    assertThat(result.devFieldCodes()).containsExactly("데이터");
    assertThat(result.workValueCodes()).containsExactly("성장성");
    assertThat(result.companyTypeCodes()).containsExactly("대기업");
    assertThat(result.completedCourses())
        .singleElement()
        .satisfies(
            course -> {
              assertThat(course.subjectCode()).isEqualTo("W080001");
              assertThat(course.year()).isEqualTo(1);
              assertThat(course.semester()).isEqualTo(1);
            });
  }

  @Test
  void read_profileWithoutTracks_returnsEmptyTrackCodes() {
    User user = sampleUser();
    UserProfile profile = UserProfile.of(user, sampleDepartment(), 1, "이재원");

    given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));
    given(userTrackRepository.findByUserIdOrderByTrackOrderAsc(1L)).willReturn(List.of());
    given(userInterestRepository.findByUserIdOrderByIdAsc(1L))
        .willReturn(List.of(UserInterest.of(user, Interest.builder().code("IT/인터넷").build())));
    given(userDevFieldRepository.findByUserIdOrderByIdAsc(1L)).willReturn(List.of());
    given(userWorkValueRepository.findByUserIdOrderByIdAsc(1L))
        .willReturn(List.of(UserWorkValue.of(user, WorkValue.builder().code("성장성").build())));
    given(userCompanyTypeRepository.findByUserIdOrderByIdAsc(1L))
        .willReturn(List.of(UserCompanyType.of(user, CompanyType.builder().code("대기업").build())));
    given(userCompletedSubjectRepository.findByUserIdOrderByYearAscSemesterAsc(1L))
        .willReturn(List.of());

    OnboardingProfileSnapshot result = reader.read(1L).orElseThrow();

    assertThat(result.currentSemester()).isEqualTo(1);
    assertThat(result.trackCodes()).isEmpty();
    assertThat(result.interestCodes()).containsExactly("IT/인터넷");
  }

  private static User sampleUser() {
    return User.builder().email("a@b.com").passwordHash("hash").build();
  }

  private static Department sampleDepartment() {
    College college = College.builder().name("IT공과대학").build();
    return Department.builder().college(college).name("컴퓨터공학부").build();
  }
}
