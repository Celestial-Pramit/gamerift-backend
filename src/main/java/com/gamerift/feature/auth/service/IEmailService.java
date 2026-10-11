package com.gamerift.feature.auth.service;

import com.gamerift.feature.auth.model.OtpPurpose;

/** Sends the e-mails of the auth feature. */
public interface IEmailService {

    void sendOtp(String to, String code, OtpPurpose purpose);

    void sendPasswordReset(String to, String link);
}
