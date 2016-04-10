package ru.spbau.mit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SecondPartTasks {

    private static final double R = 0.5;
    private static final int NUM_OF_REP = 10000000;
    private static final Random RND = new Random();

    private SecondPartTasks() {}

    // Найти строки из переданных файлов, в которых встречается указанная подстрока.
    public static List<String> findQuotes(List<String> paths, CharSequence sequence) {
        return paths
                .stream()
                .flatMap(p -> {
                    try {
                        return Files.lines(Paths.get(p));
                    } catch (IOException ignored) {
                    }
                    return null;
                })
                .filter(s -> s.contains(sequence))
                .collect(Collectors.toList());
    }

    // В квадрат с длиной стороны 1 вписана мишень.
    // Стрелок атакует мишень и каждый раз попадает в произвольную точку квадрата.
    // Надо промоделировать этот процесс с помощью класса java.util.Random и посчитать, какова вероятность попасть в мишень.
    public static double piDividedBy4() {
        return Stream
                .generate(() -> Math.pow(RND.nextDouble() - R, 2) + Math.pow(RND.nextDouble() - R, 2))
                .limit(NUM_OF_REP)
                .filter(x -> x <= Math.pow(R, 2))
                .count() / ((double) NUM_OF_REP);
    }

    private static final class AuthorInfo {
        public final int count;
        public final String name;

        public AuthorInfo(String name, int count) {
            this.count = count;
            this.name = name;
        }
    }

    // Дано отображение из имени автора в список с содержанием его произведений.
    // Надо вычислить, чья общая длина произведений наибольшая.
    public static String findPrinter(Map<String, List<String>> compositions) {
        return compositions
                .entrySet()
                .stream()
                .map((entry) -> new AuthorInfo(entry.getKey(), entry
                        .getValue()
                        .stream()
                        .mapToInt(String::length)
                        .sum()
                ))
                .max(Comparator.comparingInt(a -> a.count))
                .get().name;
    }

    // Вы крупный поставщик продуктов. Каждая торговая сеть делает вам заказ в виде Map<Товар, Количество>.
    // Необходимо вычислить, какой товар и в каком количестве надо поставить.
    public static Map<String, Integer> calculateGlobalOrder(List<Map<String, Integer>> orders) {
        return orders
                .stream()
                .flatMap(map -> map
                        .entrySet()
                        .stream())
                .collect(Collectors
                        .toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum));
    }
}
