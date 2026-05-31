package com.hansung.tracktory.domain.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.hansung.tracktory.domain.catalog.classification.entity.Interest;
import com.hansung.tracktory.domain.catalog.classification.entity.WorkValue;
import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.organization.entity.Department;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.profile.dto.ProfileResponse;
import com.hansung.tracktory.domain.profile.entity.UserCompletedSubject;
import com.hansung.tracktory.domain.profile.entity.UserInterest;
import com.hansung.tracktory.domain.profile.entity.UserProfile;
import com.hansung.tracktory.domain.profile.entity.UserTechStackCustom;
import com.hansung.tracktory.domain.profile.entity.UserTrack;
import com.hansung.tracktory.domain.profile.entity.UserWorkValue;
import com.hansung.tracktory.domain.profile.repository.UserCompanyTypeRepository;
import com.hansung.tracktory.domain.profile.repository.UserCompletedSubjectRepository;
import com.hansung.tracktory.domain.profile.repository.UserDevFieldRepository;
import com.hansung.tracktory.domain.profile.repository.UserInterestRepository;
import com.hansung.tracktory.domain.profile.repository.UserProfileRepository;
import com.hansung.tracktory.domain.profile.repository.UserTechStackCustomRepository;
import com.hansung.tracktory.domain.profile.repository.UserTechStackRepository;
import com.hansung.tracktory.domain.profile.repository.UserTrackRepository;
import com.hansung.tracktory.domain.profile.repository.UserWorkValueRepository;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileQueryServiceTest {

  @InjectMocks private ProfileQueryService profileQueryService;

  @Mock private UserProfileRepository userProfileRepository;
  @Mock private UserTrackRepository userTrackRepository;
  @Mock private UserInterestRepository userInterestRepository;
  @Mock private UserDevFieldRepository userDevFieldRepository;
  @Mock private UserCompanyTypeRepository userCompanyTypeRepository;
  @Mock private UserWorkValueRepository userWorkValueRepository;
  @Mock private UserTechStackRepository userTechStackRepository;
  @Mock private UserTechStackCustomRepository userTechStackCustomRepository;
  @Mock private UserCompletedSubjectRepository userCompletedSubjectRepository;

  @Test
  void getMyProfile_notOnboarded_throwsResourceNotFound() { // 프로필 미생성이면 404
    given(userProfileRepository.findByUserId(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> profileQueryService.getMyProfile(1L))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
  }

  @Test
  void getMyProfile_success_assemblesProfileAndCollections() { // 정상: 프로필 + 온보딩 정보 조립
    Department dept = mock(Department.class);
    given(dept.getId()).willReturn(1L);
    UserProfile profile = mock(UserProfile.class);
    given(profile.getCurrentYear()).willReturn(3);
    given(profile.getName()).willReturn("이재원");
    given(profile.getDepartment()).willReturn(dept);

    Track track = mock(Track.class);
    given(track.getId()).willReturn(5L);
    given(track.getName()).willReturn("SW 트랙");
    UserTrack userTrack = mock(UserTrack.class);
    given(userTrack.getTrack()).willReturn(track);
    given(userTrack.getTrackOrder()).willReturn(1);

    Interest interest = mock(Interest.class);
    given(interest.getId()).willReturn(1L);
    given(interest.getCode()).willReturn("BACKEND");
    UserInterest userInterest = mock(UserInterest.class);
    given(userInterest.getInterest()).willReturn(interest);

    WorkValue workValue = mock(WorkValue.class);
    given(workValue.getId()).willReturn(7L);
    given(workValue.getCode()).willReturn("GROWTH");
    UserWorkValue userWorkValue = mock(UserWorkValue.class);
    given(userWorkValue.getWorkValue()).willReturn(workValue);

    UserTechStackCustom custom = mock(UserTechStackCustom.class);
    given(custom.getLabel()).willReturn("Rust");

    Subject subject = mock(Subject.class);
    given(subject.getId()).willReturn(100L);
    given(subject.getName()).willReturn("자료구조");
    UserCompletedSubject completed = mock(UserCompletedSubject.class);
    given(completed.getSubject()).willReturn(subject);
    given(completed.getYear()).willReturn(1);
    given(completed.getSemester()).willReturn(1);

    given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));
    given(userTrackRepository.findByUserIdOrderByTrackOrderAsc(1L)).willReturn(List.of(userTrack));
    given(userInterestRepository.findByUserIdOrderByIdAsc(1L)).willReturn(List.of(userInterest));
    given(userWorkValueRepository.findByUserIdOrderByIdAsc(1L)).willReturn(List.of(userWorkValue));
    given(userTechStackCustomRepository.findByUserIdOrderByIdAsc(1L)).willReturn(List.of(custom));
    given(userCompletedSubjectRepository.findByUserIdOrderByYearAscSemesterAsc(1L))
        .willReturn(List.of(completed));

    ProfileResponse result = profileQueryService.getMyProfile(1L);

    assertThat(result.profile().currentYear()).isEqualTo(3);
    assertThat(result.profile().name()).isEqualTo("이재원");
    assertThat(result.profile().departmentId()).isEqualTo(1L);
    assertThat(result.profile().profileImageUrl()).isNull();

    assertThat(result.tracks())
        .singleElement()
        .satisfies(
            t -> {
              assertThat(t.trackId()).isEqualTo(5L);
              assertThat(t.name()).isEqualTo("SW 트랙");
              assertThat(t.trackOrder()).isEqualTo(1);
            });
    assertThat(result.interests())
        .singleElement()
        .satisfies(
            i -> {
              assertThat(i.id()).isEqualTo(1L);
              assertThat(i.code()).isEqualTo("BACKEND");
            });
    assertThat(result.devFields()).isEmpty();
    assertThat(result.companyTypes()).isEmpty();
    assertThat(result.workValues())
        .singleElement()
        .satisfies(
            w -> {
              assertThat(w.id()).isEqualTo(7L);
              assertThat(w.code()).isEqualTo("GROWTH");
            });
    assertThat(result.techStacks()).isEmpty();
    assertThat(result.techStackCustoms()).containsExactly("Rust");
    assertThat(result.completedSubjects())
        .singleElement()
        .satisfies(
            cs -> {
              assertThat(cs.subjectId()).isEqualTo(100L);
              assertThat(cs.name()).isEqualTo("자료구조");
              assertThat(cs.year()).isEqualTo(1);
              assertThat(cs.semester()).isEqualTo(1);
            });
  }
}
