jmorphy2
========

Java port of the pymorphy2

Compile project, build jars and run tests:

```sh
git clone https://github.com/anti-social/jmorphy2
cd jmorphy2
gradle build
```

Build with [vagga](http://vagga.readthedocs.io/en/latest/installation.html#ubuntu)
(no java and gradle needed):

```sh
vagga build
```


Elasticsearch plugin
====================

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

Or just run elasticsearch inside the container:

```sh
# build container and run elasticsearch with jmorphy2 plugin
vagga elastic
```

Test elasticsearch with jmorphy2 plugin
---------------------------------------

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

curl -XGET 'localhost:9200/test_index/_analyze?analyzer=text_ru&pretty' -d 'ёж еж'            
```
