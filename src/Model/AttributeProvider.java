package Model;

import java.util.List;

/**
 * Created by chrishawkins on 18/07/15.
 */
public interface AttributeProvider {


    public List<Attribute> getAttributes();

    public List<AttributeProvider> getChildProviders();

    public String getName();

}
