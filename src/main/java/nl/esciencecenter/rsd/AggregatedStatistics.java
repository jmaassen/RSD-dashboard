package nl.esciencecenter.rsd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class AggregatedStatistics {

    private HashMap<String, HashMap<String, Integer>> entries = new HashMap<>();

    public void add(String name, String label, int value) {

        HashMap<String, Integer> tmp = entries.get(name);

        if (tmp == null) {
            tmp = new HashMap<>();
            entries.put(name, tmp);
        }

        if (!tmp.containsKey(label)) {
            tmp.put(label, value);
        } else {
            int oldValue = tmp.get(label);
            tmp.put(label, oldValue + value);
        }
    }

    public void inc(String name, String label) {
        add(name, label, 1);
    }

    private ArrayList<Entry<String, Integer>> sort(HashMap<String, Integer> map) {
        ArrayList<Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());
        return list;
    }

    public ArrayList<Entry<String, Integer>> getSorted(String name) {

        HashMap<String, Integer> tmp = entries.get(name);

        if (tmp == null) {
            return null;
        }

        // We sort the entries of the hashmap before printing.
        return sort(tmp);
    }

    public String convertToCSV() {

        StringBuilder b = new StringBuilder("name, label, value\n");

        for (String name : entries.keySet()) {
            // We sort the entries of the hashmap before printing.
            ArrayList<Entry<String, Integer>> list = getSorted(name);

            for (int i = list.size() - 1; i >= 0; i--) {
                Entry<String, Integer> e = list.get(i);
                String label = e.getKey();
                int value = e.getValue();
                b.append(name).append(", ").append(label).append(", ").append(value).append("\n");
            }
        }

        return b.toString();
    }

    public int countEntries(String name) {

        HashMap<String, Integer> tmp = entries.get(name);

        if (tmp == null) {
            return 0;
        }

        return tmp.size();
    }
}
