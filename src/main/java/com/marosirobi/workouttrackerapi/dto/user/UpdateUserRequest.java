package com.marosirobi.workouttrackerapi.dto.user;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record UpdateUserRequest (@NotBlank @Size(min = 3, max = 20) String username,
                                 @NotBlank @Email String email,
                                 @NotNull @DecimalMin("20.0") @DecimalMax("400.0") Double bodyWeightKg) { }
