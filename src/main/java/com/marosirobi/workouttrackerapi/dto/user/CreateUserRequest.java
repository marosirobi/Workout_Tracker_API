package com.marosirobi.workouttrackerapi.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Username can not be empty!")
    @Size(min = 3, max = 20, message = "Username characters should be between 3 and 20!")
    private String username;

    @NotBlank(message = "Email can not be empty!")
    @Email(message = "Wrong email format!")
    private String email;

    @NotBlank(message = "Password can not be empty!")
    @Size(min = 6, message = "Password should be at least 6 characters")
    private String password;
}
