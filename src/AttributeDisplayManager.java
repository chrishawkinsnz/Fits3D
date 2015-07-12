
public class AttributeDisplayManager {
	public  AttributeDisplayer tweakableForAttribute(Attribute attribute, PointCloud pc) {
    	//--listen okay we are just going to assume it is foo for the moment
    	AttributeDisplayer tweakable;
    	if (attribute instanceof Attribute.RangedAttribute) {
    		Attribute.RangedAttribute rAttribute = (Attribute.RangedAttribute) attribute;
    		tweakable = new Tweakable.Slidable(rAttribute, rAttribute.min, rAttribute.max, rAttribute.value);
    	}
    	else if (attribute instanceof Attribute.BinaryAttribute) {
    		Attribute.BinaryAttribute bAttribute = (Attribute.BinaryAttribute)attribute;
    		tweakable = new Tweakable.Toggleable(bAttribute, bAttribute.value);
    	}
    	else if (attribute instanceof Attribute.PathName) {
    		Attribute.PathName pnAttribute = (Attribute.PathName)attribute;
    		//--split up url 
    		String[] urlComponentsStrings = pnAttribute.value.split("/");
    		String name = urlComponentsStrings[urlComponentsStrings.length - 1];
    		tweakable = new Tweakable.ChrisLabel(name);
    	}
    	else if (attribute instanceof Attribute.Name) {
    		Attribute.Name nAttribute = (Attribute.Name)attribute;
    		tweakable = new Tweakable.ChrisLabel(nAttribute.value);
    	}
    	else if (attribute instanceof Attribute.SteppedRangeAttribute) {
    		Attribute.SteppedRangeAttribute srAttribute = (Attribute.SteppedRangeAttribute)attribute;
    		tweakable = new Tweakable.ClickySlider(srAttribute, srAttribute.min, srAttribute.max, srAttribute.value, srAttribute.steps);
    	}
    	else if (attribute instanceof Attribute.FilterSelectionAttribute) {
    		Attribute.FilterSelectionAttribute fsAttribute = (Attribute.FilterSelectionAttribute)attribute;
    		tweakable = new Tweakable.ChristogramTweakable(pc.getHistBuckets(), fsAttribute, pc.getHistMin(), pc.getHistMax());
    	}
    	else {
    		tweakable = null;
    	}
    	return tweakable;
    }


}
