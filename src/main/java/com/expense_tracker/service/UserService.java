package com.expense_tracker.service;

import com.expense_tracker.exception.UserAlreadyExistException;
import com.expense_tracker.exception.UserNotFoundException;
import com.expense_tracker.model.Role;
import com.expense_tracker.model.User;
import com.expense_tracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;


    @Autowired
    private PasswordEncoder passwordEncoder;


    public User saveUser(User user) {

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new UserAlreadyExistException("User already exists with email : " + user.getEmail());
        }

        // encode password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }

        return userRepository.save(user);
    }

    public Page<User> getAllUsers(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> usersPage = userRepository.findAll(pageable);
        if (usersPage.isEmpty()) {
            throw new UserNotFoundException("No users found in the system");
        }
        return usersPage;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(()
                -> new UserNotFoundException("User not found with ID : " + id));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with ID : " + id);
        }
        userRepository.deleteById(id);
    }

    // update all user details { put }
    public User updateUser(Long id, User updateUser) {
        User existingUser = userRepository.findById(id).orElseThrow(()
                -> new UserNotFoundException("User not found with ID : " + id));

        // check for duplicates email (except current user)
        if (userRepository.findByEmail(updateUser.getEmail()).filter(
                user -> !user.getId().equals(id)).isPresent()) {
            throw new UserAlreadyExistException("Email already in use : " + updateUser.getEmail());
        }

        existingUser.setName(updateUser.getName());
        existingUser.setEmail(updateUser.getEmail());

//      existingUser.setPassword(updateUser.getPassword());

        // if password provided in updateUser, encode it; otherwise keep existing
        if (updateUser.getPassword() != null && !updateUser.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(updateUser.getPassword()));
        }
        return userRepository.save(existingUser);
    }


    // update partial details { patch }
    public User patchUser(Long id, Map<String, Object> updates) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID : " + id));

        if (updates.containsKey("role")) {
            throw new IllegalArgumentException("Role cannot be modified through this endpoint");
        }


        // apply only the provided updates
        updates.forEach((field, value) -> {
            switch (field) {
                case "name" -> existingUser.setName((String) value);

                case "email" -> {
                    if (userRepository.findByEmail((String) value).filter(user -> !user.getId().equals(id)).isPresent()) {
                        throw new UserAlreadyExistException("Email already in use : " + value);
                    }
                    existingUser.setEmail((String) value);
                }

                case "password" -> existingUser.setPassword(passwordEncoder.encode((String) value));

                default -> throw new IllegalArgumentException("Invalid field : " + field);
            }
        });

        return userRepository.save(existingUser);
    }

    public void deleteAllUsers() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new UserNotFoundException("No users found to delete");
        }
        userRepository.deleteAll();
    }

    public User changeUserRole(Long id, Role newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID : " + id));

        user.setRole(newRole);
        return userRepository.save(user);
    }
}