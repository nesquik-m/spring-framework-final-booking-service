package com.example.booking_service.service.impl;

import com.example.booking_service.entity.Role;
import com.example.booking_service.entity.User;
import com.example.booking_service.exception.EntityNotFoundException;
import com.example.booking_service.mapper.UserMapper;
import com.example.booking_service.model.KafkaUserEvent;
import com.example.booking_service.repository.UserRepository;
import com.example.booking_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    @Value("${app.kafka.kafkaNewUserTopic}")
    private String topicName;

    private final KafkaTemplate<String, KafkaUserEvent> kafkaUserEventTemplate;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;


    @Override
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Пользователь с ID {0} не найден!", userId)));
    }

    @Override
    @Transactional
    public User createUser(User user, Role role) {
        user.setRoles(Collections.singletonList(role));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        role.setUser(user);
        User createdUser = userRepository.saveAndFlush(user);

        kafkaUserEventTemplate.send(topicName, userMapper.bookingToKafkaNewUser(createdUser));

        return createdUser;
    }

    @Override
    @Transactional
    public User updateUser(Long userId, User user) {
        User updatedUser = findUserById(userId);
        updatedUser.setUsername(user.getUsername());
        updatedUser.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.saveAndFlush(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUserById(Long userId) {
        findUserById(userId);
        userRepository.deleteById(userId);
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Пользователь с именем {0} не найден!", username)));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Пользователь с email {0} не найден!", email)));
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

}
