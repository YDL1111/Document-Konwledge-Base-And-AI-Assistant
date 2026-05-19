package com.docbase.admin.customize.async;

import cn.hutool.core.date.DateUtil;
import com.docbase.common.enums.common.LoginStatusEnum;
import com.docbase.common.utils.ServletHolderUtil;
import com.docbase.common.utils.ip.IpRegionUtil;
import com.docbase.domain.system.log.db.SysLoginInfoEntity;
import com.docbase.domain.system.log.db.SysLoginInfoService;
import com.docbase.domain.system.log.db.SysOperationLogEntity;
import com.docbase.domain.system.log.db.SysOperationLogService;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 异步任务工厂。
 */
@Slf4j
@Component
public class AsyncTaskFactory {

    private static SysLoginInfoService sysLoginInfoService;

    private static SysOperationLogService sysOperationLogService;

    public AsyncTaskFactory(SysLoginInfoService sysLoginInfoService,
                            SysOperationLogService sysOperationLogService) {
        AsyncTaskFactory.sysLoginInfoService = sysLoginInfoService;
        AsyncTaskFactory.sysOperationLogService = sysOperationLogService;
    }

    public static Runnable loginInfoTask(final String username,
                                         final LoginStatusEnum loginStatusEnum,
                                         final String message) {
        final UserAgent userAgent = UserAgent.parseUserAgentString(
            ServletHolderUtil.getRequest().getHeader("User-Agent"));
        final String browser = userAgent.getBrowser() != null ? userAgent.getBrowser().getName() : "";
        final String ip = resolveClientIp();
        final String address = IpRegionUtil.getBriefLocationByIp(ip);
        final String os = userAgent.getOperatingSystem() != null ? userAgent.getOperatingSystem().getName() : "";

        log.info("ip: {}, address: {}, username: {}, loginStatusEnum: {}, message: {}",
            ip, address, username, loginStatusEnum, message);

        return () -> {
            SysLoginInfoEntity loginInfo = new SysLoginInfoEntity();
            loginInfo.setUsername(username);
            loginInfo.setIpAddress(ip);
            loginInfo.setLoginLocation(address);
            loginInfo.setBrowser(browser);
            loginInfo.setOperationSystem(os);
            loginInfo.setMsg(message);
            loginInfo.setLoginTime(DateUtil.date());
            loginInfo.setStatus(loginStatusEnum.getValue());

            if (sysLoginInfoService == null) {
                throw new IllegalStateException("SysLoginInfoService not initialized for async logging");
            }
            sysLoginInfoService.save(loginInfo);
        };
    }

    public static Runnable recordOperationLog(final SysOperationLogEntity operationLog) {
        return () -> {
            operationLog.setOperatorLocation(IpRegionUtil.getBriefLocationByIp(operationLog.getOperatorIp()));
            if (sysOperationLogService == null) {
                throw new IllegalStateException("SysOperationLogService not initialized for async logging");
            }
            sysOperationLogService.save(operationLog);
        };
    }

    private static String resolveClientIp() {
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
}
