package com.ethara.taskmanager.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ApiMessageResponse {

    private final String message;
}
