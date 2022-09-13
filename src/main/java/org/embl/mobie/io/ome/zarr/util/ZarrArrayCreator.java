package org.embl.mobie.io.ome.zarr.util;

import java.util.Arrays;

import org.embl.mobie.io.n5.util.ArrayCreator;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;

import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;

public class ZarrArrayCreator<A, T extends NativeType<T>> extends ArrayCreator {
    private final OMEZarrAxes OMEZarrAxes;

    public ZarrArrayCreator(CellGrid cellGrid, DataType dataType, OMEZarrAxes OMEZarrAxes ) {
        super(cellGrid, dataType);
        this.OMEZarrAxes = OMEZarrAxes;
    }

    public A createArray(DataBlock<?> dataBlock, long[] gridPosition) {
        long[] cellDims = getCellDims(gridPosition);
        int n = (int) (cellDims[0] * cellDims[1] * cellDims[2]);

        if ( OMEZarrAxes.getNumDimension() == 2)
            cellDims = Arrays.stream(cellDims).limit(2).toArray();

        return (A) VolatileDoubleArray(dataBlock, cellDims, n);
    }

    @Override
    public long[] getCellDims(long[] gridPosition) {
        long[] cellMin = new long[3];
        int[] cellDims = new int[3];

        // TODO: do something like in: private long[] toZarrChunkIndices( long[] gridPosition )
        if ( OMEZarrAxes.is4DWithChannels() || OMEZarrAxes.is4DWithTimepoints()) {
            cellMin = new long[4];
            cellDims = new int[4];
            cellDims[3] = 1; // channel
        }

        if ( OMEZarrAxes.is4DWithTimepointsAndChannels()) {
            cellMin = new long[4];
            cellDims = new int[4];
            cellDims[2] = 1; // channel
            cellDims[3] = 1; // timepoint
        }

        if ( OMEZarrAxes.is5D()) {
            cellMin = new long[5];
            cellDims = new int[5];
            cellDims[3] = 1; // channel
            cellDims[4] = 1; // timepoint
        }

        cellGrid.getCellDimensions(gridPosition, cellMin, cellDims);
        return Arrays.stream(cellDims).mapToLong(i -> i).toArray(); // casting to long for creating ArrayImgs.*
    }
}