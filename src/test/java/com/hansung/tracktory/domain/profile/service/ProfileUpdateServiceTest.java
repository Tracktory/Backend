package com.hansung.tracktory.domain.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hansung.tracktory.domain.catalog.organization.entity.Department;
import com.hansung.tracktory.domain.profile.dto.ProfileUpdateRequest;
import com.hansung.tracktory.domain.profile.dto.ProfileUpdateRequest.ProfilePart;
import com.hansung.tracktory.domain.profile.dto.ProfileUpdateResponse;
import com.hansung.tracktory.domain.profile.entity.UserProfile;
import com.hansung.tracktory.domain.profile.repository.UserCompanyTypeRepository;
import com.hansung.tracktory.domain.profile.repository.UserDevFieldRepository;
import com.hansung.tracktory.domain.profile.repository.UserInterestRepository;
import com.hansung.tracktory.domain.profile.repository.UserProfileRepository;
import com.hansung.tracktory.domain.profile.repository.UserTechStackCustomRepository;
import com.hansung.tracktory.domain.profile.repository.UserTechStackRepository;
import com.hansung.tracktory.domain.profile.repository.UserTrackRepository;
import com.hansung.tracktory.domain.profile.repository.UserWorkValueRepository;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.domain.user.repository.UserRepository;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProfileUpdateServiceTest {

  @InjectMocks private ProfileUpdateService service;

  @Mock private UserRepository userRepository;
  @Mock private ProfileAssembler profileAssembler;

  @Mock private UserProfileRepository userProfileRepository;
  @Mock private UserTrackRepository userTrackRepository;
  @Mock private UserInterestRepository userInterestRepository;
  @Mock private UserDevFieldRepository userDevFieldRepository;
  @Mock private UserCompanyTypeRepository userCompanyTypeRepository;
  @Mock private UserWorkValueRepository userWorkValueRepository;
  @Mock private UserTechStackRepository userTechStackRepository;
  @Mock private UserTechStackCustomRepository userTechStackCustomRepository;

  private final User user = User.builder().email("a@b.com").passwordHash("x").build();

  @Test
  void update_profileNotFound_throws404() { // 온보딩 미완료(프로필 없음)면 404
    given(userProfileRepository.findByUserId(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.update(1L, new ProfileUpdateRequest()))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
  }

  @Test
  void update_partialProfile_updatesOnlySentField() { // 보낸 필드(name)만 변경, 나머지 유지
    UserProfile profile = UserProfile.of(user, mock(Department.class), 2, "old");
    given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

    ProfileUpdateRequest request = new ProfileUpdateRequest();
    ReflectionTestUtils.setField(request, "profile", profilePart(null, "새이름"));

    ProfileUpdateResponse result = service.update(1L, request);

    assertThat(profile.getName()).isEqualTo("새이름");
    assertThat(profile.getCurrentYear()).isEqualTo(2); // 안 보낸 필드는 유지
    assertThat(result.updatedFields()).containsExactly("profile.name");
  }

  @Test
  void update_arrayField_replacesByDeleteThenInsert() { // 배열 전달 시 삭제 후 재삽입(전체 교체)
    UserProfile profile = UserProfile.of(user, mock(Department.class), 2, "이름");
    given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

    ProfileUpdateRequest request = new ProfileUpdateRequest();
    ReflectionTestUtils.setField(request, "interestIds", List.of(1L, 2L));

    ProfileUpdateResponse result = service.update(1L, request);

    verify(userInterestRepository).deleteByUserId(1L);
    verify(userInterestRepository).saveAll(any());
    assertThat(result.updatedFields()).containsExactly("interestIds");
  }

  @Test
  void update_emptyArray_clears() { // 빈 배열이면 삭제만 수행(전체 해제)
    UserProfile profile = UserProfile.of(user, mock(Department.class), 2, "이름");
    given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

    ProfileUpdateRequest request = new ProfileUpdateRequest();
    ReflectionTestUtils.setField(request, "interestIds", List.of());

    ProfileUpdateResponse result = service.update(1L, request);

    verify(userInterestRepository).deleteByUserId(1L);
    assertThat(result.updatedFields()).containsExactly("interestIds");
  }

  @Test
  void update_omittedFields_notTouched() { // 안 보낸 필드는 삭제/변경 안 함
    UserProfile profile = UserProfile.of(user, mock(Department.class), 2, "이름");
    given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

    ProfileUpdateResponse result = service.update(1L, new ProfileUpdateRequest());

    assertThat(result.updatedFields()).isEmpty();
    verify(userInterestRepository, never()).deleteByUserId(anyLong());
    verify(userTrackRepository, never()).deleteByUserId(anyLong());
  }

  // ------------------------------ helpers ------------------------------

  private static ProfilePart profilePart(Integer currentYear, String name) {
    ProfilePart p = new ProfilePart();
    ReflectionTestUtils.setField(p, "currentYear", currentYear);
    ReflectionTestUtils.setField(p, "name", name);
    return p;
  }
}
