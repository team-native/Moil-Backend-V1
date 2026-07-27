# 프로젝트 구조

```text
src
├── main
│   ├── kotlin/com/teamnative/moil
│   │   ├── Application.kt
│   │   ├── domain
│   │   │   ├── auth
│   │   │   │   ├── controller/AuthController.kt
│   │   │   │   ├── dto
│   │   │   │   │   ├── AuthTokenResponse.kt
│   │   │   │   │   ├── ChangePasswordRequest.kt
│   │   │   │   │   ├── ConfirmSignupRequest.kt
│   │   │   │   │   ├── DeleteAccountRequest.kt
│   │   │   │   │   ├── LoginRequest.kt
│   │   │   │   │   ├── LoginResponse.kt
│   │   │   │   │   ├── RefreshTokenRequest.kt
│   │   │   │   │   ├── RefreshTokenResponse.kt
│   │   │   │   │   ├── ResetPasswordRequest.kt
│   │   │   │   │   ├── SendEmailCodeRequest.kt
│   │   │   │   │   ├── SendEmailCodeResponse.kt
│   │   │   │   │   ├── VerifyEmailCodeRequest.kt
│   │   │   │   │   └── VerifyEmailCodeResponse.kt
│   │   │   │   ├── model
│   │   │   │   │   ├── EmailVerification.kt
│   │   │   │   │   ├── LoginSession.kt
│   │   │   │   │   ├── UserAccount.kt
│   │   │   │   │   └── VerifiedSignupSession.kt
│   │   │   │   ├── repository
│   │   │   │   │   ├── EmailVerificationRepository.kt
│   │   │   │   │   ├── LoginSessionRepository.kt
│   │   │   │   │   ├── UserAccountRepository.kt
│   │   │   │   │   └── VerifiedSignupSessionRepository.kt
│   │   │   │   └── service
│   │   │   │       ├── AuthenticatedUserService.kt
│   │   │   │       ├── AuthTokenService.kt
│   │   │   │       ├── ChangePasswordService.kt
│   │   │   │       ├── DeleteAccountService.kt
│   │   │   │       ├── EmailVerificationService.kt
│   │   │   │       ├── JwtProvider.kt
│   │   │   │       ├── LoginService.kt
│   │   │   │       ├── PasswordResetService.kt
│   │   │   │       ├── SignupService.kt
│   │   │   │       └── TokenRefreshService.kt
│   │   │   ├── event
│   │   │   │   ├── controller/EventController.kt
│   │   │   │   ├── dto
│   │   │   │   │   ├── CreateEventRequest.kt
│   │   │   │   │   ├── EventCalendarResponse.kt
│   │   │   │   │   ├── EventDetailResponse.kt
│   │   │   │   │   └── UpdateEventRequest.kt
│   │   │   │   ├── model/Event.kt
│   │   │   │   ├── repository/EventRepository.kt
│   │   │   │   └── service
│   │   │   │       ├── EventCommandService.kt
│   │   │   │       └── EventQueryService.kt
│   │   │   ├── group
│   │   │   │   ├── controller/GroupController.kt
│   │   │   │   ├── dto
│   │   │   │   │   ├── CheckGroupInviteRequest.kt
│   │   │   │   │   ├── CheckGroupInviteResponse.kt
│   │   │   │   │   ├── CreateGroupRequest.kt
│   │   │   │   │   ├── CreateGroupResponse.kt
│   │   │   │   │   ├── GroupDetailResponse.kt
│   │   │   │   │   ├── GroupMemberResponse.kt
│   │   │   │   │   ├── GroupSummaryResponse.kt
│   │   │   │   │   ├── JoinGroupRequest.kt
│   │   │   │   │   ├── JoinGroupResponse.kt
│   │   │   │   │   ├── TransferGroupOwnerRequest.kt
│   │   │   │   │   ├── TransferGroupOwnerResponse.kt
│   │   │   │   │   ├── UpdateGroupMemberRoleRequest.kt
│   │   │   │   │   ├── UpdateGroupMemberRoleResponse.kt
│   │   │   │   │   ├── UpdateGroupNameRequest.kt
│   │   │   │   │   ├── UpdateGroupNameResponse.kt
│   │   │   │   │   ├── UpdateGroupNotificationRequest.kt
│   │   │   │   │   └── UpdateGroupNotificationResponse.kt
│   │   │   │   ├── model
│   │   │   │   │   ├── Group.kt
│   │   │   │   │   ├── GroupMember.kt
│   │   │   │   │   └── GroupRole.kt
│   │   │   │   ├── repository
│   │   │   │   │   ├── GroupMemberRepository.kt
│   │   │   │   │   └── GroupRepository.kt
│   │   │   │   └── service
│   │   │   │       ├── GroupCreateService.kt
│   │   │   │       ├── GroupInviteService.kt
│   │   │   │       ├── GroupManagementService.kt
│   │   │   │       ├── GroupMemberIntegrityService.kt
│   │   │   │       ├── GroupMemberQueryService.kt
│   │   │   │       ├── GroupNotificationService.kt
│   │   │   │       ├── GroupPermissionService.kt
│   │   │   │       └── GroupQueryService.kt
│   │   │   └── health/controller/HealthController.kt
│   │   └── global
│   │       ├── config
│   │       │   ├── JwtProperties.kt
│   │       │   ├── MailConfig.kt
│   │       │   ├── SecurityConfig.kt
│   │       │   └── SmtpProperties.kt
│   │       ├── dto/ApiResponse.kt
│   │       └── exception/GlobalExceptionHandler.kt
│   └── resources/application.yml
└── test
    ├── kotlin/com/teamnative/moil
    │   ├── ApplicationTests.kt
    │   ├── domain/group/service/GroupMemberIntegrityServiceTest.kt
    │   └── global/config/SecurityConfigTest.kt
    └── resources/application-test.yml
```
