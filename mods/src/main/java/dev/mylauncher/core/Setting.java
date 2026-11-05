package dev.mylauncher.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public field of a {@link Feature} as a user setting. The field type decides the widget:
 * boolean = toggle, int/float/double = slider (min/max), String = text box, enum = cycle button,
 * int with {@code color = true} = ARGB color picker.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Setting {
    String name();

    String description() default "";

    double min() default 0;

    double max() default 100;

    /** Slider step, 0 = continuous. */
    double step() default 0;

    boolean color() default false;

    /** Starts a new titled section in the settings screen at this setting. */
    String section() default "";
}
