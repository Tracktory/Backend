package com.hansung.tracktory.domain.profile.service;

import com.hansung.tracktory.domain.catalog.career.repository.TechStackRepository;
import com.hansung.tracktory.domain.catalog.classification.repository.CompanyTypeRepository;
import com.hansung.tracktory.domain.catalog.classification.repository.DevFieldRepository;
import com.hansung.tracktory.domain.catalog.classification.repository.InterestRepository;
import com.hansung.tracktory.domain.catalog.classification.repository.WorkValueRepository;
import com.hansung.tracktory.domain.catalog.organization.entity.Department;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.catalog.organization.repository.DepartmentRepository;
import com.hansung.tracktory.domain.catalog.organization.repository.TrackRepository;
import com.hansung.tracktory.domain.profile.dto.OnboardingRequest.ProfileRequest;
import com.hansung.tracktory.domain.profile.dto.OnboardingRequest.TrackRequest;
import com.hansung.tracktory.domain.profile.entity.UserCompanyType;
import com.hansung.tracktory.domain.profile.entity.UserDevField;
import com.hansung.tracktory.domain.profile.entity.UserInterest;
import com.hansung.tracktory.domain.profile.entity.UserProfile;
import com.hansung.tracktory.domain.profile.entity.UserTechStack;
import com.hansung.tracktory.domain.profile.entity.UserTechStackCustom;
import com.hansung.tracktory.domain.profile.entity.UserTrack;
import com.hansung.tracktory.domain.profile.entity.UserWorkValue;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

/** 온보딩 요청을 카탈로그 검증과 함께 프로필 엔티티로 조립 / 저장은 호출 측(서비스) 책임 */
@Component
@RequiredArgsConstructor
public class ProfileAssembler {

  private final DepartmentRepository departmentRepository;
  private final TrackRepository trackRepository;
  private final InterestRepository interestRepository;
  private final DevFieldRepository devFieldRepository;
  private final CompanyTypeRepository companyTypeRepository;
  private final WorkValueRepository workValueRepository;
  private final TechStackRepository techStackRepository;

  public UserProfile profile(User user, ProfileRequest req) {
    Department department =
        departmentRepository
            .findById(req.getDepartmentId())
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.VALIDATION_FAILED, "profile.departmentId 가 존재하지 않습니다."));
    return UserProfile.of(user, department, req.getCurrentYear(), req.getName());
  }

  public List<UserTrack> tracks(User user, List<TrackRequest> reqs) {
    if (reqs == null || reqs.isEmpty()) {
      return List.of();
    }
    validateTrackOrders(reqs);
    List<Long> trackIds = reqs.stream().map(TrackRequest::getTrackId).toList();
    Map<Long, Track> trackMap =
        resolveAll(trackRepository, trackIds, "tracks.trackId").stream()
            .collect(Collectors.toMap(Track::getId, t -> t));
    return reqs.stream()
        .map(t -> UserTrack.of(user, trackMap.get(t.getTrackId()), t.getTrackOrder()))
        .toList();
  }

  public List<UserInterest> interests(User user, List<Long> ids) {
    return resolveAll(interestRepository, ids, "interestIds").stream()
        .map(interest -> UserInterest.of(user, interest))
        .toList();
  }

  public List<UserDevField> devFields(User user, List<Long> ids) {
    return resolveAll(devFieldRepository, ids, "devFieldIds").stream()
        .map(devField -> UserDevField.of(user, devField))
        .toList();
  }

  public List<UserCompanyType> companyTypes(User user, List<Long> ids) {
    return resolveAll(companyTypeRepository, ids, "companyTypeIds").stream()
        .map(companyType -> UserCompanyType.of(user, companyType))
        .toList();
  }

  public List<UserWorkValue> workValues(User user, List<Long> ids) {
    return resolveAll(workValueRepository, ids, "workValueIds").stream()
        .map(workValue -> UserWorkValue.of(user, workValue))
        .toList();
  }

  public List<UserTechStack> techStacks(User user, List<Long> ids) {
    return resolveAll(techStackRepository, ids, "techStackIds").stream()
        .map(techStack -> UserTechStack.of(user, techStack))
        .toList();
  }

  public List<UserTechStackCustom> techStackCustoms(User user, List<String> labels) {
    if (labels == null || labels.isEmpty()) {
      return List.of();
    }
    return labels.stream()
        .filter(label -> label != null && !label.isBlank())
        .map(String::trim)
        .distinct()
        .map(label -> UserTechStackCustom.of(user, label))
        .toList();
  }

  // ------------------------------ 메서드 ------------------------------

  private void validateTrackOrders(List<TrackRequest> tracks) {
    boolean hasPrimary =
        tracks.stream().anyMatch(t -> t.getTrackOrder() != null && t.getTrackOrder() == 1);
    if (!hasPrimary) {
      throw new BusinessException(ErrorCode.VALIDATION_FAILED, "tracks: 1트랙은 필수입니다.");
    }
    long distinctOrders = tracks.stream().map(TrackRequest::getTrackOrder).distinct().count();
    if (distinctOrders != tracks.size()) {
      throw new BusinessException(ErrorCode.VALIDATION_FAILED, "tracks.trackOrder 가 중복되었습니다.");
    }
    long distinctTrackIds = tracks.stream().map(TrackRequest::getTrackId).distinct().count();
    if (distinctTrackIds != tracks.size()) {
      throw new BusinessException(ErrorCode.VALIDATION_FAILED, "tracks.trackId 가 중복되었습니다.");
    }
  }

  // ID 목록으로 카탈로그 엔티티를 조회하고, 존재하지 않는 ID 가 있으면 VALIDATION_FAILED
  private <T> List<T> resolveAll(JpaRepository<T, Long> repository, List<Long> ids, String field) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    List<Long> distinctIds = ids.stream().distinct().toList();
    List<T> found = repository.findAllById(distinctIds);
    if (found.size() != distinctIds.size()) {
      throw new BusinessException(
          ErrorCode.VALIDATION_FAILED, field + " 에 존재하지 않는 ID 가 포함되어 있습니다.");
    }
    return found;
  }
}
