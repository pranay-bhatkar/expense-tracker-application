package com.expense_tracker.utility;

import com.expense_tracker.dto.UserRequestDTO;
import com.expense_tracker.dto.UserResponseDTO;
import com.expense_tracker.model.User;

public class UserMapper {

    private UserMapper() {
    }


    // RequestDTO -> Entity
    public static User toEntity(UserRequestDTO dto) {
        if (dto == null) return null;

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        return user;
    }

    // Entity -> ResponseDTO
    public static UserResponseDTO toDTO(User user) {
        if (user == null) return null;

        return new UserResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}