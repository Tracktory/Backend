package com.hansung.tracktory.domain.chatbot.service;

import com.hansung.tracktory.domain.catalog.organization.entity.Department;
import com.hansung.tracktory.domain.catalog.organization.repository.DepartmentRepository;
import com.hansung.tracktory.domain.chatbot.ai.ChatbotAiRequest.UserContext;
import com.hansung.tracktory.domain.chatbot.ai.ChatbotAiRequest.UserContext.CompletedSubject;
import com.hansung.tracktory.domain.profile.dto.ProfileResponse;
import com.hansung.tracktory.domain.profile.dto.ProfileResponse.CodeItem;
import com.hansung.tracktory.domain.profile.dto.ProfileResponse.TechStackItem;
import com.hansung.tracktory.domain.profile.dto.ProfileResponse.TrackItem;
import com.hansung.tracktory.domain.profile.service.ProfileQueryService;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 사용자 프로필·온보딩 정보를 AI 챗봇 요청용 user_context 로 조립 (프로필 없으면 404)
@Component
@RequiredArgsConstructor
public class ChatbotContextAssembler {

  private final ProfileQueryService profileQueryService;
  private final DepartmentRepository departmentRepository;

  @Transactional(readOnly = true)
  public UserContext assemble(Long userId) {
    ProfileResponse profile = profileQueryService.getMyProfile(userId);
    Department department =
        departmentRepository
            .findById(profile.profile().departmentId())
            .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));

    int grade = profile.profile().currentYear();

    List<String> techStacks =
        new ArrayList<>(profile.techStacks().stream().map(TechStackItem::name).toList());
    techStacks.addAll(profile.techStackCustoms());

    List<CompletedSubject> completedSubjects =
        profile.completedSubjects().stream()
            .map(cs -> new CompletedSubject(cs.name(), cs.year(), cs.semester()))
            .toList();

    return new UserContext(
        userId,
        profile.profile().name(),
        grade,
        grade,
        department.getCollege().getName(),
        department.getName(),
        profile.tracks().stream().map(TrackItem::name).toList(),
        profile.interests().stream().map(CodeItem::code).toList(),
        profile.devFields().stream().map(CodeItem::code).toList(),
        techStacks,
        profile.companyTypes().stream().map(CodeItem::code).toList(),
        profile.workValues().stream().map(CodeItem::code).toList(),
        completedSubjects);
  }
}
