### EsGiraffe
> 这是一个利用注解和反射开发一套工具类，用来生成elastisearch的查询语句。为什么要叫Giraffe呢？一是因为我喜欢长颈鹿，二是希望可以通过工具类把像长颈鹿脖子一样长的代码简化一下，三是希望这个工具类可以像桥梁一样连接java和elaticsearch。实在编不下去了，其实就是因为喜欢长颈鹿。目前只适用于简单的查询，不过会在工作学习中慢慢完善的。由于目前在工作中用到最多的就是Bool查询，所以目前生成的查询语句最外层就是bool查询，生成的大致的样子如下:
> 
	    "query": {
             "bool": {
                      "must": [
                      		...
                      ],
                      "must_not": [
                      		...
                      ],
                      "should": [
                      		...
                      ]
                  }
              }
           
     
 
 
 
 
 
#### 0.常见的工作需求
在我工作比较常见的搜索需求就是对外提供搜索的接口。会按照搜索引擎中的字段定义一个对应的model，然后这些字段的值如果为Null，就代表调用方不想对这个字段进行查询，如果不为Null代表需要对这个字段进行查询。而且还会提前在API文档中定义好查询的类型是must、must_not还是should

#### 1.未使用工具类的代码Demo
假设有一个和订单有关的索引，需要对订单的一系列属性进行查询 其中userName和orderMode是should查询 其余都是must
查询model：

```
public class OrderModel {

	private String userName;
	
	private Integer restaurantId;
	
	private Integer orderMode;
	
	private String userPhone;

	private Integer comeFrom;
	
	private String restaurantName;
	
    private String createdAtBegin;
    
    private String createdAtEnd;

    private Integer offset = 0;

    private Integer limit = 10;
    //省略getter和setter
    }

```
查询方法：

```
public SearchService implements ISearchService{

		public String searchOrder(SearchOrderModel search) throws IllegalAccessException{
		BoolQueryBuilder baseQuery = QueryBuilders.boolQuery();
		if(search.getUserName() != null){
			baseQuery.should(QueryBuilders.termQuery("user_name", search.getUserName()));
		}
		if(search.getRestaurantId() != null){
			baseQuery.must(QueryBuilders.termQuery("restaurant_id", search.getRestaurantId()));
		}
		if(search.getOrderMode() != null){
			baseQuery.should(QueryBuilders.termQuery("order_mode", search.getOrderMode()));
		}
		if(search.getUserPhone() != null){
			baseQuery.must(QueryBuilders.termQuery("user_phone", search.getUserPhone()));
		}
		if(search.getComeFrom() != null){
			baseQuery.must(QueryBuilders.termQuery("come_from", search.getComeFrom()));
		}
		if(search.getRestaurantName() != null){
			baseQuery.must(QueryBuilders.queryStringQuery(search.getRestaurantName()).defaultField("restaurant_name"));
		}
		if(search.getCreatedAtBegin() != null){
			baseQuery.must(QueryBuilders.rangeQuery("created_at").gte(search.getCreatedAtBegin()));
		}
		if(search.getCreatedAtEnd() != null){
			baseQuery.must(QueryBuilders.rangeQuery("created_at").lte(search.getCreatedAtBegin()));
		}
		log.info("查询语句:{}",baseQuery.toString());
		SearchResponse response = client.prepareSearch("index1")
				.setTypes("type1")
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				.setQuery(baseQuery)
				.setFrom(search.getOffset()).setSize(search.getLimit()).setExplain(true)
				.execute()
				.actionGet();
		return response.toString();

	}

}


```

下面是针对餐厅名称，用户名和来源同时查询时，打印出来的拼接的查询语句:

```
{   "bool" : {     
		"must" : [ {       
			"term" : {         
				"come_from" : 1       
				}     
			}, {       
			"query_string" : {         
				"query" : "测试餐厅",         
				"default_field" : "restaurant_name"       
				}    
		 } ],     
		 "should" : {       
		 	"term" : {         
		 		"user_name" : "123123123"       
		 		}     
		 	}   
		 } 
} 

```
可是上面只是缩减版的订单model，一个订单的属性有20多个 所以每个属性都判断是否为null，代码就太不漂亮了,而且可读性差，不易维护，所以就想通过注解和反射来精简代码。

#### 2.EsGiraffe的主要内容

EsGiraffe主要是自定义了一些注解，将一些诸如model属性对应的搜索引擎的字段，查询的类型，要查询的index名，要查询的document名用注解标注在model类上，然后在工具类中利用反射获取注解的值 拼接查询语句。下面是几个比较重要的注解:

- Index注解 DocumentType注解

> 只能在类上使用 Index代表要在哪个索引中查询， Document代表要查询的文档 可以指定多个索引或文档用","分割

例子:

```
@Index("index")
@DocumentType("document1")
public class model{
}

```

- EsField注解

> 在类的属性上使用 值为该属性对应的索引字段名 如果value为空，那么对应的索引名就是该属性名。这个注解主要是考虑到在Java习惯驼峰式的命名，而搜索引擎中往往是下划线，所以需要一个注解将他们对应起来。

例子:

```
@Index("index")
@DocumentType("document1")
public class model{
	
	@EsField("come_from")
	private Integer comeFrom;
}

```

- Bool注解

>最重要的注解，里面包含两个元素value和type。其中value是MatchType(枚举类)类型，代表了该Bool查询是MUST，MUST_NOT还是SHOULD，默认是MUST。type的值是EsSearchType(枚举类)类型，代表对该字段采用什么类型的查询。默认值是TERMS，支持的其他类型还有TERM,RANGE_FROM, RANGE_TO, RANGE_GT,RANGE_LT, RANGE_GTE, RANGE_LTE, FUZZY, SHOULD_TERM, QUERY_STRING, MATCH

例子：

```
@Index("index")
@DocumentType("document1")
public class model{
	
	@EsField("come_from")
	@Bool(type = EsSearchType.TERM)
	private Integer comeFrom;
	
	@EsField("created_at")
	@Bool(type = EsSearchType.RANGE_GTE)
	private String createdAtBegin;
}

```
上面的注解代表的查询语句是

```
{   "bool" : {     "must" : [ {       "term" : {         "come_from" : 1       }     }, {       "range" : {         "created_at" : {           "from" : "2015-02-03",           "to" : null,           "include_lower" : true,           "include_upper" : true         }       }     } ]   } } 

```

- From,Size,Sort注解

> 这三个注解也是用到类的属性上的，如果一个属性上标有From注解，代表这个字段的值是查询分页的起始位置；如果一个属性上标有Size注解，代表这个字段的值是分页的长度；如果一个属性上标有Sort注解，代表查询结果按该字段排序，Sort的值有SortType.ASC和SortType.DESC两种。

- ElasticBaseSearch中两个工具方法 getIndexAndType和getQueryBuilder

已经在类和属性添加了注解，那么就需要写两个方法分别通过反射获取类和属性上的值 来拼接对应的查询语句。
其中getQueryBuilder是根据属性上的注解拼装查询语句并返回一个QueryBuilder对象，getIndexAndType是根据注解获得要查询的索引，文档等信息，并返回一个SearchRequestBuilder对象。

#### 3.使用EsGiraffe简化代码

>下面是使用EsGiraffe简化后的代码

查询model类


```
@Index("index1")
@DocumentType("type1")
public class OrderModel {

	@EsField("user_name")
    @Bool(type = EsSearchType.QUERY_STRING, value = MatchType.SHOULD)
	private String userName;
	
	@EsField("restaurant_id")
    @Bool(type = EsSearchType.TERM)
	private Integer restaurantId;
	
	@EsField("order_mode")
	@Bool(type = EsSearchType.TERM)
	private Integer orderMode;
	
	@EsField("usr_phone")
	@Bool(type = EsSearchType.TERM)
	private String userPhone;

	@EsField("come_from")
	@Bool(type = EsSearchType.TERM)
	private Integer comeFrom;
	
	@EsField("restaurant_name")
    @Bool(type = EsSearchType.QUERY_STRING)
	private String restaurantName;
	
	@EsField("created_at")
    @Bool(type = EsSearchType.RANGE_GTE)
    private String createdAtBegin;
    
    @EsField("created_at")
    @Bool(type = EsSearchType.RANGE_LTE)
    private String createdAtEnd;

	@From
    private Integer offset = 0;

	@Size
    private Integer limit = 10;
    //省略getter和setter
    }


```

接口类，也无需写大量的业务逻辑，只需要调用两个工具方法即可

```
    public String searchOrder(SearchOrderModel search) throws IllegalAccessException {
        log.info("查询参数:{}", search.toString());     
        
        QueryBuilder baseQuery = ElasticBaseSearch.getInstance().getQueryBuilder(search);
      

        if(baseQuery != null){
        	log.info(baseQuery.toString());        
            SearchResponse response = ElasticBaseSearch.getInstance().getIndexAndType(client, search).setQuery(baseQuery).execute().actionGet();
            log.info(response.toString());

            return response.toString();
        }
        
        return "";
    }
```

使用了EsGiraffe之后，大大简化了查询接口的代码，无需挨个属性判断是否为null。而且在model类上使用注解，使得程序变得更加可读，每个属性对应搜索引擎中哪个字段，采用哪种查询方式一目了然。下面是使用EsGiraffe之后，对餐厅名称，用户名和来源查询时打印的查询语句，和不用注解生成的是一样的。


```
{   "bool" : {     
		"must" : [ {       
			"term" : {         
				"come_from" : 1       
				}     
			}, {       
			"query_string" : {         
				"query" : "测试餐厅",         
				"default_field" : "restaurant_name"       
				}    
		 } ],     
		 "should" : {       
		 	"term" : {         
		 		"user_name" : "123123123"       
		 		}     
		 	}   
		 } 
}

```







          




