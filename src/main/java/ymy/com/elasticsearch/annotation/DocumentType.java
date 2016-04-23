package ymy.com.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In order to get documentType easily
 * Can only be used on class
 * Support more than one type
 * eg:@DocumentType("type1","type2")
 * Created by yemengying on 15/11/4.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DocumentType {

    //document name
    String[] value() default {};

}
