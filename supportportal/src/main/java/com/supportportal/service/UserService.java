package com.supportportal.service;

import com.supportportal.domain.AppUser;
import com.supportportal.exception.domain.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface UserService {

    AppUser register(String firstName, String lastName, String username, String email) throws UsernameExistsException, EmailExistsException, MessagingException, UserNotFoundException;

    List<AppUser> getUsers();

    AppUser findUserByUsername(String username);

    AppUser findUserByEmail(String email);

    AppUser addNewUser(String firstName, String lastName, String username, String email, String role, boolean isNotLocked, boolean isActive, MultipartFile profileImage) throws UsernameExistsException, EmailExistsException, IOException, NotAnImageFileException, UserNotFoundException;

    AppUser updateUser(String currentUsername, String newFirstName, String newLastName, String newUsername, String newEmail, String role, boolean isNotLocked, boolean isActive, MultipartFile profileImage) throws UsernameExistsException, EmailExistsException, IOException, NotAnImageFileException, UserNotFoundException;

    void deleteUser(String username) throws IOException;

    void resetPassword(String email) throws EmailNotFoundException, MessagingException;

    AppUser updateProfileImage(String username, MultipartFile profileImage) throws UsernameExistsException, EmailExistsException, IOException, NotAnImageFileException, UserNotFoundException;
}

