package com.marosirobi.workouttrackerapi.mapper;

import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.dto.user.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse mapToResponse(User user, String accessToken){
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .bodyWeightKg(user.getBodyWeightKg())
                .accessToken(accessToken)
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserResponse mapToResponse(User user){
        return  mapToResponse(user, null);
    }
}
