# Linux 설치 및 실행

## 요구사항

- Java 21
- Git
- MySQL 8 이상

## 설정

MySQL 서버와 데이터베이스를 준비합니다.

```bash
export MYSQL_HOST="localhost"
export MYSQL_PORT="3306"
export MYSQL_DATABASE="moil"
export MYSQL_USERNAME="root"
export MYSQL_PASSWORD="your-mysql-password"
```

SMTP는 기본 호스트가 `smtp.gmail.com`입니다. 실제 이메일 발송이 필요할 때만 켭니다.

```bash
export SMTP_ENABLED="true"
export SMTP_HOST="smtp.gmail.com"
export SMTP_PORT="587"
export SMTP_USERNAME="your-email@gmail.com"
export SMTP_PASSWORD="your-app-password"
export SMTP_FROM="your-email@gmail.com"
```

## 실행

```bash
./gradlew bootRun
```

## 테스트

```bash
./gradlew test
```
