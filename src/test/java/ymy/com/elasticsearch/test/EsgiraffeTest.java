package ymy.com.elasticsearch.test;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ymy.com.elasticsearch.base.ElasticBaseSearch;
import ymy.com.elasticsearch.test.model.EsModel;

public class EsgiraffeTest {

	private static final Logger log = LoggerFactory.getLogger(EsgiraffeTest.class);

	
	@Test
	public void testMessageQueue() throws IllegalAccessException {
		EsModel es = new EsModel();
		es.setId(1);
		es.setName("name");
		es.setPhone("1233221344");
		log.info("查询参数,{}",es.toString());
		QueryBuilder qb = ElasticBaseSearch.getInstance().getQueryBuilder(es);
		log.info("拼装的查询参数是:{}",qb.toString());

		es = new EsModel();
		es.setId(12);
		es.setName("name2");
		log.info("查询参数,{}",es.toString());
		qb = ElasticBaseSearch.getInstance().getQueryBuilder(es);
		log.info("拼装的查询参数是:{}",qb.toString());


//		SearchResponse searchResponse = ElasticBaseSearch.getInstance().getIndexAndType(null);
	}
	
}
