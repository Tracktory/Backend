package com.hansung.tracktory.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TrackCodeNormalizerTest {

  // AI 카탈로그가 쓰는 가운뎃점 / 본 백엔드 카탈로그가 쓰는 가운뎃점. 시각적으로 동일해 코드 포인트로 명시한다.
  private static final char AI_MIDDLE_DOT = 0x00B7; // MIDDLE DOT
  private static final char CATALOG_MIDDLE_DOT = 0x318D; // HANGUL LETTER ARAEA

  @Test
  void toCatalogForm_foldsAiMiddleDotToCatalogForm() {
    String aiCode = "디지털콘텐츠" + AI_MIDDLE_DOT + "가상현실트랙";
    String catalogCode = "디지털콘텐츠" + CATALOG_MIDDLE_DOT + "가상현실트랙";

    String result = TrackCodeNormalizer.toCatalogForm(aiCode);

    assertThat(result).isEqualTo(catalogCode);
    assertThat(result).doesNotContain(String.valueOf(AI_MIDDLE_DOT));
  }

  @Test
  void toCatalogForm_withoutMiddleDot_returnsSameInstance() {
    String plain = "빅데이터트랙";

    assertThat(TrackCodeNormalizer.toCatalogForm(plain)).isSameAs(plain);
  }

  @Test
  void toCatalogForm_alreadyCatalogForm_isUnchanged() {
    String catalogCode = "회계" + CATALOG_MIDDLE_DOT + "재무경영트랙";

    assertThat(TrackCodeNormalizer.toCatalogForm(catalogCode)).isEqualTo(catalogCode);
  }

  @Test
  void toCatalogForm_foldsMultipleMiddleDotVariants() {
    String mixed =
        "A" + (char) 0x30FB + "B" + (char) 0x2027 + "C"; // KATAKANA MIDDLE DOT, HYPHENATION POINT
    String expected = "A" + CATALOG_MIDDLE_DOT + "B" + CATALOG_MIDDLE_DOT + "C";

    assertThat(TrackCodeNormalizer.toCatalogForm(mixed)).isEqualTo(expected);
  }

  @Test
  void toCatalogForm_null_returnsNull() {
    assertThat(TrackCodeNormalizer.toCatalogForm(null)).isNull();
  }
}
