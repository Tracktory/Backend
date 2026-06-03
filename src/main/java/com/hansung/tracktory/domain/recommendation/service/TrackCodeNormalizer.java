package com.hansung.tracktory.domain.recommendation.service;

/**
 * AI 중계 서버가 보내는 트랙 식별자를 카탈로그가 저장한 정규 형태로 맞추는 anti-corruption 정규화.
 *
 * <p>두 저장소가 가운뎃점을 서로 다른 유니코드로 적재한다: AI 카탈로그는 MIDDLE DOT(U+00B7)을, 본 백엔드 카탈로그는 HANGUL LETTER
 * ARAEA(U+318D)를 쓴다. 글자 모양이 사실상 같아 눈으로는 구분되지 않지만 코드 포인트가 달라 코드 동등 비교가 실패하고, 가운뎃점을 포함한 트랙이 매핑에서 조용히
 * 누락된다. 외부 식별자의 가운뎃점 변형을 카탈로그 정규형으로 접어 매핑이 성립하게 한다.
 *
 * <p>시각적으로 구분되지 않는 코드 포인트들이라 가운뎃점은 문자 리터럴 대신 정수 코드 포인트로 다룬다.
 */
final class TrackCodeNormalizer {

  /** 카탈로그가 트랙 code 에 쓰는 가운뎃점(HANGUL LETTER ARAEA, U+318D). */
  private static final char CATALOG_MIDDLE_DOT = 0x318D;

  private TrackCodeNormalizer() {}

  /**
   * 트랙 식별자의 가운뎃점 변형을 카탈로그 정규형(U+318D)으로 치환한다. 변형이 없으면 입력 인스턴스를 그대로 돌려준다.
   *
   * @param trackId AI 중계 서버가 보낸 트랙 식별자(널 허용)
   * @return 카탈로그 정규형으로 맞춘 식별자, 입력이 널이면 널
   */
  static String toCatalogForm(String trackId) {
    if (trackId == null) {
      return null;
    }
    StringBuilder normalized = null;
    for (int i = 0; i < trackId.length(); i++) {
      if (isMiddleDotVariant(trackId.charAt(i))) {
        if (normalized == null) {
          normalized = new StringBuilder(trackId);
        }
        normalized.setCharAt(i, CATALOG_MIDDLE_DOT);
      }
    }
    return normalized == null ? trackId : normalized.toString();
  }

  /** 카탈로그 정규형과 같은 글리프로 렌더되는 가운뎃점 변형인지 정수 코드 포인트로 판정한다. */
  private static boolean isMiddleDotVariant(char c) {
    return c == 0x00B7 // MIDDLE DOT (AI 카탈로그가 실제 사용)
        || c == 0x0387 // GREEK ANO TELEIA
        || c == 0x2027 // HYPHENATION POINT
        || c == 0x30FB // KATAKANA MIDDLE DOT
        || c == 0xFF65; // HALFWIDTH KATAKANA MIDDLE DOT
  }
}
