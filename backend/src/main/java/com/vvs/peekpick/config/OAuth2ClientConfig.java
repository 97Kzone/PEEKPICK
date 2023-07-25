package com.vvs.peekpick.config;

import com.vvs.peekpick.oauth.service.CustomOAuth2UserService;
import com.vvs.peekpick.oauth.service.CustomOidcUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@EnableWebSecurity
public class OAuth2ClientConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;
    @Autowired
    private CustomOidcUserService customOidcUserService;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().antMatchers("/static/js/**", "/static/images/**", "/static/css/**","/static/scss/**");
    }

    @Bean
    SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {

        // 23.07.25 CSRF 비활성화
        // Form 로그인 처리 X
        http.csrf().disable()
                .authorizeRequests((requests) -> requests
                .antMatchers("/api/user")
                .access("hasAnyRole('SCOPE_profile','SCOPE_email')")
//                .access("hasAuthority('SCOPE_profile')")
                .antMatchers("/api/oidc")
                .access("hasRole('SCOPE_openid')")
                //.access("hasAuthority('SCOPE_openid')")
                .antMatchers("/", "/member/signup", "/login")
                .permitAll()
//                .anyRequest().authenticated());
                .anyRequest().permitAll());
//        http.formLogin()
//                .loginPage("/login")
//                .loginProcessingUrl("/loginProc")
//                .defaultSuccessUrl("/")
//                .permitAll();

        http.oauth2Login(oauth2 -> oauth2.userInfoEndpoint(
                userInfoEndpointConfig -> userInfoEndpointConfig
                        .userService(customOAuth2UserService)
                        .oidcUserService(customOidcUserService)));

//        http.logout().logoutSuccessUrl("/");
        http.exceptionHandling().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));

        return http.build();
   }

   /*@Bean // hasAuthority 일경우 정의하지 않는다
    public GrantedAuthoritiesMapper grantedAuthoritiesMapper(){
       SimpleAuthorityMapper simpleAuthorityMapper = new SimpleAuthorityMapper();
       simpleAuthorityMapper.setPrefix("ROLE_");
       return simpleAuthorityMapper;
   }*/

}
