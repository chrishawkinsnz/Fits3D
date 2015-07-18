import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by chrishawkins on 18/07/15.
 */
public interface AttributeProvider {

    default List<Attribute> aggregatableAttributes() {
        return getAttributes().stream().filter(u -> u.isAggregatable).collect(Collectors.toList());
    }

    default List<Attribute> nonAggregatable() {
        return getAttributes().stream().filter(u -> !u.isAggregatable).collect(Collectors.toList());
    }

    public List<Attribute> getAttributes();
}
