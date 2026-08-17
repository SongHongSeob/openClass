# openclass-ap

## 인증 API (SPEC-AUTH-001)

JWT 액세스 토큰 기반 인증. 공개 엔드포인트 2개(`POST /api/auth/signup`,
`POST /api/auth/login`)만 노출하며, 그 외 보호 엔드포인트는
`Authorization: Bearer <token>` 헤더로 인증한다. `/api/admin/**` 경로는
`ADMIN` 역할 토큰만 접근할 수 있다.

### 알려진 제약 — 로그아웃

이 SPEC은 **토큰 폐기 목록(denylist)이나 서버 측 강제 무효화를 도입하지
않는다.** 액세스 토큰의 수명은 30분(`app.jwt.access-token-ttl`)으로 짧게
유지되는 것이 유일한 완화 장치다.

- **로그아웃은 클라이언트가 저장된 토큰을 버리는 것으로만 이루어진다.**
  서버는 발급된 토큰을 추적하거나 무효화하지 않는다.
- 따라서 **토큰이 탈취되면 만료 시각(발급 후 최대 30분)까지 계속
  유효하다.** "로그아웃"을 호출해도 이미 탈취된 토큰은 막을 수 없다.
- 이는 숨겨진 결함이 아니라 v1 규모에 맞춰 의도적으로 선택한 트레이드오프다
  (`.moai/specs/SPEC-AUTH-001/plan.md` §C.2 참조). 다중 사용자 운영이나
  보안 사고 대응이 필요해지는 시점에 별도 SPEC으로 토큰 폐기 목록을
  도입할 예정이다.

### 최초 관리자 계정

애플리케이션 최초 기동 시 `app.admin.email` / `app.admin.password`
프로퍼티(환경변수 `ADMIN_EMAIL` / `ADMIN_PASSWORD`)로 지정된 계정이 없으면
자동으로 `ADMIN` 역할의 관리자 계정 1개가 생성된다. 이미 존재하면
아무 동작도 하지 않는다(멱등).

## 수강신청 큐·워커 (SPEC-ENROLLMENT-001)

선착순 수강신청은 DB 테이블 기반 순차 큐 + 단일 워커 방식으로 처리한다.
접수·취소·정원 증설 요청은 즉시 확정되지 않고 큐(`enrollment_request`)에
접수 순서대로 적재되며, 서버의 워커(`EnrollmentQueueWorker`)가 이를 한 건씩
순차 처리하면서 정원을 검사하고 확정한다.

### 배포 전제 — 워커 인스턴스는 반드시 1개만 구동한다

**이 프로젝트의 접수 순서 보장(선착순 정합성)은 워커 인스턴스가 정확히
1개일 때만 성립한다** (`SPEC-ENROLLMENT-001` REQ-WRK-012). 스케줄러
활성화 지점은 `EnrollmentQueueWorker.poll()` 1개소뿐이며(`@EnableScheduling`도
`OpenclassApApplication`에서 1회만 선언한다), 애플리케이션을 여러 인스턴스로
동시에 배포하면 각 인스턴스가 독립적으로 이 메서드를 폴링한다.

> ⚠️ **경고 — 다중 인스턴스 배포에서는 접수 순서 보장이 성립하지 않는다.**
> `SELECT ... FOR UPDATE SKIP LOCKED` 클레임 쿼리가 두 워커가 같은 큐 행을
> 중복 처리하는 것은 막아 주지만(REQ-WRK-013), "먼저 접수한 사람이 먼저
> 확정된다"는 순서 보장 자체는 **워커 1개** 전제 위에서만 성립하는
> 논증이다(`SPEC-ENROLLMENT-001` spec.md §A.2 / design.md §3). 순서 보장이
> 필요한 다중 인스턴스 운영은 이 SPEC의 범위 밖이며, 필요해지면 별도 SPEC으로
> 워커 리더 선출·분산 락을 설계해야 한다.

### 워커 설정값

`app.enrollment.worker.*` 프로퍼티로 폴링 주기(`polling-delay-ms`, 기본
200ms)와 배치 크기(`batch-size`, 기본 200건)를 구성한다 —
`SPEC-ENROLLMENT-001` design.md §6의 산출표(이론 처리량 1,000건/초)와
일치한다. 테스트 프로파일은 `scheduler-enabled=false`로 자동 폴링을 끄고
테스트가 직접 큐를 구동한다.

### API 엔드포인트

- `POST /api/courses/{courseId}/enrollments` — 수강신청 접수(즉시 확정 아님, 큐 적재).
- `GET /api/enrollment-requests/{requestId}` — 접수 상태 조회(요청자 소유권 범위, 대기 순번 노출).
- `DELETE /api/enrollments/{enrollmentId}` — 확정된 수강신청 취소.
- `DELETE /api/waitlist-entries/{entryId}` — 대기명단 항목 취소.
- 정원 증설은 기존 `PATCH /api/admin/courses/{id}` 관리자 강좌 수정 엔드포인트를 통해 처리되며, 증설 시 대기명단 자동 승격이 트리거된다.
