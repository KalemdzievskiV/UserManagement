package com.supportportal.constant;

public class SecurityConstant {
     public static final long EXPIRATION_TIME = 432000000;
     public static final String TOKEN_PREFIX = "Bearer";
     public static final String JWT_TOKEN_HEADER = "Jwt-Token";
     public static final String TOKEN_CANNOT_BE_VERIFIED = "Token cannot be verified";
     public static final String KALEM_TOKEN = "Kalem";
     public static final String ADMINISTRATION = "User Management";
     public static final String AUTHORITIES = "Authorities";
     public static final String FORBIDDEN_MESSAGE = "You need to log in";
     public static final String ACCESS_DENIED_MESSAGE = "You do not have permissions";
     public static final String OPTIONS_HTTP_METHOD = "OPTIONS";
     public static final String[] PUBLIC_URLS = {"/user/login", "/user/register", "/user/resetPassword/**", "/user/image/**"};

}
