package com.accountshield.service;

import com.accountshield.dto.request.UpdateProfileRequest;
import com.accountshield.dto.response.UserResponse;
import com.accountshield.entity.User;
import com.accountshield.exception.ResourceNotFoundException;
import com.accountshield.mapper.UserMapper;
import com.accountshield.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(Long userId) {
        return userMapper.toResponse(findUser(userId));
    }


    @Transactional
    public UserResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setName(request.name().trim());

        User saved = userRepository.saveAndFlush(user);
        return userMapper.toResponse(saved);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
