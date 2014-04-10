package net.uaprom.jmorphy2.nlp;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Comparator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.base.Joiner;
import com.google.common.hash.HashCode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;


public class Node {
    public final ImmutableSet<String> grammemeValues;
    public final ImmutableList<Node> children;
    public final String word;
    public final float score;
    protected final ByteBuffer digest = null;
    private Integer cachedHash = null;

    public static final Comparator<Node> scoreComparator =
        new Comparator<Node>() {
            @Override
            public int compare(Node n1, Node n2) {
                return Float.compare(n1.score, n2.score);
            }
    };

    public Node(ImmutableSet<String> grammemeValues, ImmutableList<Node> children, float score) {
        this(grammemeValues, children, null, score);
    }

    public Node(ImmutableSet<String> grammemeValues, String word, float score) {
        this(grammemeValues, null, word, score);
    }

    protected Node(ImmutableSet<String> grammemeValues, ImmutableList<Node> children, String word, float score) {
        this.grammemeValues = grammemeValues;
        this.children = children;
        this.word = word;
        this.score = score;
        // this.digest = calcDigest();
    }

    public boolean hasChildren() {
        return children != null;
    }

    public ImmutableList<Node> getChildren() {
        return children;
    }

    public int getChildrenSize() {
        return children == null ? 0 : children.size();
    }

    public String getWord() {
        return word;
    }

    public ByteBuffer getDigest() {
        return digest;
    }

    public boolean match(ImmutableSet<String> grammemeValues, String word) {
        if (grammemeValues != null) {
            if (!this.grammemeValues.containsAll(grammemeValues)) {
                return false;
            }
        }
        if (word != null) {
            if (!word.equals(this.word)) {
                return false;
            }
        }
        return true;
    }

    public static float calcScore(List<Node> nodes) {
        float score = 0.0f;
        for (Node n : nodes) {
            score += n.score;
        }
        return score;
    }

    // protected ByteBuffer calcDigest() {
    //     System.out.println(">>> calcDigest");
    //     System.out.println(this);
    //     MessageDigest md;
    //     try {
    //         md = MessageDigest.getInstance("SHA-1");
    //     } catch (NoSuchAlgorithmException e) {
    //         throw new RuntimeException(e.getMessage());
    //     }

    //     try {
    //         System.out.println(Joiner.on(",").join(grammemeValues));
    //         md.update(Joiner.on(",").join(grammemeValues).getBytes("UTF-8"));
    //     } catch (UnsupportedEncodingException e) {
    //         throw new RuntimeException(e.getMessage());
    //     }

    //     if (hasChildren()) {
    //         for (Node child : children) {
    //             System.out.println(HashCode.fromBytes(child.digest.array()));
    //             System.out.println(HashCode.fromBytes(child.calcDigest().array()));
    //             md.update(child.digest);
    //         }
    //     } else {
    //         try {
    //             System.out.println(word);
    //             md.update(word.getBytes("UTF-8"));
    //         } catch (UnsupportedEncodingException e) {
    //             throw new RuntimeException(e.getMessage());
    //         }
    //     }

    //     System.out.println(HashCode.fromBytes(md.digest()));
    //     System.out.println("<<< calcDigest");
    //     System.out.println("");
    //     return ByteBuffer.wrap(md.digest());
    // }

    @Override
    public int hashCode() {
        if (cachedHash != null) {
            return cachedHash;
        }
        int h = grammemeValues.hashCode();
        if (children != null) {
            h = h * 37 + children.hashCode();
        }
        if (word != null) {
            h = h * 37 + word.hashCode();
        }
        cachedHash = h;
        return cachedHash;
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }

        Node other = (Node) obj;
        if (word == null && other.word != null ||
            other.word == null && word != null) {
            return false;
        }
        if (children == null && other.children != null ||
            other.children == null && children != null) {
            return false;
        }
        return grammemeValues.equals(other.grammemeValues)
            && (children == other.children || children.equals(other.children))
            && (word == other.word || word.equals(other.word));
    }

    @Override
    public String toString() {
        return String.format("(%s %s)",
                             Joiner.on(",").join(grammemeValues),
                             hasChildren() ? Joiner.on(" ").join(children) : word);
    }

    public static class Top extends Node {
        public Top(ImmutableList<Node> children, float score) {
            super(ImmutableSet.of("TOP"), children, score);
        }

        @Override
        public String toString() {
            return String.format("%s [%s]", super.toString(), score);
        }
    };
}
