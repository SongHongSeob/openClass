# SPEC-FRONTEND-001 — 진행 기록 (progress)

| 항목 | 값 |
|---|---|
| SPEC ID | `SPEC-FRONTEND-001` |
| 상태 | `draft` |
| 버전 | `0.2.2` |
| Tier | L |
| 현재 단계 | plan (감사 3회차 FAIL 0.83 → 지적 사항 M1~M4 반영 완료, 델타 범위 재감사 대기) |

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

plan-auditor 감사는 3회차까지 수행되었다(FAIL 0.75 → PASS 0.89 → FAIL 0.83). 3회차 지적 M1~M4를 0.2.2에서 전부 반영했다.

남은 절차는 **델타 범위 재감사**(M1~M4 반영분 한정 — 감사자가 58 REQ / 86 AC 전수 기계 검증의 반복은 불필요하다고 명시) → Implementation Kickoff Approval(휴먼 게이트) → run 단계 진입이다. **`DEP-3` 처리 방침 확정 단계는 소멸했다** — 의존성이 해소되어 결정할 대상이 없다. 남은 절차들은 이 위임의 범위 밖이며 오케스트레이터가 수행한다.

---

## §E.2 Run-phase Evidence

_<pending run-phase>_

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
