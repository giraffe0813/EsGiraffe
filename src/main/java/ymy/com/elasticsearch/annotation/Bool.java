package ymy.com.elasticsearch.annotation;

import ymy.com.elasticsearch.constant.EsSearchType;
import ymy.com.elasticsearch.constant.MatchType;

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
    boolean escape() default false; // if query String may be contain special character, escape should be true
    int fuzziness() default -1;// only be used under type equals EsSearchType.Fuzzy. default -1 represent use auto value
    int prefix() default 0;// only be used under type equals EsSearchType.Fuzzy. default 0
    String minimumShouldMatch() default "85%";//represent minimum similarity.only be used under type equals MATCH. default 85%.


}
