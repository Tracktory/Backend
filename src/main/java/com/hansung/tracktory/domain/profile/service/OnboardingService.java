package com.hansung.tracktory.domain.profile.service;

import com.hansung.tracktory.domain.profile.dto.OnboardingRequest;
import com.hansung.tracktory.domain.profile.dto.OnboardingResponse;
import com.hansung.tracktory.domain.profile.repository.UserCompanyTypeRepository;
import com.hansung.tracktory.domain.profile.repository.UserCompletedSubjectRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingService {

  private final UserRepository userRepository;
  private final ProfileAssembler profileAssembler;

  private final UserProfileRepository userProfileRepository;
  private final UserTrackRepository userTrackRepository;
  private final UserInterestRepository userInterestRepository;
  private final UserDevFieldRepository userDevFieldRepository;
  private final UserCompanyTypeRepository userCompanyTypeRepository;
  private final UserWorkValueRepository userWorkValueRepository;
  private final UserTechStackRepository userTechStackRepository;
  private final UserTechStackCustomRepository userTechStackCustomRepository;
  private final UserCompletedSubjectRepository userCompletedSubjectRepository;

  @Transactional
  public OnboardingResponse onboard(Long userId, OnboardingRequest request) {
    if (userProfileRepository.existsByUserId(userId)) {
      throw new BusinessException(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
    }

    User user = userRepository.getReferenceById(userId);

    userProfileRepository.save(profileAssembler.profile(user, request.getProfile()));
    userTrackRepository.saveAll(profileAssembler.tracks(user, request.getTracks()));
    userInterestRepository.saveAll(profileAssembler.interests(user, request.getInterestIds()));
    userDevFieldRepository.saveAll(profileAssembler.devFields(user, request.getDevFieldIds()));
    userCompanyTypeRepository.saveAll(
        profileAssembler.companyTypes(user, request.getCompanyTypeIds()));
    userWorkValueRepository.saveAll(profileAssembler.workValues(user, request.getWorkValueIds()));
    userTechStackRepository.saveAll(profileAssembler.techStacks(user, request.getTechStackIds()));
    userTechStackCustomRepository.saveAll(
        profileAssembler.techStackCustoms(user, request.getTechStackCustoms()));
    userCompletedSubjectRepository.saveAll(
        profileAssembler.completedSubjects(user, request.getCompletedSubjects()));

    return OnboardingResponse.completed();
  }
}
