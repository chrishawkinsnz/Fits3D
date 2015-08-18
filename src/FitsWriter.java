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
        System.out.println(parent);
        System.out.println(region);

        //--we have to load up a supped up version of the region first
        boolean previousFudgeSetting = RegionRepresentation.shouldFudge;
        RegionRepresentation.shouldFudge = false;
        region.loadRepresentationAtFidelity(1.0f);
        RegionRepresentation.shouldFudge = previousFudgeSetting;

        RegionRepresentation writableRegion = RegionRepresentation.loadFromDisk(parent.getFits(), 1.0f, region.getVolume(), false);
        //--now we iterate over those points and create a float buffer
        //--inneficient I know
        int wl = region.getDimensionInPts(3);
        int zl = region.getDimensionInPts(2);
        int yl = region.getDimensionInPts(1);
        int xl = region.getDimensionInPts(0);

        //--clear the data first
        float[][][][]data = new float[wl][zl][yl][xl];
        for (int wi = 0; wi < wl; wi++) {
            for (int zi = 0; zi < zl; zi++) {
                for (int yi = 0; yi < yl; yi++) {
                    for (int xi = 0; xi < xl; xi++) {
                        float constant = 0.3f;
                        data[wi][zi][yi][xi] = Float.NaN;
                    }
                }
            }
        }


        float wStep = 1.0f / (float)wl;
        float zStep = 1.0f / (float)zl;
        float yStep = 1.0f / (float)yl;
        float xStep = 1.0f / (float)xl;

        int totalPoints = wl * zl * yl * xl;
        for (VertexBufferSlice slice : region.getRegionRepresentation().getSlices()) {

            float zProportion   = (slice.z) ;/// region.getVolume().size.z;
            int   zIndex        = (int)(zProportion / zStep);
            float wProportion   = parent.frame.getValue();
            int   wIndex        = (int)(wProportion / wStep);
            for (int i = 0; i < slice.numberOfPts; i++) {
                short xs = slice.vertexBuffer.get(i * 3 + 0);
                short ys = slice.vertexBuffer.get(i * 3 + 1);


                float xProportion = (float)xs/(float)Short.MAX_VALUE;
                float yProportion = (float)ys/(float)Short.MAX_VALUE;

                int xIndex = (int)(xProportion / xStep);
                int yIndex = (int)(yProportion / yStep);

                if (xIndex >= xl) { xIndex = xl-1;}
                if (yIndex >= yl) { yIndex = yl-1;}
                if (zIndex >= zl) { zIndex = zl-1;}
                if (wIndex >= wl) { wIndex = wl-1;}

                float value = slice.valueBuffer.get(i);
                data[wIndex][zIndex][yIndex][xIndex] = value;
                //TODO am I shifting everything evers'slightly to one side???

            }
        }


        try {
            Fits f = new Fits();
            f.addHDU(FitsFactory.HDUFactory(data));
            //--copy over the meta data I think we need
            Header oldHeader = parent.getFits().getHDU(0).getHeader();
            Header newHeader = f.getHDU(0).getHeader();



            //--okay first up iterate over the old one and copy everything we need to
            Cursor oldCursor = oldHeader.iterator();
            while(oldCursor.hasNext()) {
                Object obj = oldCursor.next();
                if (obj instanceof  HeaderCard) {
                    HeaderCard oldHeaderCard = (HeaderCard)obj;
                    if (headerAlreadyIncludesValueForKey(newHeader, oldHeaderCard.getKey())) {
                        System.out.println("oh no duplicate!");
                    }
                    else {
                        newHeader.addLine(oldHeaderCard);
                    }
                }
                else {

                }

                System.out.println(obj);
            }


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
