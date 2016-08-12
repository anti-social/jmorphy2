jmorphy2
========

Java port of the pymorphy2

Compile project, build jars and run tests:

```sh
git clone https://github.com/anti-social/jmorphy2
cd jmorphy2
gradle build
```


Elasticsearch
=============

Default elasticsearch version against which plugin is built is 2.3.5

To build for specific elastisearch version run build as:

```sh
gradle build -PesVersion=2.2.1
```

Supported elasticsearch versions: 2.1, 2.2, 2.3

Install plugin

```sh
export es_home=/usr/share/elasticsearch
cd ${es_home}

./bin/plugin install file:jmorphy2-elasticsearch/build/distributions/jmorphy2-elasticsearch-0.2-dev.zip

# Put `pymorphy2_dicts` into `config/jmorphy2/ru` direcotory:
mkdir -p /etc/elasticsearch/jmorphy2/ru/
cp -r jmorphy2-core/src/test/resources/pymorphy2_dicts /etc/elasticsearch/jmorphy2/ru/
cp jmorphy2-elasticsearch/src/test/resources/indices/analyze/config/jmorphy2/ru/replaces.json /etc/elasticsearch/jmorphy2/ru/
```

Create index with specific analyzer and test it:


```sh
cd ${es_home}
bin/elasticsearch

# open new tab
curl -XPUT 'localhost:9200/test_index' -d '
{
    "settings": {
        "index": {
            "analysis": {
                "filter": {
                    "jmorphy2": {
                        "type": "jmorphy2_stemmer",
                        "name": "ru",
                        "cache_size": 50000
                    }
                },
                "analyzer": {
                    "text_ru": {
                        "tokenizer": "whitespace",
                        "filter": [
                            "word_delimiter",
                            "lowercase",
                            "jmorphy2"
                        ]
                    }
                }
            }
        }
    }
}
'

curl -XGET 'localhost:9200/test_index/_analyze?analyzer=text_ru&pretty' -d 'Привет, лошарики!'
# you should get something like that:
{
  "tokens" : [ {
    "token" : "привет",
    "start_offset" : 0,
    "end_offset" : 6,
    "type" : "word",
    "position" : 1
  }, {
    "token" : "лошарик",
    "start_offset" : 8,
    "end_offset" : 16,
    "type" : "word",
    "position" : 2
  }, {
    "token" : "лошарика",
    "start_offset" : 8,
    "end_offset" : 16,
    "type" : "word",
    "position" : 2
  } ]
}
```
