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
/**
 * Copyright (c) 2019, Stephan Saalfeld
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.embl.mobie.io.ome.zarr.readers;

import java.io.IOException;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.embl.mobie.io.ome.zarr.util.N5ZarrImageReader;
import org.embl.mobie.io.ome.zarr.util.N5ZarrImageReaderHelper;
import org.embl.mobie.io.ome.zarr.util.OmeZArrayAttributes;
import org.embl.mobie.io.ome.zarr.util.ZArrayAttributes;
import org.embl.mobie.io.ome.zarr.util.ZarrAxes;
import org.embl.mobie.io.ome.zarr.util.ZarrAxis;
import org.embl.mobie.io.ome.zarr.util.ZarrDatasetAttributes;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonAttributesParser;
import org.janelia.saalfeldlab.n5.N5FSReader;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;




/**
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */

public class N5OmeZarrReader extends N5FSReader implements N5ZarrImageReader {
    protected final boolean mapN5DatasetAttributes;
    final N5ZarrImageReaderHelper n5ZarrImageReaderHelper;
    protected String dimensionSeparator;
    private ZarrAxes zarrAxes;
    private List<ZarrAxis> zarrAxesList;

    /**
     * Opens an {@link N5OmeZarrReader} at a given base path with a custom
     * {@link GsonBuilder} to support custom attributes.
     *
     * @param basePath               Zarr base path
     * @param gsonBuilder
     * @param dimensionSeparator
     * @param mapN5DatasetAttributes Virtually create N5 dataset attributes (dimensions, blockSize,
     *                               compression, dataType) for datasets such that N5 code that
     *                               reads or modifies these attributes directly works as expected.
     *                               This can lead to name clashes if a zarr container uses these
     *                               attribute keys for other purposes.
     * @throws IOException
     */
    public N5OmeZarrReader(final String basePath, final GsonBuilder gsonBuilder, final String dimensionSeparator, final boolean mapN5DatasetAttributes) throws IOException {
        super(basePath, N5ZarrImageReader.initGsonBuilder(gsonBuilder));
        this.dimensionSeparator = dimensionSeparator;
        this.mapN5DatasetAttributes = mapN5DatasetAttributes;
        this.n5ZarrImageReaderHelper = new N5ZarrImageReaderHelper(basePath, N5ZarrImageReader.initGsonBuilder(gsonBuilder));
    }

    /**
     * Opens an {@link N5OmeZarrReader} at a given base path with a custom
     * {@link GsonBuilder} to support custom attributes.
     *
     * @param basePath           Zarr base path
     * @param gsonBuilder
     * @param dimensionSeparator
     * @throws IOException
     */
    public N5OmeZarrReader(final String basePath, final GsonBuilder gsonBuilder, final String dimensionSeparator) throws IOException {

        this(basePath, gsonBuilder, dimensionSeparator, true);
    }

    /**
     * Opens an {@link N5OmeZarrReader} at a given base path.
     *
     * @param basePath               Zarr base path
     * @param dimensionSeparator
     * @param mapN5DatasetAttributes Virtually create N5 dataset attributes (dimensions, blockSize,
     *                               compression, dataType) for datasets such that N5 code that
     *                               reads or modifies these attributes directly works as expected.
     *                               This can lead to name collisions if a zarr container uses these
     *                               attribute keys for other purposes.
     * @throws IOException
     */
    public N5OmeZarrReader(final String basePath, final String dimensionSeparator, final boolean mapN5DatasetAttributes) throws IOException {

        this(basePath, new GsonBuilder(), dimensionSeparator, mapN5DatasetAttributes);
    }

    /**
     * Opens an {@link N5OmeZarrReader} at a given base path.
     *
     * @param basePath               Zarr base path
     * @param mapN5DatasetAttributes Virtually create N5 dataset attributes (dimensions, blockSize,
     *                               compression, dataType) for datasets such that N5 code that
     *                               reads or modifies these attributes directly works as expected.
     *                               This can lead to name collisions if a zarr container uses these
     *                               attribute keys for other purposes.
     * @throws IOException
     */
    public N5OmeZarrReader(final String basePath, final boolean mapN5DatasetAttributes) throws IOException {
        this(basePath, new GsonBuilder(), DEFAULT_SEPARATOR, mapN5DatasetAttributes);
    }

    /**
     * Opens an {@link N5OmeZarrReader} at a given base path with a custom
     * {@link GsonBuilder} to support custom attributes.
     * <p>
     * Zarray metadata will be virtually mapped to N5 dataset attributes.
     *
     * @param basePath    Zarr base path
     * @param gsonBuilder
     * @throws IOException
     */
    public N5OmeZarrReader(final String basePath, final GsonBuilder gsonBuilder) throws IOException {
        this(basePath, gsonBuilder, DEFAULT_SEPARATOR);
    }

    /**
     * Opens an {@link N5OmeZarrReader} at a given base path.
     * <p>
     * Zarray metadata will be virtually mapped to N5 dataset attributes.
     *
     * @param basePath Zarr base path
     * @throws IOException
     */
    public N5OmeZarrReader(final String basePath) throws IOException {

        this(basePath, new GsonBuilder());
    }

    @Override
    public Version getVersion() throws IOException {

        final Path path;
        if (groupExists("/")) {
            path = Paths.get(basePath, zgroupFile);
        } else if (datasetExists("/")) {
            path = Paths.get(basePath, zarrayFile);
        } else {
            return VERSION;
        }

        if (Files.exists(path)) {

            try (final LockedFileChannel lockedFileChannel = LockedFileChannel.openForReading(path)) {
                final HashMap<String, JsonElement> attributes =
                    GsonAttributesParser.readAttributes(
                        Channels.newReader(
                            lockedFileChannel.getFileChannel(),
                            StandardCharsets.UTF_8.name()),
                        gson);

                final Integer zarr_format = GsonAttributesParser.parseAttribute(
                    attributes,
                    "zarr_format",
                    Integer.class,
                    gson);

                if (zarr_format != null)
                    return new Version(zarr_format, 0, 0);
            }
        }
        return VERSION;
    }

    /**
     * @return Zarr base path
     */
    @Override
    public String getBasePath() {

        return this.basePath;
    }

    public boolean groupExists(final String pathName) {

        final Path path = Paths.get(basePath, removeLeadingSlash(pathName), zgroupFile);
        return Files.exists(path) && Files.isRegularFile(path);
    }

    @Override
    public String getZarrDataBlockString(long[] gridPosition, String dimensionSeparator, boolean isRowMajor) {
        return N5ZarrImageReader.super.getZarrDataBlockString(gridPosition, dimensionSeparator, isRowMajor);
    }

    public ZArrayAttributes getZArrayAttributes(final String pathName) throws IOException {

        final Path path = Paths.get(basePath, removeLeadingSlash(pathName), zarrayFile);
        OmeZArrayAttributes zArrayAttributes = null;
        if (Files.exists(path)) {
            try (final LockedFileChannel lockedFileChannel = LockedFileChannel.openForReading(path);
                 final Reader reader = Channels.newReader(lockedFileChannel.getFileChannel(), StandardCharsets.UTF_8.name())) {
                zArrayAttributes = gson.fromJson(reader, OmeZArrayAttributes.class);
            }
        } else {
            //System.out.println(path + " does not exist.");
        }
        this.dimensionSeparator = zArrayAttributes == null || zArrayAttributes.getDimensionSeparator() == null ?
            DEFAULT_SEPARATOR : zArrayAttributes.getDimensionSeparator();

        return zArrayAttributes;
    }

    @Override
    public DatasetAttributes getDatasetAttributes(final String pathName) throws IOException {

        final ZArrayAttributes zArrayAttributes = getZArrayAttributes(pathName);
        return zArrayAttributes == null ? null : zArrayAttributes.getDatasetAttributes();
    }

    @Override
    public boolean datasetExists(final String pathName) throws IOException {

        final Path path = Paths.get(basePath, removeLeadingSlash(pathName), zarrayFile);
        return Files.exists(path) && Files.isRegularFile(path) && getDatasetAttributes(pathName) != null;
    }

    /**
     * @returns false if the group or dataset does not exist but also if the
     * attempt to access
     */
    @Override
    public boolean exists(final String pathName) {

        try {
            return groupExists(pathName) || datasetExists(pathName);
        } catch (final IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * If {@link #mapN5DatasetAttributes} is set, dataset attributes will
     * override attributes with the same key.
     */
    @Override
    public HashMap<String, JsonElement> getAttributes(final String pathName) throws IOException {

        final Path path = Paths.get(basePath, removeLeadingSlash(pathName), zattrsFile);
        final HashMap<String, JsonElement> attributes = new HashMap<>();
        if (Files.exists(path)) {
            try (final LockedFileChannel lockedFileChannel = LockedFileChannel.openForReading(path)) {
                attributes.putAll(
                    GsonAttributesParser.readAttributes(
                        Channels.newReader(
                            lockedFileChannel.getFileChannel(),
                            StandardCharsets.UTF_8.name()),
                        gson));
            }
        }

        try {
            getDimensions(attributes);
        } catch (IllegalArgumentException e) {
            throw new IOException("Error while getting datasets dimensions", e);
        }

        if (mapN5DatasetAttributes && datasetExists(pathName)) {
            final DatasetAttributes datasetAttributes = getZArrayAttributes(pathName).getDatasetAttributes();
            n5ZarrImageReaderHelper.putAttributes(attributes, datasetAttributes);
        }

        return attributes;
    }

    @Override
    public Map<String, Class<?>> listAttributes(String pathName) throws IOException {
        return super.listAttributes(pathName);
    }

    public ZarrAxes getAxes() {
        return this.zarrAxes;
    }

    @Override
    public void setAxes(JsonElement axesJson) {
        if (axesJson != null) {
            this.zarrAxes = ZarrAxes.decode(axesJson.toString());
        } else {
            this.zarrAxes = ZarrAxes.TCZYX;
        }
    }

    @Override
    public void setAxes(List<ZarrAxis> axes) {
        this.zarrAxesList = axes;
    }

    public List<ZarrAxis> getZarrAxes() {
        return this.zarrAxesList;
    }

    @Override
    public DataBlock<?> readBlock(
        final String pathName,
        final DatasetAttributes datasetAttributes,
        final long... gridPosition) throws IOException {

        final ZarrDatasetAttributes zarrDatasetAttributes;
        if (datasetAttributes instanceof ZarrDatasetAttributes)
            zarrDatasetAttributes = (ZarrDatasetAttributes) datasetAttributes;
        else
            zarrDatasetAttributes = getZArrayAttributes(pathName).getDatasetAttributes();

        Path path = Paths.get(
            basePath,
            removeLeadingSlash(pathName),
            getZarrDataBlockString(
                gridPosition,
                dimensionSeparator,
                zarrDatasetAttributes.isRowMajor()));
        if (!Files.exists(path)) {
            return null;
        }

        try (final LockedFileChannel lockedChannel = LockedFileChannel.openForReading(path)) {
            return readBlock(Channels.newInputStream(lockedChannel.getFileChannel()), zarrDatasetAttributes, gridPosition);
        }
    }

    @Override
    public String[] list(final String pathName) throws IOException {

        final Path path = Paths.get(basePath, removeLeadingSlash(pathName));
        try (final Stream<Path> pathStream = Files.list(path)) {

            return pathStream
                .filter(Files::isDirectory)
                .map(a -> path.relativize(a).toString())
                .filter(a -> exists(pathName + "/" + a))
                .toArray(String[]::new);
        }
    }
}
