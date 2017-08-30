[![Travis Status](https://travis-ci.org/anti-social/jmorphy2.svg?branch=master)](https://travis-ci.org/anti-social/jmorphy2)
[![Appveyor status](https://ci.appveyor.com/api/projects/status/x9df34q1er8r5kc0/branch/master?svg=true)](https://ci.appveyor.com/project/anti-social/jmorphy2/branch/master)

Jmorphy2
========

Java port of the [pymorphy2](https://github.com/kmike/pymorphy2)

Clone project:

```sh
git clone https://github.com/anti-social/jmorphy2
cd jmorphy2
```

Compile project, build jars and run tests:

Build with [vagga](http://vagga.readthedocs.io/en/latest/installation.html#ubuntu)
(no java and gradle needed):

```sh
vagga build
```

Same with gradle (minimum gradle version is `3.3`):

```
gradle build
```

To see all available vagga commands just type ``vagga``


Elasticsearch plugin
====================

Default elasticsearch version against which plugin is built is 5.5.2

To build for specific elastisearch version run build as:

```sh
vagga assemble -PesVersion=5.4.1
```

Or:

```sh
gradle assemble -PesVersion=5.4.1
```

Supported elasticsearch versions: `5.4.x`, `5.5.x`

Install plugin:

```sh
export es_home=/opt/elasticsearch
sudo ${es_home}/bin/elasticsearch-plugin install file:jmorphy2-elasticsearch/build/distributions/analysis-jmorphy2-0.2.0-SNAPSHOT-es-5.5.2.zip
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
curl -XGET 'localhost:9200/test_index/_analyze?analyzer=text_ru&pretty' -d 'ёж еж ежики'

# Test ukrainian analyzer
curl -XGET 'localhost:9200/test_index/_analyze?analyzer=text_uk&pretty' -d 'Пригоди Котигорошка'
curl -XGET 'localhost:9200/test_index/_analyze?analyzer=text_uk&pretty' -d 'їжаки'
curl -XGET 'localhost:9200/test_index/_analyze?analyzer=text_uk&pretty' -d "комп'ютером"
```
