package com.dataexchange.client.domain.util;

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.MDC;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.integration.handler.advice.AbstractHandleMessageAdvice;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;

public final class LogHelper {

    public static class ClearLogContextAdvice implements AfterReturningAdvice, ThrowsAdvice {

        @Override
        public void afterReturning(Object o, Method method, Object[] objects, Object o1) {
            MDC.clear();
        }

        public void afterThrowing(Exception ex) {
            MDC.clear();
        }
    }

    public static ClearLogContextAdvice clearLogContext() {
        return new ClearLogContextAdvice();
    }

    public static MethodBeforeAdvice encrichLogsWithConnectionInfo(String username, String folder) {
        return (method, args, target) -> {
            String correlationId = MDC.get("correlation_id");
            LinkedHashSet<String> keyValues = new LinkedHashSet<>(Arrays.asList(
                    StringUtils.delimitedListToStringArray(correlationId, ";")));
            keyValues.add("username=" + username);
            keyValues.add("file_path=" + folder);
            MDC.put("correlation_id", StringUtils.collectionToDelimitedString(keyValues, ";"));
        };
    }

    public static AbstractHandleMessageAdvice enrichLogsContextWithFileInfo() {
        return new AbstractHandleMessageAdvice() {
            @Override
            protected Object doInvoke(MethodInvocation invocation, Message<?> message) throws Throwable {
                String filename = message.getHeaders().get("file_name", String.class);
                String correlationId = MDC.get("correlation_id");
                LinkedHashSet<String> keyValues = new LinkedHashSet<>(Arrays.asList(
                        StringUtils.delimitedListToStringArray(correlationId, ";")));
                keyValues.add("file_name=" + filename);
                MDC.put("correlation_id", StringUtils.collectionToDelimitedString(keyValues, ";"));

                return invocation.proceed();
            }
        };
    }
}
