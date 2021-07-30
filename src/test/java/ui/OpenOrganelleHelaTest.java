package ui;

import bdv.util.BdvFunctions;
import de.embl.cba.n5.openorganelle.OpenOrganelleS3Reader;
import mpicbg.spim.data.SpimData;

import java.io.IOException;

public class OpenOrganelleHelaTest {

    public static void main(String[] args) throws IOException {
        showHela();
    }

    public static void showHela() throws IOException {
        OpenOrganelleS3Reader reader = new OpenOrganelleS3Reader(
                "https://janelia-cosem.s3.amazonaws.com",
                "us-west-2",
                "jrc_hela-2");
        OpenOrganelleS3Reader.setLogChunkLoading(true);
        SpimData image = reader.readKey("jrc_hela-2.n5/em/fibsem-uint16");
        BdvFunctions.show(image);
    }
}
