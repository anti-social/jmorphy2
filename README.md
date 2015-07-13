jmorphy2
========

Java port of the pymorphy2

Compile project, make jars and run tests:

```sh
git clone https://github.com/anti-social/jmorphy2
cd jmorphy2

mvn package
# or
gradle build
```


Elasticsearch
=============

Copy jmorphy2 jars and dependencies into elasticsearch distribution `lib` directory:

```sh
# replace with real path to your elasticsearch distribution
export path_to_es_distrib=/path/to/es/distrib

# if mvn
cp jmorphy2-elasticsearch/target/*.jar ${path_to_es_distrib}/lib/
cp jmorphy2-elasticsearch/target/lib/*.jar ${path_to_es_distrib}/lib/
# elif gradle
gradle copyDependencies
cp jmorphy2-elasticsearch/build/libs/*.jar ${path_to_es_distrib}/lib/
cp jmorphy2-elasticsearch/build/dependencies/*.jar ${path_to_es_distrib}/lib/
```

Put `pymorphy2_dicts` into `config/jmorphy2/ru` direcotory:

```sh
mkdir -p ${path_to_es_distrib}/config/jmorphy2/ru/
cp -r jmorphy2-core/src/test/resources/pymorphy2_dicts ${path_to_es_distrib}/config/jmorphy2/ru/
cp jmorphy2-elasticsearch/src/test/resources/indices/analyze/config/jmorphy2/ru/replaces.json ${path_to_es_distrib}/config/jmorphy2/ru/
```

Create index with specific analyzer and test it:


```sh
cd ${path_to_es_distrib}
bin/elasticsearch

# open new tab
cd ${path_to_es_distrib}
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
