---
paths:
  - "src/main/java/**"
---

# Lombok

보일러플레이트 축소 용도로만 제한적으로 사용한다.

**허용**

- `@RequiredArgsConstructor` + `final` 필드 (생성자 주입 기본형)
- JPA Entity의 `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- 에러코드 enum, 커스텀 예외처럼 단순 getter만 필요한 타입의 `@Getter`
- DTO는 Lombok 대신 `record`를 우선 사용한다.

**금지**

- `@Data`
- Entity의 `@Setter`
- Entity의 `@AllArgsConstructor` / public all-args constructor
- 양방향 연관관계 Entity의 무분별한 `@ToString` / `@EqualsAndHashCode`
