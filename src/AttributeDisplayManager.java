import com.sun.tools.doclint.HtmlTag;

public class AttributeDisplayManager {
	public static AttributeDisplayManager defaultDisplayManager = new AttributeDisplayManager();

	private AttributeDisplayManager() {

	}

	public  AttributeDisplayer tweakableForAttribute(Attribute attribute, AttributeProvider attributeDisplayer) {
    	//--listen okay we are just going to assume it is foo for the moment
    	AttributeDisplayer tweakable;
    	if (attribute instanceof Attribute.RangedAttribute) {
    		Attribute.RangedAttribute rAttribute = (Attribute.RangedAttribute) attribute;
    		tweakable = new Tweakable.Slidable(rAttribute, rAttribute.getMin(), rAttribute.getMax(), rAttribute.getValue());
    	}
    	else if (attribute instanceof Attribute.BinaryAttribute) {
    		Attribute.BinaryAttribute bAttribute = (Attribute.BinaryAttribute)attribute;
    		tweakable = new Tweakable.Toggleable(bAttribute, bAttribute.getValue());
    	}
    	else if (attribute instanceof Attribute.PathName) {
    		Attribute.PathName pnAttribute = (Attribute.PathName)attribute;
    		//--split up url 
    		String[] urlComponentsStrings = pnAttribute.getValue().split("/");
    		String name = urlComponentsStrings[urlComponentsStrings.length - 1];
    		tweakable = new Tweakable.ChrisLabel(name);
    	}
    	else if (attribute instanceof Attribute.TextAttribute) {
    		Attribute.TextAttribute nAttribute = (Attribute.TextAttribute)attribute;
			if (nAttribute.isTitle) {
				tweakable = new Tweakable.ChrisTitle(nAttribute.getValue());
			}
			else {
				tweakable = new Tweakable.ChrisLabel(nAttribute.getValue());
			}
    	}
    	else if (attribute instanceof Attribute.SteppedRangeAttribute) {
    		Attribute.SteppedRangeAttribute srAttribute = (Attribute.SteppedRangeAttribute)attribute;
    		tweakable = new Tweakable.ClickySlider(srAttribute, srAttribute.min, srAttribute.max, srAttribute.getValue(), srAttribute.steps);
    	}
    	else if (attribute instanceof Attribute.FilterSelectionAttribute) {
    		Attribute.FilterSelectionAttribute fsAttribute = (Attribute.FilterSelectionAttribute)attribute;
			PointCloud pc = fsAttribute.getPointCloud();
    		tweakable = new Tweakable.ChristogramTweakable(pc.getHistBuckets(), fsAttribute, pc.getHistMin(), pc.getHistMax(), fsAttribute.getAxisName());
    	}
		else if (attribute instanceof Attribute.MultiChoiceAttribute) {
			Attribute.MultiChoiceAttribute mcAttribute = (Attribute.MultiChoiceAttribute)attribute;
			tweakable = new Tweakable.DropDown(mcAttribute, mcAttribute.choices, mcAttribute.choice);
		}
		else if (attribute instanceof Attribute.Actchin) {
			Attribute.Actchin aa = (Attribute.Actchin)attribute;
			tweakable = new Tweakable.ChrisButton(aa);
		}
    	else {
    		tweakable = null;
    	}

		if (tweakable != null) {
			attribute.setListener(tweakable);
		}
    	return tweakable;
    }

}
