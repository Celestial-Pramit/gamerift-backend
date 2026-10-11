package com.gamerift.feature.auth.service;

/** Forgot / reset password. */
public interface IPasswordResetService {

    /** Always finishes silently: the caller must not learn whether the e-mail exists. */
    void requestReset(String email);

    void resetPassword(String rawToken, String newPassword);
}
