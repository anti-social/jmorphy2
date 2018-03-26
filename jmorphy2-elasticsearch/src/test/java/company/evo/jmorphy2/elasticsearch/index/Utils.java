/*
 * Copyright 2016 Alexander Koval
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package company.evo.jmorphy2.elasticsearch.index;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

class Utils {
    static void copyFilesFromResources(Settings settings, String lang) throws IOException {
        Path confPath = PathUtils.get(settings.get(Environment.PATH_HOME_SETTING.getKey())).resolve("config");
        Path jmorphy2Path = confPath.resolve("jmorphy2").resolve(lang);
        Path pymorphy2DictsPath = jmorphy2Path.resolve("pymorphy2_dicts");
        Files.createDirectories(pymorphy2DictsPath);

        String[] pymorphy2Files = new String[] {
            "grammemes.json",
            "gramtab-opencorpora-int.json",
            "gramtab-opencorpora-ext.json",
            "meta.json",
            "paradigms.array",
            "prediction-suffixes-0.dawg",
            "prediction-suffixes-1.dawg",
            "prediction-suffixes-2.dawg",
            "p_t_given_w.intdawg",
            "suffixes.json",
            "words.dawg",
        };
        String pymorphy2ResourcePath = String.format
            (Locale.ROOT, "/company/evo/jmorphy2/%s/pymorphy2_dicts", lang);
        for (String fileName : pymorphy2Files) {
            String resourcePath = String.format
                (Locale.ROOT, "%s/%s", pymorphy2ResourcePath, fileName);
            try (InputStream resourceStream = Utils.class.getResourceAsStream(resourcePath)) {
                Files.copy(resourceStream, pymorphy2DictsPath.resolve(fileName));
            }
        }

        String[] nlpFiles = new String[] {
            "tagger_rules.txt",
            "parser_rules.txt",
            "extract_rules.txt",
        };
        String nlpResourcePath = String.format
            (Locale.ROOT, "/company/evo/jmorphy2/elasticsearch/index/%s", lang);
        for (String fileName : nlpFiles) {
            String resourcePath = String.format
                (Locale.ROOT, "%s/%s", nlpResourcePath, fileName);
            try (InputStream resourceStream = Utils.class.getResourceAsStream(resourcePath)) {
                Files.copy(resourceStream, jmorphy2Path.resolve(fileName));
            }
        }
    }
}
