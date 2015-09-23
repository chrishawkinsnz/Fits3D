package Model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by chrishawkins on 14/08/15.
 */
public class AttributeGrouping implements  AttributeProvider {

    private String name;
    private List<AttributeEntry>attributeEntries;
    private Comparator<AttributeEntry>comparator;

    public AttributeGrouping(String name) {
        this.name = name;
        this.attributeEntries = new ArrayList<>();
        this.comparator = new AttributeEntry.AttributeEntryComparator();
    }
    @Override
    public List<Attribute> getAttributes() {
        Collections.sort(attributeEntries, comparator);
        List<Attribute>attrs = new ArrayList<>();
        for (AttributeEntry ae : this.attributeEntries) {
            attrs.add(ae.attribute);
        }
        return attrs;
    }

    @Override
    public List<AttributeProvider> getChildProviders() {
        return null;
    }

    public String getName() {
        return name;
    }

    public void addAttribute(Attribute attribute, int priority) {
        AttributeEntry ae = new AttributeEntry(attribute, priority);
        this.attributeEntries.add(ae);
    }



    private static class AttributeEntry {
        public Attribute attribute;
        public int priority;

        public AttributeEntry(Attribute attribute, int priority) {
            this.attribute = attribute;
            this.priority = priority;
        }

        public static class AttributeEntryComparator implements Comparator<AttributeEntry> {

            @Override
            public int compare(AttributeEntry o1, AttributeEntry o2) {
                return o2.priority - o1.priority;
            }

        }

    }
}
