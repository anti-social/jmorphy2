package company.evo.jmorphy2.elasticsearch.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

import company.evo.jmorphy2.Jmorphy2TestsHelpers;


class Utils {
    public static void copyFilesFromResources(Settings settings, String lang)
        throws IOException
    {
        Path confPath = PathUtils.get(settings.get(Environment.PATH_CONF_SETTING.getKey()));
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
            ("/company/evo/jmorphy2/pymorphy2_dicts_%s", lang);
        for (String fileName : pymorphy2Files) {
            String resourcePath = String.format
                (Locale.ROOT, "%s/%s", pymorphy2ResourcePath, fileName);
            InputStream resourceStream = Utils.class.getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                continue;
            }
            Files.copy(resourceStream, pymorphy2DictsPath.resolve(fileName));
        }

        String[] nlpFiles = new String[] {
            "tagger_rules.txt",
            "parser_rules.txt",
            "extract_rules.txt",
        };
        String nlpResourcePath = String.format
            ("/company/evo/jmorphy2/elasticsearch/index/%s", lang);
        for (String fileName : nlpFiles) {
            String resourcePath = String.format
                (Locale.ROOT, "%s/%s", nlpResourcePath, fileName);
            InputStream resourceStream = Utils.class.getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                continue;
            }
            Files.copy(resourceStream, jmorphy2Path.resolve(fileName));
        }
    }
}
