package ru.spbau.mit;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.Files.createFile;
import static org.junit.Assert.*;
import static ru.spbau.mit.SecondPartTasks.*;

public class SecondPartTasksTest {


    @Test
    public void testFindQuotes() throws IOException {
        List<String> fileNames = new ArrayList<>(10);
        List<String> res = new ArrayList<>();
        String name = "name";
        for (int i = 0; i < 9; i++) {
            name += "abc";
            File f = new File(name);
            f.deleteOnExit();
            PrintWriter pw = new PrintWriter(f);
            pw.println("aa" + name);
            pw.println("bb" + name);
            pw.println("cc" + name);
            fileNames.add(name);
            if (i > 0) {
                res.add("bb" + name);
            }
            pw.close();
        }

        List<String> out = findQuotes(fileNames, "bnameabcabc");

        assertEquals(res, out);
    }

    @Test
    public void testFindQuotesInvalidPaths() throws IOException {
        String validPath = "valid";
        File valid = new File(validPath);
        valid.deleteOnExit();
        String invalidPath = "invalid";
        List<String> fileNames = new ArrayList<String>(){{
            add(validPath);
            add(invalidPath);
        }};
        PrintWriter pw = new PrintWriter(valid);
        pw.println("abc");
        pw.close();

        List<String> res = Collections.singletonList("abc");
        List<String> out = findQuotes(fileNames, "b");

        assertEquals(res, out);
    }

    @Test
    public void testPiDividedBy4() {
        double d = piDividedBy4();
        assertEquals(Double.toString(d), Math.PI / 4, d, 1e-03);
    }

    @Test
    public void testFindPrinter() {
        Map<String, List<String>> data = new HashMap<>();
        data.put("First", Arrays.asList("a", "aa", "aaa", "aaa"));
        data.put("Second", Arrays.asList("a", "aa", "aaa"));
        data.put("Winner", Arrays.asList("aaaa", "aaaa", "aaaa"));
        assertEquals("Winner", findPrinter(data));
    }


    @Test
    public void testCalculateGlobalOrderEmpty() {
        Map<String, Integer> result = calculateGlobalOrder(Collections.emptyList());
        assertEquals(Collections.emptyMap(), result);
    }

    @Test
    public void testCalculateGlobalOrder() {
        List<Map<String, Integer>> data = new ArrayList<>();

        data.add(new HashMap<String, Integer>(){{
            put("A", 1);
            put("B", 7);
        }});

        data.add(new HashMap<String, Integer>(){{
            put("D", 6);
            put("C", 3);
        }});

        data.add(new HashMap<String, Integer>() {{
            put("D", 2);
            put("B", 10);
        }});

        Map<String, Integer> expected = new HashMap<String, Integer>(){{
            put("A", 1);
            put("B", 17);
            put("C", 3);
            put("D", 8);
        }};

        Map<String, Integer> result = calculateGlobalOrder(data);
        assertEquals(expected, result);
    }
}