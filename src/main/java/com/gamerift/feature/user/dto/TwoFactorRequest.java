package com.gamerift.feature.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of PUT /api/users/me/2fa. */
@Getter
@Setter
@NoArgsConstructor
public class TwoFactorRequest {

    private boolean enabled;
}
