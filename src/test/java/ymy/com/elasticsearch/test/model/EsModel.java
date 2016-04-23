package ymy.com.elasticsearch.test.model;

import ymy.com.elasticsearch.annotation.Bool;
import ymy.com.elasticsearch.annotation.DocumentType;
import ymy.com.elasticsearch.annotation.EsField;
import ymy.com.elasticsearch.annotation.Index;
import ymy.com.elasticsearch.constant.EsSearchType;
import ymy.com.elasticsearch.constant.MatchType;

/**
 * Created by yemengying on 16/4/23.
 */
@Index("index")
@DocumentType("document")
public class EsModel {

    //针对id字段进行TERM查询 匹配要求是MUST
    @EsField("id")
    @Bool(value= MatchType.MUST,type = EsSearchType.TERM)
    private Integer id;

    //针对user_name字段进行TERM查询 匹配要求是MUST_NOT
    @EsField("user_name")
    @Bool(value= MatchType.MUST_NOT,type = EsSearchType.TERM)
    private String name;

    //针对user_phone字段进行MATCH查询 匹配要求是MUST_NOT
    @EsField("user_phone")
    @Bool(value= MatchType.MUST_NOT,type = EsSearchType.MATCH)
    private String phone;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "EsModel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
