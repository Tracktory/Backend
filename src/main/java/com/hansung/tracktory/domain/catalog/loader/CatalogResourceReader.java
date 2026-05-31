package com.hansung.tracktory.domain.catalog.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** 클래스패스의 카탈로그 기준 데이터(YAML/JSON)를 안전하게 읽어 파싱 결과 맵으로 반환한다. */
@Component
public class CatalogResourceReader {

  /**
   * 클래스패스 리소스를 SnakeYAML(SafeConstructor)로 파싱한다. JSON 은 YAML 의 부분집합이라 동일 파서로 처리된다.
   *
   * @param classpathLocation 예: {@code "catalog/tracks.yaml"}
   * @return 최상위가 맵인 파싱 결과
   * @throws IllegalStateException 리소스가 없거나 비어 있을 때
   */
  public Map<String, Object> readAsMap(String classpathLocation) {
    LoaderOptions options = new LoaderOptions();
    options.setCodePointLimit(Integer.MAX_VALUE);
    Yaml yaml = new Yaml(new SafeConstructor(options));
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(classpathLocation)) {
      if (in == null) {
        throw new IllegalStateException("카탈로그 리소스를 찾을 수 없습니다: " + classpathLocation);
      }
      Map<String, Object> parsed = yaml.load(in);
      if (parsed == null) {
        throw new IllegalStateException("카탈로그 리소스가 비어 있습니다: " + classpathLocation);
      }
      return parsed;
    } catch (IOException e) {
      throw new UncheckedIOException("카탈로그 리소스 읽기 실패: " + classpathLocation, e);
    }
  }
}
