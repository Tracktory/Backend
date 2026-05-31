package com.hansung.tracktory.domain.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CompletedSubjectServiceTest {

  @InjectMocks private CompletedSubjectService service;

  @Mock private UserRepository userRepository;
  @Mock private SubjectRepository subjectRepository;
  @Mock private UserCompletedSubjectRepository userCompletedSubjectRepository;

  private final User user = User.builder().email("a@b.com").passwordHash("x").build();

  @Test
  void add_success_returnsSavedRow() { // 정상: 저장 후 id/과목/학년/학기 반환
    Subject subject = mock(Subject.class);
    given(subject.getId()).willReturn(142L);
    given(subjectRepository.findById(142L)).willReturn(Optional.of(subject));
    given(userCompletedSubjectRepository.existsByUserIdAndSubjectId(1L, 142L)).willReturn(false);
    given(userRepository.getReferenceById(1L)).willReturn(user);

    UserCompletedSubject saved = UserCompletedSubject.of(user, subject, 2, 1);
    ReflectionTestUtils.setField(saved, "id", 87L);
    given(userCompletedSubjectRepository.save(any())).willReturn(saved);

    CompletedSubjectResponse result = service.add(1L, addRequest(142L, 2, 1));

    assertThat(result.id()).isEqualTo(87L);
    assertThat(result.subjectId()).isEqualTo(142L);
    assertThat(result.year()).isEqualTo(2);
    assertThat(result.semester()).isEqualTo(1);
  }

  @Test
  void add_subjectNotFound_throws422() { // 없는 과목 ID면 422
    given(subjectRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.add(1L, addRequest(99L, 1, 1)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED));
  }

  @Test
  void add_duplicate_throws409() { // 이미 이수한 과목이면 409
    Subject subject = mock(Subject.class);
    given(subjectRepository.findById(142L)).willReturn(Optional.of(subject));
    given(userCompletedSubjectRepository.existsByUserIdAndSubjectId(1L, 142L)).willReturn(true);

    assertThatThrownBy(() -> service.add(1L, addRequest(142L, 2, 1)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SUBJECT_ALREADY_COMPLETED));
  }

  @Test
  void delete_success_returnsDeletedId() { // 정상 삭제 시 삭제된 row id 반환
    given(userCompletedSubjectRepository.deleteByUserIdAndId(1L, 87L)).willReturn(1L);

    CompletedSubjectDeleteResponse result = service.delete(1L, 87L);

    assertThat(result.deletedId()).isEqualTo(87L);
  }

  @Test
  void delete_notFound_throws404() { // 해당 row 없음(또는 타 사용자 소유)이면 404
    given(userCompletedSubjectRepository.deleteByUserIdAndId(1L, 99L)).willReturn(0L);

    assertThatThrownBy(() -> service.delete(1L, 99L))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
  }

  // ------------------------------ helpers ------------------------------

  private static CompletedSubjectAddRequest addRequest(
      Long subjectId, Integer year, Integer semester) {
    CompletedSubjectAddRequest r = new CompletedSubjectAddRequest();
    ReflectionTestUtils.setField(r, "subjectId", subjectId);
    ReflectionTestUtils.setField(r, "year", year);
    ReflectionTestUtils.setField(r, "semester", semester);
    return r;
  }
}
