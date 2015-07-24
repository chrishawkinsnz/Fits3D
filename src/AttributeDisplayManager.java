
public class AttributeDisplayManager {
	public  AttributeDisplayer tweakableForAttribute(Attribute attribute, AttributeProvider attributeDisplayer) {
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
    	else if (attribute instanceof Attribute.TextAttribute) {
    		Attribute.TextAttribute nAttribute = (Attribute.TextAttribute)attribute;
    		tweakable = new Tweakable.ChrisLabel(nAttribute.value);
    	}
    	else if (attribute instanceof Attribute.SteppedRangeAttribute) {
    		Attribute.SteppedRangeAttribute srAttribute = (Attribute.SteppedRangeAttribute)attribute;
    		tweakable = new Tweakable.ClickySlider(srAttribute, srAttribute.min, srAttribute.max, srAttribute.value, srAttribute.steps);
    	}
    	else if (attribute instanceof Attribute.FilterSelectionAttribute) {
    		Attribute.FilterSelectionAttribute fsAttribute = (Attribute.FilterSelectionAttribute)attribute;
			PointCloud pc = (PointCloud)attributeDisplayer;
    		tweakable = new Tweakable.ChristogramTweakable(pc.getHistBuckets(), fsAttribute, pc.getHistMin(), pc.getHistMax());
    	}
		else if (attribute instanceof Attribute.MultiChoiceAttribute) {
			Attribute.MultiChoiceAttribute mcAttribute = (Attribute.MultiChoiceAttribute)attribute;
			tweakable = new Tweakable.DropDown(mcAttribute, mcAttribute.choices, mcAttribute.choice);
		}
    	else {
    		tweakable = null;
    	}
    	return tweakable;
    }

}
