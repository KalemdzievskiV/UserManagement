package com.supportportal.service.impl;

import com.supportportal.domain.AppUser;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.enumeration.Role;
import com.supportportal.exception.domain.*;
import com.supportportal.repository.UserRepository;
import com.supportportal.service.EmailService;
import com.supportportal.service.LoginAttemptService;
import com.supportportal.service.UserService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.supportportal.constant.FileConstant.*;
import static com.supportportal.constant.FileConstant.DEFAULT_USER_IMAGE_PATH;
import static com.supportportal.constant.UserImplConstant.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.springframework.http.MediaType.*;

@Service
@Transactional
@Qualifier("UserDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {

    private UserRepository userRepository;
    private Logger LOGGER = LoggerFactory.getLogger(getClass());
    private BCryptPasswordEncoder passwordEncoder;
    private LoginAttemptService loginAttemptService;
    private EmailService emailService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder,
                           LoginAttemptService loginAttemptService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.emailService = emailService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findAppUserByUsername(username);
        if (user == null){
            throw new UsernameNotFoundException("User" + username + "Not Found");
        }
        else {
            validateLoginAttempt(user);
            user.setLastLoginDateDisplay(user.getLastLoginDate());
            user.setLastLoginDate(new Date());
            userRepository.save(user);
            UserPrincipal userPrincipal = new UserPrincipal(user);
            LOGGER.info("Found User by username:" + username);

            return userPrincipal;
        }

    }

    @Override
    public AppUser register(String firstName, String lastName, String username, String email) throws UsernameExistsException, EmailExistsException, MessagingException, UserNotFoundException {
        validateNewUsernameAndEmail(StringUtils.EMPTY, username, email);
        AppUser user = new AppUser();
        user.setUserId(generateUserId());
        String password = generatePassword();
        String encodePassword = encodePassword(password);

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setJoinDate(new Date());
        user.setPassword(encodePassword);
        user.setActive(true);
        user.setNotLocked(true);
        user.setRole(Role.ROLE_USER.name());
        user.setAuthorities(Role.ROLE_USER.getAuthorities());
        user.setProfileImageUrl(getTemporaryProfileImageUrl(username));
        userRepository.save(user);
        emailService.sendNewPasswordEmail(firstName, password, email);

        userRepository.save(user);
        LOGGER.info("New User password:" + password);
        return user;
    }

    private String getTemporaryProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH + username).toUriString();
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }

    private AppUser validateNewUsernameAndEmail(String currentUsername, String newUsername, String newEmail) throws UsernameExistsException, EmailExistsException {

        AppUser userByUsername = findUserByUsername(newUsername);
        AppUser userByEmail = findUserByEmail(newEmail);

        if (StringUtils.isNotBlank(currentUsername)){
            AppUser currentUser = findUserByUsername(currentUsername);
            if (currentUser == null){
                throw new UsernameNotFoundException("No user found by username" + currentUsername);
            }
            if (userByUsername != null && !currentUser.getId().equals(userByUsername.getId())){
                throw new UsernameExistsException(USERNAME_ALREADY_EXISTS);
            }
            if (userByEmail != null && !currentUser.getId().equals(userByEmail.getId())){
                throw new EmailExistsException(EMAIL_ALREADY_EXISTS);
            }
            return currentUser;
        }
        else {
            if (userByUsername != null){
                throw new UsernameExistsException(USERNAME_ALREADY_EXISTS);
            }
            if (userByEmail != null){
                throw new EmailExistsException(EMAIL_ALREADY_EXISTS);
            }
            return null;
        }
    }

    @Override
    public List<AppUser> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public AppUser findUserByUsername(String username) {
        return userRepository.findAppUserByUsername(username);
    }

    @Override
    public AppUser findUserByEmail(String email) {
        return userRepository.findAppUserByEmail(email);
    }

    @Override
    public AppUser addNewUser(String firstName, String lastName, String username, String email, String role,
                              boolean isNotLocked, boolean isActive, MultipartFile profileImage) throws UsernameExistsException, EmailExistsException, IOException, NotAnImageFileException, UserNotFoundException {
        validateNewUsernameAndEmail(StringUtils.EMPTY, username, email);
        AppUser user = new AppUser();
        user.setUserId(generateUserId());
        String password = generatePassword();
        String encodePassword = encodePassword(password);

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setJoinDate(new Date());
        user.setPassword(encodePassword);
        user.setActive(true);
        user.setNotLocked(true);
        user.setRole(getRoleEnumName(role).name());
        user.setAuthorities(getRoleEnumName(role).getAuthorities());
        user.setProfileImageUrl(getTemporaryProfileImageUrl(username));
        try {
            emailService.sendNewPasswordEmail(firstName, password, email);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        userRepository.save(user);
        saveProfileImage(user, profileImage);

        return user;
    }

    @Override
    public AppUser updateUser(String currentUsername, String newFirstName, String newLastName, String newUsername, String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws IOException, NotAnImageFileException, UsernameExistsException, EmailExistsException, UserNotFoundException {
        AppUser currentUser = validateNewUsernameAndEmail(currentUsername, newUsername, newEmail);
        currentUser.setFirstName(newFirstName);
        currentUser.setLastName(newLastName);
        currentUser.setUsername(newUsername);
        currentUser.setEmail(newEmail);
        currentUser.setActive(isActive);
        currentUser.setNotLocked(isNonLocked);
        currentUser.setRole(getRoleEnumName(role).name());
        currentUser.setAuthorities(getRoleEnumName(role).getAuthorities());
        userRepository.save(currentUser);
        saveProfileImage(currentUser, profileImage);
        return currentUser;
    }

    @Override
    public void deleteUser(String username) throws IOException {
        AppUser user = userRepository.findAppUserByUsername(username);
        Path userFolder = Paths.get(USER_FOLDER + user.getUsername()).toAbsolutePath().normalize();
        FileUtils.deleteDirectory(new File(userFolder.toString()));
        userRepository.deleteById(user.getId());
    }

    @Override
    public void resetPassword(String email) throws EmailNotFoundException, MessagingException {
        AppUser user = userRepository.findAppUserByEmail(email);
        if (user == null){
            throw new EmailNotFoundException(NO_USER_FOUND_BY_EMAIL + " " + email);
        }
        String password = generatePassword();
        user.setPassword(encodePassword(password));
        userRepository.save(user);
        emailService.sendNewPasswordEmail(user.getFirstName(), password, user.getEmail());
    }

    @Override
    public AppUser updateProfileImage(String username, MultipartFile profileImage) throws UsernameExistsException, EmailExistsException, IOException, NotAnImageFileException, UserNotFoundException {
        AppUser user = validateNewUsernameAndEmail(username, null, null);
        saveProfileImage(user, profileImage);
        return user;
    }

    private void validateLoginAttempt(AppUser user) {
        if (user.isNotLocked()){
            if (loginAttemptService.hasExceededMaxAttempts(user.getUsername())){
                user.setNotLocked(false);
            }else {
                user.setNotLocked(true);
            }
        }else {
            loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
        }
    }

    private void saveProfileImage(AppUser user, MultipartFile profileImage) throws IOException, NotAnImageFileException {
        if (profileImage != null) {
            if(!Arrays.asList(IMAGE_JPEG_VALUE, IMAGE_PNG_VALUE, IMAGE_GIF_VALUE).contains(profileImage.getContentType())) {
                throw new NotAnImageFileException(profileImage.getOriginalFilename() + NOT_AN_IMAGE_FILE);
            }
            Path userFolder = Paths.get(USER_FOLDER + user.getUsername()).toAbsolutePath().normalize();
            if(!Files.exists(userFolder)) {
                Files.createDirectories(userFolder);
                LOGGER.info(DIRECTORY_CREATED + userFolder);
            }
            Files.deleteIfExists(Paths.get(userFolder + user.getUsername() + DOT + JPG_EXTENSION));
            Files.copy(profileImage.getInputStream(), userFolder.resolve(user.getUsername() + DOT + JPG_EXTENSION), REPLACE_EXISTING);
            user.setProfileImageUrl(setProfileImageUrl(user.getUsername()));
            userRepository.save(user);
            LOGGER.info(FILE_SAVED_IN_FILE_SYSTEM + profileImage.getOriginalFilename());
        }
    }

    private String setProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(USER_IMAGE_PATH + username +
                FORWARD_SLASH + username + DOT + JPG_EXTENSION).toUriString();
    }

    private Role getRoleEnumName(String role) {
        return Role.valueOf(role.toUpperCase());
    }
}
