package net.lucgameshd.easymysql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author LucGamesYT
 * @version 1.0
 */
@Target ( ElementType.FIELD )
@Retention ( RetentionPolicy.RUNTIME )
public @interface Column {

    String name() default "";

    int length() default 255;
}
