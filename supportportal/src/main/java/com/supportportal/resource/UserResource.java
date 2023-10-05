package com.supportportal.resource;

import com.supportportal.domain.AppUser;
import com.supportportal.domain.HttpResponse;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.exception.domain.*;
import com.supportportal.service.UserService;
import com.supportportal.utility.JWTTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.supportportal.constant.FileConstant.*;
import static com.supportportal.constant.SecurityConstant.*;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@RestController
@RequestMapping(path = {"/", "/user"})
public class UserResource extends ExceptionHandling {

    private UserService userService;

    private AuthenticationManager authenticationManager;

    private JWTTokenProvider jwtTokenProvider;

    @Autowired
    public UserResource(UserService userService, AuthenticationManager authenticationManager, JWTTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<AppUser> register(@RequestBody AppUser user) throws UserNotFoundException, UsernameExistsException, EmailExistsException, MessagingException {
      AppUser newUser = userService.register(user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail());
      return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AppUser> login(@RequestBody AppUser user) throws UserNotFoundException, UsernameExistsException, EmailExistsException {
        authenticate(user.getUsername(), user.getPassword());
        AppUser loginUser = userService.findUserByUsername(user.getUsername());
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        HttpHeaders jtwHeaders = getJtwHeader(userPrincipal);
        return new ResponseEntity<>(loginUser, jtwHeaders, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<AppUser> addUser(@RequestParam("firstName") String firstName,
                                           @RequestParam("lastName") String lastName,
                                           @RequestParam("username") String username,
                                           @RequestParam("email") String email,
                                           @RequestParam("role") String role,
                                           @RequestParam("isActive") String isActive,
                                           @RequestParam("isNotLocked") String isNotLocked,
                                           @RequestParam(value = "profileImage", required = false)MultipartFile profileImage) throws UsernameExistsException, EmailExistsException, IOException, NotAnImageFileException, UserNotFoundException {
        AppUser newUser = userService.addNewUser(firstName, lastName, username, email, role,
                Boolean.parseBoolean(isNotLocked), Boolean.parseBoolean(isActive), profileImage);

        return new ResponseEntity<>(newUser,HttpStatus.OK);
    }

    @PostMapping("/update")
    public ResponseEntity<AppUser> update(@RequestParam("currentUsername") String currentUsername,
                                          @RequestParam("firstName") String firstName,
                                           @RequestParam("lastName") String lastName,
                                           @RequestParam("username") String username,
                                           @RequestParam("email") String email,
                                           @RequestParam("role") String role,
                                           @RequestParam("isActive") String isActive,
                                           @RequestParam("isNotLocked") String isNotLocked,
                                           @RequestParam(value = "profileImage", required = false)MultipartFile profileImage) throws UsernameExistsException, EmailExistsException, IOException, NotAnImageFileException, UserNotFoundException {
        AppUser updateUser = userService.updateUser(currentUsername, firstName, lastName, username, email, role,
                Boolean.parseBoolean(isNotLocked), Boolean.parseBoolean(isActive), profileImage);

        return new ResponseEntity<>(updateUser,HttpStatus.OK);
    }

    @GetMapping("/find/{username}")
    public ResponseEntity<AppUser> getUser(@PathVariable("username") String username){
        AppUser user = userService.findUserByUsername(username);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/list")
    public ResponseEntity<List<AppUser>> getAllUsers(){
        List<AppUser> users = userService.getUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("resetPassword/{email}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email) throws EmailNotFoundException, MessagingException {
        userService.resetPassword(email);

        return response(HttpStatus.OK, "Email sent to: " + email);
    }

    @DeleteMapping("/delete/{username}")
    @PreAuthorize("hasAnyAuthority('user:read')")
    public ResponseEntity<HttpResponse> deleteUser(@PathVariable("username") String username) throws IOException {
        userService.deleteUser(username);
        return response(HttpStatus.NO_CONTENT, "User deleted Successfully");
    }

    @PostMapping("/updateProfileImage")
    public ResponseEntity<AppUser> updateProfileImage(@RequestParam("username") String username, @RequestParam("profileImage") MultipartFile profileImage) throws UsernameExistsException, EmailExistsException, IOException, NotAnImageFileException, UserNotFoundException {
        AppUser user = userService.updateProfileImage(username, profileImage);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping(path = "/image/profile/{username}", produces = IMAGE_JPEG_VALUE)
    public byte[] getTempProfileImage(@PathVariable("username") String username) throws IOException {
        URL url = new URL(TEMP_PROFILE_IMAGE_BASE_URL + username);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = url.openStream()) {
            int bytesRead;
            byte[] chunk = new byte[1024];
            while((bytesRead = inputStream.read(chunk)) > 0) {
                byteArrayOutputStream.write(chunk, 0, bytesRead);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    @GetMapping(path = "/image/{username}/{fileName}", produces = IMAGE_JPEG_VALUE)
    public byte[] getProfileImage(@PathVariable("username") String username, @PathVariable("fileName") String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(USER_FOLDER + username + FORWARD_SLASH + fileName));
    }

    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String s) {
        return new ResponseEntity<>(new HttpResponse(httpStatus.value(), httpStatus,
                httpStatus.getReasonPhrase().toUpperCase(), s.toUpperCase()), httpStatus);
    }


    private HttpHeaders getJtwHeader(UserPrincipal userPrincipal) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(userPrincipal));
        return httpHeaders ;
    }

    private void authenticate(String username, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException e) {
            // Handle authentication failure
            e.printStackTrace();
            // For example, you can redirect the user to a login error page or return a 401 Unauthorized response.
        }
    }
}
