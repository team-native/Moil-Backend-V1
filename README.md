# Moil Backend (V1)

각자의 시간이 모여, 우리의 약속이 되는 곳.
**"Moil"** 의 백엔드 서버입니다.

## 기술 스택

- Kotlin 1.9
- Spring Boot 3.3
- Spring Security
- Gradle Kotlin DSL
- JUnit 5
- Java 21

## 프로젝트 구조

```text
src/main/kotlin/com/teamnative/moil
├── Application.kt
├── domain
│   ├── auth
│   │   ├── controller
│   │   └── dto
│   └── health
└── global
    ├── config
    └── exception
```

## 실행

macOS 또는 Linux:

```bash
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

## 테스트

```bash
./gradlew test
```

Windows PowerShell:

```powershell
.\gradlew.bat test
```

## 상태 확인 API

- `GET /health`

## 인증 API

- `POST /api/v1/auth/login`

## 템플릿 유지보수

- `.github/workflows/ci.yml`: Pull request와 `main` 브랜치 push 시 테스트를 실행합니다.
- `.github/dependabot.yml`: Gradle과 GitHub Actions 의존성 업데이트 PR을 주기적으로 생성합니다.
- `.github/pull_request_template.md`: 새 프로젝트에서도 기본 PR 체크리스트를 제공합니다.
