package com.hansung.tracktory.domain.profile.service;

import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.profile.dto.CompletedSubjectAddRequest;
import com.hansung.tracktory.domain.profile.dto.CompletedSubjectDeleteResponse;
import com.hansung.tracktory.domain.profile.dto.CompletedSubjectResponse;
import com.hansung.tracktory.domain.profile.entity.UserCompletedSubject;
import com.hansung.tracktory.domain.profile.repository.UserCompletedSubjectRepository;
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
public class CompletedSubjectService {

  private final UserRepository userRepository;
  private final SubjectRepository subjectRepository;
  private final UserCompletedSubjectRepository userCompletedSubjectRepository;

  @Transactional
  public CompletedSubjectResponse add(Long userId, CompletedSubjectAddRequest request) {
    Subject subject =
        subjectRepository
            .findByName(request.getSubjectName())
            .orElseThrow(
                () ->
                    new BusinessException(ErrorCode.VALIDATION_FAILED, "subjectName 이 존재하지 않습니다."));

    if (userCompletedSubjectRepository.existsByUserIdAndSubjectId(userId, subject.getId())) {
      throw new BusinessException(ErrorCode.SUBJECT_ALREADY_COMPLETED);
    }

    User user = userRepository.getReferenceById(userId);
    UserCompletedSubject saved =
        userCompletedSubjectRepository.save(
            UserCompletedSubject.of(user, subject, request.getYear(), request.getSemester()));

    return CompletedSubjectResponse.from(saved);
  }

  @Transactional
  public CompletedSubjectDeleteResponse delete(Long userId, Long id) {
    long deleted = userCompletedSubjectRepository.deleteByUserIdAndId(userId, id);
    if (deleted == 0) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 이수 과목이 존재하지 않습니다.");
    }
    return CompletedSubjectDeleteResponse.of(id);
  }
}
