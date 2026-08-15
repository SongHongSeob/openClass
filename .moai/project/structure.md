# 프로젝트 구조 (Structure)

## 현재 상태

표준 Gradle/Spring Boot 단일 모듈(single-module) 레이아웃이며, 아직 부트스트랩 클래스 외에는 컨트롤러/엔티티/서비스가 존재하지 않는 초기 상태다.

```
openclass-ap/
├── build.gradle                 # Spring Boot 4.1.0, Java 17 toolchain
├── settings.gradle
├── gradlew / gradlew.bat
├── src/
│   ├── main/
│   │   ├── java/com/hongseob/openclass_ap/
│   │   │   └── OpenclassApApplication.java   # 애플리케이션 부트스트랩만 존재
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/hongseob/openclass_ap/
│           └── OpenclassApApplicationTests.java
├── .moai/                       # MoAI-ADK 설정 및 SPEC 워크스페이스
│   ├── config/                  # quality.yaml, language.yaml, user.yaml 등
│   ├── project/                 # product.md, structure.md, tech.md (본 문서)
│   └── specs/                   # 아직 SPEC 없음 (최초 SPEC 예정)
└── .claude/                     # Claude Code 에이전트/스킬/규칙
```

## 백엔드 패키지 구조 (예상 — 향후 SPEC에서 구체화)

`com.hongseob.openclass_ap` 하위에 도메인별 패키지(예: `member`, `course`, `enrollment`, `waitlist`, `admin` 등)를 추가할 것으로 예상되나, 실제 패키지 분할은 첫 SPEC의 plan 단계에서 결정한다. 현재는 부트스트랩 클래스만 존재하므로 구조를 미리 고정하지 않는다.

## 프론트엔드

아직 존재하지 않음(그린필드). React 워크스페이스의 정확한 위치(별도 디렉터리 vs. 저장소 내 `frontend/`)는 SPEC plan 단계의 열린 결정 사항이다 — 자세한 내용은 `tech.md`의 "미결정 사항" 참고.

## SPEC 워크스페이스

`.moai/specs/`에 아직 등록된 SPEC이 없다. 이 프로젝트의 첫 SPEC(예: 회원가입/로그인, 강좌 조회, 선착순 수강신청 큐 등)부터 시작한다.

## 참고

- 데이터베이스: PostgreSQL (JPA 기반 엔티티는 아직 미작성)
- 개발 방법론: TDD — 테스트가 먼저 작성되고 `src/test/java` 하위에 도메인별 테스트 구조가 함께 성장할 것으로 예상
