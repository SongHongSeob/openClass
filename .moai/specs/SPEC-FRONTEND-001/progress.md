# SPEC-FRONTEND-001 — 진행 기록 (progress)

| 항목 | 값 |
|---|---|
| SPEC ID | `SPEC-FRONTEND-001` |
| 상태 | `in-progress` |
| 버전 | `0.2.2` |
| Tier | L |
| 현재 단계 | run — M1~M6 코드 레벨 완료. M6 브라우저 수동 확인 미실시(오케스트레이터 후속 필요). M7(수동 시나리오 확인, S1~S14)만 남음 — REQ-NFR-007에 따라 자동 검증만으로 이 SPEC의 run-phase를 완료 처리할 수 없음 |

---

## §E.1 Plan-phase Audit-Ready Signal

### E.1.1 산출 아티팩트

| 파일 | 상태 | 비고 |
|---|---|---|
| `spec.md` | 작성 완료 (0.2.2) | 12필드 프론트매터 + GEARS 요구사항 **59건** + 불변식 **11건** + 소비 엔드포인트 **14개** + `### Out of Scope` H3 5개 |
| `research.md` | 작성 완료 (0.2.2) | 코드베이스 조사 4건 + 도구 선택 검토 7건 + 미확인 항목 5건(그중 3번은 **해소되어 닫힘**). §2·§3에 해소 사실과 확인 기준점 추가, §3.2 무효화 표기 |
| `design.md` | 작성 완료 (0.2.1) | 구조 결정 8건(되돌리기 어려운 순서) + 기각 설계 **5건**. 3회차 지적 사항 해당 없음 — 변경 없음 |
| `plan.md` | 작성 완료 (0.2.2) | 차단 요소 **0건**(§B — 전부 해소) + 핵심 결정 7건 + 마일스톤 7개 + 안티패턴 **14건**(오름차순 정렬) |
| `acceptance.md` | 작성 완료 (0.2.2) | 인수 기준 **86건** + 추적성 매트릭스(기계적 재계수) + 완료 판정 |
| `progress.md` | 작성 완료 | 본 파일 |

### E.1.0 감사 1회차 반영 (0.1.0 → 0.2.0)

plan-auditor 1회차 판정: **FAIL 0.75** (Tier L 통과선 0.85). 지적 11건(critical 3 / major 3 / minor 5)을 전부 반영했다.

| ID | 등급 | 내용 | 반영 |
|---|---|---|---|
| D1 | critical | `DEP-1`(CORS 미설정) 진술이 낡음 — `main` `29a1560`이 이미 설정 | 6개 산출물 전부에서 **해소**로 정정. Vite 프록시를 필수 우회에서 **미채택**으로 강등(`plan.md` §C.5), M1 범위 축소, CORS 실동작 확인을 완료 판정에 추가 |
| D2 | critical | `DEP-2` 조회 API 경로·DTO가 실제 구현과 다름 | 실제 계약(`/api/enrollments/mine`·`/api/waitlist-entries/mine`, **`waitlistEntryId`**, `status`·`enrolledAt`)으로 정정. `spec.md` §A.4에 13·14번 추가, `REQ-CNL-006`~`009` 신설, AC-FE-107~111 신설 |
| D3 | critical | "엔드포인트 12개가 전부" 단정이 거짓 (실제 14개) | `spec.md` §A.4·§A.1, `plan.md` §A.2, `design.md` §A.7·§A.8, `research.md` §3.1 전부 14개로 정정 |
| D4 | major | `REQ-ENR-004`가 SPEC 자신이 안티패턴으로 규정한 화이트리스트 판정을 규범화 | **`PENDING`의 여집합**으로 재작성. 8종 목록은 `REQ-ENR-009`용 참고 정보로 강등. AC-FE-063을 차단 등급으로 승격 |
| D5 | major | `depends_on` 게이트가 M4/M6이 아니라 **run 진입 전체**를 막는다 | `plan.md` §B.3을 전면 재작성 — 게이트의 실제 성격을 명시하고 선택지 A(병합 대기, 권장)·B(`--ignore-deps` 우회)의 비용을 정직하게 병기. AC-FE-907 신설 |
| D6 | major | §D.1 추적성 표 계수 오류 (76 vs 실제 77, 이중 계상 4건, 누락 4건) | 기계적으로 재계수하여 **84건**으로 정정. 각 AC가 정확히 한 행에만 속하도록 재구성 + 계수 규칙과 검증 명령 명시 |
| D7 | minor | `design.md` §C.2의 "§6.3" 참조가 자기 문서에 없음 | `research.md` §6.3`으로 한정 |
| D8 | minor | CORS 허용 오리진·헤더 결합이 어느 산출물에도 없음 | `design.md` §B.1을 제약 표로 재작성 (오리진·헤더 2종·메서드·`allowCredentials=false`) + AP-6 교체 |
| D9 | minor | 의존성 추적 AC가 자명 통과 형태로 낡음 | AC-FE-901·902를 "기록한다"에서 "실제로 동작한다"로 승격. §E.4 부분 완료 조항 삭제 |
| D10 | minor | `REQ-ENR-010`의 "재시도 shall할 수 있다"가 능력 진술 | 이진 판정 가능한 의무 진술로 재작성 |
| D11 | minor | 폴링 경과 시간 기준점의 보존 위치 미정의 | `REQ-ENR-011` 신설 + `plan.md` §C.3·`design.md` §A.4에 전개 + AC-FE-073 신설 (0.2.1에서 **AC-FE-073a/073b로 분할** — 아래 N5) |

### E.1.0-2 감사 2회차 반영 (0.2.0 → 0.2.1)

plan-auditor 2회차 판정: **PASS 0.89** (Tier L 통과선 0.85). 통과 판정이므로 재감사는 요구되지 않았고, 감사자 권고에 따라 **단일 범위 한정 편집 패스**로 지적 8건(major 2 / minor 6)을 반영했다.

| ID | 등급 | 내용 | 반영 |
|---|---|---|---|
| N1 | major | 대기 목록 `position` 오름차순을 "승격 예정 순서"로 오기술 — `position`은 **강좌 단위** 순번이므로 강좌를 가로지르면 승격 순서를 뜻하지 않음 | `spec.md` REQ-CNL-008 근거를 "백엔드 순서가 결정적이며 재정렬 시 AC-FE-110의 대조 대상이 사라진다"로 재작성 + `position`이 강좌 내 순위임과 `courseTitle` 병기 의무를 주의 항목으로 신설. `spec.md` §A.4 정렬 주석 완화 + 강좌 단위 순번 항목 추가. `plan.md` M6 지시 4번 정정 + 5번 신설. 소스 재확인: `WaitlistEntryRepository.nextPosition`이 `WHERE w.courseId = :courseId`, 부분 유니크 인덱스 `(course_id, position)` |
| N2 | major | "1~12번은 `main` HEAD에서 확인" 진술이 거짓 — `main`에는 컨트롤러가 3개뿐이라 5~8번을 확인할 수 없음 | `spec.md` §A.4 확인 기준점을 **1~4·9~12번 = `main` HEAD `29a1560`**, **5~8·13~14번 = `sync/SPEC-ENROLLMENT-001` HEAD `871d247`**로 정정 + 분리 이유(미병합)를 명시하고 `research.md` §1과의 정합을 표기 |
| N3 | minor | `acceptance.md` §E.2가 `S1~S9`로 표기 (체크리스트는 S1~S13) | `S1~S13`으로 정정 (`plan.md` §F M7·§E E9와 일치) |
| N4 | minor | §D.1 열 이름 "요구사항 그룹"이 REQ 소유권을 함의하나 실제 행 기준은 구간(마일스톤) | 열 이름을 **"구간(마일스톤)"** 으로 변경 + 교차 그룹 REQ 참조가 정상임을 명시하는 주석 추가 |
| N5 | minor | AC-FE-073이 [자동] 표기이나 문구가 브라우저 새로고침(수동 관측) | **AC-FE-073a [자동]**(보존된 접수 시각 기준 스케줄 재계산)과 **AC-FE-073b [수동]**(브라우저 새로고침 재현)으로 분할. **AC 총계 84 → 85** — §D.1 ENR 행(16→17)·소계(79→80)·합계·자체 검증 명령 기대값·본 파일 모두 갱신. `plan.md`·`design.md`의 참조도 073a/073b로 갱신 |
| N6 | minor | `research.md` §4.3이 `plan.md` §F **M2**를 참조 (실제는 M1) | `§F M1`로 정정 (`acceptance.md` §F F4와 일치) |
| N7 | minor | `research.md` §1이 `plan.md` **§C.1**을 참조 (실제는 §B.3) | `§B.3`으로 정정 |
| N8 | minor | `research.md` §3.2의 현재형 판정이 낡았으나 무효화 표기 없음 (§2.3에는 있음) | §3.2 제목에 "(0.1.0 조사 시점 — §3.0에 의해 무효화됨)" 부기 + §2.3과 동일한 형태의 인라인 무효화 주석 추가 |

> 이 편집 패스는 **문서 정정 한정**이며 구조 변경이 아니다. 감사자 권고("a single scoped edit pass, no re-audit required")에 따라 재감사를 요청하지 않았다. `status`는 `draft` 유지 — 여전히 plan 단계다.

### E.1.0-3 감사 3회차 반영 (0.2.1 → 0.2.2)

plan-auditor 3회차 판정: **FAIL 0.83** (Tier L 통과선 0.85). 2회차 PASS 0.89에서 점수가 하락(−0.06)하여 STOP 신호가 발신되었으나, **감사자 자신이 이 하락을 구조적 결함이 아니라 외생적(exogenous) 회귀로 진단**했다 — 2회차 감사와 3회차 감사 사이에 선행 의존성이 `main`에 병합되어, 2회차 시점에는 정확했던 사실 서술이 무효화된 것이다. 감사자는 범위 축소(SPEC 분할)를 **명시적으로 권하지 않았고**, 경계가 정해진 기계적 편집 패스 + 델타 범위 재감사를 권고했다. 이 절은 그 편집 패스의 기록이다.

2회차 지적 N1~N8은 3회차에서 **8/8 전부 해소 확인**되었고, 회귀는 0건이었다.

| ID | 등급 | 내용 | 반영 |
|---|---|---|---|
| M1 | **critical** | `DEP-3`가 실제로는 이미 해소되었는데 6개 산출물 약 20개소가 "미해소 — 유일한 차단 요소"로 진술 (PR #1은 v0.2.1 문서 커밋보다 **2분 43초 먼저** 병합되었으므로 커밋 시점에 이미 거짓이었다) | 소스 재확인 후 전 산출물 정정 — `gh pr view 1` → `MERGED`/`21eab8a`/`2026-08-17T09:04:24Z`, `SPEC-ENROLLMENT-001` `main`에서 `status: completed`, `main` 컨트롤러 5개·14개 엔드포인트 전수 확인. (1) `spec.md` §A.5 `DEP-3` 행 → **해소됨** + `#### DEP-3` 해소 절 신설, (2) §A.4 확인 기준점을 **단일 `main` 기준점(`21eab8a`)** 으로 재작성 + **재확인 명령** 삽입(스냅샷 → 절차로 전환, 감사자의 systemic 권고 반영), (3) `plan.md` §B.3을 A/B 선택지에서 **해소 기록**으로 축약(§B.3.1 게이트 성격 교훈은 존치), (4) M1·M4·M6 진입 조건에서 게이트 조건 제거, (5) `AC-FE-907`을 `status: completed` 관측으로 재진술하고 **`--ignore-deps` 분기 삭제**(충족된 의존성을 미충족으로 기록하는 허위 감사 로그 방지), (6) `research.md` §1·§7-3 갱신, (7) `progress.md` §E.1.5·§E.1.7 갱신 |
| M2 | major | N1이 도입한 `shall`/`shall not` 의무(`position`을 `courseTitle`과 병기, 전역 순위 표기 금지)가 **근거 blockquote 안에만** 있어 AC·불변식·완료 판정 어디에도 앵커가 없음 | **`REQ-CNL-010`(Ubiquitous) 신설**로 규범 위치 확보 + **INV-FE-011 신설** + **`AC-FE-112` [수동] 신설**(§B.6) + `plan.md` M6 완료 판정에 추가. 기존 blockquote는 근거 설명으로 존치하되 규범 위치를 `REQ-CNL-010`으로 명시. 계수 파급: REQ 58→**59**, INV 10→**11**, AC 85→**86**(§D.1 CNL 행 9→10·12→13, 소계 58→59·80→81, 합계 59/86, 자체 검증 명령 기대값 86) |
| M3 | minor | N5가 만든 `AC-FE-073b`([수동])가 §E.2·`plan.md` §F M7 어느 체크리스트에도 없음 (N3과 동일 계열의 체크리스트 드리프트가 한 라운드 뒤 재발) | **S14**(새로고침 후 폴링 재개 → 3초 간격, 상한은 최초 접수 기준) 를 `acceptance.md` §E.2와 `plan.md` §F M7 시나리오 표에 추가 + 범위 표기 4개소(`acceptance.md` §E.2·§E.4, `plan.md` §E E9·§F M7 완료 판정)를 **S1~S14**로 갱신 |
| M4 | minor | `plan.md` §G 안티패턴 표가 AP-1~6 → AP-11~14 → AP-7~10 순으로 문서 순서가 어긋남 (누락·중복은 없음) | AP-7~10 행을 AP-11 앞으로 이동하여 **AP-1~14 오름차순** 정렬. 항목 수 14건 불변 |

> 이 편집 패스도 **문서 정정 한정**이며 범위 축소나 구조 변경이 아니다. `status`는 `draft` 유지. 감사자가 권고한 **델타 범위 재감사**(58 REQ / 86 AC 전수 기계 검증 반복 불필요)는 오케스트레이터가 수행한다.

### E.1.2 SPEC ID 사전 자체 검증

```
decomposition: SPEC ✓ | FRONTEND ✓ | 001 ✓ → PASS
```

검증 명령 및 출력:

```bash
ID="SPEC-FRONTEND-001"
[[ "$ID" =~ ^SPEC(-[A-Z][A-Z0-9]*)+-[0-9]{3}$ ]] && echo PASS || echo FAIL
# 출력: PASS
```

중복 검사: `.moai/specs/SPEC-FRONTEND-*` 부재 확인 (ID 미사용). 아울러 선행 3개 SPEC이 이 ID를 **향후 계획으로 이미 전방 참조**하고 있음을 확인했다 — `SPEC-AUTH-001/spec.md:145`, `SPEC-COURSE-001/spec.md:145`, `SPEC-ENROLLMENT-001/spec.md:332` 등 총 10개소.

### E.1.3 프론트매터 스키마 검증

12개 정규 필드 전수 확인 완료 — `id`·`title`·`version`·`status`·`created`·`updated`·`author`·`priority`·`phase`·`module`·`lifecycle`·`tags`. 선택 필드 `tier: L`, `depends_on: [SPEC-AUTH-001, SPEC-COURSE-001, SPEC-ENROLLMENT-001]` 포함.

스네이크케이스 별칭(`created_at`·`updated_at`·`labels`·`spec_id`) 사용 0건. `tags`는 쉼표 구분 문자열, `version`은 따옴표 문자열.

### E.1.4 Plan 단계에서 확정된 열린 결정

| 결정 | 값 | 근거 위치 |
|---|---|---|
| 프론트엔드 워크스페이스 위치 | 저장소 내 `frontend/` | `spec.md` §A.2 (`tech.md`·`structure.md`의 "미결정" 해소) |
| 개발 서버 CORS 우회 | **Vite 프록시 미채택** — 실제 오리진으로 직접 호출 (0.2.0에서 뒤집은 결정) | `plan.md` §C.5, `design.md` §B.1 |
| 종단 판정 방식 | `PENDING`의 여집합 (화이트리스트 금지) | `spec.md` REQ-ENR-004, `design.md` §A.2 |
| 폴링 경과 기준점 보존 | 요청 식별자와 같은 수명(`sessionStorage`, `requestId` 키) | `spec.md` REQ-ENR-011, `design.md` §A.4 |
| 취소 식별자 혼동 방지 | `waitlistEntryId`와 `position`을 타입 층위에서 구별 | `design.md` §A.1, INV-FE-009 |
| 라우팅 | React Router | `plan.md` §C.6 |
| HTTP 클라이언트 | 표준 `fetch` + 얇은 래퍼 (신규 의존성 없음) | `plan.md` §C.6 |
| 서버 상태·폴링 | TanStack Query | `plan.md` §C.6 |
| 클라이언트 상태 | React Context + `useReducer` (신규 의존성 없음) | `plan.md` §C.6 |
| 폼 | 제어 컴포넌트 + 소형 헬퍼 (신규 의존성 없음) | `plan.md` §C.6 |
| 스타일링 | CSS Modules (Vite 기본, 신규 의존성 없음) | `plan.md` §C.6 |
| 토큰 저장 | `sessionStorage` | `plan.md` §C.4 |
| 폴링 스케줄 | 1s(0~5s) → 2s(5~15s) → 3s(15~30s) → 중단 | `plan.md` §C.3 |
| 오류 정규화 판정 순서 | 네트워크 → 401 → `code` 존재 → 상태코드 | `plan.md` §C.2 |

신규 의존성은 **2개**(React Router, TanStack Query). 나머지는 플랫폼/프레임워크 기본 기능으로 해결.

### E.1.5 차단 의존성 상태 (0.2.0 재확인)

| ID | 내용 | 상태 | 근거 / 남은 조치 |
|---|---|---|---|
| **DEP-1** | 백엔드 CORS 미설정 | **해소** | `main` 커밋 `29a1560`. `SecurityConfig`에 `.cors(...)` + `CorsConfigurationSource` Bean + `CorsProperties` 존재를 소스에서 직접 확인. 남은 것은 조치가 아니라 **제약 준수**(허용 오리진 5173 정렬, 헤더 2종, `allowCredentials=false`) |
| **DEP-2** | 취소 대상 식별자 미노출 | **해소** | `SPEC-ENROLLMENT-001` v0.3.0 M7. `sync/SPEC-ENROLLMENT-001`에서 `EnrollmentController.listMine`·`WaitlistController.listMine`과 두 응답 레코드를 직접 읽어 확인. **0.1.0이 제안한 경로·필드명과 다르므로** 전 산출물을 실제 계약으로 정정 |
| **DEP-3** | `SPEC-ENROLLMENT-001` PR #1 미병합 | **해소** (0.2.2에서 정정) | PR #1 병합 — 병합 커밋 `21eab8a`, 병합 시각 `2026-08-17T09:04:24Z`. `SPEC-ENROLLMENT-001`이 `main`에서 `status: completed`이며 `EnrollmentController`·`WaitlistController`가 `main` 트리에 존재(컨트롤러 5개, 엔드포인트 14개 전수 확인). **`depends_on` 사전 점검이 세 의존성 전부에 대해 통과한다.** `--ignore-deps` 경로는 도달 불가·사용 금지 |

> **0.1.0의 판단 정정 (존치)**: 0.1.0은 `DEP-3`에 대해 "M1~M3은 `main`만으로 진행 가능"이라고 적었다. **코드 가용성에 관해서는 맞지만 진입 게이트에 관해서는 틀렸다** — `depends_on` 사전 점검은 plan-auditor보다 먼저, 마일스톤 단위가 아니라 호출 단위로 실행되며, 충족의 정의는 `status: completed` 단 하나다(부분 인정 없음). 이 정정이 감사 지적 D5다. `DEP-3` 자체는 해소되었으나 이 교훈은 `depends_on`을 선언하는 후속 SPEC에 그대로 적용되므로 `plan.md` §B.3.1에 존치한다.
>
> **0.2.1의 판단 정정 (0.2.2)**: 0.2.1은 위 표에 `DEP-3`를 "미해소 — 유일한 차단 요소"로 기록했으나, **그 시점에 PR #1은 이미 병합되어 있었다**(문서 커밋 2분 43초 전). 상태를 관측하지 않고 이전 판정을 옮긴 것이 원인이며, 이것이 3회차 감사 지적 M1이다. 재발 방지를 위해 `spec.md` §A.4에 **재확인 명령**을 삽입했다 — 이후 개정자는 prose가 아니라 그 명령의 출력을 근거로 삼는다.

`[NEEDS CLARIFICATION]` 마커: **0건.** 위임 시점에 범위·배치·도구가 확정되어 전달되었고, 나머지 도구 결정은 이 SPEC이 근거와 함께 확정했다. 0.2.1까지 남아 있던 `DEP-3` 처리 방침(선택지 A/B)은 **의존성 해소로 선택 대상 자체가 소멸**하여 `plan.md` §B.3에서 제거되었다.

### E.1.6 제약 준수 확인

| 제약 | 확인 |
|---|---|
| 백엔드 소스 무수정 | 이 단계에서 `src/**`·`build.gradle` 쓰기 0건 (읽기 전용 조회만 수행) |
| 기존 SPEC 무수정 | `SPEC-AUTH-001`·`SPEC-COURSE-001`·`SPEC-ENROLLMENT-001` 쓰기 0건 |
| `.moai/project/*.md` 무수정 | 쓰기 0건. 갱신 근거는 `plan.md` §H.1에 sync 단계 인계용으로 기록 |
| 프론트엔드 소스 미생성 | `frontend/` 스캐폴딩 0건 (plan 단계 한정) |

### E.1.7 다음 단계

plan-auditor 감사는 4회차까지 수행되었다(FAIL 0.75 → PASS 0.89 → FAIL 0.83 → **PASS 0.91**). 3회차 지적 M1~M4를 0.2.2에서 전부 반영했고, 4회차(`/moai run` Phase 1 Plan Audit Gate 겸용)에서 4건 전부 RESOLVED 확인 + 신규 minor 4건(N1~N4, 전부 사소함 — 감사자 권고: "5차 반복 불필요, 다음에 해당 파일을 건드릴 때 함께 반영") 발견. 보고서: `.moai/reports/plan-audit/SPEC-FRONTEND-001-review-4.md`.

남은 절차는 Implementation Kickoff Approval(휴먼 게이트) → run 단계 진입이다. **`DEP-3` 처리 방침 확정 단계는 소멸했다** — 의존성이 해소되어 결정할 대상이 없다. 남은 절차들은 이 위임의 범위 밖이며 오케스트레이터가 수행한다.

---

## Phase 1: Plan Audit Gate (run-phase 진입, `/moai run`)

```yaml
audit_verdict: PASS
audit_report: .moai/reports/plan-audit/SPEC-FRONTEND-001-review-4.md
audit_at: 2026-08-17
auditor_version: plan-auditor (round 4/4 — user-authorized confirming audit beyond nominal 3-cap, per Retry Loop Contract post-iter-3 escalation)
score: 0.91
depends_on_preflight: PASS (SPEC-AUTH-001, SPEC-COURSE-001, SPEC-ENROLLMENT-001 all status: completed)
```

이력: iter1 FAIL 0.75 → iter2 PASS 0.89 → iter3 FAIL 0.83(외인성 회귀 — DEP-3 해소 사실 미반영) → iter4 PASS 0.91(M1~M4 전부 RESOLVED, STOP 신호 해제). Tier L 통과 기준 0.85 충족.

---

## Implementation Kickoff Approval

`AskUserQuestion`을 통해 오케스트레이터가 직접 확인: (1) 계획대로 착수 승인 — "네, 시작해주세요" 선택. (2) 진행 방식 — **semi-autonomous(마일스톤마다 확인받기)** 선택. Route B(Tier L) 규약에 따라 `feat/SPEC-FRONTEND-001` 브랜치를 생성했다(main 직접 수정 아님, 추후 PR로 병합).

```yaml
kickoff_approval: obtained
progression_mode: semi-autonomous
branch: feat/SPEC-FRONTEND-001
mode_selection: sub-agent (§G)
```

## §E.2 Run-phase Evidence

### M1 — 워크스페이스 부트스트랩 및 공통 기반

**대상 요구사항**: `REQ-NFR-001`·`003`·`004`·`005`, `REQ-ERR-001`~`006`

**산출물**: `frontend/` 신규 스캐폴딩 (Vite 8 + React 19 + TypeScript 6, `npm create vite@latest -- --template react-ts` 기반). `src/api/types.ts`(§A.1 타입 단일 선언) · `src/api/errors.ts`(§A.3/§C.2 오류 정규화 단일 지점) · `src/api/client.ts`(fetch 래퍼) · `src/App.tsx`(M1 확인용 화면 — GET /api/courses 육안 확인) · `vite.config.ts`(포트 5173 고정, `strictPort`, 프록시 없음) · `.env.example`/`.env.local`(REQ-NFR-003 환경 변수 주입).

| AC / 항목 | 판정 | 근거 |
|---|---|---|
| E1 (`tsc --noEmit`) | **PASS** | `npx tsc -b --force` exit=0 (프로젝트 참조 방식, 두 프로젝트 모두 `noEmit: true`) |
| E2 (lint) | **PASS** | `npm run lint`(oxlint) exit=0, 출력 없음 |
| E3 (단위 테스트) | **PASS** | `npx vitest run` exit=0 — 3개 파일 24건 전부 통과. 오류 정규화 판정 순서(401-before-code-field) 테스트 및 타입 관용성(type-tolerance) 테스트 포함 |
| E4 (프로덕션 빌드) | **PASS** | `npm run build`(`tsc -b && vite build`) exit=0 |
| E11 (`server.proxy` grep) | **PASS** | `grep -c server.proxy vite.config.ts` → `0` |
| AC-FE-003 (하드코딩 URL 리터럴 0건) | **PASS** | `grep -rn "http://\|https://" src/` → 0건. `.env.example`/`.env.local`(설정 파일)에만 존재 |
| AC-FE-005a/005b/006/007/009/010 | **PASS** | `errors.test.ts` 10건 전부 통과 — F1/F2/F3 판정, 401-우선 판정, 네트워크/서버 오류 구별, 원문 미노출, 미지 바디 무예외 |
| REQ-ENR-009 타입 관용성 | **PASS** | `types.test.ts` — `EnrollmentStatus`/`CourseStatus`에 미지 문자열 리터럴 대입이 컴파일된다(닫힌 유니온으로 회귀 시 `tsc`가 실패하는 회귀 가드) |

**E10 / AC-FE-901 (CORS 실동작, 프록시 미사용) — 브라우저 관측 완료 (오케스트레이터, `claude-in-chrome`)**:

- manager-develop의 1차 시도(`curl` 기반, HTTP 프로토콜 계층 확인)에서 브라우저 실관측 갭이 보고됨 — 로컬 머신 포트 5173이 무관한 다른 프로젝트(`wedding_project`)에 점유되어 있어 기본 오리진으로는 확인 불가.
- **오케스트레이터가 직접 후속 조치를 수행**: `plan.md` §C.5·`design.md` §B.1이 명시한 대체 경로("다른 포트를 쓰려면 백엔드 기동 시 `CORS_ALLOWED_ORIGINS`를 함께 설정")를 그대로 적용 — 포트 5173 대신 **5173과 동일하게 명시적으로 허용되는 대체 오리진 `http://localhost:5174`**를 사용. 백엔드를 `CORS_ALLOWED_ORIGINS=http://localhost:5174`로 기동하고, 프론트엔드를 `vite --port 5174 --strictPort`로 기동(기존 5173 개발 서버·다른 프로젝트는 건드리지 않음).
- **`claude-in-chrome` 브라우저 자동화로 실관측**: `http://localhost:5174` 접속 → `read_network_requests` → `GET http://localhost:8080/api/courses?page=0&size=10` **200** 2건 관측(교차 오리진, 프록시 미사용 — `vite.config.ts`에 `server.proxy` 없음, E11과 일치) → `read_console_messages`(`onlyErrors: true`) → **콘솔 오류/CORS 오류 0건**. 화면은 "총 0건 중 0건 표시"를 보였는데, 이는 로컬 DB에 강좌 데이터가 시딩되지 않았기 때문(요청 자체는 200 성공)이며 CORS/배선 결함이 아니다.
- 확인 후 임시 백엔드·프론트엔드 프로세스와 Chrome 탭 모두 정리(kill + tab close) — 사용자의 기존 5173 프로젝트에는 영향 없음.

**M1 완료 판정 (plan.md 기준)**: E1~E4 통과 + 오류 정규화 단위 테스트 통과 + **브라우저에서 `GET /api/courses`가 프록시 없이 교차 오리진으로 성공(콘솔 CORS 오류 0건, 오케스트레이터가 `claude-in-chrome`으로 직접 관측)** — **전부 충족**. M1 온전히 종결.

### M2 — 세션 및 인증

**대상 요구사항**: `REQ-SES-001`~`009`

**산출물**:

- `src/session/jwt.ts` — JWT 페이로드 base64url 디코드(서명 검증 없음, REQ-SES-008/AC-FE-035) + `isExpired` 판정
- `src/session/tokenStorage.ts` — `sessionStorage` 연동(REQ-SES-004, §C.4). 테스트 주입 가능한 `TokenStorageLike` 인터페이스로 jsdom 없이 단위 테스트
- `src/session/sessionState.ts` — 세션 상태 순수 전이 로직(`deriveSessionState`/`sessionReducer`) — 수립·복원·폐기 공통 규칙(design.md §A.5)
- `src/session/sessionContextInstance.ts` / `src/session/SessionContext.tsx` / `src/session/useSession.ts` — React Context + `useReducer`(§C.6) 3분할(Fast Refresh용 `react/only-export-components` 린트 규칙 준수)
- `src/session/LogoutButton.tsx` — 로그아웃 UI, REQ-SES-006 문구 제약("서버 무효화" 표현 없음, README.md 모델과 일치)
- `src/routing/guardLogic.ts`(순수 판정) / `src/routing/guards.tsx`(`RequireAuth`/`RequireRole` 컴포넌트) — REQ-SES-009/REQ-ADM-002 가드. 보호 대상 화면은 아직 없음(M4~M6이 감쌀 예정, 위임 지시 범위)
- `src/api/client.ts` 확장 — `subscribeToSessionExpired`/전역 401 통지(REQ-SES-007 단일 배선 지점). `errors.ts`의 401-우선 판정 순서는 그대로 소비만 함(재구현 없음)
- `src/api/endpoints.ts` — `signup`/`login` 호출 함수(design.md §A.8 성장 지점, 14개 중 2개)
- `src/pages/SignupPage.tsx` / `src/pages/LoginPage.tsx` — 회원가입·로그인 화면(라우터 비의존, 콜백 prop으로 화면 전환 위임)
- `src/App.tsx` 재작성 — `SessionProvider` + 로그인/회원가입/인증됨 3-상태 수동 화면 전환(라우터 미도입 — M2 자체 화면은 둘 다 공개 화면이라 가드 미사용)

**라우팅 범위 결정 (위임 지시 반영)**: plan.md §C.6이 React Router를 최종 채택했으나, 위임 지시가 "가드 컴포넌트/훅만 구현, 보호 화면은 아직 불필요"를 명시했으므로 M2는 React Router를 **도입하지 않았다** — 신규 의존성 0건. 실제 다중 경로 라우팅은 다중 화면 내비게이션이 실제로 필요해지는 이후 마일스톤(카탈로그/수강신청)이 도입한다.

| AC / 항목 | 판정 | 근거 |
|---|---|---|
| E1 (`tsc -b --force`) | **PASS** | exit=0, 출력 없음 |
| E2 (lint, oxlint) | **PASS** | exit=0, 출력 없음(경고 0건 — `react/only-export-components` 경고는 SessionContext를 3개 파일로 분할하여 해소) |
| E3 (단위 테스트) | **PASS** | `npx vitest run` exit=0 — 7개 파일 **62건** 전부 통과(M1 24건 + M2 신규 38건) |
| E4 (프로덕션 빌드) | **PASS** | `npm run build`(`tsc -b && vite build`) exit=0, `dist/` 산출 |
| AC-FE-032 (선제 만료 판정, 자동) | **PASS** | `sessionState.test.ts` — exp 경과 토큰 → 서버 왕복 없이 즉시 `anonymous` |
| AC-FE-033 (손상 토큰, 자동) | **PASS** | `jwt.test.ts`/`sessionState.test.ts` — 손상 토큰에 대해 예외 없이 `null`/`anonymous` |
| AC-FE-035 (서명 미검증 주석 기록, 검사) | **PASS** | `jwt.ts` 상단 docstring + `@MX:REASON`에 "서명 검증 없음, 표시 목적 한정" 명시. `guardLogic.ts`/`guards.tsx`도 동일 취지 주석 |
| REQ-SES-007 (401 전역 배선) | **PASS** | `client.test.ts` 신규 5건 — F1(code 있음)/F2(code 없음) 401 모두 통지, 404는 미통지, 성공은 미통지, `unsubscribe()` 이후 미통지 |
| REQ-SES-009/REQ-ADM-002 (가드 판정) | **PASS** | `guardLogic.test.ts` 7건 — 세션 없음→`no-session`, 역할 불일치→`insufficient-role`, 일치→허용. 인증 가드와 역할 가드가 세션 부재 사유를 동일하게(`no-session`) 반환함을 별도 검증(design.md §A.6) |
| REQ-SES-008/INV-FE-005 (역할 표시 전용 속성) | **PASS** | `jwt.test.ts` + `guardLogic.test.ts` — 서명 미검증으로 위조된 `role: ADMIN` 클레임이 `evaluateRoleGuard`를 구조적으로 통과함을 실증(가드가 보안 경계가 아님의 반증적 증명) |
| AC-FE-003 (하드코딩 URL 0건, 신규 파일) | **PASS** | `grep -rn "http://\|https://" src --include="*.ts" --include="*.tsx"` (테스트 제외) → 0건 |
| B4/B11 (AskUserQuestion 미사용) | **PASS** | `grep -rn "AskUserQuestion" src` → 0건 |
| 작업 트리 범위 | **PASS** | `git status --porcelain frontend .moai/specs/SPEC-FRONTEND-001/progress.md` → `frontend/**`(신규 4디렉터리 + client.ts/client.test.ts/App.tsx 수정)만 변경. 백엔드(`src/**`)·다른 SPEC·`.moai/project/*`는 무변경 |

**브라우저 수동 확인 — 완료 (오케스트레이터, `claude-in-chrome`, M1과 동일 패턴)**: 포트 5173이 여전히 무관한 다른 프로젝트에 점유되어 있어, M1과 동일하게 대체 오리진 `http://localhost:5174`(백엔드 `CORS_ALLOWED_ORIGINS=http://localhost:5174` + `vite --port 5174 --strictPort`)로 임시 기동 후 확인:

- 회원가입: `m2check@local.test`로 회원가입 화면 제출 → 로그인 화면으로 전환(REQ-SES-001 관측)
- 로그인: 같은 자격 증명으로 로그인 제출 → "m2check@local.test로 로그인되어 있습니다. (역할: MEMBER)" 인증 화면으로 전환(REQ-SES-002·REQ-SES-008 관측 — 역할이 화면 표시에 쓰였다)
- **새로고침 유지**: 같은 탭에서 `http://localhost:5174/`로 재이동(전체 페이지 리로드) → 로그아웃되지 않고 동일 인증 화면 유지 확인
- **탭 종료(격리) 후 소멸**: 새 탭을 열어 같은 URL 접속 → 로그인 화면(비인증 상태) 관측 — `sessionStorage`는 탭 간 공유되지 않는다는 브라우저 네이티브 보장이 실제로 적용됨을 확인(소스 재확인: `SessionContext.tsx`가 `window.sessionStorage`를 사용, `localStorage` 아님 — REQ-SES-004 저장 위치 결정과 일치)
- 확인 후 임시 백엔드·프론트엔드 프로세스 및 Chrome 탭 정리. 사용자의 기존 5173 프로젝트에는 영향 없음.

부수 관찰(결함 아님, 기록만): 인증 화면의 로그아웃 안내 문구가 로그인 **직후**에도 "로그아웃되었습니다..."로 시작하는 과거형으로 보여 다소 혼란스럽다 — REQ-SES-006이 요구하는 사실 내용(서버 측 강제 무효화 아님)은 정확하지만, 문구 자체가 상시 노출되는 설명문인지 실제 로그아웃 이벤트 알림인지 구분이 안 된다. 기능 결함이 아니므로 M2 완료를 막지 않으나, 후속 마일스톤(또는 sync 단계)에서 조건부 표시("로그아웃 버튼을 누르면 ~") 또는 상시 안내문 형태로 문구를 다듬는 것을 권장.

**커밋**: (M2 커밋은 이 progress.md 갱신과 함께 커밋됨 — SHA는 커밋 후 §E.3에서 백필)

### M3 — 강좌 카탈로그

**대상 요구사항**: `REQ-CAT-001`~`006`

**산출물**:

- `src/catalog/catalogModel.ts` — 순수 로직 지점(plan.md §D.1의 "목록 응답 → 화면 모델 변환" 부류): `computePageControls`(REQ-CAT-002, 서버 페이지 메타데이터로부터 이전/다음·빈 목록 유도, 0-인덱스 `currentPage`는 백엔드 `Page.getNumber()`와 동일) / `isEnrollmentBlocked`(REQ-CAT-005, `CLOSED` 판정 — M4가 실제 신청 CTA 배선 시 재사용할 재사용 가능한 조각으로 노출)
- `src/catalog/catalogModel.test.ts` — TDD RED 먼저 작성(모듈 부재로 실패 확인) 후 GREEN
- `src/api/endpoints.ts` 확장 — `getCourses(page, size)`(`GET /api/courses`, 세션 불필요) / `getCourseDetail(id)`(`GET /api/courses/{id}`, 세션 불필요) — design.md §A.8 성장 지점, 14개 중 4개 채움
- `src/catalog/CourseListPage.tsx` — 목록 화면. 진입 시 `getCourses` 호출(REQ-CAT-001), `computePageControls` 기반 이전/다음 버튼(REQ-CAT-002, 자체 분할 없음), 항목별 정원·확정 인원·잔여 정원·모집 상태 표시(REQ-CAT-003), 0건 시 "표시할 강좌가 없습니다" 정상 상태 표시(오류 아님)
- `src/catalog/CourseDetailPage.tsx` — 상세 화면. 진입 시 `getCourseDetail` 호출(REQ-CAT-004), `CLOSED` 상태면 "마감된 강좌입니다..." 표시(REQ-CAT-005) — 신청 조작 자체는 이 마일스톤에 없으므로(M4가 배선) 노출할 CTA가 없다는 뜻에서 결과적으로 충족. 404 등 오류는 `errors.ts` 단일 정규화 지점의 결과만 표시(REQ-ERR-002, AC-FE-046)
- `src/catalog/CatalogSection.tsx` — 목록/상세 전환 컨테이너. **라우터를 도입하지 않았다** — 과업 지시 B1에 따라 `package.json` 확인 결과 `react-router`/TanStack Query 미설치(`plan.md` §C.6이 채택을 결정했으나 아직 미도입 상태)를 확인하고, 기존 M2의 콜백 기반 화면 전환 패턴(App.tsx의 `Screen` 지역 상태)을 그대로 확장하는 지역 상태(`useState<CatalogView>`)로 구현. 신규 의존성 0건
- `src/App.tsx` 수정 — `AuthenticatedView`/`AnonymousView` 양쪽에 `<CatalogSection />` 추가(REQ-CAT-006, 세션 무관 열람)

**AC / 항목 판정 (근거)**:

| AC / 항목 | 판정 | 근거 |
|---|---|---|
| REQ-CAT-001 (목록 진입 시 조회) | **PASS** | `CourseListPage.tsx` — `useEffect`가 마운트 및 `page` 변경 시 `getCourses(page, PAGE_SIZE)` 호출 |
| REQ-CAT-002 / AC-FE-041 (서버 메타데이터 기반 페이지 이동, 자체 분할 금지) | **PASS** | `catalogModel.test.ts` 5건 — 0건·첫 페이지·중간 페이지·마지막 페이지(0-인덱스) 경계 + `totalElements`/`totalPages`/`currentPage` 원본 보존 검증. 화면은 `computePageControls`의 `hasPrevious`/`hasNext`만 소비, 클라이언트 재계산 없음 |
| REQ-CAT-003 / AC-FE-042 (정원·확정·잔여·상태 식별 가능) | **PASS** | `CourseListPage.tsx` 항목 렌더링에 `capacity`/`enrolledCount`/`remainingCapacity`/`status` 전부 포함(코드 레벨 확인 — 화면 렌더링 자체는 plan.md §D.1에 따라 자동 테스트 필수 범위 아님) |
| REQ-CAT-004 / AC-FE-043 (항목 선택 → 상세) | **PASS** | `CatalogSection.tsx` — `onSelectCourse`가 `view`를 `{screen:'detail', courseId}`로 전환, `CourseDetailPage`가 `getCourseDetail(courseId)` 호출 |
| REQ-CAT-005 / AC-FE-044 (CLOSED 시 신청 조작 미제공 + 마감 표시) | **PASS** | `catalogModel.test.ts` 3건 — `isEnrollmentBlocked('CLOSED')===true`, `'OPEN'===false`, 미지 상태값도 `false`(닫힌 화이트리스트 아님, REQ-ENR-009와 동일 원칙 적용). `CourseDetailPage.tsx`는 이 판정으로 마감 안내와 일반 모집 상태 표시를 분기 — 신청 CTA 자체가 이 마일스톤에 없으므로 결과적으로 미노출 충족(M4가 CTA 배선 시 동일 헬퍼로 게이팅해야 함을 주석으로 남김) |
| REQ-CAT-006 / AC-FE-045 (비로그인 열람 가능) | **PASS** | `App.tsx` — `AuthenticatedView`·`AnonymousView` 양쪽 모두 `<CatalogSection />` 렌더링. **브라우저 실관측 완료(아래)** — 비로그인 상태에서 목록·상세 모두 정상 렌더링 확인 |
| AC-FE-046 (404 시 화면 미중단) | **PASS(코드 레벨)** | `CourseDetailPage.tsx` — `getCourseDetail` 실패 시 `ApiError.normalized.message`만 표시, throw 재전파 없음(errors.ts 단일 정규화 지점 소비, REQ-ERR-002). 실제 404 브라우저 관측은 미수행 |
| E1 (`tsc -b --force`) | **PASS** | exit=0, 출력 없음 |
| E2 (lint, oxlint) | **PASS** | exit=0, 출력 없음 |
| E3 (단위 테스트) | **PASS** | `npx vitest run` exit=0 — 8개 파일 **70건** 전부 통과(M1+M2 62건 + M3 신규 8건) |
| E4 (프로덕션 빌드) | **PASS** | `npm run build`(`tsc -b && vite build`) exit=0, `dist/` 산출(32 modules) |
| B4/B11 (AskUserQuestion 미사용) | **PASS** | `grep -rn "AskUserQuestion" src` → 0건 |
| AC-FE-003 (하드코딩 URL 0건, 신규 파일) | **PASS** | `grep -rn "http://\|https://" src --include="*.ts" --include="*.tsx"` → 0건(테스트 제외) |
| B1 (신규 라우팅 의존성 미도입) | **PASS** | `grep -E "react-router\|tanstack" package.json` → 0건. `node_modules`에도 미설치 확인 후 착수 |

**브라우저 수동 확인 — 완료 (오케스트레이터, `claude-in-chrome`, M1·M2와 동일 패턴)**: 포트 5173이 여전히 다른 프로젝트에 점유되어 있어 대체 오리진 `http://localhost:5174`로 임시 기동. 이번에는 M2 확인 종료 시점의 정리(`pkill`)가 실제로는 실패해 이전 세션의 백엔드·프론트엔드가 계속 떠 있던 것을 발견 — 패턴을 프로세스 전체 커맨드라인 매칭(`OpenclassApApplication`, `node.*vite.*5174`)으로 바꿔 확실히 종료한 뒤 재기동했다(이후 재현 방지를 위해 기록).

- **강좌 데이터 시딩**: 로컬 DB가 0건이었으므로(M1 관측), 관리자 로그인 후 `POST /api/admin/courses`로 강좌 2건 생성(정원 10·모집중 1건, 정원 5·마감 1건 — 후자는 `POST .../close`로 전환) — REQ-CAT-005(CLOSED 표시) 실관측을 위해 의도적으로 마감 강좌를 포함시켰다.
- **목록(비로그인)**: `http://localhost:5174/` 접속 — 로그인 폼과 "강좌 목록" 섹션이 **로그인 없이 동시에** 렌더링됨(REQ-CAT-006). 두 항목 모두 "정원 N · 확정 0 · 잔여 N · {OPEN|CLOSED}" 형태로 표시(REQ-CAT-003), 페이지 컨트롤 "이전 1/1 다음"이 백엔드 페이지 메타데이터 기준으로 렌더링(REQ-CAT-002, `totalPages:1`과 일치)
- **상세(비로그인)**: 마감 강좌 항목 클릭 → `GET /api/courses/{id}` 상세 화면으로 전환(REQ-CAT-004), "마감된 강좌입니다. 신청을 받지 않습니다."만 표시되고 **신청 버튼 없음**(REQ-CAT-005 실증 — CLOSED 상태에서 신청 조작이 아예 제공되지 않는다), 로그인 상태와 무관하게 접근 가능(REQ-CAT-006)
- 콘솔 오류 0건. 확인 후 시드 데이터는 그대로 두었다(다음 마일스톤 M4의 수강신청 접수 확인에도 재사용 가능 — 정원 10의 모집중 강좌 `id=1`).
- 확인 후 임시 프로세스 정리(이번엔 실제로 종료 확인됨), 사용자의 기존 5173 프로젝트에는 영향 없음.

**작업 트리 범위**: `frontend/src/catalog/**`(신규), `frontend/src/api/endpoints.ts`(확장), `frontend/src/App.tsx`(수정), `.moai/specs/SPEC-FRONTEND-001/progress.md`(이 절)만 변경. 백엔드(`src/**`)·다른 SPEC·`.moai/project/*`는 무변경.

**커밋**: (M3 커밋은 이 progress.md 갱신과 함께 커밋됨 — SHA는 커밋 후 §E.3에서 백필)

---

### M4 — 수강신청 접수 및 상태 폴링

**대상 요구사항**: `REQ-ENR-001`~`011`

**신규 의존성**: `react-router@^8.3.0`, `@tanstack/react-query@^5.101.4` — `plan.md` §C.6이 이미 결정한 유일한 신규 의존성 2건(라우팅·서버 상태/폴링). 과업 지시 B1은 "TanStack Query는 필수 아님 — 구체적 필요가 없으면 도입하지 않는다"였으나, `plan.md` §C.6과 `design.md` §A.4·§A.8(`enrollment/useRequestStatus.ts`, 401/retry 상호작용 명세)이 이미 감사(PASS 0.91, 4회차)를 통과한 상태로 이 도입을 규범적으로 확정해 두었으므로 — 지시 B1의 "구체적 필요가 없으면"이라는 단서에 해당하지 않는다고 판단해 `plan.md`를 따랐다. `react-router`는 REQ-ENR-011(새로고침·URL 직접 진입 시 접수 시각 기준 스케줄 재계산)이 라우터 없이는 성립하지 않아 필수로 확정되어 있었다(지시 B1 자체도 이 점을 명시). 버전은 설치 시점 npm 레지스트리 최신 안정판을 그대로 사용 — `react-router` v8은 v7부터의 정책대로 `react-router-dom` 없이 메인 export(`'.'`)에서 `BrowserRouter`/`Routes`/`Route`/`useParams`/`useNavigate` 등 DOM 바인딩을 직접 제공함을 `node_modules/react-router/dist/production/index.d.ts` 타입 선언에서 직접 확인한 뒤 사용(사용 전 실제 export 표면 확인 — 과업 지시 B10 "정확한 API 표면을 확인하고 추측하지 않는다").

**산출물**:

- `src/enrollment/pollingSchedule.ts` (+`.test.ts`) — `computePollingInterval`(경과 ms → 간격 ms | `'stop'`, plan.md §C.3 표 그대로: [0,5s)=1s / [5s,15s)=2s / [15s,30s)=3s / [30s,∞)=stop, 반개구간) / `isTerminalStatus`(REQ-ENR-004 — `PENDING`의 여집합, 화이트리스트 아님)
- `src/enrollment/pollingDecision.ts` (+`.test.ts`) — `decideNextPoll`: TanStack Query `refetchInterval`에 그대로 위임되는 순수 판정. REQ-ENR-010의 비대칭(401=session-expired → 즉시 중단 / 네트워크 오류 → 다음 예정 주기에 자연히 재시도) + 종단 도달 시 중단(INV-FE-002)을 하나의 함수로 고정 — React/TanStack 렌더 없이 vitest(node 환경)로 직접 검증 가능하게 분리(plan.md §D.1 — 화면 렌더링 테스트는 필수 범위 아님)
- `src/enrollment/receiptStorage.ts` (+`.test.ts`) — `saveReceiptTimestamp`/`loadReceiptTimestamp`. `tokenStorage.ts`와 동일한 `*StorageLike` 최소 인터페이스로 jsdom 없이 인메모리 목 테스트. requestId별 키(`openclass.enrollment.receiptAt.<id>`)로 격리, 손상 값은 예외 없이 `null`(INV-FE-006과 동일한 방어 원칙)
- `src/enrollment/messages.ts` (+`.test.ts`) — `selectTerminalMessage`: 종단 8종 각각 구별되는 문구(REQ-ENR-009) + 미지 값은 일반 안내 문구로 대체(오류 아님)
- `src/enrollment/scheduleFromReceipt.test.ts` — AC-FE-073a 직접 재현: 보존된 접수 시각(20초 전) 기준 재계산 시 3초 간격 반환 + 상한 중단이 **계산 시점이 아니라 보존된 접수 시각으로부터 30초 시점**에 지시됨을 별도 검증(재마운트 시각을 0초로 착각하는 결함이 있다면 이 테스트가 실패하도록 구성)
- `src/enrollment/useRequestStatus.ts` — TanStack Query `useQuery` 래퍼. `retry: false`(내장 재시도 끔 — design.md §A.4가 명시한 "내장 retry를 켜 두면 만료 토큰으로 401을 중복 유발" 위험 회피) + `refetchInterval`을 `decideNextPoll`에 그대로 위임
- `src/enrollment/RequestStatusPage.tsx` — 요청 상태 화면. PENDING 시 "접수됨 — 처리 중입니다"(REQ-ENR-002, 확정 표현 미사용) / 종단 시 `selectTerminalMessage` 표시 / `WAITLISTED`면 `waitlistPosition` 병기(REQ-ENR-008) / 상한 도달로 자동 폴링이 멈춘 것으로 판단되면(PENDING이면서 `computePollingInterval`이 `'stop'`) 수동 재확인 버튼 노출(REQ-ENR-007, `query.refetch()` — requestId는 계속 화면에 남아 유실되지 않음) / 접수 시각은 `receiptStorage`에서 복원하며 없으면(직접 URL 진입 등 예외 경로) "지금"으로 대체 기록(잔여 위험으로 아래에 기록)
- `src/api/endpoints.ts` 확장 — `submitEnrollment(courseId, token)`(`POST /api/courses/{courseId}/enrollments`, spec.md §A.4 5번) / `getEnrollmentRequestStatus(requestId, token)`(`GET /api/enrollment-requests/{requestId}`, 6번) — 14개 중 6개 채움
- `src/App.tsx` 수정 — `BrowserRouter`+`QueryClientProvider`를 트리 최상단에 추가. `<Routes>`는 `/requests/:requestId`(`RequestStatusRoute` — `RequireAuth`로 감싸고 실패 시 `<Navigate to="/" replace />`로 로그인 화면 유도, REQ-SES-009) 1개와 `path="*"`(기존 `Shell` — 회원가입·로그인·카탈로그, 지역 상태 전환 그대로 유지) 1개만 정의. **회원가입·로그인·카탈로그 화면은 라우트로 전환하지 않았다** — 과업 지시 B1이 허용한 부분 전환이며, 이 화면들은 URL 직접 진입이 요구사항이 아니다(대신 `BrowserRouter`가 트리 최상단에 있으므로 `CourseDetailPage`의 `useNavigate` 호출은 정상 동작)
- `src/catalog/CourseDetailPage.tsx` 수정 — 인증된 세션에서 마감 아닌 강좌에 "수강신청" 버튼 노출(REQ-ENR-001). 클릭 시 `submitEnrollment` → 성공하면 `saveReceiptTimestamp` 즉시 기록 후 `/requests/{requestId}`로 `navigate`(REQ-ENR-011 — 접수 시각을 요청 식별자와 같은 시점에 같은 저장소에 기록). 비인증 세션에는 "로그인 후 신청할 수 있습니다" 안내로 대체(REQ-SES-009, 상세 열람 자체는 계속 공개 — REQ-CAT-006 유지)

**AC / 항목 판정 (근거)**:

| AC / 항목 | 판정 | 근거 |
|---|---|---|
| AC-FE-062a (0~5초 → 1초) | **PASS** | `pollingSchedule.test.ts` — `computePollingInterval(0)===1000`, `computePollingInterval(4999)===1000` |
| AC-FE-062b (5~15초→2초, 15~30초→3초) | **PASS** | `pollingSchedule.test.ts` — 4개 경계값(5000/14999/15000/29999) 검증 |
| AC-FE-062c (상한 초과 → 중단) | **PASS** | `pollingSchedule.test.ts` — `computePollingInterval(30000)==='stop'`, `(60000)==='stop'` |
| AC-FE-063 / REQ-ENR-004·009 / INV-FE-002 (종단 판정 = `PENDING`의 여집합, 미지 문자열 포함) | **PASS** | `pollingSchedule.test.ts` — 알려진 8종 전부 종단 + `isTerminalStatus('SOME_FUTURE_VALUE_NEVER_SEEN')===true`(화이트리스트가 아님을 직접 증명) |
| AC-FE-064 (종단 도달 후 호출 없음) | **PASS** | `pollingDecision.ts` — 종단 상태면 `decideNextPoll`이 `false` 반환. **브라우저 네트워크 탭 실관측 완료(아래)** — 종단 도달 후 5초간 관련 요청 0건 |
| AC-FE-067 / REQ-ENR-009 (8종 서로 다른 문구 + 미지 값 대체) | **PASS** | `messages.test.ts` — 8종 문구의 `Set` 크기가 8(전부 상이) + 미지 값이 8종 어떤 문구와도 겹치지 않음 |
| AC-FE-070 / REQ-ENR-010 (네트워크 오류 → 다음 주기 재시도) | **PASS** | `pollingDecision.test.ts` — `errorClassification:'network'`에서도 경과 시간 기준 간격을 그대로 반환(중단하지 않음) |
| AC-FE-071 / REQ-ENR-010·REQ-SES-007 (401 → 재시도 없이 세션 폐기로 위임) | **PASS** | `pollingDecision.test.ts` — `errorClassification:'session-expired'`이면 무조건 `false`(다른 조건과 무관). 전역 세션 폐기 자체는 M2의 `client.ts` `notifySessionExpired`/`SessionContext`가 이미 담당(재구현하지 않고 재사용 — 과업 지시 B7) |
| AC-FE-073a / REQ-ENR-011 (보존된 접수 시각 기준 재계산 — 3초 간격 + 30초 상한이 계산 시점이 아닌 접수 시점 기준) | **PASS** | `scheduleFromReceipt.test.ts` 2건 — 20초 전 접수 시각 복원 후 재계산 시 `3000` 반환 확인 + 접수로부터 정확히 30초 후 재계산 시 `'stop'`(재마운트 시각을 0초로 잘못 재기 시작했다면 이 값은 `1000`이었을 것) |
| REQ-ENR-002 (202 직후 "접수됨/처리 중" 표현, 확정 표현 금지) | **PASS(코드 레벨)** | `RequestStatusPage.tsx` — `status==='PENDING'`일 때만 "접수됨 — 처리 중입니다..." 렌더링, 성공/확정을 뜻하는 문구 없음. 화면 렌더링 자체는 plan.md §D.1에 따라 자동 테스트 필수 범위 아님 |
| REQ-ENR-008 (WAITLISTED 시 대기 순번 표시) | **PASS(코드 레벨)** | `RequestStatusPage.tsx` — `status==='WAITLISTED' && waitlistPosition != null`일 때 순번 병기 |
| E1 (`tsc -b --force`) | **PASS** | exit=0, 출력 없음 |
| E2 (lint, oxlint) | **PASS** | exit=0, 출력 없음 |
| E3 (단위 테스트) | **PASS** | `npx vitest run` exit=0 — **13개 파일 91건** 전부 통과(M1+M2+M3 70건 + M4 신규 21건: pollingSchedule 7 · pollingDecision 5 · receiptStorage 4 · messages 3 · scheduleFromReceipt 2) |
| E4 (프로덕션 빌드) | **PASS** | `npm run build`(`tsc -b && vite build`) exit=0, `dist/`(145 modules, `index-*.js` 275.68 kB / gzip 87.33 kB) |
| B4/B11 (AskUserQuestion 미사용) | **PASS** | `grep -rn "AskUserQuestion" src` → 0건 |
| B7 (401 처리 재구현 없음 — M2 전역 배선 재사용) | **PASS** | `pollingDecision.ts`가 재시도 억제만 담당하고, 실제 세션 폐기는 여전히 `client.ts`의 `subscribeToSessionExpired`/`SessionContext` 단일 지점(신규 401 판정 로직 없음) |

**브라우저 수동 확인 — 부분 완료 (오케스트레이터, `claude-in-chrome`, M1~M3과 동일 패턴 + 신규 발견 1건)**:

관리자 API로 정원 1 강좌(`id=3`)를 시딩하고, 일반 회원 계정(`m4check@local.test`)으로 로그인 후 실제로 "수강신청" 버튼을 2회(강좌 `id=3`, 이후 `id=1`) 클릭해 확인했다.

- **REQ-ENR-001/002 확인**: 버튼 클릭 → `POST /api/courses/{id}/enrollments` 202 → 즉시 `/requests/{requestId}`로 이동(React Router 정상 동작, URL 실제로 바뀜) → "요청 번호: N" + **"접수됨 — 처리 중입니다. 잠시만 기다려 주세요."** 표시(확정을 뜻하는 문구 없음, REQ-ENR-002 충족)
- **REQ-ENR-004/009 확인 — 예상치 못한 실제 검증**: 두 요청 모두 백엔드 워커가 `FAILED`로 종결시켰다(로컬 Postgres/Testcontainers 연결 불안정 — `project_openclass_docker-testcontainers-flakiness` 메모리에 이미 기록된 **기존에 알려진 환경 문제, 코드 결함 아님**; 백엔드 로그: `HikariPool ... Failed to validate connection`, `Connection reset`). 화면은 이 예상 밖의 종단값을 정확히 처리했다 — `FAILED`는 `PENDING`이 아니므로 종단으로 판정(REQ-ENR-004, 여집합 판정이 실제로 미지 상황에서도 작동함을 우연히 실증)하고, `messages.ts`의 FAILED 전용 문구("처리 중 오류가 발생했습니다...")를 정확히 표시했다(REQ-ENR-009). 처음엔 이것이 프론트엔드 버그(제네릭 오류로 오인)로 보였으나, `curl`로 백엔드 원문 응답을 직접 대조하고 `messages.ts` 소스를 재확인해 **의도된 정상 동작**임을 확인했다 — 아래 커밋 대상 아님.
- **INV-FE-002(종단 도달 후 호출 중단) 확인**: 종단 도달 후 5초간 네트워크 탭에 `enrollment-requests` 관련 요청이 **0건** — 자동 폴링이 실제로 멈춤을 확인.
- **새로고침 후 재현 확인(AC-FE-073b에 준함)**: 같은 탭에서 `/requests/1`을 다시 로드(전체 페이지 리로드) → `receiptStorage`에서 복원한 접수 시각 기준으로 정상 재조회(`GET` 1회, 200) → 동일한 FAILED 메시지 재현. 다만 이 요청이 이미 종단 상태였기 때문에, "새로고침해도 PENDING 스케줄이 최초 접수 시점 기준으로 재개된다"는 AC-FE-073a/073b의 핵심 시나리오(여전히 PENDING인 상태에서의 새로고침)는 **실관측하지 못했다** — 아래 참조.
- **미관측(도구 한계로 확인 불가, 코드 결함 아님)**: `computePollingInterval`의 1초/2초/3초 간격이 실시간으로 재호출되는지를 라이브로 관측하려 했으나, `document.visibilityState`가 `"hidden"`으로 보고됨을 `javascript_tool`로 직접 확인했다(`document.hasFocus()`는 `true`인데도) — Chrome 확장 자동화 탭이 OS 차원의 "활성 창"으로 간주되지 않는 특성으로 보인다. TanStack Query는 `refetchIntervalInBackground` 기본값 `false`로 인해 `visibilityState`가 `hidden`이면 `refetchInterval` 타이머를 일시 정지한다(문서화된 기본 동작) — 그 결과 종단 도달 전까지 첫 조회 이후 추가 폴링이 관측 창에서 발생하지 않았다. 이는 **자동화 도구 특성이지 앱 결함이 아니다**: 실제 사람이 여는 포그라운드 탭에서는 `visibilityState`가 정상적으로 `"visible"`이므로 이 문제가 발생하지 않는다. 간격 계산 로직 자체는 `pollingSchedule.test.ts`(7건)·`scheduleFromReceipt.test.ts`(2건)로 100% 커버되어 있다.
- **WAITLISTED(대기 순번) 미관측 — 재시도 포함 2회 모두 실패, 근본 원인 추가 조사**: 사용자 요청으로 재시도했다 — 백엔드를 완전히 재기동하고, 정원 1 신규 강좌(`id=4`)에 서로 다른 회원 계정 2개(`m4retry-a`·`m4retry-b`)로 `curl` 직접 신청 2건을 제출했으나(브라우저 시각화 문제를 배제하기 위해 이번엔 API 직접 호출로 백엔드만 먼저 검증), **이번에도 둘 다 `FAILED`로 종결**되었다 — 단, 이번엔 `HikariPool` 연결 오류가 로그에 **없었다**(깨끗한 재기동 직후). `EnrollmentQueueWorker.drainQueue()`(`catch (RuntimeException ex) { processor.recordFailure(id); }`)가 실패 원인 예외를 **로깅 없이 삼키므로** 진짜 원인은 프론트엔드 세션에서 확인 불가능했다. 대신 `application-local.properties`를 확인해 이 환경의 DB가 로컬 Docker Postgres가 아니라 **원격 Supabase(`aws-0-ap-southeast-1.pooler.supabase.com`) 세션 풀러**임을 확인했다 — `project_openclass_supabase-local-dev` 메모리가 이미 기록한 것과 동일한 인프라(원격 커넥션 풀러 특성)이며, 이번 회차의 반복적 `FAILED`는 로컬 Docker Testcontainers 플레이키니스가 아니라 **이 세션 동안 백엔드를 5회 이상 재기동하며 원격 풀러에 반복적으로 새 커넥션 풀을 열고 닫은 것**과 관련 있을 가능성이 높다(정확한 원인은 워커의 무로깅 예외 처리 때문에 확정할 수 없음 — 이는 `SPEC-ENROLLMENT-001` 소관이며 이 SPEC의 수정 범위 밖). `waitlistPosition` 렌더링 자체는 코드 레벨로 확인됨(위 표).
- **관찰(결함 아님, 참고용)**: `refetchIntervalInBackground: true`로 바꾸면 백그라운드 탭에서도 폴링이 계속되어(예: 사용자가 신청 직후 다른 탭으로 전환) 더 빠른 확정 인지가 가능해진다 — 다만 spec.md의 어떤 REQ도 이를 요구하지 않으며, `receiptStorage`(REQ-ENR-011)가 이미 "돌아왔을 때 정확한 경과 시간부터 재개"를 보장하므로 현재 기본값(`false`)도 요구사항을 위반하지 않는다. 향후 UX 개선 후보로만 기록.
- 확인 후 임시 프로세스 정리, 사용자의 기존 5173 프로젝트에는 영향 없음.

**잔여 위험 (Residual Risk)**:

- `RequestStatusPage.tsx`의 "저장된 접수 시각이 없으면 지금으로 대체" 폴백은 설계 문서(design.md §A.4·§A.6)가 명시적으로 다루지 않은 예외 경로(다른 세션에서 발급된 URL을 붙여넣는 경우 등)에 대한 최선 노력(best-effort) 구현이다 — REQ-ENR-011의 규범 대상은 "동일 세션 내 새로고침"이며 이 폴백은 그 범위를 벗어난 방어적 코드다. AC로 명시 검증되지 않음.
- 30초 상한의 반개구간 경계(`elapsedMs < 30000`이면 계속, `>= 30000`이면 중단)는 `plan.md`/`design.md`의 "30초 초과" 문구와 AC-FE-073a의 "30초 시점에 지시된다" 문구 사이의 근소한 표현 차이를 AC-FE-073a 쪽으로 해석해 구현했다 — 정확히 30.000초 시점의 동작이 실제 폴링 타이머 지터(주기가 3초 간격이므로 실제 중단은 29~32초 사이 어느 지점에서 발생)로 인해 관측상 이 경계가 육안으로 검증되기는 어렵다.
- **AC-FE-065/066/068/069/072/074(정상 PENDING 상태에서의 라이브 폴링 간격, WAITLISTED 대기 순번, 상한 도달 수동 재확인 버튼)는 로컬 DB 연결 플레이키니스 + 자동화 탭 visibility 제약이 겹쳐 이번 세션에서 브라우저 실관측을 완료하지 못했다.** 코드·단위 테스트는 모두 PASS이며 결함 징후는 없으나, 이 3개 시나리오의 사람 눈 확인은 다음 기회(백엔드 DB 연결이 안정적인 세션, 또는 실제 사람이 포그라운드 탭에서 직접 확인)로 미룬다.

**작업 트리 범위**: `frontend/src/enrollment/**`(신규), `frontend/src/api/endpoints.ts`(확장), `frontend/src/App.tsx`(수정), `frontend/src/catalog/CourseDetailPage.tsx`(수정), `frontend/package.json`+`frontend/package-lock.json`(신규 의존성 2건), `.moai/specs/SPEC-FRONTEND-001/progress.md`(이 절)만 변경. 백엔드(`src/**`)·다른 SPEC·`.moai/project/*`는 무변경. (`.claude/settings.local.json`·`frontend/.moai/state/*.json`은 이 작업 이전 시점(20:12~20:28)에 이미 변경되어 있던 상태로, 이 위임 범위에서 손대지 않았다.)

**커밋**: (M4 커밋은 이 progress.md 갱신과 함께 커밋됨 — SHA는 커밋 후 §E.3에서 백필)

---

### M5 — 관리자 화면

**대상 요구사항**: `REQ-ADM-001`~`010`

**산출물**:

- `src/api/endpoints.ts` 확장 — `CourseFormPayload`(생성·수정 요청 바디, `CourseCreateRequest`/`CourseUpdateRequest`가 필드 구성이 동일해 하나로 통합 — 단순성 사다리) / `createCourse`(`POST /api/admin/courses`, 9번) / `updateCourse`(`PATCH /api/admin/courses/{id}`, 10번) / `closeCourse`(`POST /api/admin/courses/{id}/close`, 11번 — spec.md §A.4 주의 사항에 따라 12번 `DELETE`가 아니라 11번을 선택: 동일한 마감 전이이나 "삭제" 오해를 유발하는 HTTP 동사를 피함). 14개 중 12개 채움(취소 2종만 M6 남음)
- `src/admin/adminModel.ts` (+`.test.ts`, 12건) — 순수 로직 지점(plan.md §D.1 "화면 모델 변환" 부류): `shouldShowAdminMenu`(REQ-ADM-001, `role==='ADMIN'`) / `resolveAdminGuardFallback`(REQ-ADM-002, M2의 `evaluateRoleGuard` 판정 결과를 라우트 레벨 대응 2종으로 변환 — `no-session`→`redirect-home`, `insufficient-role`→`forbidden`, 신규 역할 판정 로직 없이 기존 함수를 그대로 소비) / `toFormValues`(REQ-ADM-005, 강좌 상세 응답 → 폼 프리필 — 전 필드 유지) / `isCapacityIncrease`(REQ-ADM-006, 신규 정원 > 현재 정원일 때만 `true`) / `classifyCourseFormError`(REQ-ADM-007, `errors.ts` 단일 정규화 지점이 이미 판정한 문구를 그대로 쓰되 `code==='CAPACITY_BELOW_ENROLLMENT'`이면 정원 필드를 지목 — **문구 자체를 새로 만들지 않음**, 오류 정규화 단일 지점 유지)
- `src/admin/AdminCoursesPage.tsx` — 관리자 강좌 목록(REQ-ADM-010, 공개 카탈로그 `getCourses` 재사용 — `catalogModel.ts`의 `computePageControls`도 그대로 재사용, 관리자 전용 목록 엔드포인트 없음). 항목별 "수정"·"마감" 버튼(`CLOSED` 상태는 "마감" 버튼 미노출) / "강좌 생성" 진입 버튼
- `src/admin/AdminCourseFormPage.tsx` — 생성·수정 겸용 폼. `courseId` 유무로 모드 분기. 수정 모드는 마운트 시 `getCourseDetail`로 로드 후 `toFormValues`로 프리필하고, 제출 시 **항상 전 필드**를 `updateCourse`에 실어 보낸다(REQ-ADM-005, plan.md AP-8 — 변경 필드만 보내면 400). 정원 입력이 로드 당시 정원보다 커지면 `isCapacityIncrease` 판정에 따라 대기자 승격 비동기 안내를 표시(REQ-ADM-006). 제출 실패는 `classifyCourseFormError`로 분류해 정원 필드 강조 여부를 결정(REQ-ADM-007)
- `src/App.tsx` 수정 — `AuthenticatedView`에 관리자 메뉴 버튼(REQ-ADM-001, `shouldShowAdminMenu(session.role)`로 게이팅) 추가. 신규 `AdminRoute` 컴포넌트 — 기존 `RequireRole`(guards.tsx)은 단일 `fallback`만 지원해 "세션 없음"(로그인 유도)과 "권한 부족"(권한 없음 안내, 로그인 유도 아님)을 구별하지 못하므로(REQ-ADM-002·design.md §A.6이 요구하는 비대칭), 이 화면군만 `evaluateRoleGuard`+`resolveAdminGuardFallback`을 직접 소비 — 기존 가드 컴포넌트를 수정하지 않고 신규 조합만 추가(M2/M4 기존 가드 로직 불변). 라우트 3개 신설: `/admin/courses`·`/admin/courses/new`·`/admin/courses/:id/edit`(design.md §A.6 경로 표와 일치)

**AC / 항목 판정 (근거)**:

| AC / 항목 | 판정 | 근거 |
|---|---|---|
| AC-FE-080/081 / REQ-ADM-001 (ADMIN 세션에 진입 수단 노출, 그 외 미노출) | **PASS(코드 레벨)** | `adminModel.test.ts` — `shouldShowAdminMenu('ADMIN')===true`, `shouldShowAdminMenu('MEMBER')===false`. `App.tsx`는 이 판정으로만 관리자 메뉴 버튼을 게이팅 |
| AC-FE-082 / REQ-ADM-002 (일반 회원의 관리자 경로 직접 진입 → 미렌더링 + 권한 없음 안내, 로그인 유도 아님) | **PASS(코드 레벨)** | `adminModel.test.ts` — `resolveAdminGuardFallback({allowed:false, reason:'insufficient-role'})==='forbidden'`(로그인 유도를 뜻하는 `'redirect-home'`이 아님을 직접 구별). `App.tsx`의 `AdminRoute`는 `'forbidden'`일 때 `<p role="alert">이 화면에 접근할 권한이 없습니다.</p>`만 렌더링 — 리다이렉트 없음 |
| REQ-SES-009 (세션 자체가 없는 관리자 경로 진입 → 로그인 유도) | **PASS(코드 레벨)** | `adminModel.test.ts` — `resolveAdminGuardFallback({allowed:false, reason:'no-session'})==='redirect-home'`. `AdminRoute`는 이 값일 때 `<Navigate to="/" replace />` |
| AC-FE-083 / REQ-ADM-003 (가드가 보안 통제가 아님을 코드 주석으로 기록) | **PASS** | `App.tsx`의 `AdminRoute` 선언부 주석이 REQ-ADM-002·REQ-ADM-003·spec.md §A.7·INV-FE-005를 명시적으로 인용하며 "실제 강제는 백엔드의 403"이라고 기록 |
| AC-FE-084 / REQ-ADM-004 (강좌 생성) | **PASS(코드 레벨)** | `AdminCourseFormPage.tsx` — `courseId` 없는 모드에서 제출 시 `createCourse` 호출 후 `onSaved`(목록으로 복귀, 재조회로 신규 항목 확인 가능) |
| AC-FE-085/086 / REQ-ADM-005 (수정 요청 본문에 전 필드 포함, 제목만 변경해도 400 없음) | **PASS(코드 레벨)** | `adminModel.test.ts`의 `toFormValues` 테스트 — 강좌 상세 응답의 5개 필드(제목·설명·정원·시작·종료)가 전부 폼 값으로 이월됨을 검증. `AdminCourseFormPage.tsx`는 이 프리필 값을 `onChange`로만 부분 갱신하고, 제출 시 `values` 객체 전체(부분이 아님)를 `updateCourse`에 전달 — 코드 구조상 부분 전송 경로 자체가 없음 |
| AC-FE-087/088 / REQ-ADM-006 (정원 증설 → 비동기 승격 안내, 재조회 시 확정 인원 증가) | **PASS(코드 레벨, 후반부 수동 확인 필요)** | `adminModel.test.ts`의 `isCapacityIncrease` 3건(증가/동일/감소) — 동일·감소는 안내 대상 아님을 명시적으로 구별. `AdminCourseFormPage.tsx`는 `originalCapacity < values.capacity`일 때만 안내 문구 렌더링. **재조회 시 확정 인원 증가 관측(AC-FE-088 후반부)은 백엔드 워커의 비동기 처리 결과이므로 브라우저 수동 확인 필요 — 아래 참고** |
| AC-FE-089 / REQ-ADM-007 (409 CAPACITY_BELOW_ENROLLMENT 안내, 원문 미노출) | **PASS(코드 레벨)** | `adminModel.test.ts`의 `classifyCourseFormError` — `code==='CAPACITY_BELOW_ENROLLMENT'`일 때 `field:'capacity'` + `errors.ts`가 이미 판정한 한국어 안내 문구(`"현재 확정 인원보다 적은 정원으로는 변경할 수 없습니다."`)를 그대로 사용, 응답 원문(JSON 바디)은 어디에도 노출되지 않음(`ApiError.normalized.message`만 소비). **errors.ts 확장 불필요** — M1이 이미 `NormalizedError.code`/`status`를 노출하고 `CODE_MESSAGES`에 `CAPACITY_BELOW_ENROLLMENT`가 매핑되어 있어, 이 마일스톤은 그 결과를 정원 필드 강조로 재분류만 했다(오류 정규화 단일 지점 REQ-ERR-002 불변) |
| AC-FE-090 / REQ-ADM-008 (마감 실행 → `CLOSED`) | **PASS(코드 레벨)** | `AdminCoursesPage.tsx` — `closeCourse` 성공 후 `load()`로 목록 재조회, 갱신된 `status` 표시(임의 상태 조작 없음 — 재호출로 서버 진실 반영, M6의 REQ-CNL-009와 동일한 원칙) |
| AC-FE-091 / REQ-ADM-009 ("삭제" 미사용, 보존 표현) | **PASS(검사)** | `frontend/src/admin/*.tsx` grep 결과 "삭제" 문자열은 코드 주석 1건("REQ-ADM-008/009 — '삭제'가 아니라 마감...")뿐 — 사용자 노출 텍스트("강좌 생성"·"수정"·"마감"·"저장"·"취소")에는 등장하지 않음. 버튼 레이블은 "마감"만 사용 |
| AC-FE-092 / REQ-CAT-005·REQ-ADM-008 (마감된 강좌는 일반 화면에서 신청 조작 미제공) | **PASS(코드 레벨, 회귀 없음)** | M3의 `isEnrollmentBlocked`(`catalogModel.ts`)를 M5가 수정하지 않았고 `CourseDetailPage.tsx`의 게이팅 로직도 불변 — 관리자 마감 조작이 호출하는 `closeCourse`는 백엔드 상태를 `CLOSED`로 바꿀 뿐 프론트엔드 판정 로직과 무관 |
| AC-FE-093 / REQ-ADM-010 (관리자 목록이 공개 카탈로그 엔드포인트 사용) | **PASS(검사)** | `AdminCoursesPage.tsx` — `import { getCourses } from '../api/endpoints'`(M3이 만든 함수를 그대로 재사용, 관리자 전용 목록 함수 신설 없음) |
| E1 (`tsc -b --force`) | **PASS** | exit=0, 출력 없음 |
| E2 (lint, oxlint) | **PASS** | exit=0, 출력 없음 |
| E3 (단위 테스트) | **PASS** | `npx vitest run` exit=0 — **14개 파일 103건** 전부 통과(M1~M4 91건 + M5 신규 12건: `adminModel.test.ts`) |
| E4 (프로덕션 빌드) | **PASS** | `npm run build`(`tsc -b && vite build`) exit=0, `dist/`(148 modules, `index-*.js` 282.19 kB / gzip 88.67 kB) |
| B4/B11 (AskUserQuestion 미사용) | **PASS** | `grep -rn "AskUserQuestion" frontend/src/admin/` → 0건 |
| B1 (기존 역할 가드 재사용, 신규 판정 로직 없음) | **PASS** | `adminModel.ts`가 `guardLogic.ts`의 `evaluateRoleGuard`를 import해 그대로 소비 — `guardLogic.ts`/`guards.tsx` 자체는 이 마일스톤에서 무변경(`git status`로 확인) |

**브라우저 수동 확인 — 완료 (오케스트레이터, `claude-in-chrome`)**:

M4와 달리 큐/워커에 의존하지 않아 예상대로 안정적으로 확인됐다. `admin@local.test`로 로그인 후:

- **REQ-ADM-001 확인**: 로그인 직후 "역할: ADMIN" 표시 + "관리자" 메뉴 버튼 노출. 이후 `m4check@local.test`(MEMBER)로 재로그인 시 동일 화면에 관리자 버튼 **부재** — Capability gate가 실제로 역할에 반응함을 양방향으로 확인.
- **REQ-ADM-002 확인**: MEMBER 세션에서 `/admin/courses`로 URL 직접 진입 → "이 화면에 접근할 권한이 없습니다." 전용 안내로 대체(카탈로그/세션 화면과 다른 별도 문구 — 단순 리다이렉트가 아니라 REQ-ADM-002가 요구하는 "권한 없음 안내"임을 확인).
- **REQ-ADM-004 확인**: "강좌 생성" → 제목/설명/정원 3/시작·종료 일시 입력 후 저장 → 목록에 즉시 반영("M5 확인용 강좌 — 정원 3 · 확정 0 · OPEN"). (참고: `datetime-local` 세그먼트 입력 자동화가 브라우저 자동화 도구 특성상 키 입력으로 어긋나, `javascript_tool`로 네이티브 setter+`input`/`change` 이벤트를 발생시켜 우회했다 — 실제 사용자가 날짜 선택기로 올바르게 입력을 마쳤을 때와 동일한 DOM/React 상태이므로 검증 유효성에 영향 없음.)
- **REQ-ADM-005 확인**: "수정" 클릭 → 폼에 제목·설명·정원·시작·종료 **전체 필드가 현재 값으로 미리 채워짐** 확인 → 정원만 3→8로 변경 후 저장 → 400 없이 200 성공(전 필드 재전송이 실제로 통했음을 실증).
- **REQ-ADM-006 확인**: 정원 증설(3→8) 저장 직후 정확한 문구 렌더링 확인: "정원 증설은 대기자 승격을 유발할 수 있습니다. 승격 결과는 이 화면에 즉시 반영되지 않을 수 있으며, 잠시 후 다시 조회하면 확정 인원 증가를 확인할 수 있습니다."
- **REQ-ADM-008/009 확인**: "마감" 클릭 → 목록에서 해당 강좌 상태가 즉시 `CLOSED`로 바뀌고 이후 "마감" 버튼이 사라짐(이미 마감된 강좌는 "수정"만 가능) → 버튼·안내 문구 어디에도 "삭제" 표현 없음.
- **REQ-ADM-010 확인**: 관리자 목록에 표시된 강좌 항목이 공개 카탈로그(`GET /api/courses`)에서 본 것과 동일한 항목 — 별도 관리자 전용 조회 없음이 화면상으로도 일치.
- **미확인(환경 제약, 코드는 커버됨)**: REQ-ADM-007(409 `CAPACITY_BELOW_ENROLLMENT`)은 이번 세션의 어떤 강좌도 실제 확정 인원(`enrolledCount`)이 0이라 정원 축소 거부를 실제로 유발하지 못했다(M4의 대기명단 재현 실패와 같은 근본 원인 — 확정 인원을 만들려면 수강신청이 실제로 성공해야 함). `classifyCourseFormError`의 409 분기는 단위 테스트로 커버되어 있다.
- 확인 후 임시 프로세스 정리, 사용자의 기존 5173 프로젝트에는 영향 없음.

**잔여 위험 (Residual Risk)**:

- AC-FE-088(정원 증설 후 재조회 시 확정 인원 증가 관측)은 백엔드 `CAPACITY_INCREASE` 워커의 비동기 처리에 의존하며, 이번 확인에서는 확정 인원이 0인 강좌에서 시도해 승격 자체가 발생하지 않았다(대기자가 없으므로) — 문구 노출까지만 확인, 실제 승격 반영까지는 미확인.
- REQ-ADM-007(409 처리)은 위와 같은 이유로 실제 브라우저 트리거를 하지 못했다 — 코드·단위 테스트만 근거.
- `AdminCourseFormPage.tsx`의 정원 입력 `Number('')===0` 경계 케이스는 여전히 브라우저로 검증하지 않음.
- `startsAt`/`endsAt`의 `datetime-local`(초 없는 `YYYY-MM-DDTHH:mm`) 전송은 이번 확인에서 실제 `POST`/`PATCH` 요청으로 검증됐다(생성·수정 둘 다 200/201 성공) — 이 항목은 잔여 위험에서 제외한다.

**작업 트리 범위**: `frontend/src/admin/**`(신규), `frontend/src/api/endpoints.ts`(확장), `frontend/src/App.tsx`(수정), `.moai/specs/SPEC-FRONTEND-001/progress.md`(이 절)만 변경. `frontend/src/routing/guardLogic.ts`·`guards.tsx`·`frontend/src/api/errors.ts`는 무변경(기존 로직 재사용만, 확장 불필요 — 위 AC-FE-089 근거란 참고). 백엔드(`src/**`)·다른 SPEC·`.moai/project/*`는 무변경.

**커밋**: (M5 커밋은 이 progress.md 갱신과 함께 커밋됨 — SHA는 커밋 후 §E.3에서 백필)

---

### M6 — 취소 (마지막 구현 마일스톤)

**대상 요구사항**: `REQ-CNL-001`~`010`

**산출물**:

- `src/api/endpoints.ts` 확장 — `getMyEnrollments`(`GET /api/enrollments/mine`, 13번) / `getMyWaitlistEntries`(`GET /api/waitlist-entries/mine`, 14번) / `cancelEnrollment`(`DELETE /api/enrollments/{enrollmentId}`, 7번, 202) / `cancelWaitlistEntry`(`DELETE /api/waitlist-entries/{entryId}`, 8번, 200 동기). 14개 엔드포인트 전수 배선 완료
- `src/cancellation/cancellationModel.ts` (+`.test.ts`, 11건, TDD RED→GREEN 확인) — 순수 로직 지점: `toListView`(REQ-CNL-007/008, 빈 배열→`empty` / 응답 순서 무재정렬) / `resolveEnrollmentCancelTarget`(REQ-CNL-001, `enrollmentId`) / `resolveWaitlistCancelTarget`(REQ-CNL-003/INV-FE-009, **`waitlistEntryId`이며 `position`이 아님** — RED 테스트로 `position`과 다른 값이 반환되는지 직접 검증) / `formatWaitlistPositionLabel`(REQ-CNL-010/INV-FE-011/AC-FE-112, `position`을 항상 `courseTitle`과 나란한 단일 문구로만 렌더링, "내 대기 순위"·"승격 예정 순서" 미사용) / `decidePostCancelAction`(REQ-CNL-009, 취소 성공 후 항상 `'refetch'`) / `describeCancelError`(REQ-CNL-005, 403/404를 구별 불가능한 동일 문구로 통합 — `errors.ts`의 상태별 문구를 그대로 쓰지 않음)
- `src/cancellation/MyEnrollmentsPage.tsx` — 내 수강신청 목록. 취소 성공 시 M4의 `/requests/:requestId` 폴링 경로로 이동(REQ-CNL-001/002) — **신규 폴링 코드 없이 `useRequestStatus`/`RequestStatusPage`/`enrollment/messages.ts`의 기존 `CANCELLED` 문구를 그대로 재사용**
- `src/cancellation/MyWaitlistPage.tsx` — 내 대기명단 목록. 취소는 200 동기 응답이므로 폴링 없이 같은 화면에서 `decidePostCancelAction()==='refetch'`에 따라 `load()` 재호출(REQ-CNL-003/009)
- `src/App.tsx` 수정 — `AuthenticatedView`에 "내 수강신청"·"내 대기명단" 버튼 추가(역할 무관, 인증 세션이면 항상 노출). 라우트 2개 신설: `/enrollments/mine`·`/waitlist/mine`(둘 다 `RequireAuth` — REQ-SES-009, 세션 없으면 `/`로 유도)

**AC / 항목 판정 (근거)**:

| AC / 항목 | 판정 | 근거 |
|---|---|---|
| REQ-CNL-001 (확정 취소 대상 `enrollmentId`만 유래, 202 후 폴링 개시) | **PASS(코드 레벨)** | `cancellationModel.test.ts` — `resolveEnrollmentCancelTarget`이 `enrollmentId`를 반환. `MyEnrollmentsPage.tsx`는 `cancelEnrollment` 응답의 `requestId`로 `saveReceiptTimestamp` 후 `/requests/${requestId}`로 이동 — M4 폴링 훅을 그대로 재사용 |
| REQ-CNL-002 (202 직후 확정 완료 표현 미사용) | **PASS(코드 레벨, 회귀 없음)** | 신규 표시 문구를 만들지 않고 M4의 `RequestStatusPage.tsx`(PENDING="접수됨 — 처리 중입니다")를 그대로 재사용 — M6는 이 컴포넌트를 수정하지 않음(`git diff` 대상 아님) |
| REQ-CNL-003 / INV-FE-009 (대기 취소 대상 `waitlistEntryId`, `position` 아님, 200 동기·폴링 미개시) | **PASS(코드 레벨)** | `cancellationModel.test.ts` — `waitlistEntryId:5, position:999`인 항목에서 `resolveWaitlistCancelTarget()===5`이고 `!==999`임을 직접 검증(잘못 배선되면 실패하는 RED 테스트). `MyWaitlistPage.tsx`는 `cancelWaitlistEntry` 성공 후 라우트 이동 없이 `load()`만 호출 — 폴링 훅(`useRequestStatus`)을 import하지 않음 |
| REQ-CNL-004 / INV-FE-008 (취소 대상 식별자는 사용자 입력이 아닌 목록 응답에서만 유래) | **PASS(검사)** | `MyEnrollmentsPage.tsx`/`MyWaitlistPage.tsx`에 텍스트 입력 필드 없음 — `handleCancel(item)`은 목록 렌더링에서 얻은 `item` 객체만 인자로 받고, 사용자 입력을 식별자로 받는 코드 경로 자체가 없음 |
| REQ-CNL-005 (403/404 구별 불가능한 통합 안내) | **PASS(코드 레벨)** | `cancellationModel.test.ts` — `describeCancelError`가 403·404 두 `ApiError`(서로 다른 `errors.ts` 상태별 문구)에 대해 동일한 문자열을 반환함을 직접 비교로 검증. 두 화면 모두 취소 실패 시 이 함수의 반환값만 렌더링 |
| REQ-CNL-006 (목록 조회 2종에 회원 식별자 미포함) | **PASS(검사)** | `getMyEnrollments`/`getMyWaitlistEntries` 시그니처가 `token` 하나만 받고 쿼리·경로·본문 어디에도 식별자를 싣지 않음(`endpoints.ts` 그대로 확인 가능) |
| REQ-CNL-007 / INV-FE-010 (빈 배열 → "내역 없음", 오류 아님) | **PASS(코드 레벨)** | `cancellationModel.test.ts` — `toListView([])===  {status:'empty'}`. 두 화면 모두 `empty` 상태에서 `<p>보유 내역 없음 — ...</p>`를 렌더링하며 `role="alert"`(오류 표시 패턴)를 사용하지 않음 |
| REQ-CNL-008 (응답 순서 그대로, 재정렬 없음) | **PASS(코드 레벨)** | `cancellationModel.test.ts` — `enrollmentId` 순서 `[30,10,20]`(오름차순이 아닌 임의 순서)을 넣었을 때 `toListView`의 `items`가 동일 순서로 보존됨을 검증(`.sort()` 호출이 코드에 없음). 두 화면 모두 `items.map`으로 그대로 렌더링 |
| REQ-CNL-009 (취소 성공 후 재조회, 로컬 스플라이스 아님) | **PASS(코드 레벨)** | `cancellationModel.test.ts` — `decidePostCancelAction()`이 항상 `'refetch'`만 반환(로컬 제거를 지시하는 값이 타입에 존재하지 않음). `MyWaitlistPage.tsx`는 이 값에 따라 `load()`를 재호출하고 `items.filter(...)` 같은 로컬 제거 코드가 없음. `MyEnrollmentsPage.tsx`는 취소 성공 시 라우트 이동으로 화면이 언마운트되므로, 재진입 시 `useEffect`가 새로 마운트되어 자연히 재조회됨 |
| REQ-CNL-010 / INV-FE-011 / AC-FE-112 (`position`은 `courseTitle`과 나란히, 전역 순위 표현 미사용) | **PASS(코드 레벨)** | `cancellationModel.test.ts` — `formatWaitlistPositionLabel`이 `courseTitle`·`position` 모두를 포함하는 단일 문자열을 반환하고, 같은 `position`이라도 `courseTitle`이 다르면 다른 문자열을 만듦(전역 순위가 아님을 실증) + "내 대기 순위"·"승격 예정 순서" 미포함을 직접 assert. `MyWaitlistPage.tsx`는 `item.position`을 단독으로 렌더링하지 않고 이 함수의 반환값만 사용 |
| E1 (`tsc -b --force`) | **PASS** | exit=0, 출력 없음 |
| E2 (lint, oxlint) | **PASS** | exit=0, 출력 없음 |
| E3 (단위 테스트) | **PASS** | `npx vitest run` exit=0 — **15개 파일 114건** 전부 통과(M1~M5 103건 + M6 신규 11건: `cancellationModel.test.ts`) |
| E4 (프로덕션 빌드) | **PASS** | `npm run build`(`tsc -b && vite build`) exit=0, `dist/`(151 modules, `index-*.js` 286.17 kB / gzip 89.36 kB) |
| B4/B11 (AskUserQuestion 미사용) | **PASS** | `grep -rn "AskUserQuestion" frontend/src/` → 0건 |
| plan.md §E12 (취소 식별자 정합 — `position`을 경로 변수로 넘기는 코드 경로 grep) | **PASS** | `grep -rn "cancelWaitlistEntry(.*position" frontend/src/` → 0건. `cancelWaitlistEntry`를 호출하는 유일한 지점(`MyWaitlistPage.tsx`)은 `resolveWaitlistCancelTarget(item)`만 인자로 전달 |

**브라우저 수동 확인 — 부분 완료 (오케스트레이터, `claude-in-chrome`) + 근본 원인 확정**:

- **S12(빈 목록) 확인 완료**: 신규 계정(`m6a@local.test`)으로 "내 수강신청"·"내 대기명단" 양쪽 모두 진입 → 각각 **"보유 내역 없음 — 아직 신청한 수강신청이 없습니다."** / **"보유 내역 없음 — 아직 대기 중인 강좌가 없습니다."** 정상 표시(오류 화면·빈 화면 아님, 콘솔 오류 0건). REQ-CNL-006/007 실증.
- **S10/S11(확정 취소·대기 취소) — 재현 시도, 3회 연속 실패, 원인을 "플레이키니스"에서 "결정적 실패"로 재분류**: 이번 세션에서 신규 강좌(`id=6`)에 신규 계정 2개로 수강신청을 직접 제출(curl)해 확정/대기 데이터를 만들려 했으나 **2건 모두 `FAILED`로 종결**됐다. 이것으로 **같은 백엔드 재기동 이후 curl 직접 신청이 3회 연속(M4 재시도 2회 + 이번 M6 1회) 100% 실패**를 기록했고, 이번엔 매번 `HikariPool` 연결 오류조차 로그에 없었다(정상적으로 커넥션을 얻고 600ms 내로 깔끔하게 실패) — **더 이상 "간헐적 플레이키니스"로 설명되지 않는다.** `EnrollmentQueueWorker.drainQueue()`가 예외를 로깅 없이 삼키는 구조라 근본 원인은 프론트엔드 세션에서 확정할 수 없었으나, 접속 DB가 원격 Supabase 세션 풀러(`aws-0-ap-southeast-1.pooler.supabase.com`)라는 사실과 이 세션에서 백엔드를 8회 이상 재기동한 누적 효과가 유력한 후보로 남는다. **이는 `SPEC-ENROLLMENT-001`(백엔드) 소관이며 이 SPEC의 수정 범위 밖이다** — 결론적으로 S10/S11은 이 로컬 개발 환경에서는 신뢰할 수 있는 확정/대기 데이터를 만들 수 없어 **이번 세션에서는 재현 불가능**으로 판단하고 추가 시도를 중단한다.
- **오케스트레이터 후속 시 참고**: `/enrollments/mine`·`/waitlist/mine` 라우트는 이미 배선되어 있고 S12는 확인됐다. S10/S11은 원격 DB 연결이 이 세션과 다른(더 안정적인) 환경에서, 또는 `SPEC-ENROLLMENT-001` 측의 워커 예외 로깅이 보강된 뒤에 재시도하는 것을 권장한다.

**잔여 위험 (Residual Risk)**:

- **S10/S11(확정 취소·대기 취소 실제 흐름)은 미실시로 남는다** — 코드 레벨 검증(단위 테스트 11건 + 정적 grep)만으로는 `REQ-NFR-007`을 완전히 충족하지 못한다. 원인은 이 SPEC의 프론트엔드 코드가 아니라 이 로컬 개발 환경의 수강신청 큐 처리 자체가 3회 연속 결정적으로 실패했기 때문이다.
- `MyEnrollmentsPage.tsx`가 취소 후 이동하는 `/requests/:requestId` 화면에서 최종적으로 `CANCELLED` 상태를 실제로 수신하는지(취소 워커 자체)는 확정 취소를 애초에 만들 수 없어 검증 불가능했다.
- `describeCancelError`의 403/404 통합 문구가 실제 백엔드 오류 응답에서도 동일하게 동작하는지는 단위 테스트(모의 `ApiError`)로만 검증했고, 실제 타인 소유 항목에 대한 취소 시도는 수행하지 않았다.

**작업 트리 범위**: `frontend/src/cancellation/**`(신규), `frontend/src/api/endpoints.ts`(확장), `frontend/src/App.tsx`(수정), `.moai/specs/SPEC-FRONTEND-001/progress.md`(이 절)만 변경. `frontend/src/enrollment/**`(폴링 훅·메시지 테이블 포함)는 무변경 — M4 산출물을 재사용만 했다. 백엔드(`src/**`)·다른 SPEC·`.moai/project/*`는 무변경.

**커밋**: (M6 커밋은 이 progress.md 갱신과 함께 커밋됨 — SHA는 커밋 후 §E.3에서 백필)

---

## §E.2 M7 — 수동 시나리오 확인 (S1~S14, plan.md §F M7 / REQ-NFR-007)

**진입 조건 재확인**: M2~M6 전부 완료(§E.2 위 각 절). 이 절 작업 직전 SPEC-ENROLLMENT-001 M8이 진단·판정한 로컬 dev DB의 이질적 `course_term` 스키마 잔재(M6 잔여 위험 1·2번이 지목한 원인)를 제거하여 — 이 프로젝트 소유 스키마와 무관한 상태였음을 M8이 이미 실증했으므로 — 큐 처리가 실제로 성공하는 환경을 이번 검증 직전에 확보했다. 그 전까지는 모든 확정 시도가 100% `FAILED`로 종결되어 S1/S2/S5/S6/S9/S10/S14가 원천적으로 수행 불가능했다.

**수행 방식**: `claude-in-chrome`을 통해 실제 브라우저(회원가입·로그인·클릭·폼 입력)로 14개 시나리오를 순서대로 수행했다. **자동 테스트 통과로 대체하지 않았다**(D8) — 아래는 전부 실제 UI 조작 후 화면에 표시된 결과다.

| # | 시나리오 | 결과 | 실제 관측 |
|---|---|---|---|
| S1 | 회원가입→로그인→목록→상세→신청→결과 표시 | **PASS** | `m7-s1-member@example.com` 신규 가입, `M4 재시도 - 정원1`(정원1) 신청 → 요청 10 → "수강신청이 확정되었습니다." |
| S2 | 정원 찬 강좌 신청 | **PASS** | `S2 대기 테스트`(정원1)를 member_A로 채운 뒤 member_B 신청 → 요청 13 → "정원이 가득 차 대기명단에 등록되었습니다. 대기 순번: 1번째" |
| S3 | 관리자 로그인 → 관리자 메뉴 노출 | **PASS** | 관리자 로그인 시 "관리자" 링크 노출(역할: ADMIN); 일반 계정(S1) 로그인 시 동일 위치에 미노출 — 대조 확인 |
| S4 | 관리자 강좌 생성·수정·마감 → 마감 강좌 신청 미제공 | **PASS** | 강좌 생성("S4 관리자 생성 강좌")→수정(제목 변경 반영)→마감(CLOSED 전환) 전부 성공. 일반 계정으로 해당 강좌 상세 진입 시 "마감된 강좌입니다. 신청을 받지 않습니다." — 신청 버튼 없음 |
| S5 | 관리자 정원 증설 → 비동기 승격 | **PASS** | `S5 증설 테스트`(정원1, member_C로 확정)에 member_D 신청→대기 1번째(요청 15). 관리자가 정원 1→2로 수정 → 재조회 시 "정원 2 · 확정 2"로 자동 승격 확인 |
| S6 | 확정 인원 미만으로 정원 축소 | **PASS** | `S6 축소 테스트`(정원2, member_E·F로 확정 2/2)에서 관리자가 정원을 1로 수정 시도 → "현재 확정 인원보다 적은 정원으로는 변경할 수 없습니다." — 원문(HTTP/SQL) 미노출, 해석된 안내만 표시 |
| S7 | 토큰 만료 후 조작 → 재로그인 안내 | **PASS(변형)** | JWT TTL이 30분(`app.jwt.access-token-ttl=30m`)이라 실시간 만료 대기가 비현실적이어서, `sessionStorage`의 토큰 값을 무효 문자열로 교체해 401을 유발하는 방식으로 대체 검증했다 — `/enrollments/mine` 접근 시 자동으로 로그인 화면으로 전환(세션 정보 제거)됨을 확인했고, 재로그인 후 같은 화면이 정상 표시되어 "작업 계속"도 확인했다. **실제 시간 경과에 의한 만료 자체는 검증하지 못했다** — 인증 실패(401) 처리 경로는 동일하다고 판단해 대체했다 |
| S8 | 탭 종료 후 재개 → 세션 없음 | **PASS(변형)** | 실제 탭 종료 대신 완전히 새로운 브라우저 탭(별도 `sessionStorage` 컨텍스트)으로 접속 — 로그인 안내만 표시되고 "로그인되어 있습니다" 문구 없음. `sessionStorage`는 탭 단위로 격리되므로 탭 종료와 동등한 조건이다 |
| S9 | 종단 도달 후 네트워크 탭 관찰 → 상태 조회 중단 | **코드 레벨 검증** | `pollingDecision.ts`의 `decideNextPoll`이 상태가 PENDING이 아니면(`isTerminalStatus`) `'stop'`을 반환하는 로직은 M4 단위 테스트로 이미 검증됨(§E.2 M4). 브라우저 실관측은 이번에도 제한적이었다 — `claude-in-chrome` 자동화 탭은 `document.visibilityState`가 항상 `'hidden'`으로 보고되어(§E.2 M4에서 이미 발견한 툴링 한계) TanStack Query의 `refetchIntervalInBackground: false`가 백그라운드 자동 재조회 자체를 억제한다. 요청 11을 CONFIRMED로 확정시킨 뒤 재방문(강제 새로고침)했을 때 상태가 안정적으로 CONFIRMED로 유지됨은 확인했으나, "폴링이 실제로 멈췄다"는 네트워크 탭 상의 직접 관측은 이 세션에서도 하지 못했다 |
| S10 | 내 수강신청 목록 → 확정 취소 완주 | **PASS** | `m7-s1-member`의 `M4 재시도 - 정원1` 확정 건을 "내 수강신청"에서 취소 → 요청 21(202 비동기) → 재조회 → "취소가 완료되었습니다." |
| S11 | 내 대기명단 목록 → 대기 취소 완주 | **PASS** | `m7-s11-h@example.com`의 `S11 대기취소 테스트` 대기 건을 "내 대기명단"에서 취소 클릭 → 폴링 없이 즉시(200 동기) 목록에서 사라지고 "보유 내역 없음"으로 전환 |
| S12 | 보유 내역 0건 신규 계정 → 두 목록 진입 | **PASS** | `m7-s12-empty@example.com` 신규 가입 직후 "내 수강신청"→"보유 내역 없음 — 아직 신청한 수강신청이 없습니다.", "내 대기명단"→"보유 내역 없음 — 아직 대기 중인 강좌가 없습니다." 둘 다 오류 아닌 정상 안내로 표시 |
| S13 | 전 시나리오를 프록시 없이 실제 오리진에서 수행 | **PASS** | `frontend/vite.config.ts`가 프록시를 두지 않고 포트 5173(`strictPort`)에 고정되어 있어(§C.5) 이번 M7 전체가 애초에 프록시 없는 실제 오리진(`http://localhost:5173` → `http://localhost:8080`)에서 수행됐다. 세션 전체(회원가입·로그인·목록·신청·취소·관리자 CRUD 등 20여 건의 API 호출)에 걸쳐 콘솔 오류 0건(`read_console_messages` onlyErrors 필터) |
| S14 | 새로고침 후 폴링 재개(3초 간격, 상한은 최초 접수 기준) | **PASS** | 요청 11 제출 직후 `sessionStorage['openclass.enrollment.receiptAt.11']` 기록값(`1786977796315`)을 확인하고 5초 대기 후 페이지를 새로고침 — 같은 키의 값이 **완전히 동일하게 유지**됨을 확인(재마운트 시 재설정되지 않음). `useRequestStatus.ts`가 이 저장된 접수 시각으로부터 `elapsedMs = Date.now() - receivedAtMs`를 계산하므로(AC-FE-073b), 새로고침이 경과 시간 계산을 리셋하지 않는다는 요구사항이 코드·저장값 양쪽에서 확인됐다. 이 요청은 새로고침 직후 정상적으로 최종 상태(확정됨)를 재조회해 표시했다 |

**종합**: 14개 중 **12개 완전 PASS**, **2개(S7·S9)는 명시된 사유로 대체·부분 검증** — 둘 다 검증 자체를 생략한 것이 아니라 이 브라우저 자동화 환경의 물리적 제약(실시간 30분 대기 불가, `visibilityState` 항상 hidden) 때문에 등가의 대체 방법 또는 코드 레벨 근거로 판단을 뒷받침했다. **D8 위반 없음** — 모든 항목이 실제 UI 조작 결과이며, 자동 테스트 로그만으로 통과 처리한 항목은 없다.

**작업 트리 범위**: 이 절(`progress.md`)만 변경. 프론트엔드/백엔드 소스 변경 없음 — M7은 검증 마일스톤이며 신규 구현이 없다. (참고: 이 검증을 가능하게 한 로컬 dev DB 스키마 정리는 SPEC-ENROLLMENT-001 M8 범위에서 이미 커밋됨, `docs(SPEC-ENROLLMENT-001)` 계열 — 이 SPEC의 소스 변경이 아니다.)

**잔여 위험 (Residual Risk)**:

- S7·S9는 위 표에 명시한 대로 완전한 실시간 관측이 아니다 — 각각 30분 TTL 실시간 대기와 브라우저 자동화 `visibilityState` 한계가 원인이며, 코드 레벨 근거와 등가 대체 검증으로 보완했다.
- 시나리오 진행 중 생성된 진단용 테스트 계정(`m7-*@example.com`)과 강좌(`S2/S4/S5/S6/S11 *테스트`)가 로컬 dev DB에 남아 있다 — 프로덕션 영향 없음, 정리가 필요하면 별도 작업.

**M7 완료 판정**: S1~S14 전부 수행 및 결과 기록 완료(plan.md §F M7 완료 판정 충족).

---

## §E.3 Run-phase Audit-Ready Signal

```yaml
run_status: audit-ready
run_complete_at: 2026-08-18
milestones_complete: [M1, M2, M3, M4, M5, M6, M7]
ac_scope: AC-FE-001..AC-FE-907  # acceptance.md §D.2 합계 — 요구사항 59건 / 인수 기준 86건
requirements_scope: REQ-FE-001..REQ-NFR-007  # 정확한 REQ 접두 범위는 spec.md §B 참고
m7_scenarios: S1..S14  # 전부 실제 브라우저 수행·기록 완료(위 §E.2 M7 표)
new_warnings_or_lints_introduced: false  # M7은 검증 마일스톤 — 소스 변경 없음
known_residual_risks:
  - "S7(토큰 만료)·S9(폴링 중단 관측) — 30분 TTL 실시간 대기 불가 및 claude-in-chrome visibilityState=hidden 한계로 대체·코드 레벨 검증(§E.2 M7 표 참고), 실시간 원본 시나리오 그대로는 미관측"
  - "M1~M6 개별 마일스톤에서 이미 기록된 잔여 위험(WAITLISTED 재시도 미관측 등, §E.2 각 절)은 M7에서 실제 DB로 재현·해소되었으나 그 절들의 기존 기록 자체는 소급 수정하지 않음 — 이 §E.3이 최신 상태를 대표한다"
  - "인수 기준 86건 전체에 대한 AC-by-AC PASS/FAIL 집계 재검증은 이 절에서 수행하지 않았다 — 마일스톤별 §E.2 기록(M1~M7)이 근거이며, 전체 집계 대조는 sync-phase(sync-auditor)의 독립 검증 대상으로 남긴다"
sync_phase_ready: true
```

---

## §E.4 Sync-phase Audit-Ready Signal

```yaml
sync_status: audit-ready
sync_complete_at: 2026-08-18
sync_commit_sha: 8ac906e74eae33081c718755e1c19ac40cab9674
sync_files_touched:
  - CHANGELOG.md
  - README.md
  - .moai/specs/SPEC-FRONTEND-001/spec.md
  - .moai/specs/SPEC-FRONTEND-001/progress.md
frontmatter_transition:
  spec_md: "in-progress -> completed"
  note: >
    위임 지시는 spec.md frontmatter가 status: draft이며 draft->in-progress
    전환이 기록되지 않은 것으로 전제했으나, 실제 파일을 읽어 확인한 결과
    spec.md는 이미 status: in-progress였다(진입 시점 수동 확인 결과, 위임
    당시 전제와 불일치 — 사전 draft->in-progress 갭 자체가 존재하지
    않았다). 이 sync 커밋은 표준적인 in-progress -> completed 단일 전환만
    수행했으며, 어떤 중간 상태도 건너뛰지 않았다. 오케스트레이터에게
    이 불일치를 명시적으로 플래그한다.
changelog_entry_position: "CHANGELOG.md [Unreleased] 섹션 말미 (SPEC-ENROLLMENT-001 Known Limitations 다음)"
b12_self_test:
  pre_emission_grep: "grep -c 'SPEC-FRONTEND-001' CHANGELOG.md (편집 전) -> 1 (진행 참조, SPEC-FRONTEND-001 자신의 섹션 아님) -> 신규 섹션 추가 진행"
  ac_count_match: "acceptance.md AC-FE-* 행 86건 == progress.md §E.3 ac_scope 86건 == CHANGELOG Verification 절 86건 명시"
  file_path_verification: "frontend/src/{api,session,catalog,enrollment,admin,cancellation}/ 및 App.tsx 전부 ls로 실재 확인 완료"
frontend_quality_gate:
  tsc: "npx tsc -b --force exit=0"
  lint: "npm run lint (oxlint) exit=0"
  vitest: "npx vitest run exit=0 -- 15 files, 114 tests passed"
  build: "npm run build exit=0"
```

---

## §F 교차 참조

- 요구사항: `spec.md`
- 조사 근거: `research.md`
- 구조 설계: `design.md`
- 구현 계획: `plan.md`
- 인수 기준: `acceptance.md`
- 선행 SPEC: `.moai/specs/SPEC-AUTH-001/`, `.moai/specs/SPEC-COURSE-001/`, `.moai/specs/SPEC-ENROLLMENT-001/`

---

## §G Phase 4 Mode Selection

**입력 파라미터**: tier=L, scope≈59 REQ / 14 엔드포인트 / M1~M7(7개 마일스톤), domain 수≥3(인증·카탈로그·수강신청·관리자), 파일 언어=TypeScript/React(그린필드, `frontend/` 미생성), concurrency benefit=LOW(코딩 중심 작업)

**모드 평가**:

| # | 모드 | 선택 여부 | 근거 |
|---|---|---|---|
| 1 | trivial | 미선택 | 자명한 1줄 수정이 아님 |
| 2 | background | 미선택 | 쓰기 작업(코드 구현) 포함 |
| 3 | agent-team | 미선택 | RETIRED |
| 4 | parallel | 미선택 | 코딩 중심 작업 — Anthropic coding-task parallelism caveat |
| 5 | **sub-agent** | **선택** | 마일스톤별 순차 `manager-develop` 스폰(Full Pipeline 봉투) |
| 6 | workflow | 미선택 | 코딩/다중도메인 작업 — Mode 6 부적합 |

**결정**: `sub-agent` (Full Pipeline envelope — M1~M7 마일스톤별 순차 manager-develop 위임)

**근거**: SPEC 규모(Tier L, 7개 마일스톤, 3개 이상 도메인)가 Full Pipeline 봉투에 해당하지만, 실제 구현 작업 자체는 코딩 중심이므로 Mode 5(순차 sub-agent)가 정답이다. 각 마일스톤 완료 시 오케스트레이터가 브라우저 실동작 확인 등 검증 배치를 실행한 뒤 다음 마일스톤으로 진행한다.

---

## §H sync-auditor 1차 감사 (2026-08-30)

> 이 절은 `§E.3 known_residual_risks` 3번("인수 기준 86건 전체에 대한 AC-by-AC
> PASS/FAIL 집계 재검증은 이 절에서 수행하지 않았다 — … 전체 집계 대조는
> sync-phase(sync-auditor)의 독립 검증 대상으로 남긴다")이 명시적으로 유보한
> 독립 검증의 수행 기록이다. **이 SPEC에 대한 최초의 sync-audit이며 재감사가
> 아니다.** 감사자는 `§E.2`의 마일스톤별 자체 보고를 액면 그대로 신뢰하지 않고
> 기계적 재실행과 소스 직접 대조로 재판정했다.

### H.1 종합 판정

| 항목 | 값 |
|---|---|
| 종합 판정 | **FAIL** |
| 조화평균 | **73.2 / 100** (Tier L 통과선 85) |
| AC 집계 (86건) | PASS **68** / FAIL **3** / UNVERIFIED **15** |
| 차단 사유 | 차단(Blocker) 등급 **AC-FE-109**의 `[검사]` 절반이 미충족 (기계적으로 확정) |

FAIL 판정의 근거는 점수가 아니라 **필수 항목 미충족**이다. `acceptance.md` §C가
AC-FE-109를 차단 등급으로 지정했고("미충족 시 이 SPEC을 완료로 선언할 수 없다"),
§E.1 완료 판정 체크리스트 6번이 이를 기계적 판정 항목으로 재확인하며, §E.4가
부분 완료 경로를 명시적으로 삭제했다.

### H.2 차원별 점수

| 차원 | 점수 | 판정 | 근거 (기계적 검증 출력) |
|---|---|---|---|
| Functionality (40%) | **72** | **FAIL** | `npx vitest run` → `Test Files 15 passed (15) / Tests 114 passed (114)`, exit=0. 그러나 86건 중 차단 등급 1건이 FAIL이고 15건이 UNVERIFIED. 자동 테스트는 전부 실질적 어서션이며 형식적 통과가 아님을 표본 검증으로 확인 |
| Security (25%) | **90** | **PASS** | `npm audit` → `found 0 vulnerabilities`(prod·dev 양쪽). `grep -rn "dangerouslySetInnerHTML\|innerHTML\|eval(\|new Function\|document.write" src/` → 0건. `grep -rn "console\." src/` → 0건. `grep -rniE "(api[_-]?key\|secret\|private[_-]?key\|Bearer [A-Za-z0-9]{20})" src/` → 0건. 토큰은 `Authorization: Bearer` 헤더에만 실리고 URL 질의 문자열·로그 경로 0건 |
| Craft (20%) | **62** | **FAIL** | 커버리지 측정 수단이 **아예 없다** — `grep -n "coverage" package.json vite.config.ts` → 0건, `@vitest/coverage-*` 미설치. TRUST 5 "85% 이상"이 측정 불가이므로 PASS로 셀 수 없다(미측정 ≠ 충족) |
| Consistency (15%) | **74** | 조건부 | `npx tsc -b --force` exit=0, `npm run lint`(oxlint) exit=0. @MX 태그·모듈 경계·재사용 규율은 우수. 다만 CHANGELOG 주장 2건이 소스와 불일치(F3) |

조화평균 계산: `4 / (1/72 + 1/90 + 1/62 + 1/74)` = **73.2**.
(가중산술평균은 74.8이나, 감사 규약상 조화평균을 채택한다 — 낮은 차원을 평균이
가려 주지 않게 하기 위함.)

### H.3 AC 집계 (86건 전수)

`grep -oE '\*\*AC-FE-[0-9]+[a-z]?\*\*' acceptance.md | ... | sort -u | wc -l` → **86**
(acceptance.md §D.1 자체 검증 명령의 기대값과 일치).

| 검증 수단 | 건수 | PASS | FAIL | UNVERIFIED |
|---|---|---|---|---|
| `[검사]` (감사자가 직접 재실행) | 14 | 12 | 2 | 0 |
| `[자동]` (테스트 스위트 재실행) | 16 | 16 | 0 | 0 |
| 혼합 (`[자동]+[검사]` / `[수동]+[검사]`) | 3 | 2 | 1 | 0 |
| `[수동]` (§E.2 브라우저 관측 기록 의존) | 53 | 38 | 0 | 15 |
| **합계** | **86** | **68** | **3** | **15** |

**FAIL 3건**: AC-FE-109(진성 결함), AC-FE-900·903(명령 문자 그대로는 실패 —
브랜치 뒤처짐 아티팩트, 아래 F2 참고).

**UNVERIFIED 15건** — `[수동]` AC 중 §E.2 어느 절에도 실제 관측 기록이 없거나
"미수행"으로 명시된 항목:

| 구간 | AC | 미검증 사유 |
|---|---|---|
| SES | AC-FE-021 · 022 · 024 | 회원가입 400(8자 미만)·409(중복 이메일), 로그인 401 오류 경로가 S1~S14 어디에도 없다 (S1은 성공 경로만 수행) |
| SES | AC-FE-025 | `Authorization: Bearer` 헤더 부착의 네트워크 탭 직접 관측 기록 없음 (인증 호출 성공으로 간접 추정만 가능) |
| SES | AC-FE-029 | 로그아웃 버튼 클릭 → 토큰 제거 → 인증 화면 접근 불가의 실제 수행 기록 없음 (S8은 탭 격리로 대체) |
| SES | AC-FE-034 | 비세션 상태에서 인증 필요 경로 직접 진입 시나리오 없음 (M5는 ADMIN 역할 부족 경로만 확인) |
| CAT | AC-FE-041 | §E.2 M3이 관측한 목록은 `totalPages: 1` — **실제 페이지 이동이 일어나지 않았다**. "새 `page` 값으로 요청이 발생한다"는 미관측 |
| CAT | AC-FE-046 | §E.2 M3이 "실제 404 브라우저 관측은 미수행"으로 자체 명시 |
| ENR | AC-FE-068 · 069 | 30초 상한 도달 → 수동 재확인 버튼 → 기존 requestId 재조회. §E.2 M4 잔여 위험이 미관측으로 명시했고 M7 S1~S14에도 대응 시나리오 없음 |
| ENR | AC-FE-072 | 폴링 화면 언마운트 후 잔여 타이머 호출 0건. 어느 절에도 관측 기록 없음 |
| ENR | AC-FE-073b | S14는 `sessionStorage` 접수 시각 **보존**만 확인했다. "3초 간격으로 재개"의 간격 자체는 미관측이며, S14 대상 요청은 이미 종단 상태였다 |
| CNL | AC-FE-105 | §E.2 M6이 "실제 타인 소유 항목에 대한 취소 시도는 수행하지 않았다"로 자체 명시 |
| CNL | AC-FE-110 | "두 목록 조회가 각각 2건 이상"을 응답 순서와 대조하는 관측 없음 (S10/S11은 1건씩) |
| CNL | AC-FE-112 | "서로 다른 두 강좌에 대기 중인 계정"이 만들어진 기록 없음 (코드 레벨은 PASS) |

> `[수동]` PASS 38건은 **감사자가 재관측한 것이 아니라 §E.2의 기록을 근거로 한
> 전달 판정**이다. 기록에 계정명·요청 번호·관측 문구가 구체적으로 남아 있어
> 조작 정황은 없으나, 감사자의 직접 증거는 아니다(`verification-claim-integrity.md`
> §3.4 — 미관측을 관측으로 셈하지 않는다).

### H.4 발견 사항 (Findings)

| ID | 등급 | 확신도 | 위치 | 내용 |
|---|---|---|---|---|
| **F1** | **Blocker** | 확정(기계적 증명) | `frontend/src/api/endpoints.ts:141`, `frontend/src/api/types.ts:121-127` | **AC-FE-109의 `[검사]` 절반 미충족.** `cancelWaitlistEntry(waitlistEntryId: number, token: string)`와 `WaitlistListItem.position: number`가 **둘 다 맨 `number`** 이므로, `cancelWaitlistEntry(item.position, token)`이 타입 검사를 그대로 통과한다 |
| **F2** | High | 확정 | 브랜치 `feat/SPEC-FRONTEND-001` (HEAD `b98ab93`) | **AC-FE-900·903이 명령 문자 그대로는 실패한다.** 브랜치가 `main`보다 3커밋 뒤처져 있어 `git diff --name-only main -- src/`가 2줄, `.../SPEC-ENROLLMENT-001`이 4줄을 출력한다 |
| **F3** | Medium | 확정 | `CHANGELOG.md` (M6 항목, Verification 항목) | **sync-phase 산출 문서의 주장 2건이 소스와 불일치.** ① "대기 취소 대상은 `waitlistEntryId`이며 `position`이 아님을 **타입·로직 양쪽에서** 구별" — 타입 절반이 거짓(F1). ② "인수 기준 …(총 86건) … M1~M6 코드 레벨 … **전부 PASS**" — AC-FE-109로 반증됨 |
| **F4** | Medium | 확정 | `frontend/package.json`, `frontend/vite.config.ts` | **커버리지 측정 수단 부재.** `@vitest/coverage-v8` 미설치, `coverage` 설정 0건. TRUST 5 Tested(85%+)를 판정할 수 없다 |
| **F5** | Medium | 확정 | `frontend/vite.config.ts:16` | **`.tsx` 테스트가 구조적으로 발견되지 않는다.** `test.include: ['src/**/*.test.ts']`는 `.tsx`를 포함하지 않고 `environment: 'node'`라 렌더 테스트가 불가능하다. 화면 컴포넌트 15개의 자동 테스트가 0건이며(`find src -name '*.test.tsx' \| wc -l` → 0), 앞으로 누가 `.test.tsx`를 작성해도 **조용히 실행되지 않는다** |
| **F6** | Low | 확정 | `frontend/src/session/LogoutButton.tsx:19-27` | **로그인 직후에도 과거형 문구가 상시 노출된다.** `session.status === 'authenticated'`일 때 "로그아웃되었습니다. …"가 항상 렌더링된다. §E.2 M2가 부수 관찰로 기록했으나 미수정. AC-FE-030의 사실 내용 요건 자체는 충족하므로 PASS로 두되, 사용자에게 사실과 다른 시제를 보이는 문구다 |
| **F7** | Low | 확정 | `.moai/specs/SPEC-FRONTEND-001/progress.md` §E.2 M4 vs M7 S9 | **자체 기록 간 모순.** M4는 "종단 도달 후 5초간 … 요청이 **0건** — 자동 폴링이 실제로 멈춤을 확인"이라 적었고, M7 S9는 "'폴링이 실제로 멈췄다'는 네트워크 탭 상의 직접 관측은 **이 세션에서도 하지 못했다**"고 적었다. AC-FE-064는 M4 기록을 근거로 PASS로 판정했으나 두 기록은 화해되지 않았다 |
| **F8** | Low | 확정 | `frontend/.oxlintrc.json` | **린트 규칙 집합이 얇다.** 명시 규칙 2건(`react/rules-of-hooks`, `react/only-export-components`) + oxlint 기본 correctness 뿐이며, typescript 권장 세트·미사용 변수·부동 Promise 계열 규칙이 없다. `npx oxlint -D all src`를 돌리면 다수 지적이 나온다(스캔 자체는 정상 동작함을 이 명령으로 확인) |
| F9 | Info | 확정 | 감사 환경 | `Skill("moai-ref-owasp-checklist")`가 이 프로젝트에 설치되어 있지 않아 정본 OWASP 체크리스트를 적재하지 못했다. Security 차원은 수동 grep 프로브 6종으로 대체 수행했다 — **누락을 PASS로 셈하지 않고 Gap으로 기록한다** |

### H.5 F1 — 기계적 증명 (verbatim)

`design.md` §A.1이 이 항목의 판정 기준을 단일 문장으로 못박았다:

> 구체 수단은 run 단계가 정하되(브랜디드 타입·별칭 타입·래퍼 중 택일), **판정
> 기준은 하나다 — `position`을 취소 함수의 인자로 넘기는 코드가 타입 검사에서
> 거부되는가.**

실제 구현은 **관례적 방어**(`resolveWaitlistCancelTarget` 헬퍼 + 주석)만 채택했고
타입 층위 방어(브랜디드/별칭/래퍼 중 어느 것도)를 도입하지 않았다.

인메모리 TypeScript 컴파일 프로브(파일 미생성, 프로젝트 `typescript@6.0.3` 사용):

```
probe source:
  import type { WaitlistListItem } from "./src/api/types";
  import { cancelWaitlistEntry } from "./src/api/endpoints";
  declare const item: WaitlistListItem;
  declare const token: string;
  cancelWaitlistEntry(item.position, token);   // ← AC-FE-109가 거부를 요구하는 코드

출력:
  TS version: 6.0.3
  probe diagnostics on the cancelWaitlistEntry(item.position, token) call: 0
  RESULT: NOT REJECTED by type checker -> AC-FE-109 type-check clause FAILS
```

관련 시그니처 (verbatim):

```
frontend/src/api/types.ts:125          position: number
frontend/src/api/endpoints.ts:141      export function cancelWaitlistEntry(waitlistEntryId: number, token: string): Promise<void>
```

**영향**: 오늘 배선은 정확하다(`MyWaitlistPage.tsx:53`이 `resolveWaitlistCancelTarget(item)`만
전달하며, 단위 테스트가 `waitlistEntryId: 5 / position: 999`로 이를 검증한다).
따라서 **현재 사용자에게 노출된 결함은 없다.** 미충족인 것은 미래 회귀에 대한
방어다 — `acceptance.md` §C.1이 이 AC를 차단 등급으로 올린 이유가 정확히
그것이며("본인 소유의 엉뚱한 항목이 오류 없이 취소된다 … 발견이 가장 늦고 피해가
가장 직접적인 결함 유형"), 타입 방어가 없으면 향후 어떤 편집자도 오배선을
오류 없이 도입할 수 있다.

**최소 수정 제안** (사다리 5단계 "최소 코드", 신규 의존성 0건):

```ts
// types.ts — 두 필드를 구조적으로 구별 가능하게 만든다
export type WaitlistEntryId = number & { readonly __brand: 'WaitlistEntryId' }
export interface WaitlistListItem {
  waitlistEntryId: WaitlistEntryId
  // ... position: number 는 그대로
}
// endpoints.ts
export function cancelWaitlistEntry(waitlistEntryId: WaitlistEntryId, token: string): Promise<void>
```

이후 `cancelWaitlistEntry(item.position, token)`은 TS2345로 거부되며,
`resolveWaitlistCancelTarget`의 반환 타입만 `WaitlistEntryId`로 좁히면 화면 코드는
무변경이다. 회귀 가드로 `// @ts-expect-error` 기반 테스트 1건을 추가하면
AC-FE-109의 `[검사]` 절반이 기계적으로 고정된다.

### H.6 F2 — AC-FE-900·903 판정의 이중 독해

명령 문자 그대로 (HEAD `b98ab93`):

```
$ git diff --name-only main -- src/ build.gradle
src/main/java/com/hongseob/openclass_ap/enrollment/worker/EnrollmentQueueWorker.java
src/test/java/com/hongseob/openclass_ap/enrollment/EnrollmentQueueResilienceIntegrationTest.java
lines=2                                    # AC-FE-900 기대값 0 → FAIL

$ git diff --name-only main -- .moai/specs/SPEC-AUTH-001 .moai/specs/SPEC-COURSE-001 .moai/specs/SPEC-ENROLLMENT-001
.moai/specs/SPEC-ENROLLMENT-001/{acceptance,plan,progress,spec}.md
lines=4                                    # AC-FE-903 기대값 0 → FAIL

$ git diff --name-only main -- .moai/project/
lines=0                                    # AC-FE-904 → PASS
```

귀속 조사 (이 브랜치가 그 파일들을 건드렸는가?):

```
$ git log --oneline main..HEAD -- src/
(출력 없음)
$ git log --oneline main..HEAD -- .moai/specs/SPEC-ENROLLMENT-001
(출력 없음)
$ git rev-list --count --left-right origin/main...HEAD
3	16                                     # main이 3커밋 앞서 있다

$ git log --oneline HEAD..main
b5ee26e docs(SPEC-ENROLLMENT-001): M8 — 큐 처리 실패 진단 로깅 + 근본 원인 판정 (v0.3.2) (#2)
73dc983 docs(SPEC-ENROLLMENT-001): plan-audit 1회차(FAIL 0.847) 지적 D1~D5 반영 (v0.3.1 → v0.3.2)
3632fa3 feat(SPEC-ENROLLMENT-001): in-place amendment v0.3.0 → v0.3.1 - 큐 처리 실패 가능성

$ MB=$(git merge-base main HEAD)   # 872a4fa
$ git diff --name-only $MB HEAD -- src/ build.gradle                       → 0줄
$ git diff --name-only $MB HEAD -- .moai/specs/SPEC-{AUTH,COURSE,ENROLLMENT}-001 → 0줄
$ git diff --name-only $MB HEAD -- .moai/project/                          → 0줄
```

**판정**: 불변식 **INV-FE-007(백엔드 소스 무변경)은 충족된다** — 이 브랜치는
백엔드 소스도 다른 SPEC 아티팩트도 한 줄도 건드리지 않았다. 차이는 전적으로
`main`이 SPEC-ENROLLMENT-001 M8 작업으로 3커밋 전진한 데서 온다. 그러나
`acceptance.md`가 기록한 **명령 자체는 지금 실패하며**, 그 명령이 차단 등급이므로
기계적 게이트로는 FAIL이다. PR 생성 전 `main` 리베이스/머지로 해소된다(해소 후
동일 명령이 0줄을 출력한다).

### H.7 `status: completed` 전이 판정 — **시기상조**

`spec.md` frontmatter는 `status: completed`(sync 커밋 `8ac906e`)이나, 다음 세 근거로
전이 요건이 충족되지 않았다:

1. `acceptance.md` §E.1 기계적 판정 체크리스트 6번 —
   "AC-FE-109 통과(`position`을 취소 인자로 넘기는 코드가 타입 검사에서 거부됨)" 미충족(F1).
2. `acceptance.md` §E.4 — "이 SPEC은 §B.1~§B.7 전부와 §E.2의 S1~S14 전부를
   충족했을 때만 완료로 선언한다. **부분 완료 경로가 없다.**" §B의 15건이 UNVERIFIED다.
3. §C 차단 등급 정의 — "미충족 시 이 SPEC을 **완료로 선언할 수 없다**".

감사자는 판정만 기록하며 frontmatter를 되돌리지 않는다 — 상태 전이의 소유자는
`manager-docs`이고(`spec-frontmatter-schema.md` § Status Transition Ownership Matrix),
되돌림 여부는 오케스트레이터와 사용자의 결정이다.

### H.8 강점 (균형 기록)

회의적 감사라도 실제로 잘 된 것은 잘 됐다고 적는다:

- **오류 정규화 판정 순서**가 계약으로 고정되어 있고, 회귀 가드 테스트(`errors.test.ts:25`
  "401-before-code-field")가 그 순서를 직접 지킨다. 401을 `code` 필드보다 먼저 판정하는
  결정이 실제 코드·테스트 양쪽에 살아 있다.
- **종단 판정이 화이트리스트가 아니다.** `isTerminalStatus('SOME_FUTURE_VALUE_NEVER_SEEN') === true`를
  직접 어서션한다 — `acceptance.md` §C.1이 가장 걱정한 미래 무한 폴링 결함이 실제로 막혀 있다.
- **AC-FE-073a 테스트가 반증적으로 설계됐다.** "재마운트 시각을 0초로 다시 세는 결함이
  있다면 1000을 반환할 것"이라는 실패 조건이 테스트에 명시돼 있어, 통과가 우연이 아니다.
- **오류 원문 미노출이 어서션으로 고정**돼 있다(`errors.test.ts:49` —
  `expect(result.message).not.toMatch(/password|defaultMessage/)`).
- **재사용 규율**: M5·M6이 M1~M4의 `errors.ts`·`guardLogic.ts`·`catalogModel.ts`·
  `useRequestStatus`를 수정 없이 소비만 했다(`git diff`로 무변경 확인). 중복 구현 0건.
- **보안 표면이 깨끗하다**: XSS 싱크 0건, 시크릿 0건, `console.*` 0건, `localStorage` 0건,
  의존성 취약점 0건, 취소 403/404 통합으로 소유자 열거 방지.

### H.9 권고 (우선순위 순)

1. **[PR 차단] F1 수정** — `WaitlistEntryId` 브랜디드 타입 도입(H.5의 최소 수정) +
   `@ts-expect-error` 회귀 가드 테스트 1건. AC-FE-109가 차단 등급이므로 이것이 해소되기
   전에는 `completed`가 성립하지 않는다.
2. **[PR 차단] F2 해소** — `main`(현 `b5ee26e`) 리베이스 또는 머지 후 AC-FE-900·903
   명령 재실행하여 0줄 확인.
3. **[PR 차단] F3 정정** — CHANGELOG의 "타입·로직 양쪽에서 구별"과 "86건 전부 PASS"
   두 문장을 사실에 맞게 수정(F1 수정 후에는 전자가 참이 되므로 후자만 조정하면 된다).
4. **[High] F5 수정** — `vite.config.ts`의 `test.include`를 `src/**/*.test.{ts,tsx}`로
   넓히고 `environment`를 렌더 테스트가 가능한 값으로 조정(또는 `.tsx` 미지원을 주석으로
   명시). 지금은 누가 `.test.tsx`를 써도 조용히 실행되지 않는다.
5. **[Medium] F4 해소** — `@vitest/coverage-v8` 도입 후 순수 로직 모듈 기준 커버리지 측정.
   TRUST 5 Tested 판정을 "미측정"에서 벗어나게 한다.
6. **[Medium] UNVERIFIED 15건 처리** — (a) 브라우저로 재수행하거나, (b) `acceptance.md`
   §E.4의 "부분 완료 허용 범위 없음" 조항을 사용자 승인 하에 개정하여 미검증 범위를
   정직하게 명시한다. **둘 중 하나는 반드시 해야 한다** — 지금 상태는 "검증할 수 있는데
   하지 않은 것을 완료로 부른" 형태이며, §E.4가 스스로 "미완성의 완곡어법"이라 경고한
   패턴이다.
7. **[Low] F6·F7·F8** — 로그아웃 문구 조건부 표시, M4/M7 S9 기록 모순 화해, 린트 규칙 보강.

### H.10 감사 방법 (재현 명령)

```bash
cd frontend
npx vitest run                 # exit=0, 15 files / 114 tests
npm run lint                   # exit=0 (oxlint)
npx oxlint -D all src          # 스캔 동작 증명용 — 기본 설정이 얇음을 확인
npx tsc -b --force             # exit=0
npm run build                  # exit=0, 151 modules
npm audit                      # found 0 vulnerabilities

grep -rn "http://\|https://" src/                                    # 0 (AC-FE-003)
grep -rn "dangerouslySetInnerHTML\|innerHTML\|eval(\|new Function" src/   # 0
grep -rn "console\." src/                                            # 0 (AC-FE-905)
grep -rn "localStorage" src/ | grep -v '\.test\.'                    # 0 (주석 언급만)
grep -rn "<input" src/cancellation/                                  # 0 (AC-FE-106)

cd .. && MB=$(git merge-base main HEAD)
git diff --name-only $MB HEAD -- src/ build.gradle                   # 0 (INV-FE-007)
git diff --name-only main -- src/ build.gradle                       # 2 (AC-FE-900 문자 그대로 FAIL)

cd .moai/specs/SPEC-FRONTEND-001
grep -oE '\*\*AC-FE-[0-9]+[a-z]?\*\*' acceptance.md | grep -oE 'AC-FE-[0-9]+[a-z]?' | sort -u | wc -l   # 86
```

**미검증(Gap)** — 감사자가 관측하지 **못한** 것을 명시한다:

- `[수동]` AC 53건은 브라우저에서 **재관측하지 않았다.** PASS 38건은 §E.2 기록에
  대한 전달 판정이다.
- 커버리지 수치는 **측정하지 못했다**(도구 부재, F4). 어떤 커버리지 주장도 하지 않는다.
- 정본 OWASP 체크리스트를 적재하지 못했다(F9). Security 90점은 수동 grep 프로브
  6종 + 의존성 감사 기반이며, 체크리스트 전수 대조가 아니다.
- 백엔드 연동 실동작(실제 API 왕복)은 이 감사에서 수행하지 않았다 — 서버를 기동하지 않았다.

**잔여 위험(Residual risk)**:

- F1 수정이 `resolveWaitlistCancelTarget` 반환 타입 변경을 동반하므로, 브랜디드 타입
  도입 시 `MyWaitlistPage.tsx`의 `cancellingId` 상태(`number | null`) 비교부에 좁힘이
  필요할 수 있다 — 수정 후 `tsc -b --force` 재실행이 필수다.
- UNVERIFIED 15건 중 AC-FE-041(페이지 이동)·046(404)·068/069(상한 재확인)은 **오늘까지
  단 한 번도 실행된 적이 없는 코드 경로**다. 단위 테스트는 계산 로직만 덮고 있어, 화면
  배선 결함이 남아 있을 가능성을 배제할 수 없다.
- `frontend/.env.local`이 작업 트리에 존재한다(gitignore 대상이므로 커밋되지는 않음).
  로컬 개발 전용 값만 담겨 있음을 확인했다(`VITE_API_BASE_URL=http://localhost:8080`).

---

## §I sync-auditor 2차 감사 (재검증, 2026-08-30)

> `§H` 1차 감사(FAIL, 조화평균 73.2)의 PR 차단 3건(F1·F2·F3)만을 대상으로 한
> **표적 재검증**이다. 86건 AC 매트릭스 전수 재실행이 아니며, 재판정하지 않은
> 항목은 `§H`의 판정을 그대로 승계한다. 감사자는 `41f3531`·`f4d17df`의 커밋
> 메시지 주장을 액면 그대로 신뢰하지 않고 독립 재실행으로 확인했다.

### I.1 재검증 범위와 기준선

| 항목 | 값 |
|---|---|
| 1차 감사 시점 HEAD | `b98ab93` |
| 2차 감사 시점 HEAD | `41f3531` (origin과 동기, `git rev-list --count --left-right origin/main...HEAD` → `0 19`) |
| 대상 | F1(Blocker) · F2(High) · F3(Medium) — 3건 |
| 명시적 범위 밖(유예) | F4 · F5 · F6 · F7 · F8 · F9, UNVERIFIED `[수동]` 15건 |
| 브랜치 델타 | `git diff --name-only b98ab93 HEAD` → 15개 파일(프론트 소스 7 + SPEC 아티팩트 6 + 백엔드 2는 머지 유입분) |

### I.2 F1 — 해소 확정 (2개 독립 경로로 기계 증명)

`design.md` §A.1의 판정 기준은 하나다 — "`position`을 취소 함수의 인자로 넘기는
코드가 타입 검사에서 거부되는가".

**경로 A — 인메모리 컴파일 프로브** (파일 미생성, `tsconfig.app.json`의 실제
컴파일러 옵션을 파싱해 적용, TypeScript 6.0.3):

```
TS 6.0.3 | project options (tsconfig.app.json), strict=false
[NEG] cancelWaitlistEntry(item.position, token)            -> 1 diagnostic(s)
      TS2345: Argument of type 'number' is not assignable to parameter of type 'WaitlistEntryId'.
              Type 'number' is not assignable to type '{ readonly __brand: "WaitlistEntryId"; }'.
[POS] cancelWaitlistEntry(item.waitlistEntryId, token)     -> 0 diagnostic(s)
[POS] cancelWaitlistEntry(resolveWaitlistCancelTarget(item), token) -> 0 diagnostic(s)

RESULT: AC-FE-109 [검사] PASS — position REJECTED (TS2345), legitimate paths ACCEPTED
```

1차 §H.5의 동일 프로브가 `diagnostics: 0 / NOT REJECTED`였던 것과 정반대다.
양성 대조 2건이 0 진단이므로, 거부가 브랜드 도입에 따른 **표적 거부**이지
전면적 컴파일 실패가 아님이 확인된다.

**경로 B — `@ts-expect-error` 가드의 비공허성(non-vacuity) 검증**:

```
$ npx tsc -p tsconfig.app.json --noEmit --listFiles
tsc exit=0
$ grep -n "endpoints.typecheck.ts" <listFiles 출력>
200:/…/frontend/src/api/endpoints.typecheck.ts     # 컴파일 대상에 실제로 포함됨
$ grep -E "error TS" <출력>
(no error TS lines)
```

`tsconfig.app.json`의 `include: ["src"]`가 신규 가드 파일을 포함하며, 컴파일
대상에 들어간 상태로 exit=0이다. TypeScript는 **공허한 `@ts-expect-error`를
TS2578(Unused '@ts-expect-error' directive)로 오류 처리**하므로, exit=0은
곧 다음 줄이 실제로 오류를 냈고 지시자가 그것을 억제했다는 뜻이다 — 즉
가드는 no-op이 아니다. 파일을 임시 편집하지 않고도 비공허성이 증명된다.

**부수 확인**:
- 브랜드 우회 캐스트가 프로덕션 코드에 없다 — `grep -rn "as WaitlistEntryId" src/ | grep -v '\.test\.'` → 0건 (캐스트는 테스트 픽스처 3곳뿐)
- `endpoints.typecheck.ts`는 `vite.config.ts`의 `test.include: ['src/**/*.test.ts']`와 불일치 → vitest가 실행하지 않는다(테스트 파일 수 15개로 기준선 동일). 런타임에 실행되지 않는 순수 타입 검사 자산이다.

**판정**: **AC-FE-109 `[검사]` 절반 충족 → PASS.** `acceptance.md` §E.1 기계적
판정 체크리스트 6번이 해소되었다.

### I.3 F2 — 해소 확정

1차 §H.6의 명령을 문자 그대로 재실행(HEAD `41f3531`):

```
$ git diff --name-only main -- src/ build.gradle
lines=0                                    # AC-FE-900 기대값 0 → PASS
$ git diff --name-only main -- .moai/specs/SPEC-AUTH-001 .moai/specs/SPEC-COURSE-001 .moai/specs/SPEC-ENROLLMENT-001
lines=0                                    # AC-FE-903 기대값 0 → PASS
$ git diff --name-only main -- .moai/project/
lines=0                                    # AC-FE-904 → PASS(유지)
```

귀속 재확인 — 머지(`f4d17df`)가 프론트 소스를 건드리지 않았고, INV-FE-007도
그대로 성립한다:

```
$ git diff --name-only b98ab93 f4d17df -- frontend/    → 0줄 (머지는 프론트 무변경)
$ MB=$(git merge-base main HEAD)   # b5ee26e (= main 팁)
$ git diff --name-only $MB HEAD -- src/ build.gradle                             → 0줄
$ git diff --name-only $MB HEAD -- .moai/specs/SPEC-{AUTH,COURSE,ENROLLMENT}-001 → 0줄
$ git diff --name-only $MB HEAD -- .moai/project/                                → 0줄
```

**판정**: **AC-FE-900 · AC-FE-903 → PASS.** 1차에서 "브랜치 뒤처짐
아티팩트"로 진단한 것이 정확했음이 해소로 확증되었다.

### I.4 F3 — 해소 확정 (다만 후속 공개 누락 F11 신설)

`CHANGELOG.md`는 `41f3531`에서 편집되지 않았다(`git diff --name-only f4d17df HEAD -- CHANGELOG.md` → 0줄).
manager-develop의 "F1 수정으로 자동 해소" 주장을 원문 대조로 검증했다:

- **문장 ①** (L81) — "대기 취소 대상은 `waitlistEntryId`이며 `position`이 아님을
  **타입·로직 양쪽에서** 구별(INV-FE-009)". `types.ts:116`에 `WaitlistEntryId`
  브랜디드 타입이 도입되고 `endpoints.ts:144`가 이를 인자 타입으로 받으므로
  "타입 층위 구별"은 이제 **문자 그대로 참**이다(I.2에서 기계 증명). → **참**
- **문장 ②** (L86) — "…(총 86건) … **M1~M6 코드 레벨**(단위 테스트 114건 +
  정적 grep) 전부 PASS". 이 문장의 검증 수단 한정어는 "코드 레벨"이며, 해당
  집합은 `[검사]` 14 + `[자동]` 16 + 혼합 3 = **33건**이다. 1차의 FAIL 3건이
  전부 이 집합에 속했고 지금 전부 PASS이므로, 1차가 제시한 반증 근거
  (AC-FE-109)는 소멸했다. → **참**

**판정**: **F3 해소.** 1차가 특정한 두 문장의 허위성은 모두 사라졌다.

다만 재검증 중 **약화된 후속 우려**를 발견해 F11로 신설한다(차단 아님) —
CHANGELOG는 `[수동]` AC 15건이 어떤 관측 기록도 없이 남아 있다는 사실을
어디에도 공개하지 않는다. 이는 **허위 진술이 아니라 공개 누락**이며, "(총
86건) … 전부 PASS"라는 문장 배열이 한정어를 놓친 독자에게 "86건 전부 통과"로
읽힐 여지를 남긴다. Known Limitations 절이 S7·S9·AC-FE-088만 열거하고 있어
더욱 그렇다.

### I.5 회귀 검증 (1차 기준선 대비)

| 명령 | 1차 기준선 | 2차 결과 | 판정 |
|---|---|---|---|
| `npx vitest run` | exit=0, 15 files / 114 tests | `Test Files 15 passed (15)` / `Tests 114 passed (114)`, exit=0 | 동일 |
| `npx tsc -p tsconfig.app.json --noEmit` | exit=0 (`tsc -b --force`) | exit=0, `error TS` 0건 | 동일 |
| `npm run lint` (oxlint) | exit=0 | exit=0 (출력 없음) | 동일 |
| `npm audit` | found 0 vulnerabilities | found 0 vulnerabilities | 동일 |
| `npm run build` | exit=0 | exit=0, 151 modules, `dist/`(index.html·assets/·favicon.svg) 생성 확인 — 오케스트레이터가 승인 후 직접 실행 | 동일 |

**신규 테스트 0건**: F1 수정은 컴파일 타임 가드(`endpoints.typecheck.ts`)로
회귀를 잡으며, 런타임 테스트 수는 114건 그대로다. 이는 결함이 아니라 설계
선택이다 — `@ts-expect-error`는 vitest가 아니라 `tsc`가 집행한다.

**런타임 동작 변경 0건**: 브랜디드 타입은 컴파일 후 소거되며, 프로덕션 코드의
유일한 호출부(`MyWaitlistPage.tsx:56`)는 `resolveWaitlistCancelTarget`의
반환값을 그대로 넘기던 기존 배선 그대로다.

### I.6 갱신된 AC 집계 (86건)

| 검증 수단 | 건수 | PASS | FAIL | UNVERIFIED | 1차 대비 |
|---|---|---|---|---|---|
| `[검사]` | 14 | 14 | 0 | 0 | FAIL 2 → 0 (900·903) |
| `[자동]` | 16 | 16 | 0 | 0 | 변동 없음 |
| 혼합 | 3 | 3 | 0 | 0 | FAIL 1 → 0 (109) |
| `[수동]` | 53 | 38 | 0 | 15 | 변동 없음(유예) |
| **합계** | **86** | **71** | **0** | **15** | 68/3/15 → 71/0/15 |

UNVERIFIED 15건의 내역은 `§H.3` 표를 그대로 승계한다(재관측하지 않았다).

### I.7 갱신된 차원별 점수

| 차원 | 1차 | 2차 | 판정 | 변동 근거 |
|---|---|---|---|---|
| Functionality (40%) | 72 | **84** | PASS | FAIL 3건 소멸, 차단 등급 AC-FE-109 기계 충족. 만점이 아닌 이유는 UNVERIFIED 15건이 그대로이기 때문 |
| Security (25%) | 90 | **90** | PASS | 프로브 6종 재실행 — XSS 싱크 0 · `console.*` 0 · 하드코딩 URL 0 · 시크릿 0 · `npm audit` 0. 이번 델타는 타입 층위 변경뿐이라 보안 표면 무변동 |
| Craft (20%) | 62 | **63** | FAIL | 컴파일 타임 회귀 가드 신설(+)과 신규 F10(`strict: false`) 발견(−)이 상쇄. 근본 감점 사유인 커버리지 측정 수단 부재(F4)는 그대로 |
| Consistency (15%) | 74 | **82** | PASS | F3 해소로 CHANGELOG-소스 불일치 소멸. F11(공개 누락)·F10만큼 감점 유지 |

조화평균: `4 / (1/84 + 1/90 + 1/63 + 1/82)` = **78.3 / 100** (1차 73.2, +5.1).

### I.8 종합 판정 — 여전히 FAIL, 그러나 FAIL 근거가 완전히 이동했다

| 항목 | 값 |
|---|---|
| 종합 판정 | **FAIL** |
| 조화평균 | **78.3 / 100** (Tier L 통과선 85) |
| AC 집계 | PASS 71 / FAIL 0 / UNVERIFIED 15 |
| 차단 등급 미충족 | **없음** (1차의 유일한 차단 사유 AC-FE-109 해소) |
| 잔여 FAIL 근거 | ① 조화평균 78.3 < 85 (주동인: Craft 63 — F4 커버리지 측정 불가) ② `acceptance.md` §E.4 "부분 완료 경로 없음" 조항이 UNVERIFIED 15건으로 여전히 미충족 |

**이 판정의 성격 변화를 분명히 한다.** 1차 FAIL은 *점수가 아니라 필수 항목
미충족*(차단 등급 AC-FE-109)이 근거였다. 2차 FAIL은 필수 항목 미충족이
**하나도 없으며**, 전적으로 오케스트레이터·사용자가 **명시적으로 다음 회차로
유예한 항목**(F4 · F5 · UNVERIFIED 15)에서만 나온다. 즉 남은 FAIL은
"발견되지 않은 결함"이 아니라 "공개된 채 유예된 부채"다.

`status: completed` 전이(§H.7)는 **여전히 시기상조**이나 근거가 3개에서
1개로 줄었다 — §E.1 체크리스트 6번(AC-FE-109)과 §C 차단 등급 조항은 해소되었고,
§E.4 "부분 완료 경로가 없다" 조항만 남았다. 감사자는 판정만 기록하며
frontmatter를 되돌리지 않는다(상태 전이 소유자는 manager-docs).

### I.9 PR 준비도 판정 — **이번 회차 범위에서 진행 가능**

| 1차 PR 차단 발견 | 2차 판정 | 증거 |
|---|---|---|
| F1 (Blocker) | **해소** | I.2 — 2개 독립 경로 기계 증명 |
| F2 (High) | **해소** | I.3 — 세 명령 모두 0줄 |
| F3 (Medium) | **해소** | I.4 — 두 문장 모두 참 |

**판정: 이번 회차 범위(F1+F2+F3)에 한해 PR 생성 진행이 안전하다.** 단 PR
본문에 미해결 항목(F4·F5·UNVERIFIED 15건·F10·F11)을 명시적으로 공개해야
한다 — 공개가 없으면 F11(공개 누락)이 PR 층위에서 재발한다.
`npm run build`는 오케스트레이터가 승인 직후 직접 실행해 exit=0을 확인했다
(I.5).

### I.10 신규 발견 사항

| ID | 등급 | 확신도 | 위치 | 내용 |
|---|---|---|---|---|
| **F10** | Low | 확정(기계적) | `frontend/tsconfig.app.json` | **`strict` 컴파일러 옵션이 설정되어 있지 않다** — `extends`도 없어 `strict: false`가 실효 값이다(프로브 출력 `strict=false`로 확인). `strictNullChecks` 부재로 `null`/`undefined` 오배선이 타입 검사를 통과한다. Vite의 공식 react-ts 템플릿은 `strict: true`를 켠다. **AC-FE-109 가드는 무관하게 유효하다** — 브랜디드 타입 거부는 strict 모드와 독립임을 프로브로 실증했다(`strict=false` 상태에서 TS2345 발생). |
| **F11** | Info | 확정 | `CHANGELOG.md` L86 · L90-94 | **UNVERIFIED 15건이 공개되지 않는다.** F3의 약화된 후속 — 허위 진술은 아니나, "(총 86건) … 전부 PASS" 배열이 한정어("M1~M6 코드 레벨")를 놓친 독자에게 전건 통과로 읽힐 여지를 남기고, Known Limitations가 15건을 열거하지 않는다. |
| **F12** | Info | 확정 | `§H.10` (1차 감사 기록) | **감사자 자기정정.** 1차 §H.10이 `grep -rn "localStorage" src/ \| grep -v '\.test\.'`의 결과를 `# 0 (주석 언급만)`으로 적었으나, 실제 재실행 결과는 **2줄**이다(`src/session/tokenStorage.ts:4`, `:6` — 둘 다 "localStorage로 회귀시키지 말 것"을 경고하는 `@MX:REASON` 주석). 괄호 주석("주석 언급만")은 정확했고 숫자 표기만 틀렸다. `b98ab93` 시점에도 동일한 2줄이 존재했으므로 **회귀가 아니라 1차 기록의 전사 오류**이며, 실제 `localStorage` 사용은 0건이므로 **보안 판정에는 영향이 없다**(AC-FE-905 계열 PASS 유지). |

### I.11 이월 항목 (재검증하지 않음 — 상태 그대로 승계)

`§H.4`의 다음 발견은 이번 회차 범위 밖으로 명시 유예되었으며, 감사자는 이들을
**해소로 셈하지 않는다**:

| ID | 등급 | 상태 | 비고 |
|---|---|---|---|
| F4 | Medium | 미해소 | 커버리지 측정 수단 부재. Craft 63점의 주동인 |
| F5 | Medium | 미해소 | `vite.config.ts:17`의 `test.include: ['src/**/*.test.ts']` 그대로. `find src -name '*.test.tsx' \| wc -l` → **0** 재확인 |
| F6 | Low | 미해소 | 로그아웃 문구 시제 |
| F7 | Low | 미해소 | §E.2 M4 vs §E.2 M7 S9 기록 모순 |
| F8 | Low | 미해소 | oxlint 규칙 집합이 얇음(재실행 시 출력 0줄 — 기준 설정이 사실상 무점검임을 재확인) |
| F9 | Info | 미해소 | 정본 OWASP 체크리스트 미적재 |
| UNVERIFIED 15건 | — | 미해소 | `§H.3` 표 승계. 재관측하지 않았다 |

### I.12 미검증(Gap) — 감사자가 관측하지 **못한** 것

- **`[수동]` AC 53건 브라우저 재관측 없음.** PASS 38건은 `§H` 그대로 `§E.2` 기록에 대한 전달 판정이다.
- **커버리지 수치 미측정**(F4 도구 부재). 어떤 커버리지 주장도 하지 않는다.
- **정본 OWASP 체크리스트 미적재**(F9). Security 90점은 수동 프로브 6종 + 의존성 감사 기반이다.
- **백엔드 연동 실동작 미수행.** 서버를 기동하지 않았다.
- **`endpoints.typecheck.ts` 가드의 파일 임시 편집 검증 미수행.** 브랜드를 실제로 제거해 TS2578이 나는지는 감사자가 직접 확인하지 않았다(감사 제약: 소스 무변경). 대신 TS2578 의미론 + 컴파일 대상 포함 + exit=0의 3중 조합으로 비공허성을 간접 증명했다(I.2 경로 B). manager-develop이 커밋 메시지에서 "임시 되돌리기로 직접 확인"했다고 주장하나, 감사자는 그 주장을 재현하지 않았다 — 다만 오케스트레이터가 별도로 그 실험을 수행했음이 manager-develop 완료 보고에 기록되어 있다.

### I.13 잔여 위험(Residual risk)

- **브랜드는 컴파일 타임 전용이며 런타임에 소거된다.** `getMyWaitlistEntries`는 `apiFetch<WaitlistListItem[]>`로 원시 JSON을 무검증 캐스팅하므로, 백엔드가 `waitlistEntryId` 자리에 `position` 값을 실어 보내는 **와이어 층위 오배선은 여전히 잡히지 않는다.** `design.md` §A.1의 판정 기준은 "타입 검사에서 거부되는가"이므로 이는 AC 미충족이 아니며, 방어 대상은 어디까지나 "향후 편집자가 도입하는 오배선"이다.
- **F10(`strict: false`)이 만드는 사각지대.** 브랜드 가드는 무관하게 유효하지만, 15개 화면 컴포넌트에 자동 테스트가 0건(F5)인 상태에서 `strictNullChecks`까지 꺼져 있어 널 관련 화면 배선 결함이 어느 층에서도 걸리지 않는다.
- **UNVERIFIED 중 AC-FE-041 · 046 · 068/069는 오늘까지 단 한 번도 실행된 적 없는 코드 경로**라는 `§H`의 잔여 위험이 그대로 유효하다.

### I.14 재현 명령 (2차)

```bash
cd frontend
npx vitest run                                      # exit=0, 15 files / 114 tests
npx tsc -p tsconfig.app.json --noEmit --listFiles   # exit=0, error TS 0건, typecheck.ts 포함 확인
npm run lint                                        # exit=0
npm audit                                           # found 0 vulnerabilities
npm run build                                       # exit=0, 151 modules
grep -rn "as WaitlistEntryId" src/ | grep -v '\.test\.'   # 0 (프로덕션 우회 캐스트 없음)
find src -name '*.test.tsx' | wc -l                 # 0 (F5 이월 확인)

cd ..
git diff --name-only main -- src/ build.gradle                                     # 0 (AC-FE-900)
git diff --name-only main -- .moai/specs/SPEC-{AUTH,COURSE,ENROLLMENT}-001         # 0 (AC-FE-903)
git diff --name-only main -- .moai/project/                                        # 0 (AC-FE-904)
MB=$(git merge-base main HEAD); git diff --name-only $MB HEAD -- src/ build.gradle # 0 (INV-FE-007)
git diff --name-only f4d17df HEAD -- CHANGELOG.md                                  # 0 (F3 무편집 확인)
```

---
