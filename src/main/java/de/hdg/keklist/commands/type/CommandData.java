package de.hdg.keklist.commands.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandData {

    String name();

    String descriptionKey() default "";

    String[] aliases() default {};

}
