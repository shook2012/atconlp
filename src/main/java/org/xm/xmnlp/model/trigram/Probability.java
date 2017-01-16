package org.xm.xmnlp.model.trigram;

import java.io.DataOutputStream;
import java.util.Collection;
import java.util.Set;

import org.xm.xmnlp.collection.trie.bintrie.BaseNode;
import org.xm.xmnlp.collection.trie.bintrie.BinTrie;
import org.xm.xmnlp.collection.trie.bintrie.ValueArray;
import org.xm.xmnlp.corpus.io.ByteArray;
import org.xm.xmnlp.corpus.io.ICacheAble;

/**
 * 概率统计工具
 * <p/>
 * Created by mingzai on 2016/7/30.
 */
public class Probability implements ICacheAble {
    public BinTrie<Integer> d;
    int total;

    public Probability() {
        d = new BinTrie<Integer>() {
            @Override
            public boolean load(ByteArray byteArray, ValueArray valueArray) {
                BaseNode<Integer>[] nchild = new BaseNode[child.length - 1];    // 兼容旧模型
                System.arraycopy(child, 0, nchild, 0, nchild.length);
                child = nchild;
                return super.load(byteArray, valueArray);
            }
        };
    }

    public boolean exists(String key) {
        return d.containsKey(key);
    }

    public int getsum() {
        return total;
    }

    Integer get(String key) {
        return d.get(key);
    }

    public int get(char[]... keyArray) {
        Integer f = get(convert(keyArray));
        if (f == null) return 0;
        return f;
    }

    public int get(char... key) {
        Integer f = d.get(key);
        if (f == null) return 0;
        return f;
    }

    public double freq(String key) {
        Integer f = get(key);
        if (f == null) f = 0;
        return f / (double) total;
    }

    public double freq(char[]... keyArray) {
        return freq(convert(keyArray));
    }

    public double freq(char... keyArray) {
        Integer f = d.get(keyArray);
        if (f == null) f = 0;
        return f / (double) total;
    }

    public Set<String> samples() {
        return d.keySet();
    }

    void add(String key, int value) {
        Integer f = get(key);
        if (f == null) f = 0;
        f += value;
        d.put(key, f);
        total += value;
    }

    void add(int value, char... key) {
        Integer f = d.get(key);
        if (f == null) f = 0;
        f += value;
        d.put(key, f);
        total += value;
    }

    public void add(int value, char[]... keyArray) {
        add(convert(keyArray), value);
    }

    public void add(int value, Collection<char[]> keyArray) {
        add(convert(keyArray), value);
    }

    private String convert(Collection<char[]> keyArray) {
        StringBuilder sbKey = new StringBuilder(keyArray.size() * 2);
        for (char[] key : keyArray) {
            sbKey.append(key[0]);
            sbKey.append(key[1]);
        }
        return sbKey.toString();
    }

    static private String convert(char[]... keyArray) {
        StringBuilder sbKey = new StringBuilder(keyArray.length * 2);
        for (char[] key : keyArray) {
            sbKey.append(key[0]);
            sbKey.append(key[1]);
        }
        return sbKey.toString();
    }

    @Override
    public void save(DataOutputStream out) throws Exception {
        out.writeInt(total);
        Integer[] valueArray = d.getValueArray(new Integer[0]);
        out.writeInt(valueArray.length);
        for (Integer v : valueArray) {
            out.writeInt(v);
        }
        d.save(out);
    }

    @Override
    public boolean load(ByteArray byteArray) {
        total = byteArray.nextInt();
        int size = byteArray.nextInt();
        Integer[] valueArray = new Integer[size];
        for (int i = 0; i < valueArray.length; ++i) {
            valueArray[i] = byteArray.nextInt();
        }
        d.load(byteArray, valueArray);
        return true;
    }
}