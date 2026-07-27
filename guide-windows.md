# Windows 설치 및 실행

## 요구사항

- Java 21
- Git
- MySQL 8 이상

## 설정

MySQL 서버와 데이터베이스를 준비합니다.

```powershell
$env:MYSQL_HOST = "localhost"
$env:MYSQL_PORT = "3306"
$env:MYSQL_DATABASE = "moil"
$env:MYSQL_USERNAME = "root"
$env:MYSQL_PASSWORD = "your-mysql-password"
```

SMTP는 기본 호스트가 `smtp.gmail.com`입니다. 실제 이메일 발송이 필요할 때만 켭니다.

```powershell
$env:SMTP_ENABLED = "true"
$env:SMTP_HOST = "smtp.gmail.com"
$env:SMTP_PORT = "587"
$env:SMTP_USERNAME = "your-email@gmail.com"
$env:SMTP_PASSWORD = "your-app-password"
$env:SMTP_FROM = "your-email@gmail.com"
```

## 실행

```powershell
.\gradlew.bat bootRun
```

## 테스트

```powershell
.\gradlew.bat test
```
