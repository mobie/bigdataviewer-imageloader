/*-
 * #%L
 * Readers and writers for image data in MoBIE projects
 * %%
 * Copyright (C) 2021 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.io;

import bdv.cache.SharedQueue;
import bdv.img.imaris.Imaris;
import bdv.spimdata.SpimDataMinimal;
import ch.epfl.biop.bdv.img.OpenersToSpimData;
import ch.epfl.biop.bdv.img.bioformats.BioFormatsHelper;
import ch.epfl.biop.bdv.img.imageplus.ImagePlusToSpimData;
import ch.epfl.biop.bdv.img.opener.OpenerSettings;
import ij.IJ;
import ij.ImagePlus;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.embl.mobie.io.imagedata.*;
import org.embl.mobie.io.n5.openers.N5Opener;
import org.embl.mobie.io.n5.openers.N5S3Opener;
import org.embl.mobie.io.ome.zarr.loaders.N5S3OMEZarrImageLoader;
import org.embl.mobie.io.ome.zarr.loaders.xml.XmlN5OmeZarrImageLoader;
import org.embl.mobie.io.ome.zarr.openers.OMEZarrOpener;
import org.embl.mobie.io.ome.zarr.openers.OMEZarrS3Opener;
import org.embl.mobie.io.openorganelle.OpenOrganelleS3Opener;
import org.embl.mobie.io.toml.TOMLOpener;
import org.embl.mobie.io.util.InputStreamXmlIoSpimData;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.io.util.SharedQueueHelper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_TAG;
import static mpicbg.spim.data.XmlKeys.SEQUENCEDESCRIPTION_TAG;


public class ImageDataOpener
{
    public static final String ERROR_WHILE_READING = "Error while trying to read ";

    public ImageDataOpener() {
    }


    public ImageData< ? > open( String uri, ImageDataFormat imageDataFormat) {
        switch (imageDataFormat) {
            case Toml:
                return new TOMLOpener( uri ).asSpimData();
//            case Tiff:
//                final File file = new File(imagePath);
//                return open((new Opener()).openTiff( file.getParent(), file.getName()));
//            case ImageJ:
//                return open(IJ.openImage(imagePath));
//            case BioFormats:
//                return openWithBDVBioFormats(imagePath);
//            case BioFormatsS3:
//                return openWithBioFormatsFromS3(imagePath, 0, null );
//            case Imaris:
//                return openImaris(imagePath);
            case Bdv:
            case BdvHDF5:
            case BdvN5:
            case BdvN5S3:
                return openBdvXml(uri);
            case OmeZarr:
            case OmeZarrS3:
            case OpenOrganelleS3:
                return new N5ImageData<>( uri, );
            default:
                throw new UnsupportedOperationException("Opening of " + imageDataFormat + " is not supported.");
        }
    }

    public ImageData< ? > open( String uri, ImageDataFormat imageDataFormat, SharedQueue sharedQueue) throws UnsupportedOperationException, SpimDataException {
        switch (imageDataFormat)
        {
            case Toml:
                return new TOMLOpener( uri ).asSpimData( sharedQueue );
            case Tiff:
                return open( IOHelper.openTiffFromFile( uri ), sharedQueue );
            case ImageJ:
               return new IJImageData<>( uri, sharedQueue );
            case BioFormats:
                return new BioFormatsImageData<>( uri, sharedQueue );
            case BioFormatsS3:
                return new BioFormatsS3ImageData<>( uri, sharedQueue );
            case Bdv:
            case BdvHDF5:
            case BdvN5:
            case BdvN5S3:
                return new BDVXMLImageData<>( uri, sharedQueue );
            case OmeZarr:
            case OmeZarrS3:
            case OpenOrganelleS3:
                return new N5ImageData<>( uri, sharedQueue );
            case BdvOmeZarr:
            case BdvOmeZarrS3:
            default:
                throw new RuntimeException( "Opening " + imageDataFormat + " is not supported; " +
                        "if you need it please report here: " +
                        "https://github.com/mobie/mobie-io/issues" );
        }
    }

    public AbstractSpimData open( ImagePlus imagePlus )
    {
        return ImagePlusToSpimData.getSpimData( imagePlus );
    }

    public AbstractSpimData open( ImagePlus imagePlus, SharedQueue sharedQueue )
    {
        final AbstractSpimData< ? > spimData = open(  imagePlus );
        SharedQueueHelper.setSharedQueue( sharedQueue, spimData );
        return spimData;
    }

    @NotNull
    private SpimDataMinimal openImaris(String imagePath) throws RuntimeException {
        try {
            return Imaris.openIms(imagePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SpimData openBdvXml(String path) throws SpimDataException {
        try {
            InputStream stream = IOHelper.getInputStream(path);
            SpimData spimData = new InputStreamXmlIoSpimData().open( stream, path );

            return spimData;
        } catch (Exception e) {
            System.err.println("Error opening " + path);
            throw new RuntimeException(e);
        }
    }

    private SpimData openBdvN5(String path, SharedQueue queue) throws SpimDataException {
        try {
            return N5Opener.openFile(path, queue);
        } catch (Exception e) {
            System.err.println("Error opening " + path);
            throw new RuntimeException(e);
        }
    }

    private SpimData openBdvN5S3(String path, SharedQueue queue) throws SpimDataException {
        try {
            return N5S3Opener.readURL(path, queue);
        } catch (Exception e) {
            System.err.println("Error opening " + path);
            throw new RuntimeException(e);
        }
    }

    private SpimData openOmeZarr(String path) throws SpimDataException {
        try {
            return OMEZarrOpener.openFile(path);
        } catch (Exception e) {
            System.err.println("Error opening " + path);
            throw new RuntimeException(e);
        }
    }

    private SpimData openOmeZarr(String path, SharedQueue sharedQueue) throws SpimDataException {
        try {
            return OMEZarrOpener.openFile(path, sharedQueue);
        } catch (Exception e) {
            System.err.println("Error opening " + path);
            throw new RuntimeException(e);
        }
    }

    private SpimData openOmeZarrS3(String path) throws SpimDataException {
        try {
            return OMEZarrS3Opener.readURL(path);
        } catch (Exception e) {
            System.err.println("Error opening " + path);
            throw new RuntimeException(e);
        }
    }

    private SpimData openOmeZarrS3(String path, SharedQueue sharedQueue) throws SpimDataException {
        try {
            return OMEZarrS3Opener.readURL(path, sharedQueue);
        } catch (Exception e) {
            System.err.println("Error opening " + path);
            throw new RuntimeException(e);
        }
    }

    private SpimData openOpenOrganelleS3(String path) throws SpimDataException {
        try {
            return OpenOrganelleS3Opener.readURL(path);
        } catch (Exception e) {
            System.err.println("Error opening " + path);
            throw new RuntimeException(e);
        }
    }

    @NotNull

    public AbstractSpimData< ? > openWithBDVBioFormats( String path )
    {
        final File file = new File( path );
        List< OpenerSettings > openerSettings = new ArrayList<>();
        int numSeries = BioFormatsHelper.getNSeries(file);
        for (int i = 0; i < numSeries; i++) {
            openerSettings.add(
                    OpenerSettings.BioFormats()
                            .location(file)
                            .setSerie(i) );
        }
        return OpenersToSpimData.getSpimData( openerSettings );
    }

    public AbstractSpimData< ? > openWithBDVBioFormats( String path, SharedQueue sharedQueue )
    {
        final AbstractSpimData< ? > spimData = openWithBDVBioFormats( path );
        SharedQueueHelper.setSharedQueue( sharedQueue, spimData );
        return spimData;
    }

    // TODO: Currently does not support resolution pyramids
    //
    public AbstractSpimData< ? > openWithBioFormatsFromS3( String path, int seriesIndex, SharedQueue sharedQueue )
    {
        ImagePlus imagePlus = IOHelper.openWithBioFormatsFromS3( path, seriesIndex );

        if ( sharedQueue != null )
        {
            return open( imagePlus, sharedQueue );
        }
        else
        {
            return open( imagePlus );
        }
    }

    private SpimData openBdvOmeZarr(String path, @Nullable SharedQueue sharedQueue) throws SpimDataException {
        SpimData spimData = openBdvXml(path);
        SpimData spimDataWithImageLoader = getSpimDataWithImageLoader(path, sharedQueue);
        if (spimData != null && spimDataWithImageLoader != null) {
            spimData.getSequenceDescription().setImgLoader(spimDataWithImageLoader.getSequenceDescription().getImgLoader());
            spimData.getSequenceDescription().getAllChannels().putAll(spimDataWithImageLoader.getSequenceDescription().getAllChannels());
            return spimData;
        } else {
            throw new SpimDataException( ERROR_WHILE_READING );
        }
    }

    @NotNull
    private N5S3OMEZarrImageLoader createN5S3OmeZarrImageLoader(String path, @Nullable SharedQueue queue) throws IOException, JDOMException {
        final SAXBuilder sax = new SAXBuilder();
        InputStream stream = IOHelper.getInputStream(path);
        final Document doc = sax.build(stream);
        final Element imgLoaderElem = doc.getRootElement().getChild(SEQUENCEDESCRIPTION_TAG).getChild(IMGLOADER_TAG);
        String bucketAndObject = imgLoaderElem.getChild("BucketName").getText() + "/" + imgLoaderElem.getChild("Key").getText();
        final String[] split = bucketAndObject.split("/");
        String bucket = split[0];
        String object = Arrays.stream(split).skip(1).collect(Collectors.joining("/"));
        if (queue == null) {
            return new N5S3OMEZarrImageLoader(imgLoaderElem.getChild("ServiceEndpoint").getText(), imgLoaderElem.getChild("SigningRegion").getText(), bucket, object, ".");
        } else {
            return new N5S3OMEZarrImageLoader(imgLoaderElem.getChild("ServiceEndpoint").getText(), imgLoaderElem.getChild("SigningRegion").getText(), bucket, object, ".", queue);
        }
    }

    private SpimData getSpimDataWithImageLoader(String path, @Nullable SharedQueue sharedQueue) {
        try {
            final SAXBuilder sax = new SAXBuilder();
            InputStream stream = IOHelper.getInputStream(path);
            final Document doc = sax.build(stream);
            final Element imgLoaderElem = doc.getRootElement().getChild(SEQUENCEDESCRIPTION_TAG).getChild(IMGLOADER_TAG);
            String imagesFile = XmlN5OmeZarrImageLoader.getDatasetsPathFromXml(imgLoaderElem, path);
            if (imagesFile != null) {
                if (new File(imagesFile).exists()) {
                    return sharedQueue != null ? OMEZarrOpener.openFile(imagesFile, sharedQueue)
                        : OMEZarrOpener.openFile(imagesFile);
                } else {
                    return sharedQueue != null ? OMEZarrS3Opener.readURL(imagesFile, sharedQueue)
                        : OMEZarrS3Opener.readURL(imagesFile);
                }
            }
        } catch (JDOMException | IOException e) {
            IJ.log(e.getMessage());
        }
        return null;
    }
}
