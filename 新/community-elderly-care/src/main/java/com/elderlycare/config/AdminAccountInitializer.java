package com.elderlycare.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elderlycare.auth.AuthProperties;
import com.elderlycare.entity.User;
import com.elderlycare.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final AuthProperties authProperties;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(AuthProperties authProperties,
                                   UserService userService,
                                   PasswordEncoder passwordEncoder) {
        this.authProperties = authProperties;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        AuthProperties.BootstrapAdmin bootstrapAdmin = authProperties.getBootstrapAdmin();
        if (!bootstrapAdmin.isEnabled()) {
            log.info("跳过管理员引导账号初始化");
            return;
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, bootstrapAdmin.getUsername()).last("LIMIT 1");
        User admin = userService.getOne(wrapper, false);
        String encodedPassword = passwordEncoder.encode(bootstrapAdmin.getPassword());

        if (admin == null) {
            admin = new User();
            admin.setUsername(bootstrapAdmin.getUsername());
            admin.setPassword(encodedPassword);
            admin.setRealName(bootstrapAdmin.getRealName());
            admin.setPhone(bootstrapAdmin.getPhone());
            admin.setUserType(3);
            admin.setStatus(1);
            admin.setCreateTime(LocalDateTime.now());
            admin.setUpdateTime(LocalDateTime.now());
            userService.save(admin);
            log.info("已创建默认管理员账号: {}", bootstrapAdmin.getUsername());
            return;
        }

        boolean changed = false;
        if (bootstrapAdmin.isSyncPasswordOnStartup()) {
            admin.setPassword(encodedPassword);
            changed = true;
        }
        if (!Objects.equals(admin.getUserType(), 3)) {
            admin.setUserType(3);
            changed = true;
        }
        if (!Objects.equals(admin.getStatus(), 1)) {
            admin.setStatus(1);
            changed = true;
        }
        if (StringUtils.hasText(bootstrapAdmin.getRealName())
                && !Objects.equals(admin.getRealName(), bootstrapAdmin.getRealName())) {
            admin.setRealName(bootstrapAdmin.getRealName());
            changed = true;
        }
        if (StringUtils.hasText(bootstrapAdmin.getPhone())
                && !Objects.equals(admin.getPhone(), bootstrapAdmin.getPhone())) {
            admin.setPhone(bootstrapAdmin.getPhone());
            changed = true;
        }

        if (changed) {
            admin.setUpdateTime(LocalDateTime.now());
            userService.updateById(admin);
            log.info("已同步默认管理员账号配置: {}", bootstrapAdmin.getUsername());
        }
    }
}
