package net.uaprom.jmorphy2.contrib;

import java.util.List;
import java.util.ArrayList;

import net.uaprom.jmorphy2.MorphAnalyzer;


class Phrase {
    public final String originalPhrase;
    private final MorphAnalyzer analyzer;

    public List<Word> parsedWords;
    
    public Phrase(String phrase, MorphAnalyzer analyzer) {
        this.originalPhrase = phrase;
        this.analyzer = analyzer;
        parse();
    }

    private void parse() {
        parsedWords = new ArrayList<Word>();
        for (String w : originalPhrase.split(" ")) {
            parsedWords.add(new Word(w));
        }
    }

    public enum Grammar {
        SUBJECT,    // подлежащее
        PREDICATE,  // сказуемое
        MODIFIER,   // определение
        ADVERBIAL,  // обстоятельство
        APPOSITION, // приложение
        OBJECT,     // дополнение
    };

    public static class Word {
        public final String word;
        

        public Word(String word) {
            this.word = word;
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass()) {
                return false;
            }

            Word other = (Word) obj;
            return word.equals(other.word);
        }
        
        @Override
        public String toString() {
            return String.format("{Word: %s}", word);
        }
    };
}
