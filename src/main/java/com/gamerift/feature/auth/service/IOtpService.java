package com.gamerift.feature.auth.service;

import com.gamerift.feature.auth.model.OtpPurpose;

/** Creates and checks one-time codes. */
public interface IOtpService {

    /** Creates a new code for this e-mail and sends it. Throws 429 if one was sent a few seconds ago. */
    void sendCode(String email, OtpPurpose purpose);

    /** Checks the code. Throws 400 if it is wrong, expired or tried too often. A correct code is used up. */
    void verifyCode(String email, String code, OtpPurpose purpose);
}
