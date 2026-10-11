# Phase 7 notes (for explaining to faculty)

## 1. Sign-up with e-mail verification
```
POST /api/auth/register  -> AuthServiceImpl.register
     save user (emailVerified=false, BCrypt hash) -> OtpServiceImpl.sendCode(SIGNUP)
     code = 6 random digits (SecureRandom) -> only its BCrypt hash is stored in "otp_tokens" (TTL 5 min)
     EmailServiceImpl (@Async) -> Brevo API (or console log in dev)
POST /api/auth/verify-otp -> OtpServiceImpl.verifyCode: not expired? < 5 tries? hash matches? -> code deleted
     user.emailVerified = true
```
## 2. Two-step login (always for ADMIN, optional for viewers)
```
POST /login            password ok + emailVerified + not banned
                       ADMIN or twoFactorEnabled?  -> send LOGIN code, answer {needsOtp:true}  (no token yet)
POST /login/verify-otp code ok -> JWT token + user
```
## 3. Forgot / reset password
```
POST /forgot-password  same answer for every e-mail. If the user exists:
                       token = 32 random bytes (Base64URL), store SHA-256 hash (TTL 15 min), e-mail the link
POST /reset-password   hash(token) found? not expired? -> new BCrypt hash saved, token deleted (single use)
```
## 4. Rate limiting (class RateLimiter, sliding window in memory)
login 10/min per e-mail . verify code 10 per 10 min . resend code 5 per 10 min . forgot password 3 per 10 min .
all POST /api/auth/** 30/min per IP (AuthRateLimitInterceptor). Refused requests -> HTTP 429.

## Viva cheat sheet
1. **Why verify the e-mail?** Proves the user owns the address and stops fake accounts.
2. **Why store the OTP as a hash?** If the database leaks, the codes are not readable. We reuse the BCrypt PasswordEncoder.
3. **Why 5 tries and 5 minutes?** A 6-digit code has 1,000,000 possibilities; limiting tries and time makes guessing useless.
4. **What is a TTL index?** MongoDB deletes a document automatically when the date in the indexed field passes (`@Indexed(expireAfter = "0s")`).
5. **Why the same answer for forgot-password?** An attacker must not learn which e-mails are registered (user enumeration).
6. **Why SHA-256 for the reset token but BCrypt for passwords?** The token is 32 random bytes (very high entropy), so a fast hash is safe; passwords are weak, so they need a slow salted hash.
7. **Why is the reset token single use and short lived?** If the e-mail is read later by someone else, the link is already dead.
8. **What does two-step verification protect against?** A stolen password alone is not enough; the attacker also needs the mailbox.
9. **Why @Async for e-mails?** The user should not wait for the e-mail provider; failures are logged, not shown.
10. **Why the Brevo HTTPS API instead of SMTP?** Free hosting (Render) blocks SMTP ports; HTTPS (443) always works.
11. **How does the rate limiter work?** For each key we keep the times of recent requests; if the count inside the window reaches the limit, we answer 429.
12. **Why a console mode?** Development and tests work without any e-mail account; the code is printed in the log.

## Known limits (honest list)
- The rate limiter is in memory: it resets on restart and is per server instance (fine for one Render instance).
- Behind a proxy the IP limit needs `server.forward-headers-strategy=framework` (set it in production, Phase 11).
- JWT tokens cannot be revoked: after a password reset an old token stays valid until it expires (1 hour).
