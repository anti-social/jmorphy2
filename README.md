![Java CI](https://github.com/anti-social/jmorphy2/workflows/Java%20CI/badge.svg)
[![Appveyor status](https://ci.appveyor.com/api/projects/status/x9df34q1er8r5kc0/branch/master?svg=true)](https://ci.appveyor.com/project/anti-social/jmorphy2/branch/master)

# Jmorphy2

Java port of the [pymorphy2](https://github.com/kmike/pymorphy2)

Clone project:

```sh
git clone https://github.com/anti-social/jmorphy2
cd jmorphy2
```

Compile project, build jars and run tests:

```
./gradlew build
```

## Elasticsearch plugin

### Plugin installation

- From a debian package:

```shell
curl -SLO https://github.com/anti-social/jmorphy2/releases/download/v0.2.3-es7.14.2/elasticsearch-analysis-jmorphy2-plugin_0.2.3-es7.14.2_all.deb
dpkg -i elasticsearch-analysis-jmorphy2-plugin_0.2.3-es7.14.2_all.deb
```

- Using `elasticsearch-plugin` command:
```shell
# Specify correct path of your Elasticsearch installation
export es_home=/usr/share/elasticsearch
${es_home}/bin/elasticsearch-plugin install "https://github.com/anti-social/jmorphy2/releases/download/v0.2.3-es7.14.2/analysis-jmorphy2-0.2.3-es7.14.2.zip"
```

### Building plugin

Default elasticsearch version against which plugin is built is `7.14.2`

To build for specific elastisearch version run build as:

```shell
./gradlew assemble -PesVersion=7.13.4
```

Supported elasticsearch versions: `6.6.x`, `6.7.x`, `6.8.x`, `7.0.x`, `7.1.x`, `7.2.x`, `7.3.x`, `7.4.x`, `7.5.x`, `7.6.x`, `7.7.x`, `7.8.x`, `7.9.x`, `7.10.x`, `7.11.x`, `7.12.x`, `7.13.x`, `7.14.x`

For older elasticsearch version use specific branches:

- `es-5.4` for Elasticsearch `5.4.x`, `5.5.x` and `5.6.x`
- `es-5.1` for Elasticsearch `5.1.x`, `5.2.x` and `5.3.x`

And install assembled plugin:

```shell
# Specify correct path of your Elasticsearch installation
export es_home=/usr/share/elasticsearch
sudo ${es_home}/bin/elasticsearch-plugin install file:jmorphy2-elasticsearch/build/distributions/analysis-jmorphy2-0.2.2-SNAPSHOT-es7.13.2.zip
```

Or just run elasticsearch inside the container 
(only works for plugin built for default Elasticsearch version):

```shell
# build container and run elasticsearch with jmorphy2 plugin
vagga elastic
```

Using podman or docker:

```shell
podman build -t elasticsearch-jmorphy2 -f Dockerfile.elasticsearch .github
podman run --name elasticsearch-jmorphy2 -p 9200:9200 -e "ES_JAVA_OPTS=-Xmx1g" -e "discovery.type=single-node" elasticsearch-jmorphy2
```

### Test elasticsearch with jmorphy2 plugin

Create index with specific analyzer and test it:


```shell
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
curl -X GET -H 'Content-Type: application/yaml' 'localhost:9200/test_index/_analyze' -d '---
analyzer: text_ru
text: путин
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
