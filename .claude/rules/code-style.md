---
paths:
  - "src/**"
---

# 코드 포맷 (Google Java Style, AOSP)

- 포맷 기준(source of truth)은 `build.gradle`의 Spotless(`googleJavaFormat().aosp()`)다. 4-space 들여쓰기, 8-space continuation indent.
- 코드를 작성/수정한 뒤 `./gradlew spotlessApply`로 정리하고, 완료를 주장하기 전 `./gradlew spotlessCheck`가 통과하는지 확인한다(CI가 이를 강제한다).
- import는 와일드카드 없이 개별 import, 미사용 import는 제거한다(Spotless `removeUnusedImports()`가 자동 처리하지만 직접 작성 시에도 지킨다).
- IntelliJ에서 사람이 직접 편집할 때는 [`docs/ide/intellij-java-google-aosp-style.xml`](../../docs/ide/intellij-java-google-aosp-style.xml) 스킴(`docs/ide/README.md` 참고)을 import해 저장 시 자동 포맷을 맞춘다. 에이전트는 이 XML을 직접 파싱할 필요 없이 위 Gradle 명령으로 충분하다.
