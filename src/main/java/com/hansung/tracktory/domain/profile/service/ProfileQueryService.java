package com.hansung.tracktory.domain.profile.service;

import com.hansung.tracktory.domain.profile.dto.ProfileResponse;
import com.hansung.tracktory.domain.profile.dto.ProfileResponse.CodeItem;
import com.hansung.tracktory.domain.profile.dto.ProfileResponse.CompletedSubjectItem;
import com.hansung.tracktory.domain.profile.dto.ProfileResponse.ProfileSummary;
import com.hansung.tracktory.domain.profile.dto.ProfileResponse.TechStackItem;
import com.hansung.tracktory.domain.profile.dto.ProfileResponse.TrackItem;
import com.hansung.tracktory.domain.profile.entity.UserProfile;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 로그인 사용자의 프로필 + 온보딩 정보 전체를 조회한다. 프로필 미생성(온보딩 미완료) 시 404. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileQueryService {

  private final UserProfileRepository userProfileRepository;
  private final UserTrackRepository userTrackRepository;
  private final UserInterestRepository userInterestRepository;
  private final UserDevFieldRepository userDevFieldRepository;
  private final UserCompanyTypeRepository userCompanyTypeRepository;
  private final UserWorkValueRepository userWorkValueRepository;
  private final UserTechStackRepository userTechStackRepository;
  private final UserTechStackCustomRepository userTechStackCustomRepository;
  private final UserCompletedSubjectRepository userCompletedSubjectRepository;

  public ProfileResponse getMyProfile(Long userId) {
    UserProfile profile =
        userProfileRepository
            .findByUserId(userId)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "온보딩이 완료되지 않은 사용자입니다."));

    // department FK 는 프록시 식별자라 lazy init 없이 읽힌다. id 외 다른 Department 필드를
    // 여기서 접근하면 추가 쿼리가 발생하므로, 필요해지면 finder 에 @EntityGraph 를 건다.
    ProfileSummary summary =
        new ProfileSummary(
            profile.getCurrentYear(),
            profile.getName(),
            profile.getDepartment().getId(),
            profile.getProfileImageUrl());

    List<TrackItem> tracks =
        userTrackRepository.findByUserIdOrderByTrackOrderAsc(userId).stream()
            .map(
                t -> new TrackItem(t.getTrack().getId(), t.getTrack().getName(), t.getTrackOrder()))
            .toList();

    List<CodeItem> interests =
        userInterestRepository.findByUserIdOrderByIdAsc(userId).stream()
            .map(i -> new CodeItem(i.getInterest().getId(), i.getInterest().getCode()))
            .toList();

    List<CodeItem> devFields =
        userDevFieldRepository.findByUserIdOrderByIdAsc(userId).stream()
            .map(d -> new CodeItem(d.getDevField().getId(), d.getDevField().getCode()))
            .toList();

    List<CodeItem> companyTypes =
        userCompanyTypeRepository.findByUserIdOrderByIdAsc(userId).stream()
            .map(c -> new CodeItem(c.getCompanyType().getId(), c.getCompanyType().getCode()))
            .toList();

    List<CodeItem> workValues =
        userWorkValueRepository.findByUserIdOrderByIdAsc(userId).stream()
            .map(w -> new CodeItem(w.getWorkValue().getId(), w.getWorkValue().getCode()))
            .toList();

    List<TechStackItem> techStacks =
        userTechStackRepository.findByUserIdOrderByIdAsc(userId).stream()
            .map(t -> new TechStackItem(t.getTechStack().getId(), t.getTechStack().getName()))
            .toList();

    List<String> techStackCustoms =
        userTechStackCustomRepository.findByUserIdOrderByIdAsc(userId).stream()
            .map(c -> c.getLabel())
            .toList();

    List<CompletedSubjectItem> completedSubjects =
        userCompletedSubjectRepository.findByUserIdOrderByYearAscSemesterAsc(userId).stream()
            .map(
                cs ->
                    new CompletedSubjectItem(
                        cs.getSubject().getId(),
                        cs.getSubject().getName(),
                        cs.getYear(),
                        cs.getSemester()))
            .toList();

    return new ProfileResponse(
        summary,
        tracks,
        interests,
        devFields,
        companyTypes,
        workValues,
        techStacks,
        techStackCustoms,
        completedSubjects);
  }
}
