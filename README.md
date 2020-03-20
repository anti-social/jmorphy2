[![Travis Status](https://travis-ci.org/anti-social/jmorphy2.svg?branch=master)](https://travis-ci.org/anti-social/jmorphy2)
[![Appveyor status](https://ci.appveyor.com/api/projects/status/x9df34q1er8r5kc0/branch/master?svg=true)](https://ci.appveyor.com/project/anti-social/jmorphy2/branch/master)

# Jmorphy2

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


```
./gradlew build
```

To see all available vagga commands just type ``vagga``


## Elasticsearch plugin

### Plugin installation

```sh
# Specify correct path of your Elasticsearch installation
export es_home=/usr/share/elasticsearch
${es_home}/bin/elasticsearch-plugin install "https://bintray.com/evo/elasticsearch/download_file?file_path=analysis-jmorphy2-0.2.1-es7.5.2.zip"
```

### Building plugin

Default elasticsearch version against which plugin is built is 7.5.2

To build for specific elastisearch version run build as:

```sh
vagga assemble -PesVersion=6.7.1
```

Or:

```sh
./gradlew assemble -PesVersion=6.7.1
```

Supported elasticsearch versions: `6.6.x`, `6.7.x`, `7.0.x`, `7.1.x`, `7.2.x`, `7.3.x`, `7.4.x` and `7.5.x`

For older elasticsearch version use specific branches:

- `es-5.4` for Elasticsearch `5.4.x`, `5.5.x` and `5.6.x`
- `es-5.1` for Elasticsearch `5.1.x`, `5.2.x` and `5.3.x`

And install assembled plugin:

```sh
# Specify correct path of your Elasticsearch installation
export es_home=/usr/share/elasticsearch
sudo ${es_home}/bin/elasticsearch-plugin install file:jmorphy2-elasticsearch/build/distributions/analysis-jmorphy2-0.2.1-SNAPSHOT-es7.5.2.zip
```

Or just run elasticsearch inside the container 
(only works for plugin built for default Elasticsearch version):

```sh
# build container and run elasticsearch with jmorphy2 plugin
vagga elastic
```

### Test elasticsearch with jmorphy2 plugin

Create index with specific analyzer and test it:


```sh
curl -X PUT -H 'Content-Type: application/yaml' 'localhost:9200/test_index' -d '---
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
          tokenizer: standard
          filter:
          - delimiter
          - lowercase
          - jmorphy2_russian
        text_uk:
          tokenizer: standard
          filter:
          - delimiter
          - lowercase
          - jmorphy2_ukrainian
'

# Test russian analyzer
curl -X GET -H 'Content-Type: application/yaml' 'localhost:9200/test_index/_analyze' -d '---
analyzer: text_ru
text: Привет, лошарики!
'
curl -X GET -H 'Content-Type: application/yaml' 'localhost:9200/test_index/_analyze' -d '---
analyzer: text_ru
text: ёж еж ежики
'

# Test ukrainian analyzer
curl -X GET -H 'Content-Type: application/yaml' 'localhost:9200/test_index/_analyze' -d '---
analyzer: text_uk
text: Пригоди Котигорошка
'
curl -X GET -H 'Content-Type: application/yaml' 'localhost:9200/test_index/_analyze' -d '---
analyzer: text_uk
text: їжаки
'
curl -X GET -H 'Content-Type: application/yaml' 'localhost:9200/test_index/_analyze' -d '---
analyzer: text_uk
text: комп\'ютером
'
```
