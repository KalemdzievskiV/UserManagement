package com.supportportal.repository;

import com.supportportal.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    AppUser findAppUserByUsername(String username);

    AppUser  findAppUserByEmail(String email);
}
