package dev.mylauncher.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** Reflection wrapper around one {@link Setting} field. */
public final class SettingField {
    public enum Type { BOOLEAN, INT, FLOAT, DOUBLE, STRING, ENUM, COLOR }

    public final Object owner;
    public final Field field;
    public final Setting meta;
    public final Type type;

    private SettingField(Object owner, Field field, Setting meta) {
        this.owner = owner;
        this.field = field;
        this.meta = meta;
        Class<?> t = field.getType();
        if (t == boolean.class) type = Type.BOOLEAN;
        else if (t == int.class) type = meta.color() ? Type.COLOR : Type.INT;
        else if (t == float.class) type = Type.FLOAT;
        else if (t == double.class) type = Type.DOUBLE;
        else if (t == String.class) type = Type.STRING;
        else if (t.isEnum()) type = Type.ENUM;
        else throw new IllegalArgumentException("Unsupported setting type " + t + " on " + field);
    }

    public static List<SettingField> scan(Object owner) {
        List<SettingField> out = new ArrayList<SettingField>();
        // Superclass settings first so shared options (scale, colors) come before mod-specific ones.
        List<Class<?>> chain = new ArrayList<Class<?>>();
        for (Class<?> c = owner.getClass(); c != null && c != Object.class; c = c.getSuperclass()) chain.add(0, c);
        for (Class<?> c : chain) {
            for (Field f : c.getDeclaredFields()) {
                Setting s = f.getAnnotation(Setting.class);
                if (s == null || Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                out.add(new SettingField(owner, f, s));
            }
        }
        return out;
    }

    public String key() {
        return field.getName();
    }

    public Object get() {
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void set(Object value) {
        try {
            field.set(owner, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public double getNumber() {
        return ((Number) get()).doubleValue();
    }

    public void setNumber(double v) {
        v = Math.max(meta.min(), Math.min(meta.max(), v));
        if (meta.step() > 0) v = Math.round(v / meta.step()) * meta.step();
        switch (type) {
            case INT:
                set((int) Math.round(v));
                break;
            case FLOAT:
                set((float) v);
                break;
            default:
                set(v);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void cycleEnum(int dir) {
        Object[] values = field.getType().getEnumConstants();
        int i = ((Enum) get()).ordinal();
        set(values[(i + dir + values.length) % values.length]);
    }

    public String display() {
        Object v = get();
        switch (type) {
            case BOOLEAN:
                return ((Boolean) v) ? "ON" : "OFF";
            case INT:
                return String.valueOf(v);
            case FLOAT:
            case DOUBLE:
                double d = ((Number) v).doubleValue();
                return meta.step() >= 1 ? String.valueOf((long) d) : String.format(java.util.Locale.ROOT, "%.2f", d);
            case ENUM:
                return pretty(((Enum<?>) v).name());
            case COLOR:
                return String.format("#%08X", (Integer) v);
            default:
                return String.valueOf(v);
        }
    }

    public static String pretty(String enumName) {
        StringBuilder sb = new StringBuilder();
        for (String part : enumName.toLowerCase().split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
