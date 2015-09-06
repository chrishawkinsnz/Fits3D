import nom.tam.fits.*;
import nom.tam.fits.header.IFitsHeader;
import nom.tam.util.BufferedFile;
import nom.tam.util.Cursor;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Created by chrishawkins on 18/08/15.
 */
public class FitsWriter {

    public static void writeFits(PointCloud parent, Region region, File file) {
        Volume regionVolume = region.getVolume().rejiggeredForPositiveSize();
        Header oldHeader = null;
        try {
            oldHeader = parent.getFits().getHDU(0).getHeader();
        }catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(parent);
        System.out.println(region);

        //--we have to load up a supped up version of the region first
        boolean previousFudgeSetting = RegionRepresentation.shouldFudge;
        RegionRepresentation.shouldFudge = false;
//        region.loadRepresentationAtFidelity(1.0f);


        RegionRepresentation writableRegionRepresentation = RegionRepresentation.loadFromDisk(parent.getFits(), 1.0f, region.getVolume(), false);

        boolean fourdee = headerAlreadyIncludesValueForKey(oldHeader, "NAXIS4");

        //--now we iterate over those points and create a float buffer
        //--inneficient I know


        int wl = writableRegionRepresentation.getDimensionInPts(3);
        int zl = writableRegionRepresentation.getDimensionInPts(2);
        int yl = writableRegionRepresentation.getDimensionInPts(1);
        int xl = writableRegionRepresentation.getDimensionInPts(0);

        //--clear the data first
        float[][][][]data4 = new float[wl][zl][yl][xl];
        float[][][]  data3 = new float[zl][yl][xl];
        for (int wi = 0; wi < wl; wi++) {
            for (int zi = 0; zi < zl; zi++) {
                for (int yi = 0; yi < yl; yi++) {
                    for (int xi = 0; xi < xl; xi++) {
                        float constant = 0.3f;
                        if (fourdee) {
                            data4[wi][zi][yi][xi] = Float.NaN;
                        }
                        else {
                            data3[zi][yi][xi] = Float.NaN;
                        }

                    }
                }
            }
        }


        for (VertexBufferSlice slice : writableRegionRepresentation.getSlices()) {
            slice.region = region;
            slice.cloud = parent;
            float zProportionCloud   = ((slice.getOverallZ()) - parent.getVolume().origin.z)/parent.getVolume().size.z ;/// region.getVolume().size.z;
            float zProportion = (zProportionCloud - regionVolume.origin.z)/ regionVolume.size.z;
            int   zIndex        = (int)(zProportion * zl);
            float wProportion   = parent.frame.getValue();
            int   wIndex        = (int)(wProportion * wl);
            for (int i = 0; i < slice.numberOfPts; i++) {
                short xs = slice.vertexBuffer.get(i * 3 + 0);
                short ys = slice.vertexBuffer.get(i * 3 + 1);

                float xProportionCloud = (float)xs/(float)Short.MAX_VALUE;
                float xProportion = (xProportionCloud - regionVolume.origin.x)/ regionVolume.size.x;
                float yProportionCloud = (float)ys/(float)Short.MAX_VALUE;
                float yProportion = (yProportionCloud - regionVolume.origin.y)/ regionVolume.size.y;

                int xIndex = (int)(xProportion * xl);
                int yIndex = (int)(yProportion * yl);

                if (xIndex >= xl) {xIndex = xl-1;}
                if (yIndex >= yl) {yIndex = yl-1;}
                if (zIndex >= zl) {zIndex = zl-1;}
                if (wIndex >= wl) {wIndex = wl-1;}

                float value = slice.valueBuffer.get(i);
                try {
                    if (fourdee) {
                        data4[wIndex][zIndex][yIndex][xIndex] = value;
                    } else {
                        data3[zIndex][yIndex][xIndex] = value;
                    }
                }catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
                //TODO am I shifting everything evers'slightly to one side???

            }
        }

        try {
            Fits f = new Fits();
            if (fourdee) {
                f.addHDU(FitsFactory.HDUFactory(data4));
            }
            else {
                f.addHDU(FitsFactory.HDUFactory(data3));
            }

            //--copy over the meta data I think we need
            Header newHeader = f.getHDU(0).getHeader();



            //--okay first up iterate over the old one and copy everything we need to
            Cursor oldCursor = oldHeader.iterator();
            while(oldCursor.hasNext()) {
                Object obj = oldCursor.next();
                if (obj instanceof  HeaderCard) {
                    HeaderCard oldHeaderCard = (HeaderCard)obj;
                    if (headerAlreadyIncludesValueForKey(newHeader, oldHeaderCard.getKey())) {
                        System.out.println("duplicate header entry: " + oldHeaderCard.getKey());
                    }
//                    else if (oldHeaderCard.getKey().contains("CELLSCAL")) {
//                        continue;
//                    }
                    else {
                        newHeader.addLine(oldHeaderCard);
                    }

                }
                else {
                    System.out.println("not a header cards");
                }

                System.out.println(obj);
            }


            //--so the main thing we will need to do is change the reference pixel to be

            //--first get the old reference pixel
            int naxis = fourdee? 4 : 3;
            for (int i = 0; i < naxis; i++) {
                float oldPixel = oldHeader.getFloatValue("CRPIX" + (i+1));
                float length = oldHeader.getFloatValue("NAXIS" + (i+1));
                float offset = region.getVolume().origin.get(i) * length;
                int newPixel = (int) (oldPixel - offset);
                newHeader.addValue("CRPIX" + (i+1),newPixel,"ummmm");
            }


            Vector3 proportionalOffset = region.getVolume().origin;


            newHeader.insertHistory("initially extracted from larger fits file '" + parent.fileName.getValue() +"'");

            //--ensure has teh fits suffix
            String path = file.getAbsolutePath();
            if (!path.endsWith(".fits")) {
                path = path + ".fits";
            }
            BufferedFile bf = new BufferedFile(path, "rw");
            f.write(bf);
            bf.close();
        } catch (FitsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        RegionRepresentation.shouldFudge = previousFudgeSetting;
    }

    public static boolean headerAlreadyIncludesValueForKey(Header header, String key) {
        Cursor cursor = header.iterator();
        while(cursor.hasNext()) {
            Object nextObj = cursor.next();
            if (nextObj instanceof  HeaderCard) {
                HeaderCard nextCard = (HeaderCard) nextObj;
                if (nextCard.getKey().equals(key)) {
                    return true;
                }
            }
            else {
                System.out.println("catch");
            }

        }
        return false;
    }
}
