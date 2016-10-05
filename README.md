[![Travis Status](https://travis-ci.org/anti-social/jmorphy2.svg)](https://travis-ci.org/anti-social/jmorphy2)
[![Appveyor Status](https://ci.appveyor.com/api/projects/status/x9df34q1er8r5kc0?svg=true)](https://ci.appveyor.com/project/anti-social/jmorphy2)

Jmorphy2
========

Java port of the [pymorphy2](https://github.com/kmike/pymorphy2)

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

To see all available vagga commands just type ``vagga``


Elasticsearch plugin
====================

Default elasticsearch version against which plugin is built is 2.3.5

To build for specific elastisearch version run build as:

```sh
gradle build -PesVersion=2.2.1
```

Or:

```sh
vagga build -PesVersion=2.2.1
```

Supported elasticsearch versions: 2.1, 2.2, 2.3

Install plugin:

```sh
export es_home=/usr/share/elasticsearch
cd ${es_home}

./bin/plugin install file:jmorphy2-elasticsearch/build/distributions/jmorphy2-elasticsearch-0.2-dev.zip

# Put `pymorphy2_dicts` into `config/jmorphy2/ru` direcotory:
mkdir -p /etc/elasticsearch/jmorphy2/ru/
cp -r jmorphy2-core/src/test/resources/pymorphy2_dicts /etc/elasticsearch/jmorphy2/ru/
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
curl -XPUT 'localhost:9200/test_index' -d '---
settings:
  index:
    analysis:
      filter:
        delimiter:
          type: word_delimiter
          preserve_original: true
        jmorphy2_russian:
          type: jmorphy2_stemmer
          name: ru
        jmorphy2_ukrainian:
          type: jmorphy2_stemmer
          name: uk
      analyzer:
        text_ru:
          tokenizer: whitespace
          filter:
          - delimiter
          - lowercase
          - jmorphy2_russian
        text_uk:
          tokenizer: whitespace
          filter:
          - delimiter
          - lowercase
          - jmorphy2_ukrainian
'

# Test russian analyzer
curl -XGET 'localhost:9200/test_index/_analyze?analyzer=text_ru&pretty' -d 'Привет, лошарики!'
curl -XGET 'localhost:9200/test_index/_analyze?analyzer=text_ru&pretty' -d 'ёж еж'

# Test ukrainian analyzer
curl -XGET 'localhost:9200/test_index/_analyze?analyzer=text_uk&pretty' -d 'Пригоди Котигорошка'
curl -XGET 'localhost:9200/test_index/_analyze?analyzer=text_uk&pretty' -d 'їжаки'
curl -XGET 'localhost:9200/test_index/_analyze?analyzer=text_uk&pretty' -d "комп'ютером"
```
