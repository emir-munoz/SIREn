/**
 * Copyright (c) 2009-2012 National University of Ireland, Galway. All Rights Reserved.
 *
 * Project and contact information: http://www.siren.sindice.com/
 *
 * This file is part of the SIREn project.
 *
 * SIREn is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * SIREn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with SIREn. If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * @project siren-core
 * @author Renaud Delbru [ 27 Mar 2012 ]
 * @link http://renaud.delbru.fr/
 */
package org.sindice.siren.index.codecs.siren10;

import java.io.IOException;

import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;
import org.sindice.siren.index.codecs.block.BlockCompressor;
import org.sindice.siren.index.codecs.block.BlockIndexOutput;

public class DocsFreqBlockIndexOutput extends BlockIndexOutput {

  private final int maxBlockSize;

  private final BlockCompressor docCompressor;
  private final BlockCompressor freqCompressor;

  public DocsFreqBlockIndexOutput(final IndexOutput out, final int maxBlockSize,
                                  final BlockCompressor docCompressor,
                                  final BlockCompressor freqCompressor)
  throws IOException {
    super(out);
    this.docCompressor = docCompressor;
    this.freqCompressor = freqCompressor;
    this.maxBlockSize = maxBlockSize;
  }

  @Override
  public DocsFreqBlockWriter getBlockWriter() {
    return new DocsFreqBlockWriter();
  }

  public class DocsFreqBlockWriter extends BlockWriter {

    final IntsRef docBuffer;
    final IntsRef nodFreqBuffer;

    int firstDocId, lastDocId = 0;
    NodBlockIndexOutput.Index nodeBlockIndex;
    PosBlockIndexOutput.Index posBlockIndex;

    BytesRef docCompressedBuffer;
    BytesRef nodFreqCompressedBuffer;

    public DocsFreqBlockWriter() {
      docBuffer = new IntsRef(maxBlockSize);
      nodFreqBuffer = new IntsRef(maxBlockSize);

      // determine max size of compressed buffer to avoid overflow
      int size = docCompressor.maxCompressedValueSize() * maxBlockSize;
      size += docCompressor.getHeaderSize();
      docCompressedBuffer = new BytesRef(size);

      size = freqCompressor.maxCompressedValueSize() * maxBlockSize;
      size += freqCompressor.getHeaderSize();
      nodFreqCompressedBuffer = new BytesRef(size);
    }

    public int getMaxBlockSize() {
      return maxBlockSize;
    }

    public int getFirstDocId() {
      return firstDocId;
    }

    public void setNodeBlockIndex(final NodBlockIndexOutput.Index index) throws IOException {
      this.nodeBlockIndex = index;
    }

    public void setPosBlockIndex(final PosBlockIndexOutput.Index index) throws IOException {
      this.posBlockIndex = index;
    }

    /**
     * Called one value at a time.
     */
    public void write(final int docId) throws IOException {
      int delta;

      // compute delta - first value in the block is always 0
      if (docBuffer.offset != 0) {
        // TODO: We can decrement by one
        delta = docId - lastDocId;
      }
      else {
        delta = 0;
        firstDocId = docId;
      }
      docBuffer.ints[docBuffer.offset++] = delta;
      lastDocId = docId;
    }

    public void writeNodeFreq(final int nodeFreqInDoc) {
      // decrement freq by one
      nodFreqBuffer.ints[nodFreqBuffer.offset++] = nodeFreqInDoc - 1;
    }

    @Override
    public boolean isEmpty() {
      return docBuffer.offset == 0;
    }

    @Override
    public boolean isFull() {
      return docBuffer.offset >= maxBlockSize;
    }

    @Override
    protected void compress() {
      // Flip buffer before compression
      docBuffer.length = nodFreqBuffer.length = docBuffer.offset;
      docBuffer.offset = nodFreqBuffer.offset = 0;

      docCompressor.compress(docBuffer, docCompressedBuffer);
      freqCompressor.compress(nodFreqBuffer, nodFreqCompressedBuffer);
    }

    @Override
    protected void writeHeader() throws IOException {
      // logger.debug("Write DocFreq header - writer-id={}", this.hashCode());
      // logger.debug("DocFreq header start at fp={}", out.getFilePointer());

      // write block size (same for all of them)
      out.writeVInt(docBuffer.length);
      // logger.debug("blockSize: {}", docBuffer.length);

      // write size of each compressed data block
      out.writeVInt(docCompressedBuffer.length);
      // logger.debug("docCompressedBuffer.length: {}", docCompressedBuffer.length);
      out.writeVInt(nodFreqCompressedBuffer.length);
      // logger.debug("nodFreqCompressedBuffer.length: {}", nodFreqCompressedBuffer.length);

      // write first and last doc id
      out.writeVInt(firstDocId);
      out.writeVInt(lastDocId - firstDocId);
      // logger.debug("firstDocId: {}, lastDocId: {}", firstDocId, lastDocId);

      // write node and pos skip data
      // logger.debug("Write node and pos skip data");
      nodeBlockIndex.mark();
      nodeBlockIndex.write(out, true);
      posBlockIndex.mark();
      posBlockIndex.write(out, true);
    }

    @Override
    protected void writeData() throws IOException {
      out.writeBytes(docCompressedBuffer.bytes, docCompressedBuffer.length);
      out.writeBytes(nodFreqCompressedBuffer.bytes, nodFreqCompressedBuffer.length);
    }

    @Override
    protected void initBlock() {
      docBuffer.offset = 0;
    }

  }

}
