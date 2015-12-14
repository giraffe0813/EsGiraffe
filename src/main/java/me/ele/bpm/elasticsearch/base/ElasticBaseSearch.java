package me.ele.bpm.elasticsearch.base;

import me.ele.bpm.elasticsearch.annotation.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.Fuzziness;
import static org.elasticsearch.common.xcontent.XContentFactory.*;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * elastic search utils
 * singleton
 * Created by yemengying on 15/11/4.
 */
public class ElasticBaseSearch {

    private static final Logger logger = LoggerFactory.getLogger(ElasticBaseSearch.class);

    private static ElasticBaseSearch singleton;

    private ElasticBaseSearch(){

    }

    public static synchronized ElasticBaseSearch getInstance(){
        if(singleton == null){
            synchronized (ElasticBaseSearch.class){
                return new ElasticBaseSearch();
            }
        }
        return singleton;

    }



    /**
     * get index and type via annotation index and documentType
     * @param obj
     * @return
     */
    public SearchRequestBuilder getIndexAndType(Client client, Object obj) throws IllegalAccessException {

        SearchRequestBuilder sq = new SearchRequestBuilder(client);

        Class<?> clazz = obj.getClass();

        //check if Index annotation is present
        if (clazz.isAnnotationPresent(Index.class)) {
            Index index= clazz.getAnnotation(Index.class);
            //default value
            String[] indices = {clazz.getName()};
            indices = (index.value().length == 0) ? indices : index.value();
            sq = client.prepareSearch(indices);
        }

        //check if DocumentType annotation is present
        if (clazz.isAnnotationPresent(DocumentType.class)) {
            DocumentType type= clazz.getAnnotation(DocumentType.class);
            //default value
            String[] types = {clazz.getName()};
            types = (type.value().length == 0) ? types : type.value();
            sq = sq.setTypes(types);
        }

        //check if size annotation and from annotation are present
        Field[] fields = clazz.getDeclaredFields();
        for(Field f : fields){
            f.setAccessible(true);
            if(f.get(obj) == null && !f.isAnnotationPresent(Sort.class)){
                continue;
            }

            if(f.isAnnotationPresent(Size.class)){
                sq.setSize((Integer)f.get(obj));

            }
            if(f.isAnnotationPresent(From.class)){
                sq.setFrom((Integer) f.get(obj));
            }
            if(f.isAnnotationPresent(Sort.class)){
                EsField esField = f.getAnnotation(EsField.class);
                Sort sort = f.getAnnotation(Sort.class);
                //get es field name, default use class field name
                String esFieldName = ("".equals(esField.value())) ? f.getName() : esField.value();
                switch(sort.type()){
                    case ASC:
                        sq.addSort(esFieldName, SortOrder.ASC);
                        break;
                    case DESC:
                        sq.addSort(esFieldName, SortOrder.DESC);
                        break;
                }
            }
        }

        return sq;
    }

    /**
     * get queryBuilder via annotation
     * @param obj
     * @return
     */
    public QueryBuilder getQueryBuilder(Object obj) throws IllegalAccessException {
        QueryBuilder qb = null;

        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field  f : fields){
            //access private field
            f.setAccessible(true);

            //field value is null, skip
            if(f.get(obj) == null){
                continue;
            }

            //check if Bool annotation is present
            if(f.isAnnotationPresent(Bool.class)){
                Bool bool = f.getAnnotation(Bool.class);
                EsField esField = f.getAnnotation(EsField.class);
                //get es field name, default use class field name
                String esFieldName = ("".equals(esField.value())) ? f.getName() : esField.value();

                Object value = f.get(obj);
                //convert special character
                if(bool.escape()){
                    value = QueryParser.escape((String)f.get(obj));
                }
                //default value
                QueryBuilder nestQb = termQuery(esFieldName, f.get(obj));
                switch (bool.type()){
                    case TERMS:
                        //I don't why it's turn out to an error when pass f.get(obj) to termsQuery() directly
                        //so I cast f.get(obj) to Collection
                        Collection<?> list= (Collection<?>)f.get(obj);
                        nestQb = termsQuery(esFieldName, list);
                        break;
                    case TERM:
                        nestQb = termQuery(esFieldName, value);
                        break;
                    case RANGE_GT:
                        nestQb = rangeQuery(esFieldName).gt(value);
                        break;
                    case RANGE_GTE:
                        nestQb = rangeQuery(esFieldName).gte(value);
                        break;
                    case RANGE_LT:
                        nestQb = rangeQuery(esFieldName).lt(value);
                        break;
                    case RANGE_LTE:
                        nestQb = rangeQuery(esFieldName).lte(value);
                        break;
                    case RANGE_FROM:
                        nestQb = rangeQuery(esFieldName).from(value);
                        break;
                    case RANGE_TO:
                        nestQb = rangeQuery(esFieldName).to(value);
                        break;
                    case FUZZY:
                        int fuzziness = bool.fuzziness();
                        int prefix = bool.prefix();
                        nestQb = (fuzziness == -1) ? fuzzyQuery(esFieldName, value).prefixLength(prefix).fuzziness(Fuzziness.AUTO) :
                                fuzzyQuery(esFieldName, value).prefixLength(prefix).fuzziness(Fuzziness.fromEdits(fuzziness));
                        break;
                    case SHOULD_TERM:
                        nestQb = boolQuery().should(termQuery(esFieldName, value));
                        break;
                    case QUERY_STRING:
                        nestQb = queryStringQuery((String)value).defaultField(esFieldName).defaultOperator(QueryStringQueryBuilder.Operator.AND);
                        break;
                    case MATCH:
                        String minimumShouldMatch = bool.minimumShouldMatch();
                    	nestQb = matchQuery(esFieldName, f.get(obj)).minimumShouldMatch(minimumShouldMatch);
                    	break;

                }
                switch (bool.value()){
                    case MUST:
                        qb = (qb == null ) ? boolQuery().must(nestQb) : ((BoolQueryBuilder) qb).must(nestQb);
                        break;
                    case MUST_NOT:
                        qb = (qb == null ) ? boolQuery().mustNot(nestQb) : ((BoolQueryBuilder) qb).mustNot(nestQb);
                        break;
                    case SHOULD:
                        qb = (qb == null ) ? boolQuery().should(nestQb) : ((BoolQueryBuilder) qb).should(nestQb);
                        break;
                }
            }
        }
        return qb;
    }


    /**
     * get json for update Index via annotation
     * @param obj
     * @return
     * @throws IllegalAccessException
     * @throws IOException
     */
    public XContentBuilder getJsonBuilder4Update(Object obj) throws IllegalAccessException, IOException {

        XContentBuilder builder = jsonBuilder().startObject();

        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field  f : fields) {
            //access private field
            f.setAccessible(true);

            //field value is null, skip
            if (f.get(obj) == null) {
                continue;
            }

            if(f.isAnnotationPresent(EsField.class)) {
                EsField esField = f.getAnnotation(EsField.class);
                String esFieldName = ("".equals(esField.value())) ? f.getName() : esField.value();
                builder = builder.field(esFieldName, f.get(obj));
            }
        }
        builder.endObject();

        return builder;
    }

    /**
     * get json for insert data in Index via annotation
     * the difference from method getJsonBuilder4Update is when value of field is null, do not skip
     * @param obj
     * @return
     * @throws IllegalAccessException
     * @throws IOException
     */
    public XContentBuilder getJsonBuilder4Index(Object obj) throws IllegalAccessException, IOException {

        XContentBuilder builder = jsonBuilder().startObject();

        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field  f : fields) {
            //access private field
            f.setAccessible(true);

            if(f.isAnnotationPresent(EsField.class)) {
                EsField esField = f.getAnnotation(EsField.class);
                String esFieldName = ("".equals(esField.value())) ? f.getName() : esField.value();
                builder = builder.field(esFieldName, f.get(obj));
            }
        }
        builder.endObject();

        return builder;
    }


}
