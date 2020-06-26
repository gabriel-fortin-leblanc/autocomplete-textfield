package autocomplete.textfield;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * This class reprensents an autocomplete textfield.
 *
 * @author Gabriel Fortin
 */
public class AutocompleteTextField<T> extends TextField {

    private List<T> items;
    private ContextMenu contextMenu;
    private HashMap<T, String> reps;
    private HashMap<T, Integer> points;
    private WeakHashMap<MenuItem, T> menuItems;

    private int nbMaxProp, maxDistCompare;
    private boolean remRep, isMultithreading;
    private AtomicBoolean isNotEmpty;

    public AutocompleteTextField(List<T> items, int nbMaxProp, int maxDistCompare, boolean remRep, boolean isMultithreading) {
        this.items = new ArrayList<>(items);
        this.nbMaxProp = nbMaxProp;
        this.maxDistCompare = maxDistCompare;
        this.remRep = remRep;
        this.isMultithreading = isMultithreading;
        this.contextMenu = new ContextMenu();
        this.points = new HashMap<>();
        this.menuItems = new WeakHashMap<>();
        this.isNotEmpty = new AtomicBoolean();

        if(remRep)
            this.reps = new HashMap<>();
    }

    /**
     * Analyzes the input passed by argument and add propositions to the context menu.
     * @param input The input from the widget user.
     */
    private void analyze(String input) {
        //Calculate and store the distance from compareBetween(String, String) in a dict.
        points.clear();
        items.forEach(item -> {
            String rep = remRep ? reps.get(item) : reprentation(item);
            points.put(item, compareBetween(rep, input));
        });

        /*Filter, sort, limit the number of element in the stream, collect the items left and set it in the context
        menu.*/
        Collection<MenuItem> props = points.entrySet().stream().
                filter(entry -> entry.getValue() <= maxDistCompare).
                sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue())).
                limit(nbMaxProp).
                map(entry -> {
                    T item = entry.getKey();
                    String rep = remRep ? reps.get(item) : reprentation(item);
                    MenuItem menuItem = new MenuItem(rep);
                    menuItems.put(menuItem, item);
                    return menuItem;
                }).
                collect(Collectors.toCollection(ArrayList::new));
        Platform.runLater(() -> contextMenu.getItems().setAll(props));
    }

    /**
     * Returns the representation of the item in a string that will be used for the context menu.
     * By default, the toString() method is used to represents the object. You can change that behavior by extending the
     * actuel class and and overriding this method, or override the toString() method of the item class.
     * This method can be called several times in a time unit. You should keep this in mind when you may implement
     * method. You could also set to true the remRep (remember representation) flag to keep in mind the representation
     * of each item. The representation method will be called when the instance is building.
     *
     * @param item  The item to represent.
     * @return      A string that represent the item.
     */
    private String reprentation(T item) {
        return item.toString();
    }

    /**
     * Returns the diffrence between two strings. This method is used to sort and identify good suggestions for the
     * widget user.
     * By default, the Damerau-Levenshtein distance is used.
     * You can change that behavior by extending the actual class and overriding this method.
     *
     * @param str1  The first item to compare.
     * @param str2  The second item to compare.
     * @return      The difference between two items passed by argument.
     */
    private int compareBetween(String str1, String str2) {

        if(str1.length() == 0)
            return str2.length();
        if(str2.length() == 0)
            return str1.length();

        int cost;
        int[]   prevLine2 = new int[str1.length() + 1],
                prevLine = new int[str1.length() + 1],
                line = new int[str1.length() + 1];
        for(int i = 0; i < prevLine.length; i++)
            prevLine[i] = i;

        for(int j = 1; j < str2.length() + 1; j++) {

            line = new int[str1.length() + 1];
            line[0] = j;
            for(int i = 1; i < str1.length() + 1; i++) {

                cost = str1.charAt(i - 1) != str2.charAt(j - 1) ? 1 : 0;
                line[i] = Math.min(
                        Math.min(line[i - 1] + 1, prevLine[i] + 1),
                        prevLine[i - 1] + cost);

                if(i > 1 && j > 1 &&
                        str1.length() > i &&
                        str2.length() > j &&
                        str1.charAt(i) == str2.charAt(j - 1) &&
                        str1.charAt(i - 1) == str2.charAt(j)) {
                    line[i] = Math.min(line[i], prevLine2[i - 2] + cost);
                }

                prevLine2 = prevLine;
                prevLine = line;

            }
        }

        return line[str1.length()];
    }

    private class Analyser extends Thread {

        @Override
        public void run() {
            //TODO: Implementation of Analyser.run().
        }
    }
}
