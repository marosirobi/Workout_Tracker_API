package com.marosirobi.workouttrackerapi.dto.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
public record ApiError(LocalDateTime timestamp,
                       String error,
                       Integer status,
                       String message,
                       String path) { }
