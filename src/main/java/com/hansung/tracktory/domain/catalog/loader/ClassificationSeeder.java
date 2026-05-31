package com.hansung.tracktory.domain.catalog.loader;

import com.hansung.tracktory.domain.catalog.classification.entity.CompanyType;
import com.hansung.tracktory.domain.catalog.classification.entity.DevField;
import com.hansung.tracktory.domain.catalog.classification.entity.Interest;
import com.hansung.tracktory.domain.catalog.classification.entity.WorkValue;
import com.hansung.tracktory.domain.catalog.classification.repository.CompanyTypeRepository;
import com.hansung.tracktory.domain.catalog.classification.repository.DevFieldRepository;
import com.hansung.tracktory.domain.catalog.classification.repository.InterestRepository;
import com.hansung.tracktory.domain.catalog.classification.repository.WorkValueRepository;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.ClassificationData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 온보딩 분류 taxonomy(관심사/개발분야/회사유형/가치관)를 code 기준 멱등으로 적재한다. */
@Service
@RequiredArgsConstructor
public class ClassificationSeeder {

  private final InterestRepository interestRepository;
  private final DevFieldRepository devFieldRepository;
  private final CompanyTypeRepository companyTypeRepository;
  private final WorkValueRepository workValueRepository;

  @Transactional
  public void seed(ClassificationData data) {
    for (String code : data.interests()) {
      if (!interestRepository.existsByCode(code)) {
        interestRepository.save(Interest.builder().code(code).build());
      }
    }
    for (String code : data.devFields()) {
      if (!devFieldRepository.existsByCode(code)) {
        devFieldRepository.save(DevField.builder().code(code).build());
      }
    }
    for (String code : data.companyTypes()) {
      if (!companyTypeRepository.existsByCode(code)) {
        companyTypeRepository.save(CompanyType.builder().code(code).build());
      }
    }
    for (String code : data.workValues()) {
      if (!workValueRepository.existsByCode(code)) {
        workValueRepository.save(WorkValue.builder().code(code).build());
      }
    }
  }
}
