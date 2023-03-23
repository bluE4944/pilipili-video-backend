package com.pilipili.service;

import com.pilipili.entity.User;
import com.pilipili.repository.UserRepository;
import com.pilipili.utils.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 用户服务类
 * @author Liam
 * @version 1.0
 * @date 2023/3/10 23:02
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if(user != null){
            return user;
        }
        throw  new UsernameNotFoundException("User '" + username + "' not found");
        throw new UsernameNotFoundException("User '" + username + "' not found");
    }

    public void saveAdmin(PasswordEncoder passwordEncoder,String password){
        User admin = new User();
        admin.setUserName("admin");
        admin.setEmail("-1");
        admin.setNikeName("admin");
        admin.setPassword(passwordEncoder.encode(password));
        admin.setAuthorization("admin");
        admin.setRole(Role.ROLE_ADMIN.getCode());
        admin.setCreateId(-1L);
        admin.setCreateName("sys");
        admin.setCreateTime(new Date());
        admin.setUpdateId(-1L);
        admin.setUpdateName("sys");
        admin.setUpdateTime(new Date());
        admin.setLogicDel(0);
        userRepository.save(admin);
    }
}
