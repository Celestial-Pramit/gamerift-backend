# Gamerift backend (Phase 5: foundation)

Spring Boot 4.1 + Java 21 + MongoDB Atlas. Port 9090.

## 1. Run it in IntelliJ
1. IntelliJ: File > Open > select this `backend` folder > "Open as Project" > Trust. Let Maven import finish.
2. File > Project Structure > Project SDK = Java 21. (Settings > Build > Compiler > Annotation Processors > tick "Enable annotation processing" for Lombok.)
3. Copy `.env.example` to `.env` and fill it in (never commit or share `.env`):
   - `MONGODB_URI`: Atlas > Connect > Drivers. Make sure the database name `gamerift` is in the URI and special characters in the password are URL-encoded.
   - `ADMIN_EMAIL` / `ADMIN_PASSWORD` (at least 8 characters).
4. Atlas > Network Access must allow your IP (for development you may use 0.0.0.0/0).
5. Run `GameriftApplication` (right-click > Run). Look for "Seeding finished." in the console.
   You may see a "Using generated security password" line: ignore it until Phase 6.

## 2. Check that it works
- http://localhost:9090/swagger-ui.html  (Swagger UI)
- http://localhost:9090/actuator/health  -> {"status":"UP"}
- http://localhost:9090/api/categories   -> 5 categories
- http://localhost:9090/api/categories/nope -> clean JSON 404
- Atlas > Browse Collections: users, categories, channels, streams, events, matches, plans, coupons, ticker_messages.

## 3. Tests
`mvn test` (or the green arrow next to `CategoryServiceImplTest`). Unit tests need no database.

## 4. YouTube links for the seeded streams
Edit `src/main/resources/seed/video-links.properties` (one link per stream key). Leave empty to use the generated artwork.
Good link = official/publisher or esports channel, embedding allowed, not age-restricted. Test: open `https://www.youtube.com/embed/VIDEO_ID`; if it plays it is usable.
To re-seed after editing: set `app.seed.reset: true` in `application-dev.yml`, start once, then set it back to `false`.
(Reset deletes streams, channels, categories, events, matches, plans, coupons and ticker. Users are kept.)

## 5. Git (this folder is its own repo)
```
git init
git branch -M main
git add .
git commit -m "phase 5: backend foundation"
git remote add origin https://github.com/<your-user>/gamerift-backend.git
git push -u origin main
```
`.env` is in `.gitignore`; check with `git status` that it is NOT listed.

## 6. Docker (optional now)
`docker compose up --build` (uses your `.env`).

---

# Phase 6: auth core (register, login, JWT, roles)

## New settings (.env)
```
JWT_SECRET=<at least 32 characters>     # dev profile has a default, production MUST set it
JWT_EXPIRATION=3600000                  # 1 hour
```
Generate a secret in PowerShell:
`$b = New-Object byte[] 48; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)`

## Test it in Swagger (http://localhost:9090/swagger-ui.html)
1. `POST /api/auth/register` with `{"username":"tester1","email":"tester1@example.com","password":"Passw0rd1","country":"Bangladesh"}` -> 201.
   Same body again -> 409 "Email already registered". Password `abc` -> 400 with the field message.
2. `POST /api/auth/login` with the same email and password -> `token` + `user`. Wrong password -> 401.
3. Click **Authorize** (top right), paste the token (without the word Bearer), Authorize.
4. `GET /api/auth/me` -> your user. Without Authorize -> 401 JSON.
5. `GET /api/admin/ping` as the viewer -> **403**. Log in with your ADMIN_EMAIL / ADMIN_PASSWORD, authorize with that token -> **200**.
6. Dev demo viewers exist too: `gamer@gamerift.tech` / `Gamer@123`. `tanvir@example.com` is banned -> login gives 403.

## Connect the React app (optional now)
Apply `gamerift-frontend-auth-patch.zip`, then in `frontend/.env` set `VITE_USE_MOCK=false` and `VITE_MOCK_OTP=true`
(OTP screens stay fake until Phase 7). Restart `npm run dev`.

---

# Phase 7: e-mail OTP, two-step login, forgot password, rate limiting

## New settings (.env)
```
MAIL_MODE=console            # console = codes are printed in the IntelliJ console (no setup). brevo = really send e-mails.
BREVO_API_KEY=               # only for MAIL_MODE=brevo
MAIL_FROM_EMAIL=             # only for brevo: must be a VERIFIED sender in Brevo
MAIL_FROM_NAME=Gamerift
```
**Console mode (default, best for testing):** look in the IntelliJ console for a line like
`[DEV MAIL, not sent] to=you@example.com | Verify your Gamerift email | Your Gamerift code is 482915 ...`

**Real e-mails with Brevo:** Brevo dashboard > *SMTP & API* > *API keys* > generate a key. Then *Senders, Domains & Dedicated IPs* > add and verify the sender e-mail.
Put the key and the sender in `.env`, set `MAIL_MODE=brevo`, restart. (We use Brevo's HTTPS API, not SMTP, because free hosts such as Render block SMTP ports.)

## Test in Swagger (http://localhost:9090/swagger-ui.html), in this order
1. `POST /api/auth/register` `{"username":"tester2","email":"tester2@example.com","password":"Passw0rd2","country":"Bangladesh"}` -> **201**. The code appears in the console.
2. `POST /api/auth/login` before verifying -> **403 "Email not verified"** (a fresh code is sent).
3. `POST /api/auth/verify-otp` `{"email":"tester2@example.com","code":"<code>"}` -> 200. A wrong code -> **400 "Wrong code"** (5 wrong tries delete the code).
4. `POST /api/auth/resend-otp` twice within 30 seconds -> second one **429**.
5. `POST /api/auth/login` -> token. Use **Authorize**.
6. `PUT /api/users/me/2fa` `{"enabled":true}` -> `twoFactor: true`. Log in again -> `{"needsOtp": true}`; then `POST /api/auth/login/verify-otp` with the console code -> token.
7. Admin: log in with `ADMIN_EMAIL` / `ADMIN_PASSWORD` -> always `needsOtp: true`. `PUT /api/users/me/2fa {"enabled":false}` as admin -> **400**.
8. `POST /api/auth/forgot-password` `{"email":"tester2@example.com"}` -> always the same answer (also for an unknown e-mail). The console shows the link; copy the part after `token=`.
9. `POST /api/auth/reset-password` `{"token":"<token>","password":"NewPassw0rd3"}` -> 200. Use the same token again -> **400** (single use).
10. `PUT /api/users/me/password` with a wrong `currentPassword` -> 400.
11. Send 11 wrong logins within a minute -> **429**.

Atlas: new collections `otp_tokens` and `password_reset_tokens` (check the *Indexes* tab: TTL index on `expiresAt`, MongoDB deletes expired documents within about a minute).

## React app
Apply `gamerift-frontend-phase7-part3.zip`, then in `frontend/.env`: `VITE_USE_MOCK=false` and `VITE_MOCK_OTP=false`.

## Troubleshooting
- No code in the console -> check `MAIL_MODE`; in brevo mode read the error line ("Brevo refused the email ..."). A common cause is a sender that is not verified.
- 429 on login -> wait a minute (in-memory limit, also reset by restarting the app).
- Code expired -> codes live 5 minutes; ask for a new one.
