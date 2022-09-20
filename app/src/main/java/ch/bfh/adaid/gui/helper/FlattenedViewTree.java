package ch.bfh.adaid.gui.helper;

import android.view.accessibility.AccessibilityNodeInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;

/**
 * FlattenedViewTree is a class that represents a tree of {@link AccessibilityNodeInfo}s. But it is
 * not a tree itself but a flat list of simplified nodes ({@link SimpleView}) that only contain
 * information about the view id and view text.
 *
 * @author Niklaus Leuenberger
 */
public class FlattenedViewTree implements Serializable {

    /**
     * Package id / app name for which this view tree was created.
     */
    public String packageName;

    /**
     * List of the simplified views.
     */
    public ArrayList<SimpleView> views;

    /**
     * Construct a view tree. This recursively traverses the tree and populates the list.
     *
     * @param root The root node of the tree.
     */
    public FlattenedViewTree(AccessibilityNodeInfo root) {
        packageName = root.getPackageName().toString();
        views = new ArrayList<>();
        flattenTree(root, 0);
        removeEmptyChildren();
    }

    /**
     * Serialize the flattened view tree to a base64 encoded string.
     *
     * @return Serialized base64 encoded string.
     * @throws IOException On output stream error.
     */
    public String toSerializedString() throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(this);
        objectStream.close();
        return Base64.getEncoder().encodeToString(byteStream.toByteArray());
    }

    /**
     * Deserialize the flattened view tree from a base64 encoded string.
     *
     * @param encodedBase64 Serialized base64 encoded string.
     * @return Deserialized FlattenedViewTree object.
     * @throws IOException            On input stream error.
     * @throws ClassNotFoundException On input stream error.
     */
    public static FlattenedViewTree fromSerializedString(String encodedBase64) throws IOException, ClassNotFoundException {
        byte[] byteArray = Base64.getDecoder().decode(encodedBase64);
        ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(byteArray));
        Object obj = objectStream.readObject();
        objectStream.close();
        if (obj instanceof FlattenedViewTree) {
            return (FlattenedViewTree) obj;
        } else {
            throw new IOException("Could not cast to FlattenedViewTree object.");
        }
    }

    /**
     * Recursively traverse the tree and populate the list of simplified views.
     *
     * @param node  The current node.
     * @param level The current level in the tree. (0 = root)
     */
    private void flattenTree(AccessibilityNodeInfo node, int level) {
        if (node == null) {
            return;
        }
        views.add(new SimpleView(node, level, packageName));
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            flattenTree(child, level + 1);
        }
    }

    /**
     * Remove children in the list that have no id and text and also have no children themselves.
     */
    private void removeEmptyChildren() {
        ArrayList<SimpleView> toRemove = new ArrayList<>();
        for (int i = views.size() - 1; i >= 0; i--) {
            SimpleView view = views.get(i);
            // If the view has no id and no text, try to remove it if it has no children.
            if (view.id.isEmpty() && view.text.isEmpty()) {
                if (i == views.size() - 1) {
                    // If it is the last one, it is always save to remove it.
                    toRemove.add(view);
                } else if (views.get(i + 1).level <= view.level) {
                    // If the next view is at the same level or lower, this one has no children.
                    toRemove.add(view);
                } else if (views.get(i + 1).level > view.level && toRemove.contains(views.get(i + 1))) {
                    // If the next view is a child but already marked to be removed.
                    toRemove.add(view);
                }
            }
        }
        views.removeAll(toRemove);
    }

    /**
     * Class to represent a simplified {@link AccessibilityNodeInfo} by only view id and view text.
     *
     * @author Niklaus Leuenberger
     */
    public static class SimpleView implements Serializable {
        public final String id;
        public final String text;
        public final int level;

        /**
         * Construct a simplified view from an {@link AccessibilityNodeInfo}.
         *
         * @param viewNode    The {@link AccessibilityNodeInfo} to simplify.
         * @param level       The level in the tree. (0 = root)
         * @param packageName The package id / app name for which this view was created.
         */
        private SimpleView(AccessibilityNodeInfo viewNode, int level, String packageName) {
            String longId = viewNode.getViewIdResourceName();
            if (longId == null) {
                id = "";
            } else {
                // Remove com.app.xyz:id/ from the id. Some ids don't start with the package name,
                // maybe those are views that get generated from some default android ui stuff. Set
                // them to empty as the a11y service can't handle them at the moment.
                if (longId.startsWith(packageName)) {
                    id = longId.substring(longId.lastIndexOf("/") + 1);
                } else {
                    id = "";
                }
            }
            text = viewNode.getText() == null ? "" : viewNode.getText().toString();
            this.level = level;
        }
    }
}
