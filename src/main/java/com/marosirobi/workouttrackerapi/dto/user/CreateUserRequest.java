package com.marosirobi.workouttrackerapi.dto.user;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
}
