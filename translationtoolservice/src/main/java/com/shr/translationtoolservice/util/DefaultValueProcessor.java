package com.shr.translationtoolservice.util;
import java.lang.reflect.Field;
/**
 * @ClassName DefaultValueProcessor
 * @Description TODO
 * @USER: Cola
 * @Date 2024/2/28 0028 8:46
 **/
public class DefaultValueProcessor {
    public static <T> void setDefaultValues(T obj) {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(DefaultValue.class)) {
                DefaultValue annotation = field.getAnnotation(DefaultValue.class);
                try {
                    field.setAccessible(true);
                    field.set(obj, getValue(annotation.value(), field.getType()));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Object getValue(String value, Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        } else if (type == short.class || type == Short.class) {
            return Short.parseShort(value);
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value);
        } else {
            return value;
        }
    }
}