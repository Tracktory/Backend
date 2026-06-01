package com.hansung.tracktory.domain.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hansung.tracktory.domain.profile.dto.OnboardingRequest;
import com.hansung.tracktory.domain.profile.dto.OnboardingResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

  @InjectMocks private OnboardingService onboardingService;

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

  @Test
  void onboard_alreadyCompleted_throwsAndSkipsSave() { // 이미 온보딩했으면 409, 저장 안 함
    given(userProfileRepository.existsByUserId(1L)).willReturn(true);

    assertThatThrownBy(() -> onboardingService.onboard(1L, new OnboardingRequest()))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ONBOARDING_ALREADY_COMPLETED));

    verify(userProfileRepository, never()).save(any());
  }

  @Test
  void onboard_success_savesAllAndReturnsCompleted() { // 정상: 전부 저장 후 완료 응답 반환
    given(userProfileRepository.existsByUserId(1L)).willReturn(false);
    given(userRepository.getReferenceById(1L))
        .willReturn(User.builder().email("a@b.com").passwordHash("x").build());

    OnboardingResponse result = onboardingService.onboard(1L, new OnboardingRequest());

    assertThat(result.onboardingCompleted()).isTrue();
    verify(userProfileRepository).save(any());
    verify(userTrackRepository).saveAll(any());
    verify(userInterestRepository).saveAll(any());
  }
}
