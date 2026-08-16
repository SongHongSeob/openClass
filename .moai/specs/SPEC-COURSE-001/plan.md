---
id: SPEC-COURSE-001
title: "강좌 엔티티·카탈로그 조회 및 관리자 강좌 관리 — 구현 계획"
version: "0.1.2"
status: draft
created: 2026-08-15
updated: 2026-08-16
author: manager-spec
priority: P0
phase: "v1.0.0"
module: "src/main/java/com/hongseob/openclass_ap/course"
lifecycle: spec-anchored
tags: "course, catalog, admin, plan"
tier: M
---

# SPEC-COURSE-001 — 구현 계획 (plan)

## HISTORY

| 버전 | 날짜 | 작성자 | 변경 내용 |
|---|---|---|---|
| 0.1.0 | 2026-08-15 | manager-spec | 최초 작성 (draft) |
| 0.1.1 | 2026-08-15 | manager-spec | 2차 감사 지적 반영 — `status` DB 제약 추가(§C.1), TDD를 프로세스 제약으로 §D에 이관 |
| 0.1.2 | 2026-08-16 | manager-spec | plan-auditor 1회차 FAIL(0.75) 결함 D1~D10 수정. plan.md 해당분: D1(M2에 `SecurityConfig` 공개 경로 확장 태스크 신설 — 현재 매처는 `/api/courses` 정확 일치라 상세 경로가 인증을 요구함), D2(CHECK 제약·컬럼 기본값의 생성 수단을 Hibernate `@Check`/`@ColumnDefault`로 명시 + `ddl-auto=update`의 제약 미반영 한계를 §D에 기록), **D3(ArchUnit 도입 안 함 — 아래 결정 근거 참조)**, D5(정원 축소 거부를 409 단일 값으로 확정), D6(`CourseNotFoundException` + 기존 `GlobalExceptionHandler` 확장 명시), D8(§C.4에 `member/` 코드 규약 3종 고정 — record DTO / private 생성자 + 정적 팩토리 / `AbstractIntegrationTest` 상속), D10(`updated_at` 컬럼 제거 — 아래 결정 근거 참조) |

### 0.1.2 결정 근거 (감사 지적이 선택지를 남긴 항목)

- **D3 — ArchUnit: 도입하지 않고 AC-CRS-004에서 제거한다 (감사가 제시한 3안 중 (b)).**
  근거 셋: (1) §D의 "신규 인프라 추가 금지"는 **사용자 명시적 결정**이므로, 이 SPEC이 스스로 예외 조항을 만들어 우회하는 것은 결정을 뒤집는 행위다. (2) 이 SPEC에서 AC-CRS-004가 검증할 명제는 "`enrolled_count`를 바꾸는 프로덕션 경로가 **0건**"이라는 부재 명제이며, 정적 검색(grep/ast-grep) + DB 상태 단언 2계층으로 충분히 관찰된다 — ArchUnit이 필요한 것은 "**어느 패키지만** 접근 가능한가" 같은 패키지 스코프 규칙이고 그건 이 SPEC의 명제가 아니다. (3) 도구를 실제로 필요로 하는 쪽은 `SPEC-ENROLLMENT-001`(워커 패키지 한정 규칙)이므로, 그쪽에서 도입하는 편이 도구와 필요가 일치한다.
  교차 SPEC 정합성: `SPEC-ENROLLMENT-001` plan.md §E 사전 점검 6번("ArchUnit 의존성이 추가되어 있다")은 **전제 확인 항목**이지 "COURSE-001이 추가한다"는 서술이 아니므로, 이 결정과 모순되지 않는다. 다만 **누가 추가하는지가 미지정 상태**이므로 §H에 교차 SPEC 인수인계 항목으로 명시한다 (해당 SPEC 파일은 이 개정에서 수정하지 않는다).
- **D10 — `updated_at`: 스키마 표에서 제거한다.**
  근거: 어떤 요구사항(REQ-CRS-001)도 어떤 인수 기준(AC-CRS-001)도 이 컬럼을 요구하지 않는다. 유지하려면 `@UpdateTimestamp`/`@PreUpdate` 같은 갱신 수단이 함께 필요한데, 이는 검증되지 않는 기계 장치를 늘리는 일이다. 선행 SPEC이 만든 `Member` 엔티티도 `created_at`만 두고 `updated_at`이 없어 코드베이스 선례와도 일치한다. 관리자 감사 이력이 실제로 필요해지면 그때 별도 SPEC에서 근거와 AC를 함께 도입한다.

> **읽는 순서 안내**: §C는 **되돌리기 어려운 순서**로 배치했다. §C.1(스키마)이 가장 비싸고, 아래로 갈수록 변경 비용이 낮다.

---

## §A 배경 (Context)

- 3분할된 SPEC 중 **두 번째**다. 실행 순서: `SPEC-AUTH-001` → **`SPEC-COURSE-001`** → `SPEC-ENROLLMENT-001`.
- **선행 의존: `SPEC-AUTH-001`** (관리자 엔드포인트 인가에 JWT 필터 체인과 `ADMIN` 역할이 필요하다). 선행 SPEC이 `completed`가 되기 전에는 run 단계에 진입하지 않는다.
- `SPEC-AUTH-001`이 만든 `common/config`, `common/exception`, `common/response` 골격을 그대로 사용한다. 재설계하지 않는다.
- 미해소 클래리피케이션 마커: **0건**.

### A.1 이 SPEC의 핵심 책임 — 정원 스키마의 소유권 경계

이 SPEC이 `course.enrolled_count` 컬럼과 그 CHECK 제약을 만든다. 그러나 **값을 바꾸는 코드는 만들지 않는다.** 변경 권한은 `SPEC-ENROLLMENT-001`의 큐 워커가 단독으로 갖는다 (REQ-CRS-004 / INV-CRS-003).

이 경계를 지금 문서에 못 박고 AC-CRS-004로 기계 검증하는 이유는, 선착순 정합성의 1차 방어선이 "확정 인원을 바꾸는 코드 경로가 단 하나뿐"이라는 **구조적 성질**이기 때문이다. 강좌 서비스에 "편의상" 카운터 조정 메서드가 하나라도 생기면 그 방어선은 그 순간 사라진다. 나중에 지우기보다 처음부터 만들지 않는 편이 싸다.

---

## §B 범위 (Scope)

| 포함 | 제외 (다른 SPEC 소유) |
|---|---|
| `Course` 엔티티 + `capacity` / `enrolled_count` + CHECK 제약 | 회원·인증 → `SPEC-AUTH-001` |
| 공개 카탈로그 목록·상세 API | 큐·워커·대기명단·취소 → `SPEC-ENROLLMENT-001` |
| 관리자 강좌 생성·수정·마감·삭제(soft) API | 정원 증설 시 대기자 승격 → `SPEC-ENROLLMENT-001` |
| 페이지네이션 | 프론트엔드 화면 → `SPEC-FRONTEND-001` (**아직 생성하지 않음**) |

---

## §C 기술 설계 (되돌리기 어려운 순)

### C.1 데이터 모델 — 최우선

| 테이블 | 컬럼 | 제약 |
|---|---|---|
| `course` | `id` (BIGSERIAL), `title`, `description`, `capacity`, `enrolled_count`, `starts_at`, `ends_at`, `status`, `created_at` | `CHECK (capacity >= 1)`, `CHECK (enrolled_count >= 0 AND enrolled_count <= capacity)`, **`CHECK (status IN ('OPEN','CLOSED'))`**, `enrolled_count` NOT NULL DEFAULT 0, `status` NOT NULL DEFAULT `'OPEN'` |

> `updated_at`은 두지 않는다 — HISTORY 0.1.2 결정 근거(D10) 참조. 컬럼 목록은 spec.md REQ-CRS-001의 속성 목록 및 acceptance.md AC-CRS-001의 검증 대상과 정확히 일치한다.

#### C.1.1 제약·기본값의 생성 수단 (D2 — 반드시 명시적으로 지정할 것)

이 프로젝트에는 **마이그레이션 도구가 없다** (Flyway·Liquibase 없음, `schema.sql` 없음). 스키마는 전적으로 Hibernate `ddl-auto`로 생성된다 (local 프로파일 `update`, 통합 테스트 `create-drop` — `AbstractIntegrationTest`가 `create-drop`을 강제한다). 따라서 **평범한 JPA 애노테이션만으로는 위 CHECK 제약이 생성되지 않는다.** 생성 수단을 아래와 같이 못 박는다.

| 대상 | 생성 수단 | 비고 |
|---|---|---|
| `CHECK (capacity >= 1)` | `Course` 엔티티 클래스에 `@org.hibernate.annotations.Check(name = "ck_course_capacity_min", constraints = "capacity >= 1")` | AC-CRS-002가 우회 INSERT로 실제 동작을 검증한다 |
| `CHECK (enrolled_count >= 0 AND enrolled_count <= capacity)` | 동일 — `@Check(name = "ck_course_enrolled_range", constraints = "enrolled_count >= 0 AND enrolled_count <= capacity")` | AC-CRS-003 |
| `CHECK (status IN ('OPEN','CLOSED'))` | 동일 — `@Check(name = "ck_course_status", constraints = "status IN ('OPEN','CLOSED')")` | AC-CRS-005. PostgreSQL enum 타입 대신 CHECK를 쓰는 이유는 §C.1 설계 판단 0번 참조 |
| `enrolled_count NOT NULL DEFAULT 0` | 필드에 `@Column(nullable = false)` + `@org.hibernate.annotations.ColumnDefault("0")` | DDL의 DEFAULT 절은 `@ColumnDefault`가 없으면 생성되지 않는다 |
| `status NOT NULL DEFAULT 'OPEN'` | 필드에 `@Column(nullable = false)` + `@ColumnDefault("'OPEN'")` | 문자열 기본값은 SQL 리터럴 그대로 작성한다(따옴표 포함) |

클래스 레벨 `@Check`을 여러 개 붙일 때는 `@Checks({...})`로 묶는다. 제약 이름(`name`)을 명시하는 이유는, 이름이 없으면 Hibernate가 생성한 임의 이름이 붙어 테스트에서 실패 원인을 식별하기 어렵기 때문이다.

설계 판단:

0. **`status` 값 제약은 DB에도 건다** (REQ-CRS-005). `CHECK (status IN ('OPEN','CLOSED'))` 또는 PostgreSQL enum 타입 중 하나를 사용한다 — 둘 중 어느 쪽이든 애플리케이션을 우회한 직접 INSERT를 DB가 거부하면 요구사항을 만족한다. CHECK 쪽이 스키마 변경 비용이 낮아 기본 선택이다. 이 제약이 없으면 REQ-CRS-005의 "저장 shall not"이 애플리케이션 계층에서만 지켜지는 상태가 되어, 형제 요구사항 REQ-CRS-002/003(둘 다 DB 제약으로 검증)과 방어선 깊이가 어긋난다. AC-CRS-005가 우회 INSERT로 이를 검증한다.

1. **`enrolled_count`는 비정규화 카운터다.** `enrollment` 행을 매번 세는 대신 카운터를 유지하는 이유는, `SPEC-ENROLLMENT-001`의 워커가 정원 검사와 증가를 **한 트랜잭션 안에서 원자적으로** 수행할 수 있어야 하고, DB CHECK 제약이 그 원자성의 최종 방어선이 되어야 하기 때문이다. `COUNT(*)`로는 CHECK 제약을 걸 수 없다.
2. **잔여 정원은 저장하지 않는다.** `capacity - enrolled_count`로 조회 시 계산한다. 저장하면 동기화해야 할 상태가 하나 늘어난다 (AC-CAT-002가 컬럼 부재를 검증한다).
3. **`status`는 문자열 + Java enum.** 별도 상태 이력 테이블을 만들지 않는다.
4. **삭제는 항상 soft**다. 물리 삭제 경로를 아예 만들지 않으므로 "확정자가 있으면 삭제 금지" 같은 분기 자체가 불필요하다 (단순화). AC-ADM-007이 정적 검색으로 `delete` 호출 부재를 확인한다.

### C.2 API 계약

| 메서드 | 경로 | 인가 | 비고 |
|---|---|---|---|
| GET | `/api/courses` | 공개 | 페이지네이션 (`page`, `size`) |
| GET | `/api/courses/{id}` | 공개 | 잔여 정원 포함 |
| POST | `/api/admin/courses` | `ADMIN` | 생성 |
| PATCH | `/api/admin/courses/{id}` | `ADMIN` | 강좌명·설명·정원·일정 수정 |
| POST | `/api/admin/courses/{id}/close` | `ADMIN` | 마감 전이 |
| DELETE | `/api/admin/courses/{id}` | `ADMIN` | **soft** — 내부적으로 마감 전이 |

`/api/admin/**` 경로 인가는 `SPEC-AUTH-001`의 `SecurityConfig`가 이미 처리한다. 이 SPEC은 인가 규칙을 새로 만들지 않고, 결과(403)만 AC-ADM-002로 검증한다.

#### C.2.1 공개 경로 매처 확장 — 상세 조회 (D2·D1 결함 수정)

현재 `SecurityConfig`의 공개 매처는 다음 한 줄뿐이다.

```java
.requestMatchers(HttpMethod.GET, "/api/courses").permitAll()
```

이 매처는 **정확 경로 일치**이므로 `/api/courses/{id}`(상세)를 포함하지 **않는다**. 그대로 두면 `.anyRequest().authenticated()`에 걸려 상세 조회가 401을 반환하고 REQ-CAT-001·REQ-CAT-003이 깨진다. M2에서 아래와 같이 **경로만 추가**한다.

```java
.requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/*").permitAll()
```

- `/api/courses/*`(단일 세그먼트)를 명시하는 이유: `/api/courses/**`도 Spring Boot 3+의 `PathPatternParser`에서는 0개 세그먼트까지 매칭하므로 동작하지만, "0개 세그먼트도 매칭되는가"는 매처 구현체에 의존하는 미묘한 성질이다. 두 패턴을 명시적으로 나열하면 그 의존이 사라진다.
- 이 변경은 §G 안티패턴 "`SecurityConfig` 인가 규칙 재작성"에 **해당하지 않는다** — 규칙 구조(필터 체인·엔트리포인트·`hasRole`)는 건드리지 않고 공개 경로만 추가하는, 같은 항목이 허용한 범위다.
- 검증: AC-CAT-001(목록·비인증)과 AC-CAT-002(상세·비인증)가 `Authorization` 헤더 없이 각각 200을 요구한다.

### C.3 정원 축소 검증의 위치

REQ-ADM-005(확정 인원 미만 축소 거부)는 **애플리케이션 계층에서 검증**한다. DB CHECK 제약(`enrolled_count <= capacity`)이 이미 최종 방어선이지만, 제약 위반 예외를 그대로 500으로 흘리는 대신 명확한 상태 코드로 응답하기 위해서다. 두 계층 모두 필요하며, AC-ADM-004(애플리케이션)와 AC-CRS-003(DB)이 각각을 검증한다.

**응답 코드는 409 단일 값으로 확정한다 (D5).** 400과 409를 모두 허용하면 후속 `SPEC-FRONTEND-001` 소비자가 어느 쪽을 처리해야 할지 알 수 없는 모호한 계약이 된다. 409를 택한 이유는 이 코드베이스에 이미 선례가 있기 때문이다 — `GlobalExceptionHandler`는 입력 형식 오류(400)는 Spring `@Valid` 기본 처리에 위임하고, **도메인 규칙 위반**(중복 이메일)만 409로 매핑한다. "정원을 확정 인원 미만으로 줄일 수 없다"는 입력 형식이 아니라 도메인 규칙 위반이므로 같은 분류다. 반면 정원 1 미만 요청(REQ-ADM-004)은 값 자체가 유효하지 않은 입력 형식 오류이므로 400을 유지한다 — 두 코드의 경계가 기존 선례와 일치한다.

경계값에 주의: 정원을 **정확히 확정 인원과 같은 값**으로 축소하는 것은 **허용**이다 (`enrolled_count <= capacity`가 성립한다). AC-ADM-004가 이 경계를 명시적으로 테스트한다.

### C.4 패키지 구조 (SPEC-AUTH-001 골격 확장)

```
com.hongseob.openclass_ap
├── common/               # SPEC-AUTH-001에서 확정 — 재설계 금지
│   └── exception/        # CourseNotFoundException 추가 + GlobalExceptionHandler 확장 (아래 C.4.2)
├── member/               # SPEC-AUTH-001 소유
└── course/
    ├── Course.java, CourseStatus.java, CourseRepository.java
    ├── CourseService.java          # 조회 + 관리자 변경
    ├── CourseController.java       # 공개 조회
    ├── admin/CourseAdminController.java
    └── dto/                        # 전부 Java record (아래 C.4.1)
```

`admin`을 최상위 도메인 패키지로 두지 **않는다**. 관리자는 도메인이 아니라 **인가 관점**이며, 같은 `Course` 애그리게이트를 다루기 때문이다.

#### C.4.1 `member/`에서 이어받는 코드 규약 3종 (D8 — 배치뿐 아니라 코드 수준까지 고정)

패키지 **배치**만 `member/`를 따라가고 코드 규약이 갈라지면 두 도메인이 서로 다른 관례를 갖게 된다. 아래 3종은 배치가 아니라 코드 수준 규약이며 그대로 따른다.

1. **DTO는 전부 Java `record`.** `member/dto/`의 `SignupRequest`·`SignupResponse`·`LoginRequest`·`LoginResponse`가 모두 record다. `course/dto/`도 동일하게 record로 작성한다 (클래스 + Lombok `@Getter` 조합 금지).
2. **`Course` 엔티티는 `private` 생성자 + 정적 팩토리, 그리고 `enrolled_count`에 세터를 두지 않는다.**
   `Member`는 `private Member(...)` + `@NoArgsConstructor(access = PROTECTED)` + `createMember`/`createAdmin` 정적 팩토리로 구성되어 있고, 이는 **role 파라미터를 받는 경로 자체를 없애서** 권한 상승을 구조적으로 차단한 설계다 (`Member.java`의 주석이 이 의도를 명시한다). `Course`의 `enrolled_count`는 **정확히 같은 이유로 같은 방어가 필요하다** — 값을 바꾸는 메서드가 아예 존재하지 않아야 REQ-CRS-004 / INV-CRS-003이 "쓰지 않기로 한 약속"이 아니라 **구조적 성질**이 된다. 따라서:
   - `@NoArgsConstructor(access = AccessLevel.PROTECTED)` + `private Course(...)` 생성자
   - 유일한 생성 진입점은 정적 팩토리 `Course.create(...)` — `enrolled_count`를 **파라미터로 받지 않고** 항상 0으로 시작한다
   - 클래스에 `@Setter`를 붙이지 않는다. 관리자 수정이 필요한 필드(강좌명·설명·정원·일정·상태)는 의도가 드러나는 도메인 메서드(`updateDetails(...)`, `close()` 등)로만 변경하고, **`enrolled_count`를 변경하는 메서드는 어떤 이름으로도 만들지 않는다**
   - AC-CRS-004의 정적 검색은 이 설계가 지켜졌을 때 자연히 0건이 된다
3. **통합 테스트는 `support.AbstractIntegrationTest`를 상속한다.** 이 베이스 클래스가 Testcontainers PostgreSQL 기동 + `@ServiceConnection` 주입 + `ddl-auto=create-drop`을 담당한다. 새 컨테이너 설정을 각 테스트에 복제하지 않는다 (§D의 "H2 대체 금지"가 실제로 지켜지는 지점이다).

#### C.4.2 404 처리 — 신규 예외 + 기존 핸들러 확장 (D6)

REQ-CAT-004(존재하지 않는 강좌 조회)와 REQ-ADM-009(존재하지 않는 강좌 변경 요청)는 404를 요구하지만, 현재 `GlobalExceptionHandler`에는 409(`DuplicateEmailException`)와 401(`InvalidCredentialsException`) 핸들러만 있고 **404 매핑도, 그에 대응하는 예외 타입도 없다.** 따라서 다음을 추가한다.

- `common/exception/CourseNotFoundException.java` — 신규 도메인 예외
- **기존 `GlobalExceptionHandler`에 핸들러 메서드 추가** — `@ExceptionHandler(CourseNotFoundException.class)` → `HttpStatus.NOT_FOUND` + `ErrorResponse.of("COURSE_NOT_FOUND", ...)`

**새 `@RestControllerAdvice` 클래스를 만들지 않는다.** 어드바이스가 둘 이상이면 예외-상태 코드 매핑이 두 파일에 흩어지고 우선순위 문제가 생긴다. 기존 클래스에 메서드를 추가하는 것이 `SPEC-AUTH-001`이 세운 패턴이며, 응답 바디도 같은 `ErrorResponse.of(code, message)` 형식을 유지한다.

### C.5 프론트엔드 — 이 SPEC 범위 밖

- 사용자 결정(**백엔드 우선**)에 따라 React 스캐폴딩을 생성하지 않는다.
- 모든 인수 기준은 Spring Boot 테스트만으로 검증 가능하며, AC-NFR-002가 이를 기계적으로 확인한다.
- 후속 `SPEC-FRONTEND-001`(**아직 생성하지 않음 — 향후 계획**)이 이 SPEC의 카탈로그 API를 소비할 예정이다.

---

## §D 제약 조건

| 항목 | 값 | 출처 |
|---|---|---|
| 언어/런타임 | Java 17 | `build.gradle` |
| 프레임워크 | Spring Boot 4.1.0 | `build.gradle` |
| DB | PostgreSQL | `tech.md` |
| 통합 테스트 DB | **Testcontainers(PostgreSQL) — H2 대체 금지** | CHECK 제약의 실제 동작을 검증해야 한다 (AC-CRS-002/003). H2는 제약 위반 시 동작과 예외 유형이 달라 검증이 무의미해진다 |
| 개발 방법론 | **TDD(RED-GREEN-REFACTOR) 준수** — 프로세스 제약이며 요구사항이 아니다. 산출물 관찰로 검증할 수 없으므로 AC를 부여하지 않는다 (spec.md REQ-NFR-003 주석 참조) | `quality.yaml` |
| 커버리지 | 커밋당 ≥ 80%, 전체 목표 85% — 이쪽은 관찰 가능하므로 REQ-NFR-003 + AC-NFR-003이 검증한다 | `quality.yaml` |
| 문서 언어 | 한국어 | `language.yaml` |
| 신규 인프라 | **추가 금지 (예외 없음)** — 이 SPEC은 프로덕션·테스트를 불문하고 새 의존성을 추가하지 않는다. ArchUnit 역시 도입하지 않으며 AC-CRS-004는 2계층(정적 검색 + DB 상태 단언)으로 검증한다 (HISTORY 0.1.2 결정 근거 D3) | 사용자 명시적 결정 |
| 스키마 생성 방식 | **Hibernate `ddl-auto` 단독** — 마이그레이션 도구 없음 | `application-local.properties`(local: `update`), `AbstractIntegrationTest`(테스트: `create-drop`) |
| `ddl-auto=update`의 제약 반영 한계 | **운영상 주의 (D2)** — local 프로파일의 `update` 모드는 **이미 존재하는 테이블에 CHECK 제약 변경을 신뢰성 있게 반영하지 않는다.** Hibernate의 `update`는 주로 컬럼·테이블 추가만 수행하며, 제약 추가/변경/삭제는 누락될 수 있다. 따라서 (a) CHECK 제약의 정합성 판정은 **매번 스키마를 새로 만드는 통합 테스트(`create-drop`)를 기준으로 한다**, (b) 로컬 개발 DB에 이미 `course` 테이블이 있는 상태에서 제약을 바꾼 경우 해당 테이블을 드롭하고 재생성해야 실제 제약이 반영된다. AC-CRS-002/003/005가 통과했다는 사실이 로컬 DB에도 제약이 걸려 있음을 보장하지는 **않는다** | 이 SPEC |

---

## §E 사전 점검 (Pre-flight)

1. `SPEC-AUTH-001`이 `completed` 상태이고 `ADMIN` 역할 토큰을 발급받을 수 있다.
2. Testcontainers(PostgreSQL) 기반 통합 테스트 하네스가 `SPEC-AUTH-001`에서 이미 동작 중이다 (재사용).
3. 테스트에서 애플리케이션 계층을 우회해 `course` 행을 직접 INSERT/UPDATE할 수단(`JdbcTemplate` 또는 네이티브 쿼리)이 준비되어 있다 — AC-CRS-002/003/004, AC-CAT-002, AC-ADM-004/005가 이를 요구한다.
4. 미해소 클래리피케이션 마커가 0건이다. — **충족**

---

## §F 마일스톤

### M1 — 강좌 엔티티 및 제약 (우선순위: 높음)

- `Course` 엔티티 + `CourseStatus` + 스키마 + CHECK 제약 **3종** (`capacity >= 1`, `0 ≤ enrolled_count ≤ capacity`, `status IN ('OPEN','CLOSED')`)
- 대응 요구사항: REQ-CRS-001 ~ REQ-CRS-005
- 대응 AC: AC-CRS-001 ~ AC-CRS-005

### M2 — 공개 카탈로그 API (우선순위: 높음)

- **`SecurityConfig` 공개 경로 확장 (D1 — 이 마일스톤에서 가장 먼저)**: `GET /api/courses` 정확 일치 매처를 `GET /api/courses`, `GET /api/courses/*` 두 패턴으로 확장한다. 현재 매처로는 상세 경로가 인증을 요구해 REQ-CAT-001/003이 깨진다 (§C.2.1 참조). 인가 **규칙 구조**는 건드리지 않고 공개 경로만 추가한다
- **404 처리 골격 (D6)**: `CourseNotFoundException` 신설 + 기존 `GlobalExceptionHandler`에 404 핸들러 추가 (§C.4.2 참조). M3의 REQ-ADM-009도 이 골격을 재사용한다
- 목록(페이지네이션 — 경계 동작 포함)·상세 조회, 잔여 정원 계산
- 대응 요구사항: REQ-CAT-001 ~ REQ-CAT-006
- 대응 AC: AC-CAT-001 ~ AC-CAT-005

### M3 — 관리자 강좌 관리 API (우선순위: 높음)

- 생성·수정·마감·soft 삭제, 정원 축소 검증(경계 포함), 403/404 처리
- 대응 요구사항: REQ-ADM-001 ~ REQ-ADM-009
- 대응 AC: AC-ADM-001 ~ AC-ADM-008

### M4 — 비기능 마감 (우선순위: 중)

- 입력 검증 보강, 커버리지 도달, 정적 품질 0건
- 대응 요구사항: REQ-NFR-001 ~ REQ-NFR-003
- 대응 AC: AC-NFR-001 ~ AC-NFR-003

---

## §G 안티패턴 (구현 중 금지)

| 안티패턴 | 왜 금지인가 |
|---|---|
| 강좌 서비스에 `enrolled_count` 증감 메서드 추가 | REQ-CRS-004 / INV-CRS-003 위반. 선착순 정합성의 구조적 1차 방어선이 무너진다. AC-CRS-004가 정적 검색으로 검출한다 |
| 잔여 정원을 컬럼으로 저장 | 동기화해야 할 상태가 하나 늘고, `capacity`/`enrolled_count`와 어긋날 수 있다 |
| 강좌 물리 삭제(`delete`/`remove`) 구현 | INV-CRS-004 위반. 확정 수강신청의 참조 무결성이 깨진다 |
| 정원 축소 검증을 DB 제약에만 의존 | 제약 위반이 500으로 흘러 사용자에게 원인이 전달되지 않는다. 애플리케이션 계층 검증을 함께 둔다 |
| 정원을 확정 인원과 **같은 값**으로 축소하는 것을 거부 | 경계 오해. `enrolled_count <= capacity`가 성립하므로 허용이다 (AC-ADM-004) |
| 정원 축소 거부를 400으로 응답 | 409로 확정했다 (§C.3 / REQ-ADM-005). 400은 입력 형식 오류(정원 1 미만)에만 쓴다 — `GlobalExceptionHandler`의 기존 경계와 일치시킨다 |
| `Course`에 `@Setter` 부착 또는 `enrolled_count` 변경 메서드 정의 | §C.4.1-2 위반. `Member`가 role 파라미터를 받는 경로를 없앤 것과 같은 이유로, 카운터를 바꾸는 메서드는 이름이 무엇이든 만들지 않는다 |
| 새 `@RestControllerAdvice` 클래스 추가 | §C.4.2 위반. 예외-상태 코드 매핑이 두 파일로 흩어지고 우선순위 문제가 생긴다. 기존 `GlobalExceptionHandler`에 메서드를 추가한다 |
| ArchUnit 등 새 의존성 추가 | §D "신규 인프라 추가 금지"(사용자 명시적 결정) 위반. AC-CRS-004는 정적 검색 + DB 상태 단언 2계층으로 검증한다 (HISTORY 0.1.2 D3) |
| `updated_at` 등 요구사항에 없는 컬럼 추가 | 어떤 REQ/AC도 요구하지 않는다. 갱신 수단(`@UpdateTimestamp` 등)까지 딸려오며 검증되지 않는 기계 장치가 늘어난다 (HISTORY 0.1.2 D10) |
| `SecurityConfig` 인가 규칙 재작성 | `SPEC-AUTH-001` 소유 영역. 경로만 추가하고 규칙 구조는 건드리지 않는다 |
| 통합 테스트를 H2로 대체 | CHECK 제약 동작이 재현되지 않아 AC-CRS-002/003 검증이 무력화된다 |
| React 스캐폴딩 생성 | 사용자 결정(백엔드 우선) 위반. spec.md §D 범위 제외 |

---

## §H 교차 참조

### H.1 교차 SPEC 인수인계 항목 (D3 — 이 개정에서 신설)

| 항목 | 이 SPEC의 결정 | 후속 SPEC이 처리해야 할 것 |
|---|---|---|
| ArchUnit 의존성 | **도입하지 않는다.** AC-CRS-004는 정적 검색 + DB 상태 단언 2계층으로 검증한다 (HISTORY 0.1.2 결정 근거 D3) | `SPEC-ENROLLMENT-001` plan.md §E 사전 점검 6번이 "ArchUnit 의존성이 추가되어 있다"를 전제로 두고 있으나 **추가 주체가 미지정**이다. COURSE-001이 추가하지 않으므로, ENROLLMENT-001이 자신의 마일스톤 안에서 test-scope 의존성으로 직접 추가해야 한다. ENROLLMENT-001은 AC-ENR-008/009에서 "워커 패키지만 애그리게이트를 참조할 수 있다"는 **패키지 스코프 규칙**을 검증하며, 이는 grep으로 표현할 수 없어 ArchUnit이 실제로 필요한 쪽이다 |

> 이 개정에서 `SPEC-ENROLLMENT-001`의 산출물은 **수정하지 않았다.** 위 표는 그 SPEC의 run 단계 진입 전에 해소되어야 할 미지정 사항을 기록해 둔 것이다. ENROLLMENT-001 쪽 "신규 인프라 추가 금지" 제약과의 조정 역시 그 SPEC의 plan 단계에서 판단할 사항이다.

### H.2 문서 링크

- 요구사항 정의: `.moai/specs/SPEC-COURSE-001/spec.md`
- 인수 기준: `.moai/specs/SPEC-COURSE-001/acceptance.md`
- 진행 기록: `.moai/specs/SPEC-COURSE-001/progress.md`
- 선행 SPEC: `.moai/specs/SPEC-AUTH-001/`
- 후속 SPEC: `.moai/specs/SPEC-ENROLLMENT-001/`
- 제품/구조/기술: `.moai/project/{product,structure,tech}.md`
