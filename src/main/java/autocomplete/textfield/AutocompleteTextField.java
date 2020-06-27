package autocomplete.textfield;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * This class reprensents an autocomplete textfield.
 *
 * @author Gabriel Fortin
 */
public class AutocompleteTextField<T> extends TextField {

    private final List<T> items;
    private final ContextMenu contextMenu;
    private HashMap<T, String> reps;
    private HashMap<T, Integer> points;
    private WeakHashMap<MenuItem, T> menuItems;
    private ExecutorService executorService;

    private final int nbMaxProp, maxDistCompare;
    private final boolean remRep;
    private final AtomicBoolean isEmpty;

    /**
     * Constructs an instance of AutocompleteTextField. The analyzing task will be made in the JavaFX Application
     * Thread. The maximum of propositions the context menu can show is 5, or less if the list of items passed by
     * argument is smaller than 5. Any representation of items is not saved in memory.
     *
     * @param items             The list of items.
     */
    public AutocompleteTextField(List<T> items) {
        this(items, 5, Integer.MAX_VALUE, false);
    }

    /**
     * Constructs an instance of AutocompleteTextField. The analyzing task will be made in the JavaFX Application
     * Thread.
     *
     * @param items             The list of items.
     * @param nbMaxProp         The maximum of propositions the context menu can show.
     * @param maxDistCompare    The maximum distance to keep an item for suggesting it to the user.
     * @param remRep            True if the representation of every item is keep in memory. The representation(T) method
     *                          will be called only once by item. False, otherwise.
     */
    public AutocompleteTextField(List<T> items, int nbMaxProp, int maxDistCompare, boolean remRep) {
        this(items, nbMaxProp, maxDistCompare, remRep, false);
    }

    /**
     * Constructs an instance of AutocompleteTextField with all necessary arguments.
     *
     * @param items             The list of items.
     * @param nbMaxProp         The maximum of propositions the context menu can show.
     * @param maxDistCompare    The maximum distance to keep an item for suggesting it to the user.
     * @param remRep            True if the representation of every item is keep in memory. The representation(T) method
     *                          will be called only once by item. False, otherwise.
     * @param isMultithreading  True if analyzing of user input is done on a different thread than the JavaFX
     *                          Application Thread. False, otherwise. The analyzing thread shutdown when nothing is
     *                          writen in the autocompletion textfield or when the user pressed enter (an ActionEvent is
     *                          thown).
     */
    public AutocompleteTextField(List<T> items, int nbMaxProp, int maxDistCompare,
                                 boolean remRep, boolean isMultithreading) {
        super();
        this.items = new ArrayList<>(items);
        this.nbMaxProp = nbMaxProp;
        this.maxDistCompare = maxDistCompare;
        this.remRep = remRep;
        this.contextMenu = new ContextMenu();
        this.points = new HashMap<>();
        this.menuItems = new WeakHashMap<>();
        this.isEmpty = new AtomicBoolean(true);

        if(isMultithreading)
            executorService = Executors.newSingleThreadExecutor();

        if(remRep) {
            this.reps = new HashMap<>();
            this.items.forEach(item -> reps.put(item, reprentation(item)));
        }

        //The context menu shows itself when it contains at least one element.
        contextMenu.getItems().addListener((ListChangeListener<MenuItem>) c -> {
            if(contextMenu.getItems().size() == 0)
                Platform.runLater(() -> contextMenu.hide());
            else
                Platform.runLater(() -> contextMenu.show(AutocompleteTextField.this, Side.BOTTOM, 0, 0));
        });

        contextMenu.setOnAction((event -> {
            Platform.runLater(() -> setText(((MenuItem)event.getTarget()).getText()));
        }));

        addEventFilter(ActionEvent.ACTION, event -> {
            while(isMultithreading && !executorService.isShutdown())
                executorService.shutdown();
        });

        /*The user can uses the arrow buttons to navigate in the context menu, type what he is looking for but when he
        types the enter button AutocompleteTextField throw a ActionEvent.*/
        addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            switch(event.getCode()) {
                case LEFT:
                case RIGHT:
                case UP:
                case DOWN:
                case ENTER:
                    return;

                default:
                    if(getText().length() == 0) {
                        isEmpty.set(true);
                        while(isMultithreading && !executorService.isShutdown())
                            executorService.shutdown();
                        Platform.runLater(() -> contextMenu.getItems().clear());
                    } else {
                        isEmpty.set(false);
                        if(isMultithreading) {
                            if (executorService.isShutdown())
                                executorService = Executors.newSingleThreadExecutor();
                            executorService.submit(() -> analyze(getText()));
                        } else
                            analyze(getText());
                    }
            }
        });
    }

    /**
     * Return the item that his representation is the same then the text in the textfield. If any item is found, null is
     * returned.
     *
     * @return  The item writen in the textfield, null if there is no match with any item representation.
     */
    public T getSelectedItems() {
        return items.stream().filter(item -> {
            String rep = remRep ? reps.get(item) : reprentation(item);
            return getText().equalsIgnoreCase(rep);
        }).findAny().orElse(null);
    }

    /**
     * Analyzes the input passed by argument and add propositions to the context menu.
     *
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
                sorted(Map.Entry.comparingByValue()).
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
     * method. You could also set to true the remRep (REMember REPresentation) flag to keep in mind the representation
     * of each item. The representation method will be called when the instance is building.
     *
     * @param item  The item to represent.
     * @return      A string that represent the item.
     */
    private String reprentation(T item) {
        return item.toString();
    }

    /**
     * Returns the diffrence between two strings. This method is used to sort and to identify good suggestions for the
     * widget user.
     * By default, the Damerau-Levenshtein distance is used.
     * You can change that behavior by extending the actual class and overriding this method.
     *
     * @param str1  The first string to compare.
     * @param str2  The second string to compare.
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

            }

            prevLine2 = prevLine;
            prevLine = line;
        }

        return line[str1.length()];
    }
}
