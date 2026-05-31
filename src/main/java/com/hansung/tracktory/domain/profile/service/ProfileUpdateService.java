package com.hansung.tracktory.domain.profile.service;

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
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileUpdateService {

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

  @Transactional
  public ProfileUpdateResponse update(Long userId, ProfileUpdateRequest request) {
    UserProfile profile =
        userProfileRepository
            .findByUserId(userId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "프로필이 존재하지 않습니다. 먼저 온보딩을 완료해주세요."));

    User user = userRepository.getReferenceById(userId);
    List<String> updatedFields = new ArrayList<>();

    updateProfilePart(profile, request.getProfile(), updatedFields);

    if (request.getTracks() != null) {
      userTrackRepository.deleteByUserId(userId);
      userTrackRepository.saveAll(profileAssembler.tracks(user, request.getTracks()));
      updatedFields.add("tracks");
    }
    if (request.getInterestIds() != null) {
      userInterestRepository.deleteByUserId(userId);
      userInterestRepository.saveAll(profileAssembler.interests(user, request.getInterestIds()));
      updatedFields.add("interestIds");
    }
    if (request.getDevFieldIds() != null) {
      userDevFieldRepository.deleteByUserId(userId);
      userDevFieldRepository.saveAll(profileAssembler.devFields(user, request.getDevFieldIds()));
      updatedFields.add("devFieldIds");
    }
    if (request.getCompanyTypeIds() != null) {
      userCompanyTypeRepository.deleteByUserId(userId);
      userCompanyTypeRepository.saveAll(
          profileAssembler.companyTypes(user, request.getCompanyTypeIds()));
      updatedFields.add("companyTypeIds");
    }
    if (request.getWorkValueIds() != null) {
      userWorkValueRepository.deleteByUserId(userId);
      userWorkValueRepository.saveAll(profileAssembler.workValues(user, request.getWorkValueIds()));
      updatedFields.add("workValueIds");
    }
    if (request.getTechStackIds() != null) {
      userTechStackRepository.deleteByUserId(userId);
      userTechStackRepository.saveAll(profileAssembler.techStacks(user, request.getTechStackIds()));
      updatedFields.add("techStackIds");
    }
    if (request.getTechStackCustoms() != null) {
      userTechStackCustomRepository.deleteByUserId(userId);
      userTechStackCustomRepository.saveAll(
          profileAssembler.techStackCustoms(user, request.getTechStackCustoms()));
      updatedFields.add("techStackCustoms");
    }

    return ProfileUpdateResponse.of(updatedFields);
  }

  private void updateProfilePart(
      UserProfile profile, ProfilePart part, List<String> updatedFields) {
    if (part == null) {
      return;
    }
    if (part.getCurrentYear() != null) {
      profile.updateCurrentYear(part.getCurrentYear());
      updatedFields.add("profile.currentYear");
    }
    if (part.getName() != null) {
      profile.updateName(part.getName());
      updatedFields.add("profile.name");
    }
  }
}
