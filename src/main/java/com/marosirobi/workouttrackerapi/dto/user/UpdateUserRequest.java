package com.marosirobi.workouttrackerapi.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
public record UpdateUserRequest (@NotBlank @Size(min = 3, max = 20) String username,
                                 @NotBlank @Email String email) { }
