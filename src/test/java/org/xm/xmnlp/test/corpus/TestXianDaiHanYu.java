package org.xm.xmnlp.test.corpus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xm.xmnlp.Xmnlp;
import org.xm.xmnlp.corpus.dictionary.SimpleDictionary;
import org.xm.xmnlp.corpus.dictionary.StringDictionary;
import org.xm.xmnlp.corpus.dictionary.item.Item;
import org.xm.xmnlp.corpus.dictionary.maker.DictionaryMaker;
import org.xm.xmnlp.corpus.tag.Nature;
import org.xm.xmnlp.dictionary.CoreDictionary;
import org.xm.xmnlp.dictionary.CustomDictionary;
import org.xm.xmnlp.dictionary.pinyin.Pinyin;
import org.xm.xmnlp.dictionary.pinyin.PinyinDictionary;
import org.xm.xmnlp.dictionary.pinyin.TonePinyinString2PinyinConverter;
import org.xm.xmnlp.dictionary.traditionalsimplified.TraditionalChineseDictionary;
import org.xm.xmnlp.seg.NShortSegment;
import org.xm.xmnlp.seg.Segment;
import org.xm.xmnlp.util.IOUtil;
import org.xm.xmnlp.util.TextUtil;

import junit.framework.TestCase;

public class TestXianDaiHanYu extends TestCase {
    public void testMakeDictionary() throws Exception {
        String text = IOUtil.readTxt("D:\\Doc\\语料库\\现代汉语词典（第五版）全文_更新.txt").toLowerCase();
        Pattern pattern = Pattern.compile("【([\\u4E00-\\u9FA5]+)】([abcdefghijklmnopqrstuwxyzāáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜ∥•’－]+)");
        Matcher matcher = pattern.matcher(text);
        StringDictionary dictionary = new StringDictionary();
        while (matcher.find()) {
            String word = matcher.group(1);
            String pinyinString = matcher.group(2);
            List<Pinyin> pinyinList = TonePinyinString2PinyinConverter.convert(pinyinString, false);
            if (pinyinList.size() != word.length() || hasNull(pinyinList)) {
                System.out.println("转换失败 " + word + " " + pinyinString + " " + pinyinList);
                continue;
            }
            dictionary.add(word, convertList2String(pinyinList));
        }
        System.out.println(dictionary.size());
        dictionary.save("data/dictionary/pinyin/pinyin.xd.txt");
    }

    public void testMakePyDictionary() throws Exception {
        StringDictionary dictionaryRaw = new StringDictionary();
        dictionaryRaw.load("D:\\PythonProjects\\python-pinyin\\dic.txt");

        StringDictionary dictionary = new StringDictionary();
        for (Map.Entry<String, String> entry : dictionaryRaw.entrySet()) {
            String word = entry.getKey();
            String[] pinyinArray = entry.getValue().split(",");
            List<Pinyin> pinyinList = TonePinyinString2PinyinConverter.convert(pinyinArray);
            if (word.length() != pinyinList.size() || hasNull(pinyinList)) {
                System.out.println(entry + " | " + pinyinList);
            } else {
                dictionary.add(entry.getKey(), convertList2String(pinyinList));
            }
        }

        dictionary.save("data/dictionary/pinyin/pinyin.python.txt");
    }

    public void testCombinePy() throws Exception {
        StringDictionary dictionary = new StringDictionary();
        dictionary.load("data/dictionary/pinyin/pinyin.python.txt");
        dictionary.remove(new SimpleDictionary.Filter<String>() {
            @Override
            public boolean remove(Map.Entry<String, String> entry) {
                String key = entry.getKey();
                String[] pinyinArray = entry.getValue().split(",");
                List<Pinyin> pinyinList = TonePinyinString2PinyinConverter.convertFromToneNumber(pinyinArray);
                // 检查是否实用
                List<Pinyin> localPinyinList = PinyinDictionary.convertToPinyin(key);
                if (!isEqual(pinyinList, localPinyinList)) {
                    System.out.println("接受 " + key + "=" + pinyinList + "!=" + localPinyinList);
                    return false;
                } else {
                    return true;
                }
            }
        });

        StringDictionary dictionaryLocal = new StringDictionary();
        dictionaryLocal.load(Xmnlp.Config.PinyinDictionaryPath);
        dictionaryLocal.combine(dictionary);
        dictionaryLocal.save(Xmnlp.Config.PinyinDictionaryPath);
    }

    public void testMakeKaiFangDictionary() throws Exception {
        // data/dictionary/tc/
        LinkedList<String> lineList = IOUtil.readLineList("D:\\Doc\\语料库\\cidian_zhzh-kfcd-2013122.txt");
        StringDictionary dictionaryKFTC = new StringDictionary();
        for (String line : lineList) {
            String[] args = line.split("\\s");
            // 愛面子	爱面子	ai4 mian4 zi5
            List<Pinyin> pinyinList = new ArrayList<Pinyin>(args.length - 2);
            for (int i = 2; i < args.length; ++i) {
                pinyinList.add(TonePinyinString2PinyinConverter.convertFromToneNumber(args[i]));
            }
            if (hasNull(pinyinList) || pinyinList.size() != args[1].length()) {
//                System.out.println("忽略 " + line + " " + pinyinList);
                continue;
            }
            // 检查是否实用
            List<Pinyin> localPinyinList = PinyinDictionary.convertToPinyin(args[1]);
            if (!isEqual(pinyinList, localPinyinList)) {
                System.out.println("接受 " + args[1] + "=" + pinyinList + "!=" + localPinyinList);
                dictionaryKFTC.add(args[1], convertList2String(pinyinList));
            }
        }

        StringDictionary dictionaryLocal = new StringDictionary();
        dictionaryLocal.load(Xmnlp.Config.PinyinDictionaryPath);
        dictionaryLocal.combine(dictionaryKFTC);
        dictionaryLocal.save(Xmnlp.Config.PinyinDictionaryPath);
    }

    public void testPinyin() throws Exception {
        System.out.println(PinyinDictionary.convertToPinyin("龟背"));

    }

    private boolean isEqual(List<Pinyin> pinyinListA, List<Pinyin> pinyinListB) {
        if (pinyinListA.size() != pinyinListB.size()) return false;

        Iterator<Pinyin> iteratorA = pinyinListA.iterator();
        Iterator<Pinyin> iteratorB = pinyinListB.iterator();
        while (iteratorA.hasNext()) {
            if (iteratorA.next() != iteratorB.next()) return false;
        }

        return true;
    }

    public void testT2C() throws Exception {
        System.out.println(TraditionalChineseDictionary.convertToSimplifiedChinese("熱線"));

    }

    public void testConvertSingle() throws Exception {
        System.out.println(TonePinyinString2PinyinConverter.convert("ai"));
    }

    private String convertList2String(List<Pinyin> pinyinList) {
        StringBuilder sb = new StringBuilder();
        for (Pinyin pinyin : pinyinList) {
            sb.append(pinyin);
            sb.append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private boolean hasNull(List<Pinyin> pinyinList) {
        for (Pinyin pinyin : pinyinList) {
            if (pinyin == null) return true;
        }

        return false;
    }

    public void testEnumChar() throws Exception {
        Set<Character> characterSet = new TreeSet<Character>();
        for (Pinyin pinyin : PinyinDictionary.pinyins) {
            for (char c : pinyin.getPinyinWithToneMark().toCharArray()) {
                characterSet.add(c);
            }
        }

        for (Character c : characterSet) {
            System.out.print(c);
        }
    }

    public void testToken() throws Exception {
        System.out.println(TonePinyinString2PinyinConverter.convert("āgōng", true));
    }

    public void testMakeNatureDictionary() throws Exception {
        String text = IOUtil.readTxt("D:\\Doc\\语料库\\现代汉语词典（第五版）全文_更新.txt").toLowerCase();
//        String text = "【岸标】ànbiāo名设在岸上指示航行的标志，可以使船舶避开沙滩、暗礁等。\n" +
//                "\n" +
//                "【岸炮】ànpào名海岸炮的简称。\n" +
//                "\n" +
//                "【岸然】ànrán〈书〉形严肃的样子：道貌～。\n" +
//                "\n" +
//                "【按】1àn①动用手或指头压：～电铃｜～图钉。②动压住；搁下：～兵不动｜～下此事不说。③动抑制：～不住心头怒火。④介依照：～时｜～质论价｜～制度办事｜～每人两本计算。\n" +
//                "另见237页cuō。\n" +
//                "现用替代字【錣】*  原图片字[钅+叕]\n" +
//                "现用替代字【騣】*  原图片字[马+㚇]\n" +
//                "现用替代字【緅】*  原图片字[纟+取]";
        Pattern pattern = Pattern.compile("【([\\u4E00-\\u9FA5]{2,10})】.{0,5}([abcdefghijklmnopqrstuwxyzāáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜ∥•’－]+)(.*)");
        Matcher matcher = pattern.matcher(text);
        DictionaryMaker dictionaryMaker = new DictionaryMaker();
        dictionaryMaker.add("希望 v 7685 vn 616");
        Map<String, String> mapChineseToNature = new TreeMap<String, String>();
        mapChineseToNature.put("名", Nature.n.toString());
        mapChineseToNature.put("动", Nature.v.toString());
        mapChineseToNature.put("形", Nature.a.toString());
        mapChineseToNature.put("副", Nature.d.toString());
        mapChineseToNature.put("形容", Nature.a.toString());
        while (matcher.find()) {
            String word = matcher.group(1);
            if (CoreDictionary.contains(word) || CustomDictionary.contains(word)) continue;
            String content = matcher.group(3);
            Item item = new Item(word);
            for (Map.Entry<String, String> entry : mapChineseToNature.entrySet()) {
                int frequency = TextUtil.count(entry.getKey(), content);
                if (frequency > 0) item.addLabel(entry.getValue(), frequency);
            }
            if (item.getTotalFrequency() == 0) item.addLabel(Nature.nz.toString());
//            System.out.println(item);
            dictionaryMaker.add(item);
        }
        dictionaryMaker.saveTxtTo("data/dictionary/custom/现代汉语补充词库.txt");
    }

    public void testMakeCell() throws Exception {
        String root = "D:\\JavaProjects\\SougouDownload\\data\\";
        String[] pathArray = new String[]{"最详细的全国地名大全.txt"};
        Set<String> wordSet = new TreeSet<String>();
        for (String path : pathArray) {
            path = root + path;
            for (String word : IOUtil.readLineList(path)) {
                word = word.replaceAll("\\s", "");
                if (!TextUtil.isAllChinese(word)) continue;
                if (CoreDictionary.contains(word) || CustomDictionary.contains(word)) continue;
                wordSet.add(word);
            }
        }
        IOUtil.saveCollectionToTxt(wordSet, "data/dictionary/custom/全国地名大全.txt");
    }

    public void testMakeShanghaiCell() throws Exception {
        String root = "D:\\JavaProjects\\SougouDownload\\data\\";
        String[] pathArray = new String[]{"上海地名街道名.txt", "上海公交线路名", "上海公交站点.txt", "上海市道路名.txt", "上海市地铁站名.txt"};
        Set<String> wordSet = new TreeSet<String>();
        for (String path : pathArray) {
            path = root + path;
            for (String word : IOUtil.readLineList(path)) {
                word = word.replaceAll("\\s", "");
                if (CoreDictionary.contains(word) || CustomDictionary.contains(word)) continue;
                wordSet.add(word);
            }
        }
        IOUtil.saveCollectionToTxt(wordSet, "data/dictionary/custom/上海地名.txt");
    }

    public void testFixDiMing() throws Exception {
        Set<String> wordSet = new TreeSet<String>();
        for (String word : IOUtil.readLineList("data/dictionary/custom/全国地名大全.txt")) {
            if (!TextUtil.isAllChinese(word)) continue;
            wordSet.add(word);
        }
        IOUtil.saveCollectionToTxt(wordSet, "data/dictionary/custom/全国地名大全.txt");
    }

    public void testSeg() throws Exception {
        Segment segment = new NShortSegment().enableNameRecognize(true);
        Xmnlp.Config.enableDebug();
        System.out.println(segment.seg("我在区人保工作"));
    }

}
