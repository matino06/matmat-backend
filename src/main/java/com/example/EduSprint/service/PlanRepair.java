package com.example.EduSprint.service;

import java.util.*;

// Popravak plana kodom, nakon što je model grupirao stavke u lekcije (iz prototipa, step_repair):
// 1. premala lekcija (manje od MIN_ITEMS stavki) spaja se sa susjednom u istom ishodu, ako zajedno ne prelaze MAX_ITEMS;
// 2. preduvjeti lekcija izvode se iz preduvjeta njihovih stavki;
// 3. lekcije se topološki slažu, stabilno (što je moguće bliže redoslijedu ishoda i modela).
// Krug preduvjeta (A treba B, B treba A) ostaje i prijavljuje se: kreator ga vidi kao upozorenje o redoslijedu.
public final class PlanRepair {

    public static final int MIN_ITEMS = 4;
    public static final int MAX_ITEMS = 6;
    public static final int MAX_LESSONS = 200;

    private PlanRepair() {
    }

    public record Lesson(long outcomeId, int outcomePosition, int position, String name, String concept, List<Long> items) {
    }

    // Lekcije redom učenja; preduvjeti i krugovi su indeksi u tom popisu.
    public record Result(List<Lesson> lessons, List<List<Integer>> prerequisites, List<int[]> backwards) {
    }

    public static Result repair(List<Lesson> drafts, Map<Long, Set<Long>> itemPrerequisites) {
        List<Lesson> lessons = mergeSmall(drafts);
        int n = lessons.size();
        Map<Long, Integer> owner = new HashMap<>();
        for (int i = 0; i < n; i++) for (Long item : lessons.get(i).items()) owner.putIfAbsent(item, i);

        List<Set<Integer>> pre = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Set<Integer> p = new TreeSet<>();
            for (Long item : lessons.get(i).items()) {
                for (Long q : itemPrerequisites.getOrDefault(item, Set.of())) {
                    Integer o = owner.get(q);
                    if (o != null && o != i) p.add(o);
                }
            }
            pre.add(p);
        }

        Order order = topoOrder(pre);
        List<Lesson> ordered = new ArrayList<>();
        for (int old : order.order()) ordered.add(lessons.get(old));
        return new Result(ordered, order.prerequisites(), order.backwards());
    }

    // Poredak: order = stari indeksi novim redom; preduvjeti i krugovi su novi indeksi.
    public record Order(List<Integer> order, List<List<Integer>> prerequisites, List<int[]> backwards) {
    }

    // Stabilni topološki poredak: posjeti lekcije redom, a svakoj prvo njezine preduvjete.
    // Lekcija u krugu ostaje gdje je; brid koji tada pokazuje unaprijed vraća se kao „backwards“.
    public static Order topoOrder(List<Set<Integer>> pre) {
        int n = pre.size();
        List<Integer> order = new ArrayList<>();
        boolean[] done = new boolean[n];
        boolean[] onStack = new boolean[n];
        for (int i = 0; i < n; i++) visit(i, pre, done, onStack, order);
        int[] newIndex = new int[n];
        for (int k = 0; k < n; k++) newIndex[order.get(k)] = k;
        List<List<Integer>> prerequisites = new ArrayList<>();
        List<int[]> backwards = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            List<Integer> p = new ArrayList<>();
            for (int q : pre.get(order.get(k))) {
                p.add(newIndex[q]);
                if (newIndex[q] > k) backwards.add(new int[]{k, newIndex[q]});
            }
            Collections.sort(p);
            prerequisites.add(p);
        }
        return new Order(order, prerequisites, backwards);
    }

    // Dodaje li brid „lesson treba prerequisite“ krug: je li lesson (tranzitivno) preduvjet od prerequisite?
    public static boolean createsCycle(List<Set<Integer>> pre, int lesson, int prerequisite) {
        Deque<Integer> stack = new ArrayDeque<>(List.of(prerequisite));
        Set<Integer> seen = new HashSet<>();
        while (!stack.isEmpty()) {
            int x = stack.pop();
            if (x == lesson) return true;
            if (seen.add(x)) stack.addAll(pre.get(x));
        }
        return false;
    }

    private static void visit(int i, List<Set<Integer>> pre, boolean[] done, boolean[] onStack, List<Integer> order) {
        if (done[i] || onStack[i]) return; // već posložena, ili krug: ostaje na svom mjestu
        onStack[i] = true;
        for (int p : pre.get(i)) visit(p, pre, done, onStack, order);
        onStack[i] = false;
        done[i] = true;
        order.add(i);
    }

    static List<Lesson> mergeSmall(List<Lesson> drafts) {
        List<Lesson> sorted = new ArrayList<>(drafts);
        sorted.sort(Comparator.comparingInt(Lesson::outcomePosition).thenComparingInt(Lesson::position));
        Map<Long, List<Lesson>> byOutcome = new LinkedHashMap<>();
        for (Lesson l : sorted) byOutcome.computeIfAbsent(l.outcomeId(), k -> new ArrayList<>()).add(l);

        List<Lesson> out = new ArrayList<>();
        for (List<Lesson> ls : byOutcome.values()) {
            boolean changed = true;
            while (changed && ls.size() > 1) {
                changed = false;
                for (int i = 0; i < ls.size(); i++) {
                    if (ls.get(i).items().size() >= MIN_ITEMS) continue;
                    int best = -1;
                    for (int j : new int[]{i + 1, i - 1}) {
                        if (j < 0 || j >= ls.size()) continue;
                        if (ls.get(j).items().size() + ls.get(i).items().size() > MAX_ITEMS) continue;
                        if (best < 0 || ls.get(j).items().size() < ls.get(best).items().size()) best = j;
                    }
                    if (best < 0) continue;
                    int a = Math.min(i, best), b = Math.max(i, best);
                    Lesson x = ls.get(a), y = ls.get(b);
                    List<Long> items = new ArrayList<>(x.items());
                    items.addAll(y.items());
                    ls.set(a, new Lesson(x.outcomeId(), x.outcomePosition(), x.position(), x.name() + " + " + y.name(),
                            (x.concept() + " " + y.concept()).strip(), items));
                    ls.remove(b);
                    changed = true;
                    break;
                }
            }
            out.addAll(ls);
        }
        return out;
    }
}
