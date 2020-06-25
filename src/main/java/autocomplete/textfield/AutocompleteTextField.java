package autocomplete.textfield;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * This class reprensents an autocomplete textfield.
 *
 * @author Gabriel Fortin
 */
public class AutocompleteTextField<T> extends TextField {

    private List<T> items;
    private ContextMenu contextMenu;
    private int nbMaxProp, maxDistCompare;

    public AutocompleteTextField(List<T> items, int nbMaxProp, int maxDistCompare, boolean isMultithreading) {

    }

    /**
     * Return the representation of the item in a string that will be used for the context menu.
     * By default, the toString() method is used to represents the object. You can change that behavior by extending the
     * actuel class and and overriding this method, or override the toString() method of the item class.
     * This method can be called several times in a time unit. You should keep this in mind when you may implement
     * method.
     *
     * @param item  The item to represent.
     * @return      A string that represent the item.
     */
    private String reprentation(T item) {
        return item.toString();
    }

    /**
     * Return the diffrence between the representation of two items passed by argument.
     * By default, the Damerau-Levenshtein distance is used to sort and identify good suggestions for the widget user.
     * You can change that behavior by extending the actual class and overriding this method.
     *
     * @param item1 The first item to compare.
     * @param item2 The second item to compare.
     * @return      The difference between two items passed by argument.
     */
    private int compareBetween(T item1, T item2) {
        //TODO: Implementation of compareBetween(T, T)
        return 0;
    }

    private class Analyser extends Thread {

        @Override
        public void run() {
            //TODO: Implementation of Analyser.run().
        }
    }
}
