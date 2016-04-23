package ymy.com.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In order to get indexName easily
 * can only be used on class
 * Support more than one index
 * eg:@Index("es1","es2")
 * Created by yemengying on 15/11/4.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Index {

    String[] value() default {};//when empty, use class name as index name

}
