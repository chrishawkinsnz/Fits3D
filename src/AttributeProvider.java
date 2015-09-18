import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by chrishawkins on 18/07/15.
 */
public interface AttributeProvider {


    public List<Attribute> getAttributes();

    public List<AttributeProvider> getChildProviders();

    public String getName();

}
