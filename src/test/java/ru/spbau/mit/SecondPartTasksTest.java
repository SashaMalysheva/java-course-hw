package ru.spbau.mit;

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

    private static final String NOT_RES = "not res\n";
    private static final String FILE = "file";

    @Test
    public void testFindQuotes() throws IOException{
        List<String> fileName = new ArrayList<>(10);
        List<String> res = new ArrayList<>();
        String name = "name";
        for (int i = 0; i < 9; i++) {
            name += "abc";
            PrintWriter pw = new PrintWriter(new File(name));
            pw.print("aa" + name + "\n");
            pw.print("bb" + name + "\n");
            pw.print("cc" + name + "\n");
            fileName.add(name);
            if (i > 0) {
                res.add("bb" + name);
            }
            pw.close();
        }

        List<String> out = findQuotes(fileName, "bnameabcabc");

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
    public void testCalculateGlobalOrder() {
        List<Map<String, Integer>> data = new ArrayList<Map<String, Integer>>();

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

        Map<String, Integer> result = calculateGlobalOrder(data);
        assertEquals(1, (int)result.get("A"));
        assertEquals(17, (int)result.get("B"));
        assertEquals(3, (int)result.get("C"));
        assertEquals(8, (int)result.get("D"));
        assertEquals(4, result.entrySet().size());
    }
}