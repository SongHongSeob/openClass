# SPEC-FRONTEND-001 — 진행 기록 (progress)

| 항목 | 값 |
|---|---|
| SPEC ID | `SPEC-FRONTEND-001` |
| 상태 | `in-progress` |
| 버전 | `0.2.2` |
| Tier | L |
| 현재 단계 | run — M1 완료 (워크스페이스 부트스트랩 + 공통 기반). M2 착수 대기 |

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

## §E.3 Run-phase Audit-Ready Signal

_<pending run-phase>_

---

## §E.4 Sync-phase Audit-Ready Signal

_<pending sync-phase>_

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
