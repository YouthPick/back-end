# IntelliJ 코드 스타일 설정

CI는 Spotless의 `googleJavaFormat().aosp()`(4-space 들여쓰기, 8-space continuation)로 포맷을 강제한다. IntelliJ 기본 스타일은 이와 달라, 이 스킴을 import해 두면 IDE에서 저장/Reformat 결과가 `./gradlew spotlessCheck`를 그대로 통과한다.

## Import 방법

1. `Settings/Preferences → Editor → Code Style → Java`
2. 스킴 선택 옆 톱니바퀴(⚙️) → **Import Scheme → IntelliJ IDEA code style XML**
3. [`intellij-java-google-aosp-style.xml`](./intellij-java-google-aosp-style.xml) 선택
4. 스킴 이름 `GoogleStyleAOSP` 확인 후 적용(Apply)

## 저장 시 자동 포맷 (선택)

- `Settings/Preferences → Tools → Actions on Save`에서 **Reformat code**, **Optimize imports** 체크

## 확인

import 후 아무 Java 파일을 `⌥⌘L`(Reformat)로 정리한 뒤 다음이 통과하면 정상이다.

```bash
./gradlew spotlessCheck
```

## 참고

- 이 XML은 [google/styleguide](https://github.com/google/styleguide)의 `intellij-java-google-style.xml`을 기반으로, Java 들여쓰기를 AOSP 규칙(4/8-space)에 맞게 수정한 것이다.
- 포맷의 최종 기준(source of truth)은 CI의 Spotless이다. 스킴과 결과가 어긋나면 `./gradlew spotlessApply`로 맞춘다.
