package ymy.com.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In order to mapper class field to index field
 * Can only be used on class field
 * eg: if class field is "private int applicationId" and the field in es is "applicaion_id"
 * can use @Filed("application_id") decorate class field
 * Created by yemengying on 15/11/4.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EsField {

    String value() default "";//when empty, use field name as search field
}
