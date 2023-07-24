package com.vvs.peekpick.oauth.model.social;

import com.vvs.peekpick.oauth.model.Attributes;
import com.vvs.peekpick.oauth.model.OAuth2ProviderUser;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

public class KakaoOIdcUser extends OAuth2ProviderUser {

    public KakaoOIdcUser(Attributes attributes, OAuth2User oAuth2User, ClientRegistration clientRegistration){
        super(attributes.getMainAttributes(), oAuth2User, clientRegistration);
    }

    @Override
    public String getId() {
        return (String)getAttributes().get("id");
    }

    @Override
    public String getUsername() {
        return (String)getAttributes().get("nickname");
    }

    @Override
    public String getBirthYear() {
        return null;
    }

    @Override
    public String getPicture() {
        return (String)getAttributes().get("profile_image_url");
    }

    @Override
    public String getPhoneNumber() {
        return (String)getAttributes().get("phone_number");
    }

    @Override
    public String getGender() {
        return (String)getAttributes().get("gender");
    }
}