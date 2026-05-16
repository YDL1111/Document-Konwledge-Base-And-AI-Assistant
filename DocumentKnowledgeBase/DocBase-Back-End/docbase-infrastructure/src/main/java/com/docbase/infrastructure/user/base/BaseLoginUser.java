package com.docbase.infrastructure.user.base;

import com.docbase.common.utils.ServletHolderUtil;
import com.docbase.common.utils.ip.IpRegionUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.bitwalker.useragentutils.UserAgent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Base login user abstraction.
 */
@Data
@NoArgsConstructor
public class BaseLoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    protected Long userId;
    protected String cachedKey;
    protected String username;
    protected String password;
    protected List<GrantedAuthority> authorities = new ArrayList<>();
    protected final LoginInfo loginInfo = new LoginInfo();

    public BaseLoginUser(Long userId, String username, String password) {
        this.userId = userId;
        this.username = username;
        this.password = password;
    }

    public void fillLoginInfo() {
        UserAgent userAgent = UserAgent.parseUserAgentString(ServletHolderUtil.getRequest().getHeader("User-Agent"));
        String ip = resolveClientIp();

        this.getLoginInfo().setIpAddress(ip);
        this.getLoginInfo().setLocation(IpRegionUtil.getBriefLocationByIp(ip));
        this.getLoginInfo().setBrowser(userAgent.getBrowser().getName());
        this.getLoginInfo().setOperationSystem(userAgent.getOperatingSystem().getName());
        this.getLoginInfo().setLoginTime(System.currentTimeMillis());
    }

    public void grantAppPermission(String appName) {
        authorities.add(new SimpleGrantedAuthority(appName));
    }

    private String resolveClientIp() {
        String xForwardedFor = ServletHolderUtil.getRequest().getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = ServletHolderUtil.getRequest().getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return ServletHolderUtil.getRequest().getRemoteAddr();
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return this.password;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
