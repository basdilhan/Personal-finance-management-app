package com.example.backend.config;

import com.example.backend.entity.UserEntity;
import com.example.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserSyncInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    public UserSyncInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            if (!userRepository.existsById(userId)) {
                UserEntity newUser = new UserEntity();
                newUser.setId(userId);
                newUser.setDisplayName("User");
                newUser.setEmail(userId + "@placeholder.com"); // Placeholder required email
                try {
                    userRepository.save(newUser);
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // Ignore: User was inserted by another concurrent request
                } catch (Exception e) {
                    // Ignore other errors during sync
                }
            }
        }
        return true;
    }
}
