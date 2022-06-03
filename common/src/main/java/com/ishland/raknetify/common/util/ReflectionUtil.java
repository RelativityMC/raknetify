package com.ishland.raknetify.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtil {

    public static Field accessible(Field field) {
        field.setAccessible(true);
        return field;
    }

    public static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }

}
