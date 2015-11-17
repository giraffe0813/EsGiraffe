package me.ele.bpm.elasticsearch.annotation;

import me.ele.bpm.elasticsearch.constant.EsSearchType;
import me.ele.bpm.elasticsearch.constant.MatchType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * represent bool search
 * can only be used on filed
 * eg:
 * @Bool @Field(application_id)
 * private Integer applicationId;<------>QueryBuilders.boolQuery()
                                              .must(new TermQueryBuilder("application_id",value))
 * Created by yemengying on 15/11/4.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Bool {

    MatchType value() default MatchType.MUST;
    EsSearchType type() default EsSearchType.TERMS;


}
