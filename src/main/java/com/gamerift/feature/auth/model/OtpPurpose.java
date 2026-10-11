package com.gamerift.feature.auth.model;

/** Why a one-time code was created. SIGNUP = verify the e-mail, LOGIN = two-step verification. */
public enum OtpPurpose {
    SIGNUP,
    LOGIN
}
