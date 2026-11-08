package com.accountshield.service;

import com.accountshield.dto.request.UpdateRoleRequest;
import com.accountshield.dto.request.UpdateStatusRequest;
import com.accountshield.dto.response.PageResponse;
import com.accountshield.dto.response.UserResponse;
import com.accountshield.entity.User;
import com.accountshield.enums.UserStatus;
import com.accountshield.exception.BadRequestException;
import com.accountshield.exception.OperationNotAllowedException;
import com.accountshield.exception.ResourceNotFoundException;
import com.accountshield.mapper.UserMapper;
import com.accountshield.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AdminService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable), userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return userMapper.toResponse(findUser(userId));
    }

    @Transactional
    public UserResponse changeStatus(Long adminId, Long userId, UpdateStatusRequest request) {
        if (isSelf(adminId, userId)) {
            throw new OperationNotAllowedException("Admins cannot change their own status");
        }

        if (request.status() == UserStatus.PENDING_VERIFICATION) {
            throw new BadRequestException("Status PENDING_VERIFICATION cannot be set manually");
        }
        User user = findUser(userId);
        user.setStatus(request.status());

        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse changeRole(Long adminId, Long userId, UpdateRoleRequest request) {
        if (isSelf(adminId, userId)) {
            throw new OperationNotAllowedException("Admins cannot change their own role");
        }
        User user = findUser(userId);
        user.setRole(request.role());
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse unlockUser(Long userId) {
        User user = findUser(userId);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    private static boolean isSelf(Long adminId, Long userId) {
        return Objects.equals(adminId, userId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
