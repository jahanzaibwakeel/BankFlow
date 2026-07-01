package com.bankflow.api.security;

import com.bankflow.api.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class BankFlowUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public BankFlowUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmailIgnoreCase(username)
            .map(user -> new BankFlowPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRoles(), user.isEnabled()))
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
