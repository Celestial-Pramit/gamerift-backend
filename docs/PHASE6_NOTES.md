# Phase 6 notes (for explaining to faculty)

## Login flow
```
React  POST /api/auth/login {email, password}
   |
   v  SecurityConfig: this URL is public, so no token is needed
AuthController (@Valid checks the JSON)
   v
AuthServiceImpl.login: find user by email -> BCrypt matches? -> banned? -> JwtUtil.generateToken
   v
AuthResponse {token, user}   <- React saves both in localStorage
```
## Every later request
```
React  GET /api/auth/me   Header: Authorization: Bearer <token>
   v
JwtAuthFilter: read header -> JwtUtil checks signature + expiry -> load user from MongoDB -> not banned?
   -> put AuthUser into the SecurityContext
   v
SecurityConfig rules: .anyRequest().authenticated()  (and /api/admin/** needs ROLE_ADMIN)
   v
AuthController.me(@AuthenticationPrincipal AuthUser)
```
No token / bad token -> RestAuthenticationEntryPoint (401 JSON). Wrong role -> RestAccessDeniedHandler (403 JSON).

## Viva cheat sheet
1. **What is a JWT?** Three Base64 parts: header, payload (claims like user id and expiry) and a signature made with a secret. The server verifies the signature, so it needs no session storage.
2. **Why stateless?** No server session: easier to scale and deploy (Render), and no CSRF risk because we do not use cookies.
3. **Where is the secret?** In `.env` (`JWT_SECRET`), never in git. Token signed with HMAC-SHA256.
4. **Why load the user from the database on each request?** So a ban or a role change works immediately, not only after the token expires.
5. **Why the same message for unknown email and wrong password?** So attackers cannot find out which emails are registered.
6. **How are passwords stored?** BCrypt hash with a random salt; only the hash is saved.
7. **Difference between 401 and 403?** 401 = not logged in (or bad token). 403 = logged in but the role is not allowed.
8. **Why two layers of admin protection?** URL rule in SecurityConfig plus `@PreAuthorize` on the controller: defence in depth.
9. **Why do validation errors come back as 400 with field messages?** `@Valid` + Bean Validation annotations on the DTO, handled by GlobalExceptionHandler.
10. **Why is the filter created with `new` and not as a @Component?** A servlet Filter bean would also be registered outside the security chain and could run twice.

## Known temporary decisions
- Register verifies the e-mail automatically. Phase 7 adds the OTP e-mail, 2FA, forgot/reset password and rate limiting.
- Admin login does not ask for an OTP yet (Phase 7).
