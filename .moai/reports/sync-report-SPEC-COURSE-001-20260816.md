# Sync Report — SPEC-COURSE-001

- 날짜: 2026-08-16
- 라우트: Route A (Hybrid Trunk main-direct, Tier M — PR 없음, 브랜치 없음)
- 대상 SPEC: SPEC-COURSE-001 — 강좌 엔티티·카탈로그 조회 및 관리자 강좌 관리

## 1. 문서 동기화

| 문서 | 변경 내용 |
|---|---|
| `CHANGELOG.md` | `[Unreleased]` 섹션에 SPEC-COURSE-001 항목 추가 — M1(강좌 엔티티/제약) / M2(공개 카탈로그 API) / M3(관리자 강좌 관리 API) / M4(비기능 마감) 요약 + Verification + Known Limitations |
| `.moai/project/tech.md` | SecurityConfig 인가 규칙 서술을 `GET /api/courses` 단일 매처에서 `/api/courses`, `/api/courses/*` 두 매처로 갱신(M2 D1 변경 반영) |
| `.moai/project/structure.md` | 백엔드 패키지 구조 절의 미래형 예측("따를 것으로 예상된다")을 `course` 패키지 확정 서술로 갱신. SPEC 워크스페이스 절에서 SPEC-COURSE-001을 "완료"로 표기, SPEC-ENROLLMENT-001은 "계획 단계"로 유지(미접촉) |
| `.moai/project/product.md` | 검토 결과 변경 없음 — 제품 수준 서술(강좌 카탈로그 조회, 관리자 강좌 관리)이 이미 이번 구현을 포괄 |

## 2. SPEC 상태 전이

`spec.md` / `plan.md` / `acceptance.md` / `progress.md` frontmatter `status: in-progress → completed` (단일 sync 커밋으로 4개 파일 동시 전이, 3-phase close — 별도 Mx 커밋 없음). `updated:` 필드는 이미 2026-08-16으로 최신 상태.

## 3. 검증 요약

- AC-CRS-001~005, AC-CAT-001~005, AC-ADM-001~008, AC-NFR-001~003 (총 19건) 전부 PASS — `acceptance.md` §D.2 추적성 매트릭스 기준.
- 커버리지: `course`/`course/dto`/`course/admin` 100%, `common/validation` 89% — 목표 85% 상회.
- `acceptance.md` §D.3 Definition of Done 체크리스트 8항목 전부 충족.
- 알려진 이슈: Docker/Testcontainers 환경 플레이키니스(전체 스위트 동시 실행 시 간헐적 연결 타임아웃 — 코드 결함 아님), 로컬 VS Code Gradle 확장 배경 빌드 서버와의 충돌(로컬 개발 환경 아티팩트, CI 무관).

## 4. 이 sync 커밋에서 변경한 파일

- `CHANGELOG.md`
- `.moai/project/tech.md`
- `.moai/project/structure.md`
- `.moai/specs/SPEC-COURSE-001/spec.md` (frontmatter만)
- `.moai/specs/SPEC-COURSE-001/plan.md` (frontmatter만)
- `.moai/specs/SPEC-COURSE-001/acceptance.md` (frontmatter만)
- `.moai/specs/SPEC-COURSE-001/progress.md` (§E.4 섹션 신규 작성)
- `.moai/reports/sync-report-SPEC-COURSE-001-20260816.md` (본 파일)

`product.md`는 검토만 하고 변경하지 않음. `src/main`·`src/test` 파일은 일체 접촉하지 않음.
