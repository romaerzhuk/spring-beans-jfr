package jfr.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.Advised;

import java.util.Arrays;
import java.util.List;

/**
 * Реализация {@link LoggingJoinPoint} для {@link JoinPoint}.
 *
 * @author Roman_Erzhukov
 */
record AspectLoggingJoinPoint(JoinPoint identityPoint) implements LoggingJoinPoint {
    @Override
    public Class<?> targetClass() {
        Object target = identityPoint.getTarget();
        return !(target instanceof Advised) ? target.getClass()
                : ((Advised) target).getTargetSource().getTargetClass();
    }

    @Override
    public String name() {
        return identityPoint.getSignature()
                .getName();
    }

    @Override
    public Object method() {
        Signature signature = identityPoint.getSignature();
        return signature instanceof MethodSignature
                ? ((MethodSignature) signature).getMethod()
                : signature.getName();
    }

    @Override
    public List<Object> args() {
        return Arrays.asList(identityPoint.getArgs());
    }
}
