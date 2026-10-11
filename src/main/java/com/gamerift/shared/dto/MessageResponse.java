package com.gamerift.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** A plain {"message": "..."} answer for actions that return no data. */
@Getter
@AllArgsConstructor
public class MessageResponse {

    private String message;
}
