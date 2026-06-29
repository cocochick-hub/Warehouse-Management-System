package com.example.wms.aspect;

import com.example.wms.entity.AuditLog;
import com.example.wms.repository.AuditLogRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 操作审计 AOP 切面
 *
 * 自动拦截所有 @PostMapping / @PutMapping / @DeleteMapping 请求，
 * 记录操作人、操作对象、参数详情到 audit_log 表。
 */
@Aspect
@Component
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;

    public AuditLogAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** 拦截所有写操作 */
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String ip = "unknown";
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            ip = request.getRemoteAddr();
        }

        // 2. 获取操作对象（类名）
        String target = joinPoint.getSignature().getDeclaringType().getSimpleName();
        // 去掉 "Controller" 后缀
        if (target.endsWith("Controller")) {
            target = target.substring(0, target.length() - 10);
        }

        // 3. 判断操作类型
        String methodName = joinPoint.getSignature().getName();
        String action = "CREATE";
        if (methodName.contains("update") || methodName.contains("edit")) {
            action = "UPDATE";
        } else if (methodName.contains("delete") || methodName.contains("remove")) {
            action = "DELETE";
        }

        // 4. 提取参数摘要
        Object[] args = joinPoint.getArgs();
        StringBuilder detail = new StringBuilder();
        for (Object arg : args) {
            if (arg != null && !(arg instanceof javax.servlet.http.HttpServletRequest)
                    && !(arg instanceof javax.servlet.http.HttpServletResponse)) {
                String str = arg.toString();
                if (str.length() > 500) str = str.substring(0, 500) + "...";
                detail.append(str).append("; ");
            }
        }

        // 5. 执行目标方法
        Object result;
        String username = "anonymous";
        try {
            result = joinPoint.proceed();
            // 尝试从返回值中提取操作结果
            if (result != null && result.toString().length() < 200) {
                detail.append(" → ").append(result.toString());
            }
        } catch (Throwable e) {
            username = getCurrentUsername();
            // 失败也记录
            detail.append(" [FAILED: ").append(e.getMessage()).append("]");
            saveLog(username, action, target, detail.toString(), ip);
            throw e;
        }

        // 6. 异步保存日志（简化：同步保存，事务外）
        username = getCurrentUsername();
        saveLog(username, action, target, detail.toString(), ip);

        return result;
    }

    private void saveLog(String username, String action, String target, String detail, String ip) {
        try {
            AuditLog log = new AuditLog();
            log.setUsername(username);
            log.setAction(action);
            log.setTarget(target);
            log.setDetail(detail.length() > 1000 ? detail.substring(0, 1000) : detail);
            log.setIp(ip);
            log.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception ignored) {
            // 日志写入失败不影响业务
        }
    }

    private String getCurrentUsername() {
        try {
            return org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
