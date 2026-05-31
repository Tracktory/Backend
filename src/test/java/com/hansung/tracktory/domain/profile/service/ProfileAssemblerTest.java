package com.hansung.tracktory.domain.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.hansung.tracktory.domain.catalog.career.repository.TechStackRepository;
import com.hansung.tracktory.domain.catalog.classification.entity.Interest;
import com.hansung.tracktory.domain.catalog.classification.repository.CompanyTypeRepository;
import com.hansung.tracktory.domain.catalog.classification.repository.DevFieldRepository;
import com.hansung.tracktory.domain.catalog.classification.repository.InterestRepository;
import com.hansung.tracktory.domain.catalog.classification.repository.WorkValueRepository;
import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.catalog.organization.entity.Department;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.catalog.organization.repository.DepartmentRepository;
import com.hansung.tracktory.domain.catalog.organization.repository.TrackRepository;
import com.hansung.tracktory.domain.profile.dto.OnboardingRequest.CompletedSubjectRequest;
import com.hansung.tracktory.domain.profile.dto.OnboardingRequest.ProfileRequest;
import com.hansung.tracktory.domain.profile.dto.OnboardingRequest.TrackRequest;
import com.hansung.tracktory.domain.profile.entity.UserCompletedSubject;
import com.hansung.tracktory.domain.profile.entity.UserInterest;
import com.hansung.tracktory.domain.profile.entity.UserProfile;
import com.hansung.tracktory.domain.profile.entity.UserTechStackCustom;
import com.hansung.tracktory.domain.profile.entity.UserTrack;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProfileAssemblerTest {

  @InjectMocks private ProfileAssembler assembler;

  @Mock private DepartmentRepository departmentRepository;
  @Mock private TrackRepository trackRepository;
  @Mock private InterestRepository interestRepository;
  @Mock private DevFieldRepository devFieldRepository;
  @Mock private CompanyTypeRepository companyTypeRepository;
  @Mock private WorkValueRepository workValueRepository;
  @Mock private TechStackRepository techStackRepository;
  @Mock private SubjectRepository subjectRepository;

  private final User user = User.builder().email("a@b.com").passwordHash("x").build();

  // ------------------------------ interests / resolveAll ------------------------------

  @Test
  void interests_success() { // 정상: ID 개수만큼 UserInterest 생성
    given(interestRepository.findAllById(List.of(1L, 2L)))
        .willReturn(List.of(interest(), interest()));

    List<UserInterest> result = assembler.interests(user, List.of(1L, 2L));

    assertThat(result).hasSize(2);
  }

  @Test
  void interests_unknownId_throwsValidationFailed() { // 없는 ID 섞이면 422
    given(interestRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(interest()));

    assertThatThrownBy(() -> assembler.interests(user, List.of(1L, 2L)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED));
  }

  @Test
  void interests_null_returnsEmpty() { // null 입력이면 빈 리스트
    assertThat(assembler.interests(user, null)).isEmpty();
  }

  @Test
  void interests_duplicateIds_dedupedBeforeQuery() { // 중복 ID는 조회 전에 제거
    given(interestRepository.findAllById(List.of(1L))).willReturn(List.of(interest()));

    List<UserInterest> result = assembler.interests(user, List.of(1L, 1L));

    assertThat(result).hasSize(1);
  }

  // ------------------------------ tracks ------------------------------

  @Test
  void tracks_success_pairsTrackWithOrder() { // 트랙과 trackOrder 를 짝지어 생성
    Track t5 = mock(Track.class);
    Track t7 = mock(Track.class);
    given(t5.getId()).willReturn(5L);
    given(t7.getId()).willReturn(7L);
    given(trackRepository.findAllById(List.of(5L, 7L))).willReturn(List.of(t5, t7));

    List<UserTrack> result = assembler.tracks(user, List.of(trackReq(5L, 1), trackReq(7L, 2)));

    assertThat(result).hasSize(2);
    assertThat(result).extracting(UserTrack::getTrackOrder).containsExactly(1, 2);
    assertThat(result).extracting(UserTrack::getTrack).containsExactly(t5, t7);
  }

  @Test
  void tracks_missingPrimary_throwsValidationFailed() { // 1트랙(주전공) 없으면 422
    assertThatThrownBy(() -> assembler.tracks(user, List.of(trackReq(7L, 2))))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED));
  }

  @Test
  void tracks_duplicateOrder_throwsValidationFailed() { // trackOrder 중복이면 422
    assertThatThrownBy(() -> assembler.tracks(user, List.of(trackReq(5L, 1), trackReq(7L, 1))))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED));
  }

  @Test
  void tracks_duplicateTrackId_throwsValidationFailed() { // trackId 중복이면 422
    assertThatThrownBy(() -> assembler.tracks(user, List.of(trackReq(5L, 1), trackReq(5L, 2))))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED));
  }

  @Test
  void tracks_empty_returnsEmpty() { // 트랙 미입력(1학년 등)이면 빈 리스트
    assertThat(assembler.tracks(user, List.of())).isEmpty();
  }

  // ------------------------------ completedSubjects ------------------------------

  @Test
  void completedSubjects_success_carriesYearAndSemester() { // year/semester 보존하며 생성
    Subject s100 = mock(Subject.class);
    given(s100.getId()).willReturn(100L);
    given(subjectRepository.findAllById(List.of(100L))).willReturn(List.of(s100));

    List<UserCompletedSubject> result =
        assembler.completedSubjects(user, List.of(subjectReq(100L, 2, 1)));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getYear()).isEqualTo(2);
    assertThat(result.get(0).getSemester()).isEqualTo(1);
  }

  @Test
  void completedSubjects_duplicateSubjectId_throwsValidationFailed() { // subjectId 중복이면 422
    assertThatThrownBy(
            () ->
                assembler.completedSubjects(
                    user, List.of(subjectReq(100L, 1, 1), subjectReq(100L, 2, 1))))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED));
  }

  // ------------------------------ techStackCustoms ------------------------------

  @Test
  void techStackCustoms_filtersBlankAndDeduplicates() { // 공백·null·중복 제거 후 생성
    List<UserTechStackCustom> result =
        assembler.techStackCustoms(user, Arrays.asList("Rust", " Rust ", "", "  ", null, "Zig"));

    assertThat(result).extracting(UserTechStackCustom::getLabel).containsExactly("Rust", "Zig");
  }

  @Test
  void techStackCustoms_null_returnsEmpty() { // null이면 빈 리스트
    assertThat(assembler.techStackCustoms(user, null)).isEmpty();
  }

  // ------------------------------ profile ------------------------------

  @Test
  void profile_success() { // 학부 존재 시 프로필 생성
    Department dept = mock(Department.class);
    given(departmentRepository.findById(1L)).willReturn(Optional.of(dept));

    UserProfile result = assembler.profile(user, profileReq(1L, 3, "정종진"));

    assertThat(result.getDepartment()).isEqualTo(dept);
    assertThat(result.getCurrentYear()).isEqualTo(3);
    assertThat(result.getName()).isEqualTo("정종진");
  }

  @Test
  void profile_departmentNotFound_throwsValidationFailed() { // 학부 없으면 422
    given(departmentRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> assembler.profile(user, profileReq(99L, 3, "정종진")))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED));
  }

  // ------------------------------ helpers ------------------------------

  private static Interest interest() {
    return Interest.builder().code("x").build();
  }

  private static TrackRequest trackReq(Long trackId, Integer order) {
    TrackRequest r = new TrackRequest();
    ReflectionTestUtils.setField(r, "trackId", trackId);
    ReflectionTestUtils.setField(r, "trackOrder", order);
    return r;
  }

  private static CompletedSubjectRequest subjectReq(
      Long subjectId, Integer year, Integer semester) {
    CompletedSubjectRequest r = new CompletedSubjectRequest();
    ReflectionTestUtils.setField(r, "subjectId", subjectId);
    ReflectionTestUtils.setField(r, "year", year);
    ReflectionTestUtils.setField(r, "semester", semester);
    return r;
  }

  private static ProfileRequest profileReq(Long departmentId, Integer currentYear, String name) {
    ProfileRequest r = new ProfileRequest();
    ReflectionTestUtils.setField(r, "departmentId", departmentId);
    ReflectionTestUtils.setField(r, "currentYear", currentYear);
    ReflectionTestUtils.setField(r, "name", name);
    return r;
  }
}
