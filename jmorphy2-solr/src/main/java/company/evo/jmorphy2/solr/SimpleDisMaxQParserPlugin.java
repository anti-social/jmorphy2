package company.evo.jmorphy2.solr;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.request.SolrQueryRequest;


public class SimpleDisMaxQParserPlugin extends QParserPlugin {
    public static String NAME = "sdismax";

    @Override
    public void init(NamedList args) {}

    @Override
    public QParser createParser(String qstr,
                                SolrParams localParams,
                                SolrParams params,
                                SolrQueryRequest req) {
        return new SimpleDisMaxQParser(qstr, localParams, params, req);
    }
}
