package de.tisan.player;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

final class Huffman {
    private static final int MX_OFF = 250;
    private static final int HUFFMAN_COUNT = 34;
    public static final Huffman[] HUFFMAN;
    private final char tableName0;
    private final char tableName1;
    private final int xlen; // max. x-index+
    private final int ylen; // max. y-index+
    private final int linbits; // number of linbits
    private final int[] val0; // decoder tree
    private final int[] val1; // decoder tree
    private final int treeLen; // length of decoder tree

    private Huffman(String name, int xlen, int ylen, int linbits, int[][] val,
            int treeLen) {
        tableName0 = name.charAt(0);
        tableName1 = name.charAt(1);
        this.xlen = xlen;
        this.ylen = ylen;
        this.linbits = linbits;
        val0 = new int[val.length];
        val1 = new int[val.length];
        for (int i = 0; i < val.length; i++) {
            val0[i] = val[i][0];
            val1[i] = val[i][1];
        }
        this.treeLen = treeLen;
    }

    /**
     * Do the huffman-decoding.
     * For counta, countb - the 4 bit value is
     * returned in y, discard x.
     */
    public int decode(Layer3Decoder decoder, BitReservoir br) {
        // array of all huffcodtable headers
        // 0..31 Huffman code table 0..31
        // 32,33 count1-tables

        // table 0 needs no bits
        if (treeLen == 0) {
            decoder.x = decoder.y = 0;
            return 0;
        }
        int level = 1 << ((4 * 8) - 1);
        int point = 0;
        int error = 1;
        // Lookup in Huffman table
        do {
            if (val0[point] == 0) {
                // end of tree
                decoder.x = val1[point] >>> 4;
                decoder.y = val1[point] & 0xf;
                error = 0;
                break;
            }
            if (br.getOneBit() != 0) {
                while (val1[point] >= MX_OFF) {
                    point += val1[point];
                }
                point += val1[point];
            } else {
                while (val0[point] >= MX_OFF) {
                    point += val0[point];
                }
                point += val0[point];
            }
            level >>>= 1;
        } while (level != 0);
        // Process sign encodings for quadruples tables
        if (tableName0 == '3' && (tableName1 == '2' || tableName1 == '3')) {
            decoder.v = (decoder.y >> 3) & 1;
            decoder.w = (decoder.y >> 2) & 1;
            decoder.x = (decoder.y >> 1) & 1;
            decoder.y = decoder.y & 1;
            // v, w, x and y are reversed in the bitstream
            // switch them around to make test bistream work.
            if (decoder.v != 0) {
                if (br.getOneBit() != 0) {
                    decoder.v = -decoder.v;
                }
            }
            if (decoder.w != 0) {
                if (br.getOneBit() != 0) {
                    decoder.w = -decoder.w;
                }
            }
            if (decoder.x != 0) {
                if (br.getOneBit() != 0) {
                    decoder.x = -decoder.x;
                }
            }
            if (decoder.y != 0) {
                if (br.getOneBit() != 0) {
                    decoder.y = -decoder.y;
                }
            }
        } else {
            // Process sign and escape encodings for dual tables.
            // x and y are reversed in the test bitstream.
            // Reverse x and y here to make test bitstream work.
            if (linbits != 0) {
                if ((xlen - 1) == decoder.x) {
                    decoder.x += br.getBits(linbits);
                }
            }
            if (decoder.x != 0) {
                if (br.getOneBit() != 0) {
                    decoder.x = -decoder.x;
                }
            }
            if (linbits != 0) {
                if ((ylen - 1) == decoder.y) {
                    decoder.y += br.getBits(linbits);
                }
            }
            if (decoder.y != 0) {
                if (br.getOneBit() != 0) {
                    decoder.y = -decoder.y;
                }
            }
        }
        return error;
    }

    static {
        HUFFMAN = new Huffman[HUFFMAN_COUNT];
        HUFFMAN[0] = new Huffman("0 ", 0, 0, 0, Constants.VAL_TAB_0, 0);
        HUFFMAN[1] = new Huffman("1 ", 2, 2, 0, Constants.VAL_TAB_1, 7);
        HUFFMAN[2] = new Huffman("2 ", 3, 3, 0, Constants.VAL_TAB_2, 17);
        HUFFMAN[3] = new Huffman("3 ", 3, 3, 0, Constants.VAL_TAB_3, 17);
        HUFFMAN[4] = new Huffman("4 ", 0, 0, 0, Constants.VAL_TAB_4, 0);
        HUFFMAN[5] = new Huffman("5 ", 4, 4, 0, Constants.VAL_TAB_5, 31);
        HUFFMAN[6] = new Huffman("6 ", 4, 4, 0, Constants.VAL_TAB_6, 31);
        HUFFMAN[7] = new Huffman("7 ", 6, 6, 0, Constants.VAL_TAB_7, 71);
        HUFFMAN[8] = new Huffman("8 ", 6, 6, 0, Constants.VAL_TAB_8, 71);
        HUFFMAN[9] = new Huffman("9 ", 6, 6, 0, Constants.VAL_TAB_9, 71);
        HUFFMAN[10] = new Huffman("10", 8, 8, 0, Constants.VAL_TAB_10, 127);
        HUFFMAN[11] = new Huffman("11", 8, 8, 0, Constants.VAL_TAB_11, 127);
        HUFFMAN[12] = new Huffman("12", 8, 8, 0, Constants.VAL_TAB_12, 127);
        HUFFMAN[13] = new Huffman("13", 16, 16, 0, Constants.VAL_TAB_13, 511);
        HUFFMAN[14] = new Huffman("14", 0, 0, 0, Constants.VAL_TAB_14, 0);
        HUFFMAN[15] = new Huffman("15", 16, 16, 0, Constants.VAL_TAB_15, 511);
        HUFFMAN[16] = new Huffman("16", 16, 16, 1, Constants.VAL_TAB_16, 511);
        HUFFMAN[17] = new Huffman("17", 16, 16, 2, Constants.VAL_TAB_16, 511);
        HUFFMAN[18] = new Huffman("18", 16, 16, 3, Constants.VAL_TAB_16, 511);
        HUFFMAN[19] = new Huffman("19", 16, 16, 4, Constants.VAL_TAB_16, 511);
        HUFFMAN[20] = new Huffman("20", 16, 16, 6, Constants.VAL_TAB_16, 511);
        HUFFMAN[21] = new Huffman("21", 16, 16, 8, Constants.VAL_TAB_16, 511);
        HUFFMAN[22] = new Huffman("22", 16, 16, 10, Constants.VAL_TAB_16, 511);
        HUFFMAN[23] = new Huffman("23", 16, 16, 13, Constants.VAL_TAB_16, 511);
        HUFFMAN[24] = new Huffman("24", 16, 16, 4, Constants.VAL_TAB_24, 512);
        HUFFMAN[25] = new Huffman("25", 16, 16, 5, Constants.VAL_TAB_24, 512);
        HUFFMAN[26] = new Huffman("26", 16, 16, 6, Constants.VAL_TAB_24, 512);
        HUFFMAN[27] = new Huffman("27", 16, 16, 7, Constants.VAL_TAB_24, 512);
        HUFFMAN[28] = new Huffman("28", 16, 16, 8, Constants.VAL_TAB_24, 512);
        HUFFMAN[29] = new Huffman("29", 16, 16, 9, Constants.VAL_TAB_24, 512);
        HUFFMAN[30] = new Huffman("30", 16, 16, 11, Constants.VAL_TAB_24, 512);
        HUFFMAN[31] = new Huffman("31", 16, 16, 13, Constants.VAL_TAB_24, 512);
        HUFFMAN[32] = new Huffman("32", 1, 16, 0, Constants.VAL_TAB_32, 31);
        HUFFMAN[33] = new Huffman("33", 1, 16, 0, Constants.VAL_TAB_33, 31);
    }
}

final class Header {
    private static final int[][] FREQUENCIES = { { 22050, 24000, 16000, 1 },
            { 44100, 48000, 32000, 1 }, { 11025, 12000, 8000, 1 } };
    static final int VERSION_MPEG2_LSF = 0;
    static final int VERSION_MPEG25_LSF = 2;
    static final int VERSION_MPEG1 = 1;
    static final int MODE_JOINT_STEREO = 1;
    public static final int MODE_SINGLE_CHANNEL = 3;
    private static final int SAMPLE_FREQUENCY_FOURTYEIGHT = 1;
    private static final int SAMPLE_FREQUENCY_THIRTYTWO = 2;
    private boolean protectionBit, paddingBit;
    private int bitrateIndex, modeExtension;
    private int version;
    private int mode;
    private int sampleFrequency;
    private int numberOfSubbands, intensityStereoBound;
    private byte syncMode = Bitstream.INITIAL_SYNC;
    private int frameSize;
    private boolean vbr;
    private int slots;

    boolean readHeader(Bitstream stream) throws IOException {
        while (true) {
            int headerString = stream.syncHeader(syncMode);
            if (syncMode == Bitstream.INITIAL_SYNC) {
                version = ((headerString >>> 19) & 1);
                if (((headerString >>> 20) & 1) == 0) {
                    if (version == VERSION_MPEG2_LSF) {
                        version = VERSION_MPEG25_LSF;
                    } else {
                        throw new IOException("Unsupported version: " + version);
                    }
                }
                sampleFrequency = ((headerString >>> 10) & 3);
                if (sampleFrequency == 3) {
                    throw new IOException("Unsupported sampleFrequency: "
                            + sampleFrequency);
                }
            }
            int layer = 4 - (headerString >>> 17) & 3;
            if (layer != 3) {
                throw new IOException("Unsupported layer: " + layer);
            }
            protectionBit = ((headerString >>> 16) & 1) != 0;
            bitrateIndex = (headerString >>> 12) & 0xF;
            paddingBit = ((headerString >>> 9) & 1) != 0;
            mode = ((headerString >>> 6) & 3);
            modeExtension = (headerString >>> 4) & 3;
            if (mode == MODE_JOINT_STEREO) {
                intensityStereoBound = (modeExtension << 2) + 4;
            } else {
                intensityStereoBound = 0; // should never be used
            }
            // calculate number of subbands:
            int channelBitrate = bitrateIndex;
            // calculate bitrate per channel:
            if (mode != MODE_SINGLE_CHANNEL) {
                if (channelBitrate == 4) {
                    channelBitrate = 1;
                } else {
                    channelBitrate -= 4;
                }
            }
            if (channelBitrate == 1 || channelBitrate == 2) {
                if (sampleFrequency == SAMPLE_FREQUENCY_THIRTYTWO) {
                    numberOfSubbands = 12;
                } else {
                    numberOfSubbands = 8;
                }
            } else if (sampleFrequency == SAMPLE_FREQUENCY_FOURTYEIGHT
                    || (channelBitrate >= 3 && channelBitrate <= 5)) {
                numberOfSubbands = 27;
            } else {
                numberOfSubbands = 30;
            }
            if (intensityStereoBound > numberOfSubbands) {
                intensityStereoBound = numberOfSubbands;
            }
            calculateFramesize();
            int frameSizeLoaded = stream.readFrameData(frameSize);
            if (frameSize >= 0 && frameSizeLoaded != frameSize) {
                // Data loaded does not match to expected framesize,
                // it might be an ID3v1 Tag
                return false;
            }
            if (stream.isSyncCurrentPosition(syncMode)) {
                if (syncMode == Bitstream.INITIAL_SYNC) {
                    syncMode = Bitstream.STRICT_SYNC;
                    stream.setSyncWord(headerString & 0xFFF80CC0);
                }
                break;
            }
            stream.unreadFrame();
        }
        stream.parseFrame();
        if (!protectionBit) {
            // frame contains a crc checksum
            stream.getBits(16);
        }
        return true;
    }

    void parseVBR(byte[] firstFrame) throws IOException {
        // trying Xing header
        byte[] tmp = new byte[4];
        int offset;
        if (version == VERSION_MPEG1) {
            if (mode == MODE_SINGLE_CHANNEL) {
                offset = 21 - 4;
            } else {
                offset = 36 - 4;
            }
        } else {
            if (mode == MODE_SINGLE_CHANNEL) {
                offset = 13 - 4;
            } else {
                offset = 21 - 4;
            }
        }
        try {
            System.arraycopy(firstFrame, offset, tmp, 0, 4);
            if ("Xing".equals(new String(tmp))) {
                vbr = true;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException("Corrupt Xing VBR header");
        }
        offset = 36 - 4;
        try {
            System.arraycopy(firstFrame, offset, tmp, 0, 4);
            if ("VBRI".equals(new String(tmp))) {
                vbr = true;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException("Corrupt VBRI VBR header");
        }
    }

    int version() {
        return version;
    }

    int sampleFrequency() {
        return sampleFrequency;
    }

    public int frequency() {
        return FREQUENCIES[version][sampleFrequency];
    }

    public int mode() {
        return mode;
    }

    int slots() {
        return slots;
    }

    boolean vbr() {
        return vbr;
    }

    int modeExtension() {
        return modeExtension;
    }

    private void calculateFramesize() {
        frameSize = (144 * Constants.BITRATES[version][bitrateIndex])
                / frequency();
        if (version == VERSION_MPEG2_LSF || version == VERSION_MPEG25_LSF) {
            frameSize >>= 1;
        }
        if (paddingBit) {
            frameSize++;
        }
        // subtract header size
        frameSize -= 4;
        // side info size, crc size, header sidze
        if (version == VERSION_MPEG1) {
            slots = (mode == MODE_SINGLE_CHANNEL) ? 17 : 32;
        } else {
            slots = (mode == MODE_SINGLE_CHANNEL) ? 9 : 17;
        }
        slots = frameSize - slots - (protectionBit ? 0 : 2);
    }
}

class Decoder {
    public static final int BUFFER_SIZE = 2 * 1152;
    public static final int MAX_CHANNELS = 2;
    private static final boolean BENCHMARK = false;
    
    protected final int[] bufferPointer = new int[MAX_CHANNELS];
    protected int channels;
    private SynthesisFilter filter1;
    private SynthesisFilter filter2;
    private Layer3Decoder l3decoder;
    private boolean initialized;

    private SourceDataLine line;
    private final byte[] buffer = new byte[BUFFER_SIZE * 2];
    private boolean stop;
    private volatile boolean pause;
	public FloatControl volume;

    public void decodeFrame(Header header, Bitstream stream) throws IOException {
        
    	if (!initialized) {
            double scaleFactor = 32700.0f;
            int mode = header.mode();
            int channels = mode == Header.MODE_SINGLE_CHANNEL ? 1 : 2;
            filter1 = new SynthesisFilter(0, scaleFactor);
            if (channels == 2) {
                filter2 = new SynthesisFilter(1, scaleFactor);
            }
            initialized = true;
        }
        if (l3decoder == null) {
            l3decoder = new Layer3Decoder(stream, header, filter1, filter2,
                    this);
        }
        l3decoder.decodeFrame();
        writeBuffer();
    }

    protected void initOutputBuffer(SourceDataLine line, int numberOfChannels) {
        this.line = line;
        channels = numberOfChannels;
        for (int i = 0; i < channels; i++) {
            bufferPointer[i] = i + i;
        }
    }

    public void appendSamples(int channel, double[] f) {
        int p = bufferPointer[channel];
        for (int i = 0; i < 32; i++) {
            double sample = f[i];
            int s = (int) ((sample > 32767.0f) ? 32767 : ((sample < -32768.0f) ? -32768 : sample));
            buffer[p] = (byte) (s >> 8);
            buffer[p + 1] = (byte) (s & 0xff);
            p += 4;
        }
        bufferPointer[channel] = p;
    }

    protected void writeBuffer() throws IOException {
        if (line != null) {
            line.write(buffer, 0, bufferPointer[0]);
        }
        for (int i = 0; i < channels; i++) {
            bufferPointer[i] = i + i;
        }
    }

    public void play(String name, InputStream in) throws IOException {
        stop = false;
        int frameCount = Integer.MAX_VALUE;

        // int testing;
        // frameCount = 100;

        Decoder decoder = new Decoder();
        Bitstream stream = new Bitstream(in);
        SourceDataLine line = null;
        
        int error = 0;
        for (int frame = 0; !stop && frame < frameCount; frame++) {
            if (pause) {
                line.stop();
                while (pause && !stop) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
                line.flush();
                line.start();
            }
            try {
                Header header = stream.readFrame();
                if (header == null) {
                    break;
                }
                if (decoder.channels == 0) {
                    int channels = (header.mode() == Header.MODE_SINGLE_CHANNEL) ? 1 : 2;
                    float sampleRate = header.frequency();
                    int sampleSize = 16;
                    AudioFormat format = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED, sampleRate,
                            sampleSize, channels, channels * (sampleSize / 8),
                            sampleRate, true);
                    // big endian
                    SourceDataLine.Info info = new DataLine.Info(
                            SourceDataLine.class, format);
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    
                    if (BENCHMARK) {
                        decoder.initOutputBuffer(null, channels);
                    } else {
                        decoder.initOutputBuffer(line, channels);
                    }
                    // TODO sometimes the line can not be opened (maybe not enough system resources?): display error message
                    // System.out.println(line.getFormat().toString());
                    line.open(format);
                    line.start();
                    volume = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                    volume.setValue(-10F);
                }
                while (line.available() < 100) {
                    Thread.yield();
                    Thread.sleep(200);
                }
                decoder.decodeFrame(header, stream);
            } catch (Exception e) {
                if (error++ > 1000) {
                    break;
                }
                // TODO should not write directly
                System.out.println("Error at: " + name + " Frame: " + frame + " Error: " + e.toString());
                // e.printStackTrace();
            } finally {
                stream.closeFrame();
            }
        }
        if (error > 0) {
            System.out.println("errors: " + error);
        }
        in.close();
        if (line != null) {
            line.stop();
            line.close();
            line = null;
        }
    }

    public void stop() {
        this.stop = true;
    }
    
    public boolean pause() {
        this.pause = !pause;
        return pause;
    }

}

final class Bitstream {
    /**
     * Synchronization control constant for the initial synchronization to the start of a frame.
     */
    static final byte INITIAL_SYNC = 0;
    /**
     * Synchronization control constant for non-initial frame synchronizations.
     */
    static final byte STRICT_SYNC = 1;
    /**
     * Maximum size of the frame buffer. max. 1730 bytes per frame: 144 * 384kbit/s / 32000 Hz + 2 Bytes CRC
     */
    private static final int BUFFER_INT_SIZE = 433;
    /**
     * The frame buffer that holds the data for the current frame.
     */
    private final int[] frameBuffer = new int[BUFFER_INT_SIZE];
    /**
     * Number of valid bytes in the frame buffer.
     */
    private int frameSize;
    /**
     * The bytes read from the stream.
     */
    private final byte[] frameBytes = new byte[BUFFER_INT_SIZE * 4];
    // Index into frameBuffer where the next bits are retrieved.
    private int wordPointer;
    /**
     * Number (0-31, from MSB to LSB) of next bit for getBits()
     */
    private int bitIndex;
    private int syncWord;
    private boolean singleChMode;
    private static final int[] BITMASK = {
            0, // dummy
            0x00000001, 0x00000003, 0x00000007, 0x0000000F, 0x0000001F, 0x0000003F, 0x0000007F, 0x000000FF, 0x000001FF, 0x000003FF, 0x000007FF, 0x00000FFF, 0x00001FFF, 0x00003FFF,
            0x00007FFF, 0x0000FFFF, 0x0001FFFF };
    private final PushbackInputStream source;
    private final Header header = new Header();
    private final byte[] syncBuffer = new byte[4];
    private byte[] rawID3v2 = null;
    private boolean firstFrame = true;

    public Bitstream(InputStream in) {
        source = new PushbackInputStream(in, BUFFER_INT_SIZE * 4);
        loadID3v2();
        closeFrame();
    }

    private void loadID3v2() {
        int size = -1;
        try {
            // read ID3v2 header
            source.mark(10);
            size = readID3v2Header();
        } catch (IOException e) {
            // ignore
        } finally {
            try {
                // unread ID3v2 header
                source.reset();
            } catch (IOException e) {
                // ignore
            }
        }
        // load ID3v2 tags.
        try {
            if (size > 0) {
                rawID3v2 = new byte[size];
                this.readBytes(rawID3v2, 0, rawID3v2.length);
            }
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Parse ID3v2 tag header to find out size of ID3v2 frames.
     * 
     * @param in MP3 InputStream
     * @return size of ID3v2 frames + header
     * @throws IOException
     * @author JavaZOOM
     */
    private int readID3v2Header() throws IOException {
        byte[] buff = new byte[4];
        int size = -10;
        readBytes(buff, 0, 3);
        if (buff[0] == 'I' && buff[1] == 'D' && buff[2] == '3') {
            readBytes(buff, 0, 3);
            readBytes(buff, 0, 4);
            size = (buff[0] << 21) + (buff[1] << 14) + (buff[2] << 7) + buff[3];
        }
        return size + 10;
    }

    /**
     * Reads and parses the next frame from the input source.
     * 
     * @return the Header describing details of the frame read, or null if the end of the stream has been reached.
     * @throws IOException
     */
    public Header readFrame() throws IOException {
        try {
            Header result = readNextFrame();
            if (firstFrame) {
                result.parseVBR(frameBytes);
                firstFrame = false;
            }
            return result;
        } catch (EOFException e) {
            return null;
        }
    }

    private Header readNextFrame() throws IOException {
        if (frameSize == -1) {
            while (true) {
                boolean ok = header.readHeader(this);
                if (ok) {
                    break;
                }
                closeFrame();
            }
        }
        return header;
    }

    void unreadFrame() throws IOException {
        if (wordPointer == -1 && bitIndex == -1 && frameSize > 0) {
            source.unread(frameBytes, 0, frameSize);
        }
    }

    public void closeFrame() {
        frameSize = -1;
        wordPointer = -1;
        bitIndex = -1;
    }

    /**
     * Determines if the next 4 bytes of the stream represent a frame header.
     */
    boolean isSyncCurrentPosition(int syncMode) throws IOException {
        int read = readBytes(syncBuffer, 0, 4);
        int headerString = ((syncBuffer[0] << 24) & 0xFF000000) | ((syncBuffer[1] << 16) & 0x00FF0000) | ((syncBuffer[2] << 8) & 0x0000FF00) | ((syncBuffer[3] << 0) & 0x000000FF);
        try {
            source.unread(syncBuffer, 0, read);
        } catch (IOException ex) {
            // ignore
        }
        if (read == 0) {
            return true;
        } else if (read == 4) {
            return isSyncMark(headerString, syncMode, syncWord);
        } else {
            return false;
        }
    }

    /**
     * Get next 32 bits from bitstream. They are stored in the headerstring. syncmod allows Synchro flag ID The returned
     * value is False at the end of stream.
     * 
     * @param syncMode
     */
    int syncHeader(byte syncMode) throws IOException {
        boolean sync;
        int headerString;
        // read additional 2 bytes
        int bytesRead = readBytes(syncBuffer, 0, 3);
        if (bytesRead != 3) {
            throw new EOFException();
        }
        headerString = ((syncBuffer[0] << 16) & 0x00FF0000) | ((syncBuffer[1] << 8) & 0x0000FF00) | ((syncBuffer[2] << 0) & 0x000000FF);
        do {
            headerString <<= 8;
            if (readBytes(syncBuffer, 3, 1) != 1) {
                throw new EOFException();
            }
            headerString |= (syncBuffer[3] & 0x000000FF);
            sync = isSyncMark(headerString, syncMode, syncWord);
        } while (!sync);
        return headerString;
    }

    private boolean isSyncMark(int headerString, int syncMode, int word) {
        boolean sync = false;
        if (syncMode == INITIAL_SYNC) {
            sync = ((headerString & 0xFFE00000) == 0xFFE00000); // SZD: MPEG 2.5
        } else {
            sync = ((headerString & 0xFFF80C00) == word) && (((headerString & 0x000000C0) == 0x000000C0) == singleChMode);
        }
        // filter out invalid sample rate
        if (sync) {
            sync = (((headerString >>> 10) & 3) != 3);
        }
        // filter out invalid layer
        if (sync) {
            sync = (((headerString >>> 17) & 3) != 0);
        }
        // filter out invalid version
        if (sync) {
            sync = (((headerString >>> 19) & 3) != 1);
        }
        return sync;
    }

    /**
     * Reads the data for the next frame. The frame is not parsed until parse frame is called.
     */
    int readFrameData(int byteSize) throws IOException {
        int numread = 0;
        numread = readFully(frameBytes, 0, byteSize);
        frameSize = byteSize;
        wordPointer = -1;
        bitIndex = -1;
        return numread;
    }

    /**
     * Parses the data previously read with readFrameData().
     */
    void parseFrame() {
        // Convert bytes read to int
        int b = 0;
        byte[] byteRead = frameBytes;
        int byteSize = frameSize;
        for (int k = 0; k < byteSize; k = k + 4) {
            byte b0 = 0;
            byte b1 = 0;
            byte b2 = 0;
            byte b3 = 0;
            b0 = byteRead[k];
            if (k + 1 < byteSize) {
                b1 = byteRead[k + 1];
            }
            if (k + 2 < byteSize) {
                b2 = byteRead[k + 2];
            }
            if (k + 3 < byteSize) {
                b3 = byteRead[k + 3];
            }
            frameBuffer[b++] = ((b0 << 24) & 0xFF000000) | ((b1 << 16) & 0x00FF0000) | ((b2 << 8) & 0x0000FF00) | (b3 & 0x000000FF);
        }
        wordPointer = 0;
        bitIndex = 0;
    }

    /**
     * Read bits from buffer into the lower bits of an unsigned int. The LSB contains the latest read bit of the stream.
     * (1 <= numberOfBits <= 16)
     */
    int getBits(int numberOfBits) {
        int returnValue = 0;
        int sum = bitIndex + numberOfBits;
        // TODO There is a problem here, wordpointer could be -1 ?!
        if (wordPointer < 0) {
            System.out.println("wordPointer < 0");
            wordPointer = 0;
        }
        if (sum <= 32) {
            // all bits contained in *wordpointer
            returnValue = (frameBuffer[wordPointer] >>> (32 - sum)) & BITMASK[numberOfBits];
            bitIndex += numberOfBits;
            if (bitIndex == 32) {
                bitIndex = 0;
                wordPointer++;
            }
            return returnValue;
        }
        int right = (frameBuffer[wordPointer] & 0x0000FFFF);
        wordPointer++;
        int left = (frameBuffer[wordPointer] & 0xFFFF0000);
        returnValue = ((right << 16) & 0xFFFF0000) | ((left >>> 16) & 0x0000FFFF);
        returnValue >>>= 48 - sum;
        returnValue &= BITMASK[numberOfBits];
        bitIndex = sum - 32;
        return returnValue;
    }

    /**
     * Set the word we want to sync the header to. In Big-Endian byte order
     */
    void setSyncWord(int s) {
        syncWord = s & 0xFFFFFF3F;
        singleChMode = ((s & 0x000000C0) == 0x000000C0);
    }

    /**
     * Reads the exact number of bytes from the source input stream into a byte array.
     * 
     * @param b The byte array to read the specified number of bytes into.
     * @param offs The index in the array where the first byte read should be stored.
     * @param len the number of bytes to read.
     * 
     * @exception Exception is thrown if the specified number of bytes could not be read from the stream.
     */
    private int readFully(byte[] b, int offs, int len) throws IOException {
        // TODO does not in fact throw an exception, probably return not required
        int read = 0;
        while (len > 0) {
            int bytesRead = source.read(b, offs, len);
            if (bytesRead == -1) {
                while (len-- > 0) {
                    b[offs++] = 0;
                }
                break;
            }
            read = read + bytesRead;
            offs += bytesRead;
            len -= bytesRead;
        }
        return read;
    }

    /**
     * Simlar to readFully, but doesn't throw exception when EOF is reached.
     */
    private int readBytes(byte[] b, int offs, int len) throws IOException {
        int totalBytesRead = 0;
        while (len > 0) {
            int bytesRead = source.read(b, offs, len);
            if (bytesRead == -1) {
                break;
            }
            totalBytesRead += bytesRead;
            offs += bytesRead;
            len -= bytesRead;
        }
        return totalBytesRead;
    }
}

class BitReservoir {

    private static final int BUFFER_SIZE = 4096 * 8;
    private static final int BUFFER_SIZE_MASK = BUFFER_SIZE - 1;
    private int offset, bitCount, bufferIndex;
    private final int[] buffer = new int[BUFFER_SIZE];

    int getBitCount() {
        return bitCount;
    }

    int getBits(int n) {
        bitCount += n;
        int val = 0;
        int pos = bufferIndex;
        if (pos + n < BUFFER_SIZE) {
            while (n-- > 0) {
                val <<= 1;
                val |= ((buffer[pos++] != 0) ? 1 : 0);
            }
        } else {
            while (n-- > 0) {
                val <<= 1;
                val |= ((buffer[pos] != 0) ? 1 : 0);
                pos = (pos + 1) & BUFFER_SIZE_MASK;
            }
        }
        bufferIndex = pos;
        return val;
    }

    int getOneBit() {
        bitCount++;
        int val = buffer[bufferIndex];
        bufferIndex = (bufferIndex + 1) & BUFFER_SIZE_MASK;
        return val;
    }

    void putByte(int val) {
        int ofs = offset;
        buffer[ofs++] = val & 0x80;
        buffer[ofs++] = val & 0x40;
        buffer[ofs++] = val & 0x20;
        buffer[ofs++] = val & 0x10;
        buffer[ofs++] = val & 0x08;
        buffer[ofs++] = val & 0x04;
        buffer[ofs++] = val & 0x02;
        buffer[ofs++] = val & 0x01;
        if (ofs == BUFFER_SIZE) {
            offset = 0;
        } else {
            offset = ofs;
        }
    }

    void rewindBits(int n) {
        bitCount -= n;
        bufferIndex -= n;
        if (bufferIndex < 0) {
            bufferIndex += BUFFER_SIZE;
        }
    }

    void rewindBytes(int n) {
        int bits = (n << 3);
        bitCount -= bits;
        bufferIndex -= bits;
        if (bufferIndex < 0) {
            bufferIndex += BUFFER_SIZE;
        }
    }

}

class Constants {
    static final double[] POW2 = new double[256];
    static {
        for (int i = 0; i < POW2.length; i++) {
            POW2[i] = Math.pow(2.0, (0.25 * (i - 210.0)));
        }
    }
    private static final double[] D = { 0.0f, -4.42505E-4f, 0.003250122f, -0.007003784f, 0.031082153f, -0.07862854f,
            0.10031128f, -0.57203674f, 1.144989f, 0.57203674f, 0.10031128f, 0.07862854f, 0.031082153f, 0.007003784f,
            0.003250122f, 4.42505E-4f, -1.5259E-5f, -4.73022E-4f, 0.003326416f, -0.007919312f, 0.030517578f,
            -0.08418274f, 0.090927124f, -0.6002197f, 1.1442871f, 0.54382324f, 0.1088562f, 0.07305908f, 0.03147888f,
            0.006118774f, 0.003173828f, 3.96729E-4f, -1.5259E-5f, -5.34058E-4f, 0.003387451f, -0.008865356f,
            0.029785156f, -0.08970642f, 0.08068848f, -0.6282959f, 1.1422119f, 0.51560974f, 0.11657715f, 0.06752014f,
            0.03173828f, 0.0052948f, 0.003082275f, 3.66211E-4f, -1.5259E-5f, -5.79834E-4f, 0.003433228f, -0.009841919f,
            0.028884888f, -0.09516907f, 0.06959534f, -0.6562195f, 1.1387634f, 0.48747253f, 0.12347412f, 0.06199646f,
            0.031845093f, 0.004486084f, 0.002990723f, 3.20435E-4f, -1.5259E-5f, -6.2561E-4f, 0.003463745f,
            -0.010848999f, 0.027801514f, -0.10054016f, 0.057617188f, -0.6839142f, 1.1339264f, 0.45947266f, 0.12957764f,
            0.056533813f, 0.031814575f, 0.003723145f, 0.00289917f, 2.89917E-4f, -1.5259E-5f, -6.86646E-4f,
            0.003479004f, -0.011886597f, 0.026535034f, -0.1058197f, 0.044784546f, -0.71131897f, 1.1277466f,
            0.43165588f, 0.1348877f, 0.051132202f, 0.031661987f, 0.003005981f, 0.002792358f, 2.59399E-4f, -1.5259E-5f,
            -7.47681E-4f, 0.003479004f, -0.012939453f, 0.02508545f, -0.110946655f, 0.031082153f, -0.7383728f,
            1.120224f, 0.40408325f, 0.13945007f, 0.045837402f, 0.03138733f, 0.002334595f, 0.002685547f, 2.44141E-4f,
            -3.0518E-5f, -8.08716E-4f, 0.003463745f, -0.014022827f, 0.023422241f, -0.11592102f, 0.01651001f,
            -0.7650299f, 1.1113739f, 0.37680054f, 0.14326477f, 0.040634155f, 0.03100586f, 0.001693726f, 0.002578735f,
            2.13623E-4f, -3.0518E-5f, -8.8501E-4f, 0.003417969f, -0.01512146f, 0.021575928f, -0.12069702f,
            0.001068115f, -0.791214f, 1.1012115f, 0.34986877f, 0.1463623f, 0.03555298f, 0.030532837f, 0.001098633f,
            0.002456665f, 1.98364E-4f, -3.0518E-5f, -9.61304E-4f, 0.003372192f, -0.016235352f, 0.01953125f,
            -0.1252594f, -0.015228271f, -0.816864f, 1.0897827f, 0.32331848f, 0.1487732f, 0.03060913f, 0.029937744f,
            5.49316E-4f, 0.002349854f, 1.67847E-4f, -3.0518E-5f, -0.001037598f, 0.00328064f, -0.017349243f,
            0.01725769f, -0.12956238f, -0.03237915f, -0.84194946f, 1.0771179f, 0.2972107f, 0.15049744f, 0.025817871f,
            0.029281616f, 3.0518E-5f, 0.002243042f, 1.52588E-4f, -4.5776E-5f, -0.001113892f, 0.003173828f,
            -0.018463135f, 0.014801025f, -0.1335907f, -0.050354004f, -0.8663635f, 1.0632172f, 0.2715912f, 0.15159607f,
            0.0211792f, 0.028533936f, -4.42505E-4f, 0.002120972f, 1.37329E-4f, -4.5776E-5f, -0.001205444f,
            0.003051758f, -0.019577026f, 0.012115479f, -0.13729858f, -0.06916809f, -0.89009094f, 1.0481567f,
            0.24650574f, 0.15206909f, 0.016708374f, 0.02772522f, -8.69751E-4f, 0.00201416f, 1.2207E-4f, -6.1035E-5f,
            -0.001296997f, 0.002883911f, -0.020690918f, 0.009231567f, -0.14067078f, -0.088775635f, -0.9130554f,
            1.0319366f, 0.22198486f, 0.15196228f, 0.012420654f, 0.02684021f, -0.001266479f, 0.001907349f, 1.06812E-4f,
            -6.1035E-5f, -0.00138855f, 0.002700806f, -0.02178955f, 0.006134033f, -0.14367676f, -0.10916138f,
            -0.9351959f, 1.0146179f, 0.19805908f, 0.15130615f, 0.00831604f, 0.025909424f, -0.001617432f, 0.001785278f,
            1.06812E-4f, -7.6294E-5f, -0.001480103f, 0.002487183f, -0.022857666f, 0.002822876f, -0.1462555f,
            -0.13031006f, -0.95648193f, 0.99624634f, 0.17478943f, 0.15011597f, 0.004394531f, 0.024932861f,
            -0.001937866f, 0.001693726f, 9.1553E-5f, -7.6294E-5f, -0.001586914f, 0.002227783f, -0.023910522f,
            -6.86646E-4f, -0.14842224f, -0.15220642f, -0.9768524f, 0.9768524f, 0.15220642f, 0.14842224f, 6.86646E-4f,
            0.023910522f, -0.002227783f, 0.001586914f, 7.6294E-5f, -9.1553E-5f, -0.001693726f, 0.001937866f,
            -0.024932861f, -0.004394531f, -0.15011597f, -0.17478943f, -0.99624634f, 0.95648193f, 0.13031006f,
            0.1462555f, -0.002822876f, 0.022857666f, -0.002487183f, 0.001480103f, 7.6294E-5f, -1.06812E-4f,
            -0.001785278f, 0.001617432f, -0.025909424f, -0.00831604f, -0.15130615f, -0.19805908f, -1.0146179f,
            0.9351959f, 0.10916138f, 0.14367676f, -0.006134033f, 0.02178955f, -0.002700806f, 0.00138855f, 6.1035E-5f,
            -1.06812E-4f, -0.001907349f, 0.001266479f, -0.02684021f, -0.012420654f, -0.15196228f, -0.22198486f,
            -1.0319366f, 0.9130554f, 0.088775635f, 0.14067078f, -0.009231567f, 0.020690918f, -0.002883911f,
            0.001296997f, 6.1035E-5f, -1.2207E-4f, -0.00201416f, 8.69751E-4f, -0.02772522f, -0.016708374f,
            -0.15206909f, -0.24650574f, -1.0481567f, 0.89009094f, 0.06916809f, 0.13729858f, -0.012115479f,
            0.019577026f, -0.003051758f, 0.001205444f, 4.5776E-5f, -1.37329E-4f, -0.002120972f, 4.42505E-4f,
            -0.028533936f, -0.0211792f, -0.15159607f, -0.2715912f, -1.0632172f, 0.8663635f, 0.050354004f, 0.1335907f,
            -0.014801025f, 0.018463135f, -0.003173828f, 0.001113892f, 4.5776E-5f, -1.52588E-4f, -0.002243042f,
            -3.0518E-5f, -0.029281616f, -0.025817871f, -0.15049744f, -0.2972107f, -1.0771179f, 0.84194946f,
            0.03237915f, 0.12956238f, -0.01725769f, 0.017349243f, -0.00328064f, 0.001037598f, 3.0518E-5f, -1.67847E-4f,
            -0.002349854f, -5.49316E-4f, -0.029937744f, -0.03060913f, -0.1487732f, -0.32331848f, -1.0897827f,
            0.816864f, 0.015228271f, 0.1252594f, -0.01953125f, 0.016235352f, -0.003372192f, 9.61304E-4f, 3.0518E-5f,
            -1.98364E-4f, -0.002456665f, -0.001098633f, -0.030532837f, -0.03555298f, -0.1463623f, -0.34986877f,
            -1.1012115f, 0.791214f, -0.001068115f, 0.12069702f, -0.021575928f, 0.01512146f, -0.003417969f, 8.8501E-4f,
            3.0518E-5f, -2.13623E-4f, -0.002578735f, -0.001693726f, -0.03100586f, -0.040634155f, -0.14326477f,
            -0.37680054f, -1.1113739f, 0.7650299f, -0.01651001f, 0.11592102f, -0.023422241f, 0.014022827f,
            -0.003463745f, 8.08716E-4f, 3.0518E-5f, -2.44141E-4f, -0.002685547f, -0.002334595f, -0.03138733f,
            -0.045837402f, -0.13945007f, -0.40408325f, -1.120224f, 0.7383728f, -0.031082153f, 0.110946655f,
            -0.02508545f, 0.012939453f, -0.003479004f, 7.47681E-4f, 1.5259E-5f, -2.59399E-4f, -0.002792358f,
            -0.003005981f, -0.031661987f, -0.051132202f, -0.1348877f, -0.43165588f, -1.1277466f, 0.71131897f,
            -0.044784546f, 0.1058197f, -0.026535034f, 0.011886597f, -0.003479004f, 6.86646E-4f, 1.5259E-5f,
            -2.89917E-4f, -0.00289917f, -0.003723145f, -0.031814575f, -0.056533813f, -0.12957764f, -0.45947266f,
            -1.1339264f, 0.6839142f, -0.057617188f, 0.10054016f, -0.027801514f, 0.010848999f, -0.003463745f,
            6.2561E-4f, 1.5259E-5f, -3.20435E-4f, -0.002990723f, -0.004486084f, -0.031845093f, -0.06199646f,
            -0.12347412f, -0.48747253f, -1.1387634f, 0.6562195f, -0.06959534f, 0.09516907f, -0.028884888f,
            0.009841919f, -0.003433228f, 5.79834E-4f, 1.5259E-5f, -3.66211E-4f, -0.003082275f, -0.0052948f,
            -0.03173828f, -0.06752014f, -0.11657715f, -0.51560974f, -1.1422119f, 0.6282959f, -0.08068848f, 0.08970642f,
            -0.029785156f, 0.008865356f, -0.003387451f, 5.34058E-4f, 1.5259E-5f, -3.96729E-4f, -0.003173828f,
            -0.006118774f, -0.03147888f, -0.07305908f, -0.1088562f, -0.54382324f, -1.1442871f, 0.6002197f,
            -0.090927124f, 0.08418274f, -0.030517578f, 0.007919312f, -0.003326416f, 4.73022E-4f, 1.5259E-5f };
    static final double[] TWO_TO_NEGATIVE_HALF_POW = { 1.0000000000E+00f, 7.0710678119E-01f, 5.0000000000E-01f,
            3.5355339059E-01f, 2.5000000000E-01f, 1.7677669530E-01f, 1.2500000000E-01f, 8.8388347648E-02f,
            6.2500000000E-02f, 4.4194173824E-02f, 3.1250000000E-02f, 2.2097086912E-02f, 1.5625000000E-02f,
            1.1048543456E-02f, 7.8125000000E-03f, 5.5242717280E-03f, 3.9062500000E-03f, 2.7621358640E-03f,
            1.9531250000E-03f, 1.3810679320E-03f, 9.7656250000E-04f, 6.9053396600E-04f, 4.8828125000E-04f,
            3.4526698300E-04f, 2.4414062500E-04f, 1.7263349150E-04f, 1.2207031250E-04f, 8.6316745750E-05f,
            6.1035156250E-05f, 4.3158372875E-05f, 3.0517578125E-05f, 2.1579186438E-05f, 1.5258789062E-05f,
            1.0789593219E-05f, 7.6293945312E-06f, 5.3947966094E-06f, 3.8146972656E-06f, 2.6973983047E-06f,
            1.9073486328E-06f, 1.3486991523E-06f, 9.5367431641E-07f, 6.7434957617E-07f, 4.7683715820E-07f,
            3.3717478809E-07f, 2.3841857910E-07f, 1.6858739404E-07f, 1.1920928955E-07f, 8.4293697022E-08f,
            5.9604644775E-08f, 4.2146848511E-08f, 2.9802322388E-08f, 2.1073424255E-08f, 1.4901161194E-08f,
            1.0536712128E-08f, 7.4505805969E-09f, 5.2683560639E-09f, 3.7252902985E-09f, 2.6341780319E-09f,
            1.8626451492E-09f, 1.3170890160E-09f, 9.3132257462E-10f, 6.5854450798E-10f, 4.6566128731E-10f,
            3.2927225399E-10f };
    static final double[] TAN12 = { 0.0f, 0.26794919f, 0.57735027f, 1.0f, 1.73205081f, 3.73205081f, 9.9999999e10f,
            -3.73205081f, -1.73205081f, -1.0f, -0.57735027f, -0.26794919f, 0.0f, 0.26794919f, 0.57735027f, 1.0f };
    static final int T43_SIZE = 8192;
    static final double[] T43 = new double[T43_SIZE];
    static {
        // DOUBLE
        double d43 = (4.0 / 3.0);
        for (int i = 0; i < T43_SIZE; i++) {
            T43[i] = Math.pow(i, d43);
        }
    }
    static final int[][][] NR_OF_SFB_BLOCK = { { { 6, 5, 5, 5 }, { 9, 9, 9, 9 }, { 6, 9, 9, 9 } },
            { { 6, 5, 7, 3 }, { 9, 9, 12, 6 }, { 6, 9, 12, 6 } },
            { { 11, 10, 0, 0 }, { 18, 18, 0, 0 }, { 15, 18, 0, 0 } },
            { { 7, 7, 7, 0 }, { 12, 12, 12, 0 }, { 6, 15, 12, 0 } },
            { { 6, 6, 6, 3 }, { 12, 9, 9, 6 }, { 6, 12, 9, 6 } }, { { 8, 8, 5, 0 }, { 15, 12, 9, 0 }, { 6, 18, 9, 0 } } };
    static final double[][] WIN = {
            { -1.6141214951E-02f, -5.3603178919E-02f, -1.0070713296E-01f, -1.6280817573E-01f, -4.9999999679E-01f,
                    -3.8388735032E-01f, -6.2061144372E-01f, -1.1659756083E+00f, -3.8720752656E+00f, -4.2256286556E+00f,
                    -1.5195289984E+00f, -9.7416483388E-01f, -7.3744074053E-01f, -1.2071067773E+00f, -5.1636156596E-01f,
                    -4.5426052317E-01f, -4.0715656898E-01f, -3.6969460527E-01f, -3.3876269197E-01f, -3.1242222492E-01f,
                    -2.8939587111E-01f, -2.6880081906E-01f, -5.0000000266E-01f, -2.3251417468E-01f, -2.1596714708E-01f,
                    -2.0004979098E-01f, -1.8449493497E-01f, -1.6905846094E-01f, -1.5350360518E-01f, -1.3758624925E-01f,
                    -1.2103922149E-01f, -2.0710679058E-01f, -8.4752577594E-02f, -6.4157525656E-02f, -4.1131172614E-02f,
                    -1.4790705759E-02f },
            { -1.6141214951E-02f, -5.3603178919E-02f, -1.0070713296E-01f, -1.6280817573E-01f, -4.9999999679E-01f,
                    -3.8388735032E-01f, -6.2061144372E-01f, -1.1659756083E+00f, -3.8720752656E+00f, -4.2256286556E+00f,
                    -1.5195289984E+00f, -9.7416483388E-01f, -7.3744074053E-01f, -1.2071067773E+00f, -5.1636156596E-01f,
                    -4.5426052317E-01f, -4.0715656898E-01f, -3.6969460527E-01f, -3.3908542600E-01f, -3.1511810350E-01f,
                    -2.9642226150E-01f, -2.8184548650E-01f, -5.4119610000E-01f, -2.6213228100E-01f, -2.5387916537E-01f,
                    -2.3296291359E-01f, -1.9852728987E-01f, -1.5233534808E-01f, -9.6496400054E-02f, -3.3423828516E-02f,
                    0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f,
                    0.0000000000E+00f },
            { -4.8300800645E-02f, -1.5715656932E-01f, -2.8325045177E-01f, -4.2953747763E-01f, -1.2071067795E+00f,
                    -8.2426483178E-01f, -1.1451749106E+00f, -1.7695290101E+00f, -4.5470225061E+00f, -3.4890531002E+00f,
                    -7.3296292804E-01f, -1.5076514758E-01f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f,
                    0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f,
                    0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f,
                    0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f,
                    0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f,
                    0.0000000000E+00f },
            { 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f,
                    0.0000000000E+00f, -1.5076513660E-01f, -7.3296291107E-01f, -3.4890530566E+00f, -4.5470224727E+00f,
                    -1.7695290031E+00f, -1.1451749092E+00f, -8.3137738100E-01f, -1.3065629650E+00f, -5.4142014250E-01f,
                    -4.6528974900E-01f, -4.1066990750E-01f, -3.7004680800E-01f, -3.3876269197E-01f, -3.1242222492E-01f,
                    -2.8939587111E-01f, -2.6880081906E-01f, -5.0000000266E-01f, -2.3251417468E-01f, -2.1596714708E-01f,
                    -2.0004979098E-01f, -1.8449493497E-01f, -1.6905846094E-01f, -1.5350360518E-01f, -1.3758624925E-01f,
                    -1.2103922149E-01f, -2.0710679058E-01f, -8.4752577594E-02f, -6.4157525656E-02f, -4.1131172614E-02f,
                    -1.4790705759E-02f } };
    static final double[] CA = { -0.5144957554270f, -0.4717319685650f, -0.3133774542040f, -0.1819131996110f,
            -0.0945741925262f, -0.0409655828852f, -0.0141985685725f, -0.00369997467375f };
    static final double[] CS = { 0.857492925712f, 0.881741997318f, 0.949628649103f, 0.983314592492f, 0.995517816065f,
            0.999160558175f, 0.999899195243f, 0.999993155067f };
    static final double[][] IO = {
            { 1.0000000000E+00f, 8.4089641526E-01f, 7.0710678119E-01f, 5.9460355751E-01f, 5.0000000001E-01f,
                    4.2044820763E-01f, 3.5355339060E-01f, 2.9730177876E-01f, 2.5000000001E-01f, 2.1022410382E-01f,
                    1.7677669530E-01f, 1.4865088938E-01f, 1.2500000000E-01f, 1.0511205191E-01f, 8.8388347652E-02f,
                    7.4325444691E-02f, 6.2500000003E-02f, 5.2556025956E-02f, 4.4194173826E-02f, 3.7162722346E-02f,
                    3.1250000002E-02f, 2.6278012978E-02f, 2.2097086913E-02f, 1.8581361173E-02f, 1.5625000001E-02f,
                    1.3139006489E-02f, 1.1048543457E-02f, 9.2906805866E-03f, 7.8125000006E-03f, 6.5695032447E-03f,
                    5.5242717285E-03f, 4.6453402934E-03f },
            { 1.0000000000E+00f, 7.0710678119E-01f, 5.0000000000E-01f, 3.5355339060E-01f, 2.5000000000E-01f,
                    1.7677669530E-01f, 1.2500000000E-01f, 8.8388347650E-02f, 6.2500000001E-02f, 4.4194173825E-02f,
                    3.1250000001E-02f, 2.2097086913E-02f, 1.5625000000E-02f, 1.1048543456E-02f, 7.8125000002E-03f,
                    5.5242717282E-03f, 3.9062500001E-03f, 2.7621358641E-03f, 1.9531250001E-03f, 1.3810679321E-03f,
                    9.7656250004E-04f, 6.9053396603E-04f, 4.8828125002E-04f, 3.4526698302E-04f, 2.4414062501E-04f,
                    1.7263349151E-04f, 1.2207031251E-04f, 8.6316745755E-05f, 6.1035156254E-05f, 4.3158372878E-05f,
                    3.0517578127E-05f, 2.1579186439E-05f } };
    static final int[] PRETAB = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3, 3, 2, 0 };
    static final int[][] SLEN = { { 0, 0, 0, 0, 3, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4 },
            { 0, 1, 2, 3, 0, 1, 2, 3, 1, 2, 3, 1, 2, 3, 2, 3 } };
    static final int[][] VAL_TAB_0 = { { 0, 0 } // dummy
    };
    static final int[][] VAL_TAB_1 = { { 2, 1 }, { 0, 0 }, { 2, 1 }, { 0, 16 }, { 2, 1 }, { 0, 1 }, { 0, 17 }, };
    static final int[][] VAL_TAB_2 = { { 2, 1 }, { 0, 0 }, { 4, 1 }, { 2, 1 }, { 0, 16 }, { 0, 1 }, { 2, 1 },
            { 0, 17 }, { 4, 1 }, { 2, 1 }, { 0, 32 }, { 0, 33 }, { 2, 1 }, { 0, 18 }, { 2, 1 }, { 0, 2 }, { 0, 34 }, };
    static final int[][] VAL_TAB_3 = { { 4, 1 }, { 2, 1 }, { 0, 0 }, { 0, 1 }, { 2, 1 }, { 0, 17 }, { 2, 1 },
            { 0, 16 }, { 4, 1 }, { 2, 1 }, { 0, 32 }, { 0, 33 }, { 2, 1 }, { 0, 18 }, { 2, 1 }, { 0, 2 }, { 0, 34 }, };
    static final int[][] VAL_TAB_4 = { { 0, 0 } }; // dummy
    static final int[][] VAL_TAB_5 = { { 2, 1 }, { 0, 0 }, { 4, 1 }, { 2, 1 }, { 0, 16 }, { 0, 1 }, { 2, 1 },
            { 0, 17 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 32 }, { 0, 2 }, { 2, 1 }, { 0, 33 }, { 0, 18 }, { 8, 1 },
            { 4, 1 }, { 2, 1 }, { 0, 34 }, { 0, 48 }, { 2, 1 }, { 0, 3 }, { 0, 19 }, { 2, 1 }, { 0, 49 }, { 2, 1 },
            { 0, 50 }, { 2, 1 }, { 0, 35 }, { 0, 51 }, };
    static final int[][] VAL_TAB_6 = { { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 0 }, { 0, 16 }, { 0, 17 }, { 6, 1 },
            { 2, 1 }, { 0, 1 }, { 2, 1 }, { 0, 32 }, { 0, 33 }, { 6, 1 }, { 2, 1 }, { 0, 18 }, { 2, 1 }, { 0, 2 },
            { 0, 34 }, { 4, 1 }, { 2, 1 }, { 0, 49 }, { 0, 19 }, { 4, 1 }, { 2, 1 }, { 0, 48 }, { 0, 50 }, { 2, 1 },
            { 0, 35 }, { 2, 1 }, { 0, 3 }, { 0, 51 }, };
    static final int[][] VAL_TAB_7 = { { 2, 1 }, { 0, 0 }, { 4, 1 }, { 2, 1 }, { 0, 16 }, { 0, 1 }, { 8, 1 }, { 2, 1 },
            { 0, 17 }, { 4, 1 }, { 2, 1 }, { 0, 32 }, { 0, 2 }, { 0, 33 }, { 18, 1 }, { 6, 1 }, { 2, 1 }, { 0, 18 },
            { 2, 1 }, { 0, 34 }, { 0, 48 }, { 4, 1 }, { 2, 1 }, { 0, 49 }, { 0, 19 }, { 4, 1 }, { 2, 1 }, { 0, 3 },
            { 0, 50 }, { 2, 1 }, { 0, 35 }, { 0, 4 }, { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 64 }, { 0, 65 }, { 2, 1 },
            { 0, 20 }, { 2, 1 }, { 0, 66 }, { 0, 36 }, { 12, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 51 }, { 0, 67 },
            { 0, 80 }, { 4, 1 }, { 2, 1 }, { 0, 52 }, { 0, 5 }, { 0, 81 }, { 6, 1 }, { 2, 1 }, { 0, 21 }, { 2, 1 },
            { 0, 82 }, { 0, 37 }, { 4, 1 }, { 2, 1 }, { 0, 68 }, { 0, 53 }, { 4, 1 }, { 2, 1 }, { 0, 83 }, { 0, 84 },
            { 2, 1 }, { 0, 69 }, { 0, 85 }, };
    static final int[][] VAL_TAB_8 = { { 6, 1 }, { 2, 1 }, { 0, 0 }, { 2, 1 }, { 0, 16 }, { 0, 1 }, { 2, 1 },
            { 0, 17 }, { 4, 1 }, { 2, 1 }, { 0, 33 }, { 0, 18 }, { 14, 1 }, { 4, 1 }, { 2, 1 }, { 0, 32 }, { 0, 2 },
            { 2, 1 }, { 0, 34 }, { 4, 1 }, { 2, 1 }, { 0, 48 }, { 0, 3 }, { 2, 1 }, { 0, 49 }, { 0, 19 }, { 14, 1 },
            { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 50 }, { 0, 35 }, { 2, 1 }, { 0, 64 }, { 0, 4 }, { 2, 1 }, { 0, 65 },
            { 2, 1 }, { 0, 20 }, { 0, 66 }, { 12, 1 }, { 6, 1 }, { 2, 1 }, { 0, 36 }, { 2, 1 }, { 0, 51 }, { 0, 80 },
            { 4, 1 }, { 2, 1 }, { 0, 67 }, { 0, 52 }, { 0, 81 }, { 6, 1 }, { 2, 1 }, { 0, 21 }, { 2, 1 }, { 0, 5 },
            { 0, 82 }, { 6, 1 }, { 2, 1 }, { 0, 37 }, { 2, 1 }, { 0, 68 }, { 0, 53 }, { 2, 1 }, { 0, 83 }, { 2, 1 },
            { 0, 69 }, { 2, 1 }, { 0, 84 }, { 0, 85 }, };
    static final int[][] VAL_TAB_9 = { { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 0 }, { 0, 16 }, { 2, 1 }, { 0, 1 },
            { 0, 17 }, { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 32 }, { 0, 33 }, { 2, 1 }, { 0, 18 }, { 2, 1 }, { 0, 2 },
            { 0, 34 }, { 12, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 48 }, { 0, 3 }, { 0, 49 }, { 2, 1 }, { 0, 19 },
            { 2, 1 }, { 0, 50 }, { 0, 35 }, { 12, 1 }, { 4, 1 }, { 2, 1 }, { 0, 65 }, { 0, 20 }, { 4, 1 }, { 2, 1 },
            { 0, 64 }, { 0, 51 }, { 2, 1 }, { 0, 66 }, { 0, 36 }, { 10, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 4 },
            { 0, 80 }, { 0, 67 }, { 2, 1 }, { 0, 52 }, { 0, 81 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 21 }, { 0, 82 },
            { 2, 1 }, { 0, 37 }, { 0, 68 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 5 }, { 0, 84 }, { 0, 83 }, { 2, 1 },
            { 0, 53 }, { 2, 1 }, { 0, 69 }, { 0, 85 }, };
    static final int[][] VAL_TAB_10 = { { 2, 1 }, { 0, 0 }, { 4, 1 }, { 2, 1 }, { 0, 16 }, { 0, 1 }, { 10, 1 },
            { 2, 1 }, { 0, 17 }, { 4, 1 }, { 2, 1 }, { 0, 32 }, { 0, 2 }, { 2, 1 }, { 0, 33 }, { 0, 18 }, { 28, 1 },
            { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 34 }, { 0, 48 }, { 2, 1 }, { 0, 49 }, { 0, 19 }, { 8, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 3 }, { 0, 50 }, { 2, 1 }, { 0, 35 }, { 0, 64 }, { 4, 1 }, { 2, 1 }, { 0, 65 }, { 0, 20 },
            { 4, 1 }, { 2, 1 }, { 0, 4 }, { 0, 51 }, { 2, 1 }, { 0, 66 }, { 0, 36 }, { 28, 1 }, { 10, 1 }, { 6, 1 },
            { 4, 1 }, { 2, 1 }, { 0, 80 }, { 0, 5 }, { 0, 96 }, { 2, 1 }, { 0, 97 }, { 0, 22 }, { 12, 1 }, { 6, 1 },
            { 4, 1 }, { 2, 1 }, { 0, 67 }, { 0, 52 }, { 0, 81 }, { 2, 1 }, { 0, 21 }, { 2, 1 }, { 0, 82 }, { 0, 37 },
            { 4, 1 }, { 2, 1 }, { 0, 38 }, { 0, 54 }, { 0, 113 }, { 20, 1 }, { 8, 1 }, { 2, 1 }, { 0, 23 }, { 4, 1 },
            { 2, 1 }, { 0, 68 }, { 0, 83 }, { 0, 6 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 53 }, { 0, 69 }, { 0, 98 },
            { 2, 1 }, { 0, 112 }, { 2, 1 }, { 0, 7 }, { 0, 100 }, { 14, 1 }, { 4, 1 }, { 2, 1 }, { 0, 114 }, { 0, 39 },
            { 6, 1 }, { 2, 1 }, { 0, 99 }, { 2, 1 }, { 0, 84 }, { 0, 85 }, { 2, 1 }, { 0, 70 }, { 0, 115 }, { 8, 1 },
            { 4, 1 }, { 2, 1 }, { 0, 55 }, { 0, 101 }, { 2, 1 }, { 0, 86 }, { 0, 116 }, { 6, 1 }, { 2, 1 }, { 0, 71 },
            { 2, 1 }, { 0, 102 }, { 0, 117 }, { 4, 1 }, { 2, 1 }, { 0, 87 }, { 0, 118 }, { 2, 1 }, { 0, 103 },
            { 0, 119 }, };
    static final int[][] VAL_TAB_11 = { { 6, 1 }, { 2, 1 }, { 0, 0 }, { 2, 1 }, { 0, 16 }, { 0, 1 }, { 8, 1 },
            { 2, 1 }, { 0, 17 }, { 4, 1 }, { 2, 1 }, { 0, 32 }, { 0, 2 }, { 0, 18 }, { 24, 1 }, { 8, 1 }, { 2, 1 },
            { 0, 33 }, { 2, 1 }, { 0, 34 }, { 2, 1 }, { 0, 48 }, { 0, 3 }, { 4, 1 }, { 2, 1 }, { 0, 49 }, { 0, 19 },
            { 4, 1 }, { 2, 1 }, { 0, 50 }, { 0, 35 }, { 4, 1 }, { 2, 1 }, { 0, 64 }, { 0, 4 }, { 2, 1 }, { 0, 65 },
            { 0, 20 }, { 30, 1 }, { 16, 1 }, { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 66 }, { 0, 36 }, { 4, 1 }, { 2, 1 },
            { 0, 51 }, { 0, 67 }, { 0, 80 }, { 4, 1 }, { 2, 1 }, { 0, 52 }, { 0, 81 }, { 0, 97 }, { 6, 1 }, { 2, 1 },
            { 0, 22 }, { 2, 1 }, { 0, 6 }, { 0, 38 }, { 2, 1 }, { 0, 98 }, { 2, 1 }, { 0, 21 }, { 2, 1 }, { 0, 5 },
            { 0, 82 }, { 16, 1 }, { 10, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 37 }, { 0, 68 }, { 0, 96 }, { 2, 1 },
            { 0, 99 }, { 0, 54 }, { 4, 1 }, { 2, 1 }, { 0, 112 }, { 0, 23 }, { 0, 113 }, { 16, 1 }, { 6, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 7 }, { 0, 100 }, { 0, 114 }, { 2, 1 }, { 0, 39 }, { 4, 1 }, { 2, 1 }, { 0, 83 }, { 0, 53 },
            { 2, 1 }, { 0, 84 }, { 0, 69 }, { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 70 }, { 0, 115 }, { 2, 1 }, { 0, 55 },
            { 2, 1 }, { 0, 101 }, { 0, 86 }, { 10, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 85 }, { 0, 87 }, { 0, 116 },
            { 2, 1 }, { 0, 71 }, { 0, 102 }, { 4, 1 }, { 2, 1 }, { 0, 117 }, { 0, 118 }, { 2, 1 }, { 0, 103 },
            { 0, 119 }, };
    static final int[][] VAL_TAB_12 = { { 12, 1 }, { 4, 1 }, { 2, 1 }, { 0, 16 }, { 0, 1 }, { 2, 1 }, { 0, 17 },
            { 2, 1 }, { 0, 0 }, { 2, 1 }, { 0, 32 }, { 0, 2 }, { 16, 1 }, { 4, 1 }, { 2, 1 }, { 0, 33 }, { 0, 18 },
            { 4, 1 }, { 2, 1 }, { 0, 34 }, { 0, 49 }, { 2, 1 }, { 0, 19 }, { 2, 1 }, { 0, 48 }, { 2, 1 }, { 0, 3 },
            { 0, 64 }, { 26, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 50 }, { 0, 35 }, { 2, 1 }, { 0, 65 }, { 0, 51 },
            { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 20 }, { 0, 66 }, { 2, 1 }, { 0, 36 }, { 2, 1 }, { 0, 4 }, { 0, 80 },
            { 4, 1 }, { 2, 1 }, { 0, 67 }, { 0, 52 }, { 2, 1 }, { 0, 81 }, { 0, 21 }, { 28, 1 }, { 14, 1 }, { 8, 1 },
            { 4, 1 }, { 2, 1 }, { 0, 82 }, { 0, 37 }, { 2, 1 }, { 0, 83 }, { 0, 53 }, { 4, 1 }, { 2, 1 }, { 0, 96 },
            { 0, 22 }, { 0, 97 }, { 4, 1 }, { 2, 1 }, { 0, 98 }, { 0, 38 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 5 },
            { 0, 6 }, { 0, 68 }, { 2, 1 }, { 0, 84 }, { 0, 69 }, { 18, 1 }, { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 99 },
            { 0, 54 }, { 4, 1 }, { 2, 1 }, { 0, 112 }, { 0, 7 }, { 0, 113 }, { 4, 1 }, { 2, 1 }, { 0, 23 }, { 0, 100 },
            { 2, 1 }, { 0, 70 }, { 0, 114 }, { 10, 1 }, { 6, 1 }, { 2, 1 }, { 0, 39 }, { 2, 1 }, { 0, 85 }, { 0, 115 },
            { 2, 1 }, { 0, 55 }, { 0, 86 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 101 }, { 0, 116 }, { 2, 1 }, { 0, 71 },
            { 0, 102 }, { 4, 1 }, { 2, 1 }, { 0, 117 }, { 0, 87 }, { 2, 1 }, { 0, 118 }, { 2, 1 }, { 0, 103 },
            { 0, 119 }, };
    static final int[][] VAL_TAB_13 = { { 2, 1 }, { 0, 0 }, { 6, 1 }, { 2, 1 }, { 0, 16 }, { 2, 1 }, { 0, 1 },
            { 0, 17 }, { 28, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 32 }, { 0, 2 }, { 2, 1 }, { 0, 33 }, { 0, 18 },
            { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 34 }, { 0, 48 }, { 2, 1 }, { 0, 3 }, { 0, 49 }, { 6, 1 }, { 2, 1 },
            { 0, 19 }, { 2, 1 }, { 0, 50 }, { 0, 35 }, { 4, 1 }, { 2, 1 }, { 0, 64 }, { 0, 4 }, { 0, 65 }, { 70, 1 },
            { 28, 1 }, { 14, 1 }, { 6, 1 }, { 2, 1 }, { 0, 20 }, { 2, 1 }, { 0, 51 }, { 0, 66 }, { 4, 1 }, { 2, 1 },
            { 0, 36 }, { 0, 80 }, { 2, 1 }, { 0, 67 }, { 0, 52 }, { 4, 1 }, { 2, 1 }, { 0, 81 }, { 0, 21 }, { 4, 1 },
            { 2, 1 }, { 0, 5 }, { 0, 82 }, { 2, 1 }, { 0, 37 }, { 2, 1 }, { 0, 68 }, { 0, 83 }, { 14, 1 }, { 8, 1 },
            { 4, 1 }, { 2, 1 }, { 0, 96 }, { 0, 6 }, { 2, 1 }, { 0, 97 }, { 0, 22 }, { 4, 1 }, { 2, 1 }, { 0, 128 },
            { 0, 8 }, { 0, 129 }, { 16, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 53 }, { 0, 98 }, { 2, 1 }, { 0, 38 },
            { 0, 84 }, { 4, 1 }, { 2, 1 }, { 0, 69 }, { 0, 99 }, { 2, 1 }, { 0, 54 }, { 0, 112 }, { 6, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 7 }, { 0, 85 }, { 0, 113 }, { 2, 1 }, { 0, 23 }, { 2, 1 }, { 0, 39 }, { 0, 55 }, { 72, 1 },
            { 24, 1 }, { 12, 1 }, { 4, 1 }, { 2, 1 }, { 0, 24 }, { 0, 130 }, { 2, 1 }, { 0, 40 }, { 4, 1 }, { 2, 1 },
            { 0, 100 }, { 0, 70 }, { 0, 114 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 132 }, { 0, 72 }, { 2, 1 },
            { 0, 144 }, { 0, 9 }, { 2, 1 }, { 0, 145 }, { 0, 25 }, { 24, 1 }, { 14, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 },
            { 0, 115 }, { 0, 101 }, { 2, 1 }, { 0, 86 }, { 0, 116 }, { 4, 1 }, { 2, 1 }, { 0, 71 }, { 0, 102 },
            { 0, 131 }, { 6, 1 }, { 2, 1 }, { 0, 56 }, { 2, 1 }, { 0, 117 }, { 0, 87 }, { 2, 1 }, { 0, 146 },
            { 0, 41 }, { 14, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 103 }, { 0, 133 }, { 2, 1 }, { 0, 88 }, { 0, 57 },
            { 2, 1 }, { 0, 147 }, { 2, 1 }, { 0, 73 }, { 0, 134 }, { 6, 1 }, { 2, 1 }, { 0, 160 }, { 2, 1 },
            { 0, 104 }, { 0, 10 }, { 2, 1 }, { 0, 161 }, { 0, 26 }, { 68, 1 }, { 24, 1 }, { 12, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 162 }, { 0, 42 }, { 4, 1 }, { 2, 1 }, { 0, 149 }, { 0, 89 }, { 2, 1 }, { 0, 163 },
            { 0, 58 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 74 }, { 0, 150 }, { 2, 1 }, { 0, 176 }, { 0, 11 }, { 2, 1 },
            { 0, 177 }, { 0, 27 }, { 20, 1 }, { 8, 1 }, { 2, 1 }, { 0, 178 }, { 4, 1 }, { 2, 1 }, { 0, 118 },
            { 0, 119 }, { 0, 148 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 135 }, { 0, 120 }, { 0, 164 }, { 4, 1 },
            { 2, 1 }, { 0, 105 }, { 0, 165 }, { 0, 43 }, { 12, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 90 },
            { 0, 136 }, { 0, 179 }, { 2, 1 }, { 0, 59 }, { 2, 1 }, { 0, 121 }, { 0, 166 }, { 6, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 106 }, { 0, 180 }, { 0, 192 }, { 4, 1 }, { 2, 1 }, { 0, 12 }, { 0, 152 }, { 0, 193 },
            { 60, 1 }, { 22, 1 }, { 10, 1 }, { 6, 1 }, { 2, 1 }, { 0, 28 }, { 2, 1 }, { 0, 137 }, { 0, 181 }, { 2, 1 },
            { 0, 91 }, { 0, 194 }, { 4, 1 }, { 2, 1 }, { 0, 44 }, { 0, 60 }, { 4, 1 }, { 2, 1 }, { 0, 182 },
            { 0, 107 }, { 2, 1 }, { 0, 196 }, { 0, 76 }, { 16, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 168 },
            { 0, 138 }, { 2, 1 }, { 0, 208 }, { 0, 13 }, { 2, 1 }, { 0, 209 }, { 2, 1 }, { 0, 75 }, { 2, 1 },
            { 0, 151 }, { 0, 167 }, { 12, 1 }, { 6, 1 }, { 2, 1 }, { 0, 195 }, { 2, 1 }, { 0, 122 }, { 0, 153 },
            { 4, 1 }, { 2, 1 }, { 0, 197 }, { 0, 92 }, { 0, 183 }, { 4, 1 }, { 2, 1 }, { 0, 29 }, { 0, 210 }, { 2, 1 },
            { 0, 45 }, { 2, 1 }, { 0, 123 }, { 0, 211 }, { 52, 1 }, { 28, 1 }, { 12, 1 }, { 4, 1 }, { 2, 1 },
            { 0, 61 }, { 0, 198 }, { 4, 1 }, { 2, 1 }, { 0, 108 }, { 0, 169 }, { 2, 1 }, { 0, 154 }, { 0, 212 },
            { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 184 }, { 0, 139 }, { 2, 1 }, { 0, 77 }, { 0, 199 }, { 4, 1 }, { 2, 1 },
            { 0, 124 }, { 0, 213 }, { 2, 1 }, { 0, 93 }, { 0, 224 }, { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 225 },
            { 0, 30 }, { 4, 1 }, { 2, 1 }, { 0, 14 }, { 0, 46 }, { 0, 226 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 227 },
            { 0, 109 }, { 2, 1 }, { 0, 140 }, { 0, 228 }, { 4, 1 }, { 2, 1 }, { 0, 229 }, { 0, 186 }, { 0, 240 },
            { 38, 1 }, { 16, 1 }, { 4, 1 }, { 2, 1 }, { 0, 241 }, { 0, 31 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 170 },
            { 0, 155 }, { 0, 185 }, { 2, 1 }, { 0, 62 }, { 2, 1 }, { 0, 214 }, { 0, 200 }, { 12, 1 }, { 6, 1 },
            { 2, 1 }, { 0, 78 }, { 2, 1 }, { 0, 215 }, { 0, 125 }, { 2, 1 }, { 0, 171 }, { 2, 1 }, { 0, 94 },
            { 0, 201 }, { 6, 1 }, { 2, 1 }, { 0, 15 }, { 2, 1 }, { 0, 156 }, { 0, 110 }, { 2, 1 }, { 0, 242 },
            { 0, 47 }, { 32, 1 }, { 16, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 216 }, { 0, 141 }, { 0, 63 }, { 6, 1 },
            { 2, 1 }, { 0, 243 }, { 2, 1 }, { 0, 230 }, { 0, 202 }, { 2, 1 }, { 0, 244 }, { 0, 79 }, { 8, 1 },
            { 4, 1 }, { 2, 1 }, { 0, 187 }, { 0, 172 }, { 2, 1 }, { 0, 231 }, { 0, 245 }, { 4, 1 }, { 2, 1 },
            { 0, 217 }, { 0, 157 }, { 2, 1 }, { 0, 95 }, { 0, 232 }, { 30, 1 }, { 12, 1 }, { 6, 1 }, { 2, 1 },
            { 0, 111 }, { 2, 1 }, { 0, 246 }, { 0, 203 }, { 4, 1 }, { 2, 1 }, { 0, 188 }, { 0, 173 }, { 0, 218 },
            { 8, 1 }, { 2, 1 }, { 0, 247 }, { 4, 1 }, { 2, 1 }, { 0, 126 }, { 0, 127 }, { 0, 142 }, { 6, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 158 }, { 0, 174 }, { 0, 204 }, { 2, 1 }, { 0, 248 }, { 0, 143 }, { 18, 1 }, { 8, 1 },
            { 4, 1 }, { 2, 1 }, { 0, 219 }, { 0, 189 }, { 2, 1 }, { 0, 234 }, { 0, 249 }, { 4, 1 }, { 2, 1 },
            { 0, 159 }, { 0, 235 }, { 2, 1 }, { 0, 190 }, { 2, 1 }, { 0, 205 }, { 0, 250 }, { 14, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 221 }, { 0, 236 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 233 }, { 0, 175 }, { 0, 220 },
            { 2, 1 }, { 0, 206 }, { 0, 251 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 191 }, { 0, 222 }, { 2, 1 },
            { 0, 207 }, { 0, 238 }, { 4, 1 }, { 2, 1 }, { 0, 223 }, { 0, 239 }, { 2, 1 }, { 0, 255 }, { 2, 1 },
            { 0, 237 }, { 2, 1 }, { 0, 253 }, { 2, 1 }, { 0, 252 }, { 0, 254 } };
    static final int[][] VAL_TAB_14 = { { 0, 0 } // dummy
    };
    static final int[][] VAL_TAB_15 = { { 16, 1 }, { 6, 1 }, { 2, 1 }, { 0, 0 }, { 2, 1 }, { 0, 16 }, { 0, 1 },
            { 2, 1 }, { 0, 17 }, { 4, 1 }, { 2, 1 }, { 0, 32 }, { 0, 2 }, { 2, 1 }, { 0, 33 }, { 0, 18 }, { 50, 1 },
            { 16, 1 }, { 6, 1 }, { 2, 1 }, { 0, 34 }, { 2, 1 }, { 0, 48 }, { 0, 49 }, { 6, 1 }, { 2, 1 }, { 0, 19 },
            { 2, 1 }, { 0, 3 }, { 0, 64 }, { 2, 1 }, { 0, 50 }, { 0, 35 }, { 14, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 },
            { 0, 4 }, { 0, 20 }, { 0, 65 }, { 4, 1 }, { 2, 1 }, { 0, 51 }, { 0, 66 }, { 2, 1 }, { 0, 36 }, { 0, 67 },
            { 10, 1 }, { 6, 1 }, { 2, 1 }, { 0, 52 }, { 2, 1 }, { 0, 80 }, { 0, 5 }, { 2, 1 }, { 0, 81 }, { 0, 21 },
            { 4, 1 }, { 2, 1 }, { 0, 82 }, { 0, 37 }, { 4, 1 }, { 2, 1 }, { 0, 68 }, { 0, 83 }, { 0, 97 }, { 90, 1 },
            { 36, 1 }, { 18, 1 }, { 10, 1 }, { 6, 1 }, { 2, 1 }, { 0, 53 }, { 2, 1 }, { 0, 96 }, { 0, 6 }, { 2, 1 },
            { 0, 22 }, { 0, 98 }, { 4, 1 }, { 2, 1 }, { 0, 38 }, { 0, 84 }, { 2, 1 }, { 0, 69 }, { 0, 99 }, { 10, 1 },
            { 6, 1 }, { 2, 1 }, { 0, 54 }, { 2, 1 }, { 0, 112 }, { 0, 7 }, { 2, 1 }, { 0, 113 }, { 0, 85 }, { 4, 1 },
            { 2, 1 }, { 0, 23 }, { 0, 100 }, { 2, 1 }, { 0, 114 }, { 0, 39 }, { 24, 1 }, { 16, 1 }, { 8, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 70 }, { 0, 115 }, { 2, 1 }, { 0, 55 }, { 0, 101 }, { 4, 1 }, { 2, 1 }, { 0, 86 },
            { 0, 128 }, { 2, 1 }, { 0, 8 }, { 0, 116 }, { 4, 1 }, { 2, 1 }, { 0, 129 }, { 0, 24 }, { 2, 1 },
            { 0, 130 }, { 0, 40 }, { 16, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 71 }, { 0, 102 }, { 2, 1 },
            { 0, 131 }, { 0, 56 }, { 4, 1 }, { 2, 1 }, { 0, 117 }, { 0, 87 }, { 2, 1 }, { 0, 132 }, { 0, 72 },
            { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 144 }, { 0, 25 }, { 0, 145 }, { 4, 1 }, { 2, 1 }, { 0, 146 },
            { 0, 118 }, { 2, 1 }, { 0, 103 }, { 0, 41 }, { 92, 1 }, { 36, 1 }, { 18, 1 }, { 10, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 133 }, { 0, 88 }, { 4, 1 }, { 2, 1 }, { 0, 9 }, { 0, 119 }, { 0, 147 }, { 4, 1 }, { 2, 1 },
            { 0, 57 }, { 0, 148 }, { 2, 1 }, { 0, 73 }, { 0, 134 }, { 10, 1 }, { 6, 1 }, { 2, 1 }, { 0, 104 },
            { 2, 1 }, { 0, 160 }, { 0, 10 }, { 2, 1 }, { 0, 161 }, { 0, 26 }, { 4, 1 }, { 2, 1 }, { 0, 162 },
            { 0, 42 }, { 2, 1 }, { 0, 149 }, { 0, 89 }, { 26, 1 }, { 14, 1 }, { 6, 1 }, { 2, 1 }, { 0, 163 }, { 2, 1 },
            { 0, 58 }, { 0, 135 }, { 4, 1 }, { 2, 1 }, { 0, 120 }, { 0, 164 }, { 2, 1 }, { 0, 74 }, { 0, 150 },
            { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 105 }, { 0, 176 }, { 0, 177 }, { 4, 1 }, { 2, 1 }, { 0, 27 },
            { 0, 165 }, { 0, 178 }, { 14, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 90 }, { 0, 43 }, { 2, 1 },
            { 0, 136 }, { 0, 151 }, { 2, 1 }, { 0, 179 }, { 2, 1 }, { 0, 121 }, { 0, 59 }, { 8, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 106 }, { 0, 180 }, { 2, 1 }, { 0, 75 }, { 0, 193 }, { 4, 1 }, { 2, 1 }, { 0, 152 },
            { 0, 137 }, { 2, 1 }, { 0, 28 }, { 0, 181 }, { 80, 1 }, { 34, 1 }, { 16, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 },
            { 0, 91 }, { 0, 44 }, { 0, 194 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 11 }, { 0, 192 }, { 0, 166 },
            { 2, 1 }, { 0, 167 }, { 0, 122 }, { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 195 }, { 0, 60 }, { 4, 1 }, { 2, 1 },
            { 0, 12 }, { 0, 153 }, { 0, 182 }, { 4, 1 }, { 2, 1 }, { 0, 107 }, { 0, 196 }, { 2, 1 }, { 0, 76 },
            { 0, 168 }, { 20, 1 }, { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 138 }, { 0, 197 }, { 4, 1 }, { 2, 1 },
            { 0, 208 }, { 0, 92 }, { 0, 209 }, { 4, 1 }, { 2, 1 }, { 0, 183 }, { 0, 123 }, { 2, 1 }, { 0, 29 },
            { 2, 1 }, { 0, 13 }, { 0, 45 }, { 12, 1 }, { 4, 1 }, { 2, 1 }, { 0, 210 }, { 0, 211 }, { 4, 1 }, { 2, 1 },
            { 0, 61 }, { 0, 198 }, { 2, 1 }, { 0, 108 }, { 0, 169 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 154 },
            { 0, 184 }, { 0, 212 }, { 4, 1 }, { 2, 1 }, { 0, 139 }, { 0, 77 }, { 2, 1 }, { 0, 199 }, { 0, 124 },
            { 68, 1 }, { 34, 1 }, { 18, 1 }, { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 213 }, { 0, 93 }, { 4, 1 }, { 2, 1 },
            { 0, 224 }, { 0, 14 }, { 0, 225 }, { 4, 1 }, { 2, 1 }, { 0, 30 }, { 0, 226 }, { 2, 1 }, { 0, 170 },
            { 0, 46 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 185 }, { 0, 155 }, { 2, 1 }, { 0, 227 }, { 0, 214 },
            { 4, 1 }, { 2, 1 }, { 0, 109 }, { 0, 62 }, { 2, 1 }, { 0, 200 }, { 0, 140 }, { 16, 1 }, { 8, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 228 }, { 0, 78 }, { 2, 1 }, { 0, 215 }, { 0, 125 }, { 4, 1 }, { 2, 1 }, { 0, 229 },
            { 0, 186 }, { 2, 1 }, { 0, 171 }, { 0, 94 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 201 }, { 0, 156 },
            { 2, 1 }, { 0, 241 }, { 0, 31 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 240 }, { 0, 110 }, { 0, 242 },
            { 2, 1 }, { 0, 47 }, { 0, 230 }, { 38, 1 }, { 18, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 216 },
            { 0, 243 }, { 2, 1 }, { 0, 63 }, { 0, 244 }, { 6, 1 }, { 2, 1 }, { 0, 79 }, { 2, 1 }, { 0, 141 },
            { 0, 217 }, { 2, 1 }, { 0, 187 }, { 0, 202 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 172 }, { 0, 231 },
            { 2, 1 }, { 0, 126 }, { 0, 245 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 157 }, { 0, 95 }, { 2, 1 },
            { 0, 232 }, { 0, 142 }, { 2, 1 }, { 0, 246 }, { 0, 203 }, { 34, 1 }, { 18, 1 }, { 10, 1 }, { 6, 1 },
            { 4, 1 }, { 2, 1 }, { 0, 15 }, { 0, 174 }, { 0, 111 }, { 2, 1 }, { 0, 188 }, { 0, 218 }, { 4, 1 },
            { 2, 1 }, { 0, 173 }, { 0, 247 }, { 2, 1 }, { 0, 127 }, { 0, 233 }, { 8, 1 }, { 4, 1 }, { 2, 1 },
            { 0, 158 }, { 0, 204 }, { 2, 1 }, { 0, 248 }, { 0, 143 }, { 4, 1 }, { 2, 1 }, { 0, 219 }, { 0, 189 },
            { 2, 1 }, { 0, 234 }, { 0, 249 }, { 16, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 159 }, { 0, 220 },
            { 2, 1 }, { 0, 205 }, { 0, 235 }, { 4, 1 }, { 2, 1 }, { 0, 190 }, { 0, 250 }, { 2, 1 }, { 0, 175 },
            { 0, 221 }, { 14, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 236 }, { 0, 206 }, { 0, 251 }, { 4, 1 },
            { 2, 1 }, { 0, 191 }, { 0, 237 }, { 2, 1 }, { 0, 222 }, { 0, 252 }, { 6, 1 }, { 4, 1 }, { 2, 1 },
            { 0, 207 }, { 0, 253 }, { 0, 238 }, { 4, 1 }, { 2, 1 }, { 0, 223 }, { 0, 254 }, { 2, 1 }, { 0, 239 },
            { 0, 255 }, };
    static final int[][] VAL_TAB_16 = { { 2, 1 }, { 0, 0 }, { 6, 1 }, { 2, 1 }, { 0, 16 }, { 2, 1 }, { 0, 1 },
            { 0, 17 }, { 42, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 32 }, { 0, 2 }, { 2, 1 }, { 0, 33 }, { 0, 18 },
            { 10, 1 }, { 6, 1 }, { 2, 1 }, { 0, 34 }, { 2, 1 }, { 0, 48 }, { 0, 3 }, { 2, 1 }, { 0, 49 }, { 0, 19 },
            { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 50 }, { 0, 35 }, { 4, 1 }, { 2, 1 }, { 0, 64 }, { 0, 4 }, { 0, 65 },
            { 6, 1 }, { 2, 1 }, { 0, 20 }, { 2, 1 }, { 0, 51 }, { 0, 66 }, { 4, 1 }, { 2, 1 }, { 0, 36 }, { 0, 80 },
            { 2, 1 }, { 0, 67 }, { 0, 52 }, { 138, 1 }, { 40, 1 }, { 16, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 5 },
            { 0, 21 }, { 0, 81 }, { 4, 1 }, { 2, 1 }, { 0, 82 }, { 0, 37 }, { 4, 1 }, { 2, 1 }, { 0, 68 }, { 0, 53 },
            { 0, 83 }, { 10, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 96 }, { 0, 6 }, { 0, 97 }, { 2, 1 }, { 0, 22 },
            { 0, 98 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 38 }, { 0, 84 }, { 2, 1 }, { 0, 69 }, { 0, 99 }, { 4, 1 },
            { 2, 1 }, { 0, 54 }, { 0, 112 }, { 0, 113 }, { 40, 1 }, { 18, 1 }, { 8, 1 }, { 2, 1 }, { 0, 23 }, { 2, 1 },
            { 0, 7 }, { 2, 1 }, { 0, 85 }, { 0, 100 }, { 4, 1 }, { 2, 1 }, { 0, 114 }, { 0, 39 }, { 4, 1 }, { 2, 1 },
            { 0, 70 }, { 0, 101 }, { 0, 115 }, { 10, 1 }, { 6, 1 }, { 2, 1 }, { 0, 55 }, { 2, 1 }, { 0, 86 }, { 0, 8 },
            { 2, 1 }, { 0, 128 }, { 0, 129 }, { 6, 1 }, { 2, 1 }, { 0, 24 }, { 2, 1 }, { 0, 116 }, { 0, 71 }, { 2, 1 },
            { 0, 130 }, { 2, 1 }, { 0, 40 }, { 0, 102 }, { 24, 1 }, { 14, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 },
            { 0, 131 }, { 0, 56 }, { 2, 1 }, { 0, 117 }, { 0, 132 }, { 4, 1 }, { 2, 1 }, { 0, 72 }, { 0, 144 },
            { 0, 145 }, { 6, 1 }, { 2, 1 }, { 0, 25 }, { 2, 1 }, { 0, 9 }, { 0, 118 }, { 2, 1 }, { 0, 146 }, { 0, 41 },
            { 14, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 133 }, { 0, 88 }, { 2, 1 }, { 0, 147 }, { 0, 57 }, { 4, 1 },
            { 2, 1 }, { 0, 160 }, { 0, 10 }, { 0, 26 }, { 8, 1 }, { 2, 1 }, { 0, 162 }, { 2, 1 }, { 0, 103 }, { 2, 1 },
            { 0, 87 }, { 0, 73 }, { 6, 1 }, { 2, 1 }, { 0, 148 }, { 2, 1 }, { 0, 119 }, { 0, 134 }, { 2, 1 },
            { 0, 161 }, { 2, 1 }, { 0, 104 }, { 0, 149 }, { 220, 1 }, { 126, 1 }, { 50, 1 }, { 26, 1 }, { 12, 1 },
            { 6, 1 }, { 2, 1 }, { 0, 42 }, { 2, 1 }, { 0, 89 }, { 0, 58 }, { 2, 1 }, { 0, 163 }, { 2, 1 }, { 0, 135 },
            { 0, 120 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 164 }, { 0, 74 }, { 2, 1 }, { 0, 150 }, { 0, 105 },
            { 4, 1 }, { 2, 1 }, { 0, 176 }, { 0, 11 }, { 0, 177 }, { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 27 },
            { 0, 178 }, { 2, 1 }, { 0, 43 }, { 2, 1 }, { 0, 165 }, { 0, 90 }, { 6, 1 }, { 2, 1 }, { 0, 179 }, { 2, 1 },
            { 0, 166 }, { 0, 106 }, { 4, 1 }, { 2, 1 }, { 0, 180 }, { 0, 75 }, { 2, 1 }, { 0, 12 }, { 0, 193 },
            { 30, 1 }, { 14, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 181 }, { 0, 194 }, { 0, 44 }, { 4, 1 }, { 2, 1 },
            { 0, 167 }, { 0, 195 }, { 2, 1 }, { 0, 107 }, { 0, 196 }, { 8, 1 }, { 2, 1 }, { 0, 29 }, { 4, 1 },
            { 2, 1 }, { 0, 136 }, { 0, 151 }, { 0, 59 }, { 4, 1 }, { 2, 1 }, { 0, 209 }, { 0, 210 }, { 2, 1 },
            { 0, 45 }, { 0, 211 }, { 18, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 30 }, { 0, 46 }, { 0, 226 }, { 6, 1 },
            { 4, 1 }, { 2, 1 }, { 0, 121 }, { 0, 152 }, { 0, 192 }, { 2, 1 }, { 0, 28 }, { 2, 1 }, { 0, 137 },
            { 0, 91 }, { 14, 1 }, { 6, 1 }, { 2, 1 }, { 0, 60 }, { 2, 1 }, { 0, 122 }, { 0, 182 }, { 4, 1 }, { 2, 1 },
            { 0, 76 }, { 0, 153 }, { 2, 1 }, { 0, 168 }, { 0, 138 }, { 6, 1 }, { 2, 1 }, { 0, 13 }, { 2, 1 },
            { 0, 197 }, { 0, 92 }, { 4, 1 }, { 2, 1 }, { 0, 61 }, { 0, 198 }, { 2, 1 }, { 0, 108 }, { 0, 154 },
            { 88, 1 }, { 86, 1 }, { 36, 1 }, { 16, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 139 }, { 0, 77 }, { 2, 1 },
            { 0, 199 }, { 0, 124 }, { 4, 1 }, { 2, 1 }, { 0, 213 }, { 0, 93 }, { 2, 1 }, { 0, 224 }, { 0, 14 },
            { 8, 1 }, { 2, 1 }, { 0, 227 }, { 4, 1 }, { 2, 1 }, { 0, 208 }, { 0, 183 }, { 0, 123 }, { 6, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 169 }, { 0, 184 }, { 0, 212 }, { 2, 1 }, { 0, 225 }, { 2, 1 }, { 0, 170 }, { 0, 185 },
            { 24, 1 }, { 10, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 155 }, { 0, 214 }, { 0, 109 }, { 2, 1 },
            { 0, 62 }, { 0, 200 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 140 }, { 0, 228 }, { 0, 78 }, { 4, 1 }, { 2, 1 },
            { 0, 215 }, { 0, 229 }, { 2, 1 }, { 0, 186 }, { 0, 171 }, { 12, 1 }, { 4, 1 }, { 2, 1 }, { 0, 156 },
            { 0, 230 }, { 4, 1 }, { 2, 1 }, { 0, 110 }, { 0, 216 }, { 2, 1 }, { 0, 141 }, { 0, 187 }, { 8, 1 },
            { 4, 1 }, { 2, 1 }, { 0, 231 }, { 0, 157 }, { 2, 1 }, { 0, 232 }, { 0, 142 }, { 4, 1 }, { 2, 1 },
            { 0, 203 }, { 0, 188 }, { 0, 158 }, { 0, 241 }, { 2, 1 }, { 0, 31 }, { 2, 1 }, { 0, 15 }, { 0, 47 },
            { 66, 1 }, { 56, 1 }, { 2, 1 }, { 0, 242 }, { 52, 1 }, { 50, 1 }, { 20, 1 }, { 8, 1 }, { 2, 1 },
            { 0, 189 }, { 2, 1 }, { 0, 94 }, { 2, 1 }, { 0, 125 }, { 0, 201 }, { 6, 1 }, { 2, 1 }, { 0, 202 },
            { 2, 1 }, { 0, 172 }, { 0, 126 }, { 4, 1 }, { 2, 1 }, { 0, 218 }, { 0, 173 }, { 0, 204 }, { 10, 1 },
            { 6, 1 }, { 2, 1 }, { 0, 174 }, { 2, 1 }, { 0, 219 }, { 0, 220 }, { 2, 1 }, { 0, 205 }, { 0, 190 },
            { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 235 }, { 0, 237 }, { 0, 238 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 217 },
            { 0, 234 }, { 0, 233 }, { 2, 1 }, { 0, 222 }, { 4, 1 }, { 2, 1 }, { 0, 221 }, { 0, 236 }, { 0, 206 },
            { 0, 63 }, { 0, 240 }, { 4, 1 }, { 2, 1 }, { 0, 243 }, { 0, 244 }, { 2, 1 }, { 0, 79 }, { 2, 1 },
            { 0, 245 }, { 0, 95 }, { 10, 1 }, { 2, 1 }, { 0, 255 }, { 4, 1 }, { 2, 1 }, { 0, 246 }, { 0, 111 },
            { 2, 1 }, { 0, 247 }, { 0, 127 }, { 12, 1 }, { 6, 1 }, { 2, 1 }, { 0, 143 }, { 2, 1 }, { 0, 248 },
            { 0, 249 }, { 4, 1 }, { 2, 1 }, { 0, 159 }, { 0, 250 }, { 0, 175 }, { 8, 1 }, { 4, 1 }, { 2, 1 },
            { 0, 251 }, { 0, 191 }, { 2, 1 }, { 0, 252 }, { 0, 207 }, { 4, 1 }, { 2, 1 }, { 0, 253 }, { 0, 223 },
            { 2, 1 }, { 0, 254 }, { 0, 239 }, };
    static final int[][] VAL_TAB_24 = { { 60, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 0 }, { 0, 16 }, { 2, 1 },
            { 0, 1 }, { 0, 17 }, { 14, 1 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 32 }, { 0, 2 }, { 0, 33 }, { 2, 1 },
            { 0, 18 }, { 2, 1 }, { 0, 34 }, { 2, 1 }, { 0, 48 }, { 0, 3 }, { 14, 1 }, { 4, 1 }, { 2, 1 }, { 0, 49 },
            { 0, 19 }, { 4, 1 }, { 2, 1 }, { 0, 50 }, { 0, 35 }, { 4, 1 }, { 2, 1 }, { 0, 64 }, { 0, 4 }, { 0, 65 },
            { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 20 }, { 0, 51 }, { 2, 1 }, { 0, 66 }, { 0, 36 }, { 6, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 67 }, { 0, 52 }, { 0, 81 }, { 6, 1 }, { 4, 1 }, { 2, 1 }, { 0, 80 }, { 0, 5 }, { 0, 21 },
            { 2, 1 }, { 0, 82 }, { 0, 37 }, { 250, 1 }, { 98, 1 }, { 34, 1 }, { 18, 1 }, { 10, 1 }, { 4, 1 }, { 2, 1 },
            { 0, 68 }, { 0, 83 }, { 2, 1 }, { 0, 53 }, { 2, 1 }, { 0, 96 }, { 0, 6 }, { 4, 1 }, { 2, 1 }, { 0, 97 },
            { 0, 22 }, { 2, 1 }, { 0, 98 }, { 0, 38 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 84 }, { 0, 69 }, { 2, 1 },
            { 0, 99 }, { 0, 54 }, { 4, 1 }, { 2, 1 }, { 0, 113 }, { 0, 85 }, { 2, 1 }, { 0, 100 }, { 0, 70 },
            { 32, 1 }, { 14, 1 }, { 6, 1 }, { 2, 1 }, { 0, 114 }, { 2, 1 }, { 0, 39 }, { 0, 55 }, { 2, 1 }, { 0, 115 },
            { 4, 1 }, { 2, 1 }, { 0, 112 }, { 0, 7 }, { 0, 23 }, { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 101 }, { 0, 86 },
            { 4, 1 }, { 2, 1 }, { 0, 128 }, { 0, 8 }, { 0, 129 }, { 4, 1 }, { 2, 1 }, { 0, 116 }, { 0, 71 }, { 2, 1 },
            { 0, 24 }, { 0, 130 }, { 16, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 40 }, { 0, 102 }, { 2, 1 },
            { 0, 131 }, { 0, 56 }, { 4, 1 }, { 2, 1 }, { 0, 117 }, { 0, 87 }, { 2, 1 }, { 0, 132 }, { 0, 72 },
            { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 145 }, { 0, 25 }, { 2, 1 }, { 0, 146 }, { 0, 118 }, { 4, 1 }, { 2, 1 },
            { 0, 103 }, { 0, 41 }, { 2, 1 }, { 0, 133 }, { 0, 88 }, { 92, 1 }, { 34, 1 }, { 16, 1 }, { 8, 1 },
            { 4, 1 }, { 2, 1 }, { 0, 147 }, { 0, 57 }, { 2, 1 }, { 0, 148 }, { 0, 73 }, { 4, 1 }, { 2, 1 }, { 0, 119 },
            { 0, 134 }, { 2, 1 }, { 0, 104 }, { 0, 161 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 162 }, { 0, 42 },
            { 2, 1 }, { 0, 149 }, { 0, 89 }, { 4, 1 }, { 2, 1 }, { 0, 163 }, { 0, 58 }, { 2, 1 }, { 0, 135 }, { 2, 1 },
            { 0, 120 }, { 0, 74 }, { 22, 1 }, { 12, 1 }, { 4, 1 }, { 2, 1 }, { 0, 164 }, { 0, 150 }, { 4, 1 },
            { 2, 1 }, { 0, 105 }, { 0, 177 }, { 2, 1 }, { 0, 27 }, { 0, 165 }, { 6, 1 }, { 2, 1 }, { 0, 178 },
            { 2, 1 }, { 0, 90 }, { 0, 43 }, { 2, 1 }, { 0, 136 }, { 0, 179 }, { 16, 1 }, { 10, 1 }, { 6, 1 }, { 2, 1 },
            { 0, 144 }, { 2, 1 }, { 0, 9 }, { 0, 160 }, { 2, 1 }, { 0, 151 }, { 0, 121 }, { 4, 1 }, { 2, 1 },
            { 0, 166 }, { 0, 106 }, { 0, 180 }, { 12, 1 }, { 6, 1 }, { 2, 1 }, { 0, 26 }, { 2, 1 }, { 0, 10 },
            { 0, 176 }, { 2, 1 }, { 0, 59 }, { 2, 1 }, { 0, 11 }, { 0, 192 }, { 4, 1 }, { 2, 1 }, { 0, 75 },
            { 0, 193 }, { 2, 1 }, { 0, 152 }, { 0, 137 }, { 67, 1 }, { 34, 1 }, { 16, 1 }, { 8, 1 }, { 4, 1 },
            { 2, 1 }, { 0, 28 }, { 0, 181 }, { 2, 1 }, { 0, 91 }, { 0, 194 }, { 4, 1 }, { 2, 1 }, { 0, 44 },
            { 0, 167 }, { 2, 1 }, { 0, 122 }, { 0, 195 }, { 10, 1 }, { 6, 1 }, { 2, 1 }, { 0, 60 }, { 2, 1 },
            { 0, 12 }, { 0, 208 }, { 2, 1 }, { 0, 182 }, { 0, 107 }, { 4, 1 }, { 2, 1 }, { 0, 196 }, { 0, 76 },
            { 2, 1 }, { 0, 153 }, { 0, 168 }, { 16, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 138 }, { 0, 197 },
            { 2, 1 }, { 0, 92 }, { 0, 209 }, { 4, 1 }, { 2, 1 }, { 0, 183 }, { 0, 123 }, { 2, 1 }, { 0, 29 },
            { 0, 210 }, { 9, 1 }, { 4, 1 }, { 2, 1 }, { 0, 45 }, { 0, 211 }, { 2, 1 }, { 0, 61 }, { 0, 198 },
            { 85, 250 }, { 4, 1 }, { 2, 1 }, { 0, 108 }, { 0, 169 }, { 2, 1 }, { 0, 154 }, { 0, 212 }, { 32, 1 },
            { 16, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 184 }, { 0, 139 }, { 2, 1 }, { 0, 77 }, { 0, 199 }, { 4, 1 },
            { 2, 1 }, { 0, 124 }, { 0, 213 }, { 2, 1 }, { 0, 93 }, { 0, 225 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 30 },
            { 0, 226 }, { 2, 1 }, { 0, 170 }, { 0, 185 }, { 4, 1 }, { 2, 1 }, { 0, 155 }, { 0, 227 }, { 2, 1 },
            { 0, 214 }, { 0, 109 }, { 20, 1 }, { 10, 1 }, { 6, 1 }, { 2, 1 }, { 0, 62 }, { 2, 1 }, { 0, 46 },
            { 0, 78 }, { 2, 1 }, { 0, 200 }, { 0, 140 }, { 4, 1 }, { 2, 1 }, { 0, 228 }, { 0, 215 }, { 4, 1 },
            { 2, 1 }, { 0, 125 }, { 0, 171 }, { 0, 229 }, { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 186 }, { 0, 94 },
            { 2, 1 }, { 0, 201 }, { 2, 1 }, { 0, 156 }, { 0, 110 }, { 8, 1 }, { 2, 1 }, { 0, 230 }, { 2, 1 },
            { 0, 13 }, { 2, 1 }, { 0, 224 }, { 0, 14 }, { 4, 1 }, { 2, 1 }, { 0, 216 }, { 0, 141 }, { 2, 1 },
            { 0, 187 }, { 0, 202 }, { 74, 1 }, { 2, 1 }, { 0, 255 }, { 64, 1 }, { 58, 1 }, { 32, 1 }, { 16, 1 },
            { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 172 }, { 0, 231 }, { 2, 1 }, { 0, 126 }, { 0, 217 }, { 4, 1 }, { 2, 1 },
            { 0, 157 }, { 0, 232 }, { 2, 1 }, { 0, 142 }, { 0, 203 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 188 },
            { 0, 218 }, { 2, 1 }, { 0, 173 }, { 0, 233 }, { 4, 1 }, { 2, 1 }, { 0, 158 }, { 0, 204 }, { 2, 1 },
            { 0, 219 }, { 0, 189 }, { 16, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 234 }, { 0, 174 }, { 2, 1 },
            { 0, 220 }, { 0, 205 }, { 4, 1 }, { 2, 1 }, { 0, 235 }, { 0, 190 }, { 2, 1 }, { 0, 221 }, { 0, 236 },
            { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 206 }, { 0, 237 }, { 2, 1 }, { 0, 222 }, { 0, 238 }, { 0, 15 },
            { 4, 1 }, { 2, 1 }, { 0, 240 }, { 0, 31 }, { 0, 241 }, { 4, 1 }, { 2, 1 }, { 0, 242 }, { 0, 47 }, { 2, 1 },
            { 0, 243 }, { 0, 63 }, { 18, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 244 }, { 0, 79 }, { 2, 1 },
            { 0, 245 }, { 0, 95 }, { 4, 1 }, { 2, 1 }, { 0, 246 }, { 0, 111 }, { 2, 1 }, { 0, 247 }, { 2, 1 },
            { 0, 127 }, { 0, 143 }, { 10, 1 }, { 4, 1 }, { 2, 1 }, { 0, 248 }, { 0, 249 }, { 4, 1 }, { 2, 1 },
            { 0, 159 }, { 0, 175 }, { 0, 250 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 251 }, { 0, 191 }, { 2, 1 },
            { 0, 252 }, { 0, 207 }, { 4, 1 }, { 2, 1 }, { 0, 253 }, { 0, 223 }, { 2, 1 }, { 0, 254 }, { 0, 239 }, };
    static final int[][] VAL_TAB_32 = { { 2, 1 }, { 0, 0 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 8 }, { 0, 4 }, { 2, 1 },
            { 0, 1 }, { 0, 2 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 12 }, { 0, 10 }, { 2, 1 }, { 0, 3 }, { 0, 6 },
            { 6, 1 }, { 2, 1 }, { 0, 9 }, { 2, 1 }, { 0, 5 }, { 0, 7 }, { 4, 1 }, { 2, 1 }, { 0, 14 }, { 0, 13 },
            { 2, 1 }, { 0, 15 }, { 0, 11 }, };
    static final int[][] VAL_TAB_33 = { { 16, 1 }, { 8, 1 }, { 4, 1 }, { 2, 1 }, { 0, 0 }, { 0, 1 }, { 2, 1 },
            { 0, 2 }, { 0, 3 }, { 4, 1 }, { 2, 1 }, { 0, 4 }, { 0, 5 }, { 2, 1 }, { 0, 6 }, { 0, 7 }, { 8, 1 },
            { 4, 1 }, { 2, 1 }, { 0, 8 }, { 0, 9 }, { 2, 1 }, { 0, 10 }, { 0, 11 }, { 4, 1 }, { 2, 1 }, { 0, 12 },
            { 0, 13 }, { 2, 1 }, { 0, 14 }, { 0, 15 }, };
    static final int[][] BITRATES = {
            { 0 /* free format */, 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000,
                    128000, 144000, 160000, 0 },
            { 0 /* free format */, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 160000, 192000,
                    224000, 256000, 320000, 0 },
            { 0 /* free format */, 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000,
                    128000, 144000, 160000, 0 } };
    static final int[][] REORDER_TABLE;

    public static class SBI {
        public int[] l;
        public int[] s;

        public SBI(int[] thel, int[] thes) {
            l = thel;
            s = thes;
        }
    }

    static final SBI[] SF_BAND_INDEX = new SBI[9];
    static {
        int[] l0 = { 0, 6, 12, 18, 24, 30, 36, 44, 54, 66, 80, 96, 116, 140, 168, 200, 238, 284, 336, 396, 464, 522, 576 };
        int[] s0 = { 0, 4, 8, 12, 18, 24, 32, 42, 56, 74, 100, 132, 174, 192 };
        int[] l1 = { 0, 6, 12, 18, 24, 30, 36, 44, 54, 66, 80, 96, 114, 136, 162, 194, 232, 278, 330, 394, 464, 540, 576 };
        int[] s1 = { 0, 4, 8, 12, 18, 26, 36, 48, 62, 80, 104, 136, 180, 192 };
        int[] l2 = { 0, 6, 12, 18, 24, 30, 36, 44, 54, 66, 80, 96, 116, 140, 168, 200, 238, 284, 336, 396, 464, 522, 576 };
        int[] s2 = { 0, 4, 8, 12, 18, 26, 36, 48, 62, 80, 104, 134, 174, 192 };
        int[] l3 = { 0, 4, 8, 12, 16, 20, 24, 30, 36, 44, 52, 62, 74, 90, 110, 134, 162, 196, 238, 288, 342, 418, 576 };
        int[] s3 = { 0, 4, 8, 12, 16, 22, 30, 40, 52, 66, 84, 106, 136, 192 };
        int[] l4 = { 0, 4, 8, 12, 16, 20, 24, 30, 36, 42, 50, 60, 72, 88, 106, 128, 156, 190, 230, 276, 330, 384, 576 };
        int[] s4 = { 0, 4, 8, 12, 16, 22, 28, 38, 50, 64, 80, 100, 126, 192 };
        int[] l5 = { 0, 4, 8, 12, 16, 20, 24, 30, 36, 44, 54, 66, 82, 102, 126, 156, 194, 240, 296, 364, 448, 550, 576 };
        int[] s5 = { 0, 4, 8, 12, 16, 22, 30, 42, 58, 78, 104, 138, 180, 192 };
        int[] l6 = { 0, 6, 12, 18, 24, 30, 36, 44, 54, 66, 80, 96, 116, 140, 168, 200, 238, 284, 336, 396, 464, 522, 576 };
        int[] s6 = { 0, 4, 8, 12, 18, 26, 36, 48, 62, 80, 104, 134, 174, 192 };
        int[] l7 = { 0, 6, 12, 18, 24, 30, 36, 44, 54, 66, 80, 96, 116, 140, 168, 200, 238, 284, 336, 396, 464, 522, 576 };
        int[] s7 = { 0, 4, 8, 12, 18, 26, 36, 48, 62, 80, 104, 134, 174, 192 };
        int[] l8 = { 0, 12, 24, 36, 48, 60, 72, 88, 108, 132, 160, 192, 232, 280, 336, 400, 476, 566, 568, 570, 572, 574, 576 };
        int[] s8 = { 0, 8, 16, 24, 36, 52, 72, 96, 124, 160, 162, 164, 166, 192 };
        SF_BAND_INDEX[0] = new SBI(l0, s0);
        SF_BAND_INDEX[1] = new SBI(l1, s1);
        SF_BAND_INDEX[2] = new SBI(l2, s2);
        SF_BAND_INDEX[3] = new SBI(l3, s3);
        SF_BAND_INDEX[4] = new SBI(l4, s4);
        SF_BAND_INDEX[5] = new SBI(l5, s5);
        SF_BAND_INDEX[6] = new SBI(l6, s6);
        SF_BAND_INDEX[7] = new SBI(l7, s7);
        SF_BAND_INDEX[8] = new SBI(l8, s8);
        REORDER_TABLE = new int[9][];
        for (int i = 0; i < 9; i++) {
            REORDER_TABLE[i] = reorder(SF_BAND_INDEX[i].s);
        }
    }

    private static int[] reorder(int[] scaleFactorBand) {
        int j = 0;
        int[] ix = new int[576];
        for (int sfb = 0; sfb < 13; sfb++) {
            int start = scaleFactorBand[sfb];
            int end = scaleFactorBand[sfb + 1];
            for (int window = 0; window < 3; window++) {
                for (int i = start; i < end; i++) {
                    ix[3 * i + window] = j++;
                }
            }
        }
        return ix;
    }

    /**
     * d[] split into subarrays of length 16. This provides for more faster
     * access by allowing a block of 16 to be addressed with constant offset.
     */
    static final double[][] D16;
    static {
        D16 = splitArray(D, 16);
    }

    /**
     * Converts a 1D array into a number of smaller arrays. This is used to
     * achieve offset + constant indexing into an array. Each sub-array
     * represents a block of values of the original array.
     * 
     * @param array
     *            The array to split up into blocks.
     * @param blockSize
     *            The size of the blocks to split the array into. This must be
     *            an exact divisor of the length of the array, or some data will
     *            be lost from the main array.
     * 
     * @return An array of arrays in which each element in the returned array
     *         will be of length <code>blockSize</code>.
     */
    private static double[][] splitArray(final double[] array, final int blockSize) {
        int size = array.length / blockSize;
        double[][] split = new double[size][];
        for (int i = 0; i < size; i++) {
            split[i] = subArray(array, i * blockSize, blockSize);
        }
        return split;
    }

    /**
     * Returns a subarray of an existing array.
     * 
     * @param array
     *            The array to retrieve a subarra from.
     * @param offs
     *            The offset in the array that corresponds to the first index of
     *            the subarray.
     * @param len
     *            The number of indeces in the subarray.
     * @return The subarray, which may be of length 0.
     */
    private static double[] subArray(final double[] array, final int offs, int len) {
        if (offs + len > array.length) {
            len = array.length - offs;
        }
        if (len < 0) {
            len = 0;
        }
        double[] subarray = new double[len];
        for (int i = 0; i < len; i++) {
            subarray[i] = array[offs + i];
        }
        return subarray;
    }
}

//import org.mp3transform.Constants.SBI;

/**
 * Class Implementing Layer 3 Decoder.
 * http://www.oreilly.com/catalog/mp3/chapter/ch02.html Maximum bitreservoir is
 * 511 byte.
 * http://www.hydrogenaudio.org/forums/lofiversion/index.php/t42194.html gr:
 * granules (sub-frames)
 */
final class Layer3Decoder {
    static class GrInfo {
        int part23Length;
        int bigValues;
        int globalGain;
        int scaleFactorCompress;
        boolean windowSwitching;
        int blockType;
        boolean mixedBlock;
        int[] tableSelect = new int[3];
        int[] subblockGain = new int[3];
        int region0Count;
        int region1Count;
        int preflag;
        int scaleFactorScale;
        int count1TableSelect;
    }

    static class Channel {
        int[] scfsi = new int[4];
        GrInfo[] gr = new GrInfo[] { new GrInfo(), new GrInfo() };
    }

    static class SideInfo {
        int mainDataBegin = 0;
        Channel[] ch = new Channel[] { new Channel(), new Channel() };
    }

    static class ScaleFactor {
        int[] l = new int[23]; /* [cb] */
        int[][] s = new int[3][13]; /* [window][cb] */
    }

    private static final int SSLIMIT = 18;
    private static final int SBLIMIT = 32;
    // DOUBLE
    private static final double D43 = (4.0 / 3.0);
    private final int[] scaleFactorBuffer = new int[54];
    // TODO why +4?
    private final int[] is1d = new int[SBLIMIT * SSLIMIT + 4];
    private final double[][] ro0 = new double[SBLIMIT][SSLIMIT];
    private final double[][] ro1 = new double[SBLIMIT][SSLIMIT];
    private final double[][] lr0 = new double[SBLIMIT][SSLIMIT];
    private final double[][] lr1 = new double[SBLIMIT][SSLIMIT];
    private final double[] out1d = new double[SBLIMIT * SSLIMIT];
    private final double[][] prevBlock = new double[2][SBLIMIT * SSLIMIT];
    private final double[] k0 = new double[SBLIMIT * SSLIMIT];
    private final double[] k1 = new double[SBLIMIT * SSLIMIT];
    private final int[] nonzero = new int[2];
    private final Bitstream stream;
    private final Header header;
    private final SynthesisFilter filter1, filter2;
    private final Decoder player;
    private final BitReservoir br = new BitReservoir();
    private final SideInfo si = new SideInfo();
    private final ScaleFactor[] scaleFactors = new ScaleFactor[] { new ScaleFactor(), new ScaleFactor() };
    private int maxGr;
    private int frameStart;
    private int part2Start;
    private int channels;
    private int firstChannel;
    private int lastChannel;
    private int sfreq;
    private final int[] isPos = new int[576];
    private final double[] isRatio = new double[576];
    private final double[] tsOutCopy = new double[18];
    private final double[] rawout = new double[36];
    // subband samples are buffered and passed to the
    // SynthesisFilter in one go.
    private double[] samples1 = new double[32];
    private double[] samples2 = new double[32];
    private final int[] newSlen = new int[4];
    int x, y, v, w;

    public Layer3Decoder(Bitstream stream, Header header, SynthesisFilter filter1, SynthesisFilter filter2,
            Decoder player) {
        this.stream = stream;
        this.header = header;
        this.filter1 = filter1;
        this.filter2 = filter2;
        this.player = player;
        channels = (header.mode() == Header.MODE_SINGLE_CHANNEL) ? 1 : 2;
        maxGr = (header.version() == Header.VERSION_MPEG1) ? 2 : 1;
        sfreq = header.sampleFrequency()
                + ((header.version() == Header.VERSION_MPEG1) ? 3 : (header.version() == Header.VERSION_MPEG25_LSF) ? 6
                        : 0);
        if (channels == 2) {
            firstChannel = 0;
            lastChannel = 1;
        }
        nonzero[0] = nonzero[1] = 576;
    }

    public void decodeFrame() throws IOException {
        int slots = header.slots();
        getSideInfo();
        int flushMain = br.getBitCount() & 7;
        if (flushMain != 0) {
            br.getBits(8 - flushMain);
        }
        int mainDataEnd = br.getBitCount() >>> 3; // of previous frame
        for (int i = 0; i < slots; i++) {
            br.putByte(stream.getBits(8));
        }
        int bytesToDiscard = frameStart - mainDataEnd - si.mainDataBegin;
        frameStart += slots;
        if (bytesToDiscard < 0) {
            return;
        }
        if (mainDataEnd > 4096) {
            frameStart -= 4096;
            br.rewindBytes(4096);
        }
        for (; bytesToDiscard > 0; bytesToDiscard--) {
            br.getBits(8);
        }
        for (int gr = 0; gr < maxGr; gr++) {
            for (int ch = 0; ch < channels; ch++) {
                part2Start = br.getBitCount();
                if (header.version() == Header.VERSION_MPEG1) {
                    getScaleFactors(ch, gr);
                } else {
                    // MPEG-2 LSF, MPEG-2.5 LSF
                    getLsfScaleFactors(ch, gr);
                }
                huffmanDecode(ch, gr);
                dequantizeSample(ch == 0 ? ro0 : ro1, ch, gr);
            }
            stereo(gr);
            for (int ch = firstChannel; ch <= lastChannel; ch++) {
                reorder(ch == 0 ? lr0 : lr1, ch, gr);
                antialias(ch, gr);
                hybrid(ch, gr);
                for (int sb18 = 18; sb18 < 576; sb18 += 36) {
                    // Frequency inversion
                    for (int ss = 1; ss < SSLIMIT; ss += 2) {
                        out1d[sb18 + ss] = -out1d[sb18 + ss];
                    }
                }
                if (ch == 0) {
                    for (int ss = 0; ss < SSLIMIT; ss++) {
                        // Polyphase synthesis
                        for (int sb18 = 0, sb = 0; sb18 < 576; sb18 += 18, sb++) {
                            samples1[sb] = out1d[sb18 + ss];
                        }
                        filter1.calculatePcmSamples(samples1, player);
                    }
                } else {
                    for (int ss = 0; ss < SSLIMIT; ss++) {
                        // Polyphase synthesis
                        for (int sb18 = 0, sb = 0; sb18 < 576; sb18 += 18, sb++) {
                            samples2[sb] = out1d[sb18 + ss];
                        }
                        filter2.calculatePcmSamples(samples2, player);
                    }
                }
            }
        }
    }

    /**
     * Reads the side info from the stream, assuming the entire frame has been
     * read already. Mono : 136 bits (= 17 bytes) Stereo : 256 bits (= 32 bytes)
     */
    private void getSideInfo() throws IOException {
        if (header.version() == Header.VERSION_MPEG1) {
            si.mainDataBegin = stream.getBits(9);
            if (channels == 1) {
                stream.getBits(5);
            } else {
                stream.getBits(3);
            }
            for (int ch = 0; ch < channels; ch++) {
                Channel c = si.ch[ch];
                c.scfsi[0] = stream.getBits(1);
                c.scfsi[1] = stream.getBits(1);
                c.scfsi[2] = stream.getBits(1);
                c.scfsi[3] = stream.getBits(1);
            }
            for (int gr = 0; gr < 2; gr++) {
                for (int ch = 0; ch < channels; ch++) {
                    GrInfo gi = si.ch[ch].gr[gr];
                    gi.part23Length = stream.getBits(12);
                    gi.bigValues = stream.getBits(9);
                    gi.globalGain = stream.getBits(8);
                    gi.scaleFactorCompress = stream.getBits(4);
                    gi.windowSwitching = stream.getBits(1) != 0;
                    if (gi.windowSwitching) {
                        gi.blockType = stream.getBits(2);
                        gi.mixedBlock = stream.getBits(1) != 0;
                        gi.tableSelect[0] = stream.getBits(5);
                        gi.tableSelect[1] = stream.getBits(5);
                        gi.subblockGain[0] = stream.getBits(3);
                        gi.subblockGain[1] = stream.getBits(3);
                        gi.subblockGain[2] = stream.getBits(3);
                        // Set regionCount: implicit in this case
                        if (gi.blockType == 0) {
                            throw new IOException("Side info bad: blockType == 0 in split block");
                        } else if (gi.blockType == 2 && !gi.mixedBlock) {
                            gi.region0Count = 8;
                        } else {
                            gi.region0Count = 7;
                        }
                        gi.region1Count = 20 - gi.region0Count;
                    } else {
                        gi.tableSelect[0] = stream.getBits(5);
                        gi.tableSelect[1] = stream.getBits(5);
                        gi.tableSelect[2] = stream.getBits(5);
                        gi.region0Count = stream.getBits(4);
                        gi.region1Count = stream.getBits(3);
                        gi.blockType = 0;
                    }
                    gi.preflag = stream.getBits(1);
                    gi.scaleFactorScale = stream.getBits(1);
                    gi.count1TableSelect = stream.getBits(1);
                }
            }
        } else { // MPEG-2 LSF, MPEG-2.5 LSF
            si.mainDataBegin = stream.getBits(8);
            if (channels == 1) {
                stream.getBits(1);
            } else {
                stream.getBits(2);
            }
            for (int ch = 0; ch < channels; ch++) {
                GrInfo gi = si.ch[ch].gr[0];
                gi.part23Length = stream.getBits(12);
                gi.bigValues = stream.getBits(9);
                gi.globalGain = stream.getBits(8);
                gi.scaleFactorCompress = stream.getBits(9);
                gi.windowSwitching = stream.getBits(1) != 0;
                if (gi.windowSwitching) {
                    gi.blockType = stream.getBits(2);
                    gi.mixedBlock = stream.getBits(1) != 0;
                    gi.tableSelect[0] = stream.getBits(5);
                    gi.tableSelect[1] = stream.getBits(5);
                    gi.subblockGain[0] = stream.getBits(3);
                    gi.subblockGain[1] = stream.getBits(3);
                    gi.subblockGain[2] = stream.getBits(3);
                    // Set regionCount: implicit in this case
                    if (gi.blockType == 0) {
                        throw new IOException("Side info bad: blockType == 0 in split block");
                    } else if (gi.blockType == 2 && !gi.mixedBlock) {
                        gi.region0Count = 8;
                    } else {
                        gi.region0Count = 7;
                        gi.region1Count = 20 - gi.region0Count;
                    }
                } else {
                    gi.tableSelect[0] = stream.getBits(5);
                    gi.tableSelect[1] = stream.getBits(5);
                    gi.tableSelect[2] = stream.getBits(5);
                    gi.region0Count = stream.getBits(4);
                    gi.region1Count = stream.getBits(3);
                    gi.blockType = 0;
                }
                gi.scaleFactorScale = stream.getBits(1);
                gi.count1TableSelect = stream.getBits(1);
            }
        }
    }

    private void getScaleFactors(int ch, int gr) {
        int sfb, window;
        GrInfo gi = si.ch[ch].gr[gr];
        int scaleComp = gi.scaleFactorCompress;
        int[][] slen = Constants.SLEN;
        int length0 = slen[0][scaleComp];
        int length1 = slen[1][scaleComp];
        ScaleFactor sfc = scaleFactors[ch];
        int[] sfl = sfc.l;
        int[][] sfs = sfc.s;
        if (gi.windowSwitching && gi.blockType == 2) {
            if (gi.mixedBlock) {
                for (sfb = 0; sfb < 8; sfb++) {
                    sfl[sfb] = br.getBits(slen[0][gi.scaleFactorCompress]);
                }
                for (sfb = 3; sfb < 6; sfb++) {
                    for (window = 0; window < 3; window++) {
                        sfs[window][sfb] = br.getBits(slen[0][gi.scaleFactorCompress]);
                    }
                }
                for (sfb = 6; sfb < 12; sfb++) {
                    for (window = 0; window < 3; window++) {
                        sfs[window][sfb] = br.getBits(slen[1][gi.scaleFactorCompress]);
                    }
                }
                for (sfb = 12, window = 0; window < 3; window++) {
                    sfs[window][sfb] = 0;
                }
            } else { // SHORT
                sfs[0][0] = br.getBits(length0);
                sfs[1][0] = br.getBits(length0);
                sfs[2][0] = br.getBits(length0);
                sfs[0][1] = br.getBits(length0);
                sfs[1][1] = br.getBits(length0);
                sfs[2][1] = br.getBits(length0);
                sfs[0][2] = br.getBits(length0);
                sfs[1][2] = br.getBits(length0);
                sfs[2][2] = br.getBits(length0);
                sfs[0][3] = br.getBits(length0);
                sfs[1][3] = br.getBits(length0);
                sfs[2][3] = br.getBits(length0);
                sfs[0][4] = br.getBits(length0);
                sfs[1][4] = br.getBits(length0);
                sfs[2][4] = br.getBits(length0);
                sfs[0][5] = br.getBits(length0);
                sfs[1][5] = br.getBits(length0);
                sfs[2][5] = br.getBits(length0);
                sfs[0][6] = br.getBits(length1);
                sfs[1][6] = br.getBits(length1);
                sfs[2][6] = br.getBits(length1);
                sfs[0][7] = br.getBits(length1);
                sfs[1][7] = br.getBits(length1);
                sfs[2][7] = br.getBits(length1);
                sfs[0][8] = br.getBits(length1);
                sfs[1][8] = br.getBits(length1);
                sfs[2][8] = br.getBits(length1);
                sfs[0][9] = br.getBits(length1);
                sfs[1][9] = br.getBits(length1);
                sfs[2][9] = br.getBits(length1);
                sfs[0][10] = br.getBits(length1);
                sfs[1][10] = br.getBits(length1);
                sfs[2][10] = br.getBits(length1);
                sfs[0][11] = br.getBits(length1);
                sfs[1][11] = br.getBits(length1);
                sfs[2][11] = br.getBits(length1);
                sfs[0][12] = 0;
                sfs[1][12] = 0;
                sfs[2][12] = 0;
            } // SHORT
        } else { // LONG types 0,1,3
            if ((si.ch[ch].scfsi[0] == 0) || (gr == 0)) {
                sfl[0] = br.getBits(length0);
                sfl[1] = br.getBits(length0);
                sfl[2] = br.getBits(length0);
                sfl[3] = br.getBits(length0);
                sfl[4] = br.getBits(length0);
                sfl[5] = br.getBits(length0);
            }
            if ((si.ch[ch].scfsi[1] == 0) || (gr == 0)) {
                sfl[6] = br.getBits(length0);
                sfl[7] = br.getBits(length0);
                sfl[8] = br.getBits(length0);
                sfl[9] = br.getBits(length0);
                sfl[10] = br.getBits(length0);
            }
            if ((si.ch[ch].scfsi[2] == 0) || (gr == 0)) {
                sfl[11] = br.getBits(length1);
                sfl[12] = br.getBits(length1);
                sfl[13] = br.getBits(length1);
                sfl[14] = br.getBits(length1);
                sfl[15] = br.getBits(length1);
            }
            if ((si.ch[ch].scfsi[3] == 0) || (gr == 0)) {
                sfl[16] = br.getBits(length1);
                sfl[17] = br.getBits(length1);
                sfl[18] = br.getBits(length1);
                sfl[19] = br.getBits(length1);
                sfl[20] = br.getBits(length1);
            }
            sfl[21] = 0;
            sfl[22] = 0;
        }
    }

    private void getLsfScaleData(int ch, int gr) {
        int scaleFactorComp, intScalefacComp;
        int modeExt = header.modeExtension();
        int blockTypeNumber;
        int blockNumber = 0;
        GrInfo gi = si.ch[ch].gr[gr];
        scaleFactorComp = gi.scaleFactorCompress;
        if (gi.blockType == 2) {
            if (!gi.mixedBlock) {
                blockTypeNumber = 1;
            } else {
                blockTypeNumber = 2;
            }
        } else {
            blockTypeNumber = 0;
        }
        if (!(((modeExt == 1) || (modeExt == 3)) && (ch == 1))) {
            if (scaleFactorComp < 400) {
                newSlen[0] = (scaleFactorComp >>> 4) / 5;
                newSlen[1] = (scaleFactorComp >>> 4) % 5;
                newSlen[2] = (scaleFactorComp & 0xF) >>> 2;
                newSlen[3] = (scaleFactorComp & 3);
                si.ch[ch].gr[gr].preflag = 0;
                blockNumber = 0;
            } else if (scaleFactorComp < 500) {
                newSlen[0] = ((scaleFactorComp - 400) >>> 2) / 5;
                newSlen[1] = ((scaleFactorComp - 400) >>> 2) % 5;
                newSlen[2] = (scaleFactorComp - 400) & 3;
                newSlen[3] = 0;
                si.ch[ch].gr[gr].preflag = 0;
                blockNumber = 1;
            } else if (scaleFactorComp < 512) {
                newSlen[0] = (scaleFactorComp - 500) / 3;
                newSlen[1] = (scaleFactorComp - 500) % 3;
                newSlen[2] = 0;
                newSlen[3] = 0;
                si.ch[ch].gr[gr].preflag = 1;
                blockNumber = 2;
            }
        }
        if ((((modeExt == 1) || (modeExt == 3)) && (ch == 1))) {
            intScalefacComp = scaleFactorComp >>> 1;
            if (intScalefacComp < 180) {
                newSlen[0] = intScalefacComp / 36;
                newSlen[1] = (intScalefacComp % 36) / 6;
                newSlen[2] = (intScalefacComp % 36) % 6;
                newSlen[3] = 0;
                si.ch[ch].gr[gr].preflag = 0;
                blockNumber = 3;
            } else if (intScalefacComp < 244) {
                newSlen[0] = ((intScalefacComp - 180) & 0x3F) >>> 4;
                newSlen[1] = ((intScalefacComp - 180) & 0xF) >>> 2;
                newSlen[2] = (intScalefacComp - 180) & 3;
                newSlen[3] = 0;
                si.ch[ch].gr[gr].preflag = 0;
                blockNumber = 4;
            } else if (intScalefacComp < 255) {
                newSlen[0] = (intScalefacComp - 244) / 3;
                newSlen[1] = (intScalefacComp - 244) % 3;
                newSlen[2] = 0;
                newSlen[3] = 0;
                si.ch[ch].gr[gr].preflag = 0;
                blockNumber = 5;
            }
        }
        for (int x = 0; x < 45; x++) {
            // TODO: why 45, not 54?
            scaleFactorBuffer[x] = 0;
        }
        for (int i = 0, m = 0; i < 4; i++) {
            int len = Constants.NR_OF_SFB_BLOCK[blockNumber][blockTypeNumber][i];
            for (int j = 0; j < len; j++) {
                scaleFactorBuffer[m] = (newSlen[i] == 0) ? 0 : br.getBits(newSlen[i]);
                m++;
            }
        }
    }

    private void getLsfScaleFactors(int ch, int gr) {
        int m = 0;
        int sfb, window;
        GrInfo gi = si.ch[ch].gr[gr];
        getLsfScaleData(ch, gr);
        ScaleFactor sf = scaleFactors[ch];
        if (gi.windowSwitching && (gi.blockType == 2)) {
            if (gi.mixedBlock) {
                for (sfb = 0; sfb < 8; sfb++) {
                    sf.l[sfb] = scaleFactorBuffer[m];
                    m++;
                }
                for (sfb = 3; sfb < 12; sfb++) {
                    for (window = 0; window < 3; window++) {
                        sf.s[window][sfb] = scaleFactorBuffer[m];
                        m++;
                    }
                }
                for (window = 0; window < 3; window++) {
                    sf.s[window][12] = 0;
                }
            } else { // SHORT
                for (sfb = 0; sfb < 12; sfb++) {
                    for (window = 0; window < 3; window++) {
                        sf.s[window][sfb] = scaleFactorBuffer[m];
                        m++;
                    }
                }
                for (window = 0; window < 3; window++) {
                    sf.s[window][12] = 0;
                }
            }
        } else { // LONG types 0,1,3
            for (sfb = 0; sfb < 21; sfb++) {
                sf.l[sfb] = scaleFactorBuffer[m];
                m++;
            }
            sf.l[21] = 0;
            sf.l[22] = 0;
        }
    }

    private void huffmanDecode(final int ch, final int gr) {
        GrInfo gi = si.ch[ch].gr[gr];
        x = y = v = w = 0;
        int part23End = part2Start + gi.part23Length;
        int region1Start;
        int region2Start;
        int buf, buf1;
        Huffman huffman;
        // Find region boundary for short block case
        if (gi.windowSwitching && (gi.blockType == 2)) {
            // Region2.
            // MS: Extrahandling for 8KHZ
            region1Start = (sfreq == 8) ? 72 : 36; // sfb[9/3]*3=36 or in case
            // 8KHZ = 72
            region2Start = 576; // No Region2 for short block case
        } else { // Find region boundary for long block case
            buf = gi.region0Count + 1;
            buf1 = buf + gi.region1Count + 1;
            if (buf1 > Constants.SF_BAND_INDEX[sfreq].l.length - 1) {
                buf1 = Constants.SF_BAND_INDEX[sfreq].l.length - 1;
            }
            region1Start = Constants.SF_BAND_INDEX[sfreq].l[buf];
            region2Start = Constants.SF_BAND_INDEX[sfreq].l[buf1]; /* MI */
        }
        int index = 0;
        for (int i = 0; i < (gi.bigValues << 1); i += 2) {
            if (i < region1Start) {
                huffman = Huffman.HUFFMAN[gi.tableSelect[0]];
            } else if (i < region2Start) {
                huffman = Huffman.HUFFMAN[gi.tableSelect[1]];
            } else {
                huffman = Huffman.HUFFMAN[gi.tableSelect[2]];
            }
            huffman.decode(this, br);
            is1d[index++] = x;
            is1d[index++] = y;
        }
        // Read count1 area
        huffman = Huffman.HUFFMAN[gi.count1TableSelect + 32];
        int numBits = br.getBitCount();
        while ((numBits < part23End) && (index < 576)) {
            huffman.decode(this, br);
            is1d[index++] = v;
            is1d[index++] = w;
            is1d[index++] = x;
            is1d[index++] = y;
            numBits = br.getBitCount();
        }
        if (numBits > part23End) {
            br.rewindBits(numBits - part23End);
            index -= 4;
        }
        numBits = br.getBitCount();
        // Dismiss stuffing bits
        if (numBits < part23End) {
            br.getBits(part23End - numBits);
        }
        // Zero out rest
        if (index < 576) {
            nonzero[ch] = index;
        } else {
            nonzero[ch] = 576;
        }
        if (index < 0) {
            index = 0;
        }
        // may not be necessary
        for (; index < 576; index++) {
            is1d[index] = 0;
        }
    }

    private void iStereoKValues(int pos, int type, int i) {
        if (pos == 0) {
            k0[i] = 1.0f;
            k1[i] = 1.0f;
        } else if ((pos & 1) != 0) {
            k0[i] = Constants.IO[type][(pos + 1) >>> 1];
            k1[i] = 1.0f;
        } else {
            k0[i] = 1.0f;
            k1[i] = Constants.IO[type][pos >>> 1];
        }
    }

    private double getT43(int abv, double globalGain) {
        switch (abv) {
        case 0:
            return 0.0f;
        case 1:
            return globalGain;
        case -1:
            return -globalGain;
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
            return globalGain * Constants.T43[abv];
        case -2:
        case -3:
        case -4:
        case -5:
        case -6:
            return -globalGain * Constants.T43[-abv];
        default:
            if (abv > 0) {
                if (abv < Constants.T43_SIZE) {
                    return globalGain * Constants.T43[abv];
                }
                return globalGain * Math.pow(abv, D43);
            }
            if (-abv < Constants.T43_SIZE) {
                return -globalGain * Constants.T43[-abv];
            }
            return -globalGain * Math.pow(-abv, D43);
        }
    }

    private void dequantizeSample(double[][] xr, int ch, int gr) {
        GrInfo gi = si.ch[ch].gr[gr];
        int nextCb; // next critical band boundary
        Constants.SBI sbif = Constants.SF_BAND_INDEX[sfreq];
        int[] s = sbif.s;
        int[] l = sbif.l;
        int cbWidth = 0;
        int len = nonzero[ch];
        // Compute overall (global) scaling
        double globalGain = Constants.POW2[gi.globalGain];
        for (int i = 0, sb = 0; sb < SBLIMIT; sb++) {
            for (int ss = 0; ss < SSLIMIT; ss++, i++) {
                if (i >= len) {
                    break;
                }
                xr[sb][ss] = getT43(is1d[i], globalGain);
            }
        }
        // choose correct scalefactor band per block type, initalize boundary
        if (gi.windowSwitching && (gi.blockType == 2)) {
            if (gi.mixedBlock) {
                nextCb = l[1];
                // LONG blocks: 0,1,3
            } else {
                cbWidth = s[1];
                nextCb = (cbWidth << 2) - cbWidth;
            }
        } else {
            nextCb = l[1];
            // LONG blocks: 0,1,3
        }
        int cb = 0;
        int cbBegin = 0;
        int index = 0;
        // apply formula per block type
        for (int j = 0; j < len; j++) {
            if (index == nextCb) {
                // adjust critical band boundary
                if (gi.windowSwitching && gi.blockType == 2) {
                    if (gi.mixedBlock) {
                        if (index == l[8]) {
                            nextCb = s[4];
                            nextCb = (nextCb << 2) - nextCb;
                            cb = 3;
                            cbWidth = s[4] - s[3];
                            cbBegin = s[3];
                            cbBegin = (cbBegin << 2) - cbBegin;
                        } else if (index < l[8]) {
                            nextCb = l[(++cb) + 1];
                        } else {
                            nextCb = s[(++cb) + 1];
                            nextCb = (nextCb << 2) - nextCb;
                            cbBegin = s[cb];
                            cbWidth = s[cb + 1] - cbBegin;
                            cbBegin = (cbBegin << 2) - cbBegin;
                        }
                    } else {
                        nextCb = s[(++cb) + 1];
                        nextCb = (nextCb << 2) - nextCb;
                        cbBegin = s[cb];
                        cbWidth = s[cb + 1] - cbBegin;
                        cbBegin = (cbBegin << 2) - cbBegin;
                    }
                } else { // long blocks
                    nextCb = l[(++cb) + 1];
                }
            }
            int sb = j / SSLIMIT;
            int ss = j - sb * SSLIMIT; // % SSLIMIT
            // Do long/short dependent scaling operations
            int idx;
            if (gi.windowSwitching && gi.blockType == 2 && (!gi.mixedBlock || j >= 36)) {
                int ti = (index - cbBegin) / cbWidth;
                idx = scaleFactors[ch].s[ti][cb] << gi.scaleFactorScale;
                idx += (gi.subblockGain[ti] << 2);
            } else {
                // LONG block types 0,1,3 & 1st 2 subbands of switched blocks
                idx = scaleFactors[ch].l[cb];
                if (gi.preflag != 0) {
                    idx += Constants.PRETAB[cb];
                }
                idx = idx << gi.scaleFactorScale;
            }
            xr[sb][ss] *= Constants.TWO_TO_NEGATIVE_HALF_POW[idx];
            index++;
        }
        for (int j = len; j < 576; j++) {
            int sb = j / SSLIMIT;
            int ss = j - sb * SSLIMIT; // % SSLIMIT
            xr[sb][ss] = 0.0f;
        }
        return;
    }

    private void reorder(double[][] xr, int ch, int gr) {
        GrInfo gi = si.ch[ch].gr[gr];
        if (gi.windowSwitching && gi.blockType == 2) {
            for (int index = 0; index < 576; index++) {
                out1d[index] = 0.0f;
            }
            if (gi.mixedBlock) {
                // NO REORDER FOR LOW 2 SUBBANDS
                for (int index = 0; index < 36; index++) {
                    int sb = index / SSLIMIT;
                    int ss = index - sb * SSLIMIT; // % SSLIMIT
                    out1d[index] = xr[sb][ss];
                }
                // REORDERING FOR REST SWITCHED SHORT
                for (int sfb = 3; sfb < 13; sfb++) {
                    int sfbStart = Constants.SF_BAND_INDEX[sfreq].s[sfb];
                    int sfbLines = Constants.SF_BAND_INDEX[sfreq].s[sfb + 1] - sfbStart;
                    int sfbStart3 = (sfbStart << 2) - sfbStart;
                    for (int freq = 0, freq3 = 0; freq < sfbLines; freq++, freq3 += 3) {
                        int srcLine = sfbStart3 + freq;
                        int desLine = sfbStart3 + freq3;
                        int sb = srcLine / SSLIMIT;
                        int ss = srcLine - sb * SSLIMIT; // % SSLIMIT
                        out1d[desLine] = xr[sb][ss];
                        srcLine += sfbLines;
                        desLine++;
                        sb = srcLine / SSLIMIT;
                        ss = srcLine - sb * SSLIMIT; // % SSLIMIT
                        out1d[desLine] = xr[sb][ss];
                        srcLine += sfbLines;
                        desLine++;
                        sb = srcLine / SSLIMIT;
                        ss = srcLine - sb * SSLIMIT; // % SSLIMIT
                        out1d[desLine] = xr[sb][ss];
                    }
                }
            } else {
                // pure short
                int[] reorder = Constants.REORDER_TABLE[sfreq];
                for (int index = 0; index < 576; index++) {
                    int j = reorder[index];
                    int sb = j / SSLIMIT;
                    int ss = j - sb * SSLIMIT; // % SSLIMIT
                    out1d[index] = xr[sb][ss];
                }
            }
        } else {
            // long blocks
            for (int i = 0, sb = 0; sb < SBLIMIT; sb++) {
                for (int ss = 0; ss < SSLIMIT; ss++, i++) {
                    out1d[i] = xr[sb][ss];
                }
            }
        }
    }

    private void stereo(int gr) {
        if (channels == 1) { // mono , bypass xr[0][][] to lr[0][][]
            for (int sb = 0; sb < SBLIMIT; sb++) {
                for (int ss = 0; ss < SSLIMIT; ss += 3) {
                    lr0[sb][ss] = ro0[sb][ss];
                    lr0[sb][ss + 1] = ro0[sb][ss + 1];
                    lr0[sb][ss + 2] = ro0[sb][ss + 2];
                }
            }
            return;
        }
        GrInfo gi = si.ch[0].gr[gr];
        int modeExt = header.modeExtension();
        int sfb;
        int temp, temp2;
        boolean msStereo = ((header.mode() == Header.MODE_JOINT_STEREO) && ((modeExt & 0x2) != 0));
        boolean iStereo = ((header.mode() == Header.MODE_JOINT_STEREO) && ((modeExt & 0x1) != 0));
        boolean lsf = ((header.version() == Header.VERSION_MPEG2_LSF || header.version() == Header.VERSION_MPEG25_LSF));
        int ioType = (gi.scaleFactorCompress & 1);
        for (int i = 0; i < 576; i++) {
            isPos[i] = 7;
            isRatio[i] = 0.0f;
        }
        if (iStereo) {
            Constants.SBI sbif = Constants.SF_BAND_INDEX[sfreq];
            int[] s = sbif.s;
            int[] l = sbif.l;
            if (gi.windowSwitching && gi.blockType == 2) {
                if (gi.mixedBlock) {
                    int maxSfb = 0;
                    for (int j = 0; j < 3; j++) {
                        int sfbcnt = 2;
                        for (sfb = 12; sfb >= 3; sfb--) {
                            int i = s[sfb];
                            int lines = s[sfb + 1] - i;
                            i = (i << 2) - i + (j + 1) * lines - 1;
                            while (lines > 0) {
                                if (ro1[i / 18][i % 18] != 0.0f) {
                                    sfbcnt = sfb;
                                    sfb = -10;
                                    lines = -10;
                                }
                                lines--;
                                i--;
                            }
                        }
                        sfb = sfbcnt + 1;
                        if (sfb > maxSfb) {
                            maxSfb = sfb;
                        }
                        while (sfb < 12) {
                            temp = s[sfb];
                            int sb = s[sfb + 1] - temp;
                            int i = (temp << 2) - temp + j * sb;
                            for (; sb > 0; sb--) {
                                isPos[i] = scaleFactors[1].s[j][sfb];
                                if (isPos[i] != 7) {
                                    if (lsf) {
                                        iStereoKValues(isPos[i], ioType, i);
                                    } else {
                                        isRatio[i] = Constants.TAN12[isPos[i]];
                                    }
                                }
                                i++;
                            }
                            sfb++;
                        }
                        sfb = s[10];
                        int sb = s[11] - sfb;
                        sfb = (sfb << 2) - sfb + j * sb;
                        temp = s[11];
                        sb = s[12] - temp;
                        int i = (temp << 2) - temp + j * sb;
                        for (; sb > 0; sb--) {
                            isPos[i] = isPos[sfb];
                            if (lsf) {
                                k0[i] = k0[sfb];
                                k1[i] = k1[sfb];
                            } else {
                                isRatio[i] = isRatio[sfb];
                            }
                            i++;
                        }
                    }
                    if (maxSfb <= 3) {
                        int i = 2;
                        int ss = 17;
                        int sb = -1;
                        while (i >= 0) {
                            if (ro1[i][ss] != 0.0f) {
                                sb = (i << 4) + (i << 1) + ss;
                                i = -1;
                            } else {
                                ss--;
                                if (ss < 0) {
                                    i--;
                                    ss = 17;
                                }
                            }
                        }
                        i = 0;
                        while (l[i] <= sb) {
                            i++;
                        }
                        sfb = i;
                        i = l[i];
                        for (; sfb < 8; sfb++) {
                            sb = l[sfb + 1] - l[sfb];
                            for (; sb > 0; sb--) {
                                isPos[i] = scaleFactors[1].l[sfb];
                                if (isPos[i] != 7) {
                                    if (lsf) {
                                        iStereoKValues(isPos[i], ioType, i);
                                    } else {
                                        isRatio[i] = Constants.TAN12[isPos[i]];
                                    }
                                }
                                i++;
                            }
                        }
                    }
                } else {
                    for (int j = 0; j < 3; j++) {
                        int sfbcnt;
                        sfbcnt = -1;
                        for (sfb = 12; sfb >= 0; sfb--) {
                            temp = s[sfb];
                            int lines = s[sfb + 1] - temp;
                            int i = (temp << 2) - temp + (j + 1) * lines - 1;
                            while (lines > 0) {
                                if (ro1[i / 18][i % 18] != 0.0f) {
                                    sfbcnt = sfb;
                                    sfb = -10;
                                    lines = -10;
                                }
                                lines--;
                                i--;
                            }
                        }
                        sfb = sfbcnt + 1;
                        while (sfb < 12) {
                            temp = s[sfb];
                            int sb = s[sfb + 1] - temp;
                            int i = (temp << 2) - temp + j * sb;
                            for (; sb > 0; sb--) {
                                isPos[i] = scaleFactors[1].s[j][sfb];
                                if (isPos[i] != 7) {
                                    if (lsf) {
                                        iStereoKValues(isPos[i], ioType, i);
                                    } else {
                                        isRatio[i] = Constants.TAN12[isPos[i]];
                                    }
                                }
                                i++;
                            } // for (; sb>0 ...
                            sfb++;
                        } // while (sfb<12)
                        temp = s[10];
                        temp2 = s[11];
                        int sb = temp2 - temp;
                        sfb = (temp << 2) - temp + j * sb;
                        sb = s[12] - temp2;
                        int i = (temp2 << 2) - temp2 + j * sb;
                        for (; sb > 0; sb--) {
                            isPos[i] = isPos[sfb];
                            if (lsf) {
                                k0[i] = k0[sfb];
                                k1[i] = k1[sfb];
                            } else {
                                isRatio[i] = isRatio[sfb];
                            }
                            i++;
                        }
                    }
                }
            } else {
                int i = 31;
                int ss = 17;
                int sb = 0;
                while (i >= 0) {
                    if (ro1[i][ss] != 0.0f) {
                        sb = (i << 4) + (i << 1) + ss;
                        i = -1;
                    } else {
                        ss--;
                        if (ss < 0) {
                            i--;
                            ss = 17;
                        }
                    }
                }
                i = 0;
                while (l[i] <= sb) {
                    i++;
                }
                sfb = i;
                i = l[i];
                for (; sfb < 21; sfb++) {
                    sb = l[sfb + 1] - l[sfb];
                    for (; sb > 0; sb--) {
                        isPos[i] = scaleFactors[1].l[sfb];
                        if (isPos[i] != 7) {
                            if (lsf) {
                                iStereoKValues(isPos[i], ioType, i);
                            } else {
                                isRatio[i] = Constants.TAN12[isPos[i]];
                            }
                        }
                        i++;
                    }
                }
                sfb = l[20];
                for (sb = 576 - l[21]; (sb > 0) && (i < 576); sb--) {
                    isPos[i] = isPos[sfb]; // error here : i >=576
                    if (lsf) {
                        k0[i] = k0[sfb];
                        k1[i] = k1[sfb];
                    } else {
                        isRatio[i] = isRatio[sfb];
                    }
                    i++;
                }
            }
        }
        for (int sb = 0, i = 0; sb < SBLIMIT; sb++) {
            for (int ss = 0; ss < SSLIMIT; ss++) {
                if (isPos[i] == 7) {
                    if (msStereo) {
                        lr0[sb][ss] = (ro0[sb][ss] + ro1[sb][ss]) * 0.707106781f;
                        lr1[sb][ss] = (ro0[sb][ss] - ro1[sb][ss]) * 0.707106781f;
                    } else {
                        lr0[sb][ss] = ro0[sb][ss];
                        lr1[sb][ss] = ro1[sb][ss];
                    }
                } else if (iStereo) {
                    if (lsf) {
                        lr0[sb][ss] = ro0[sb][ss] * k0[i];
                        lr1[sb][ss] = ro0[sb][ss] * k1[i];
                    } else {
                        lr1[sb][ss] = ro0[sb][ss] / (1 + isRatio[i]);
                        lr0[sb][ss] = lr1[sb][ss] * isRatio[i];
                    }
                }
                i++;
            }
        }
    }

    private void antialias(int ch, int gr) {
        int sb18, ss, sb18lim;
        GrInfo gi = si.ch[ch].gr[gr];
        // 31 alias-reduction operations between each pair of sub-bands
        // with 8 butterflies between each pair
        if (gi.windowSwitching && (gi.blockType == 2) && !gi.mixedBlock) {
            return;
        }
        if (gi.windowSwitching && gi.mixedBlock && (gi.blockType == 2)) {
            sb18lim = 18;
        } else {
            sb18lim = 558;
        }
        for (sb18 = 0; sb18 < sb18lim; sb18 += 18) {
            for (ss = 0; ss < 8; ss++) {
                int srcIdx1 = sb18 + 17 - ss;
                int srcIdx2 = sb18 + 18 + ss;
                double bu = out1d[srcIdx1];
                double bd = out1d[srcIdx2];
                out1d[srcIdx1] = (bu * Constants.CS[ss]) - (bd * Constants.CA[ss]);
                out1d[srcIdx2] = (bd * Constants.CS[ss]) + (bu * Constants.CA[ss]);
            }
        }
    }

    private void hybrid(int ch, int gr) {
        GrInfo gi = si.ch[ch].gr[gr];
        for (int sb18 = 0; sb18 < 576; sb18 += 18) {
            int bt = (gi.windowSwitching && gi.mixedBlock && (sb18 < 36)) ? 0 : gi.blockType;
            double[] tsOut = out1d;
            double[] r = rawout;
            for (int cc = 0; cc < 18; cc++) {
                tsOutCopy[cc] = tsOut[cc + sb18];
            }
            fastInvMdct(tsOutCopy, r, bt);
            for (int cc = 0; cc < 18; cc++) {
                tsOut[cc + sb18] = tsOutCopy[cc];
            }
            // overlap addition
            double[] p = prevBlock[ch];
            tsOut[0 + sb18] = r[0] + p[sb18 + 0];
            p[sb18 + 0] = r[18];
            tsOut[1 + sb18] = r[1] + p[sb18 + 1];
            p[sb18 + 1] = r[19];
            tsOut[2 + sb18] = r[2] + p[sb18 + 2];
            p[sb18 + 2] = r[20];
            tsOut[3 + sb18] = r[3] + p[sb18 + 3];
            p[sb18 + 3] = r[21];
            tsOut[4 + sb18] = r[4] + p[sb18 + 4];
            p[sb18 + 4] = r[22];
            tsOut[5 + sb18] = r[5] + p[sb18 + 5];
            p[sb18 + 5] = r[23];
            tsOut[6 + sb18] = r[6] + p[sb18 + 6];
            p[sb18 + 6] = r[24];
            tsOut[7 + sb18] = r[7] + p[sb18 + 7];
            p[sb18 + 7] = r[25];
            tsOut[8 + sb18] = r[8] + p[sb18 + 8];
            p[sb18 + 8] = r[26];
            tsOut[9 + sb18] = r[9] + p[sb18 + 9];
            p[sb18 + 9] = r[27];
            tsOut[10 + sb18] = r[10] + p[sb18 + 10];
            p[sb18 + 10] = r[28];
            tsOut[11 + sb18] = r[11] + p[sb18 + 11];
            p[sb18 + 11] = r[29];
            tsOut[12 + sb18] = r[12] + p[sb18 + 12];
            p[sb18 + 12] = r[30];
            tsOut[13 + sb18] = r[13] + p[sb18 + 13];
            p[sb18 + 13] = r[31];
            tsOut[14 + sb18] = r[14] + p[sb18 + 14];
            p[sb18 + 14] = r[32];
            tsOut[15 + sb18] = r[15] + p[sb18 + 15];
            p[sb18 + 15] = r[33];
            tsOut[16 + sb18] = r[16] + p[sb18 + 16];
            p[sb18 + 16] = r[34];
            tsOut[17 + sb18] = r[17] + p[sb18 + 17];
            p[sb18 + 17] = r[35];
        }
    }

    private void fastInvMdct(double[] in, double[] out, int blockType) {
        double t0, t1, t2, t3, t4, t5, t6, t7, t8, t9;
        double t10, t11, t12, t13, t14, t15, t16, t17;
        if (blockType == 2) {
            for (int p = 0; p < 36; p += 9) {
                out[p] = out[p + 1] = out[p + 2] = out[p + 3] = out[p + 4] = 0.0f;
                out[p + 5] = out[p + 6] = out[p + 7] = out[p + 8] = 0.0f;
            }
            int sixI = 0;
            for (int i = 0; i < 3; i++) {
                // 12 point IMDCT
                // Begin 12 point IDCT
                // Input aliasing for 12 pt IDCT
                in[15 + i] += in[12 + i];
                in[12 + i] += in[9 + i];
                in[9 + i] += in[6 + i];
                in[6 + i] += in[3 + i];
                in[3 + i] += in[0 + i];
                // Input aliasing on odd indices (for 6 point IDCT)
                in[15 + i] += in[9 + i];
                in[9 + i] += in[3 + i];
                // 3 point IDCT on even indices
                double pp1, pp2, sum;
                pp2 = in[12 + i] * 0.500000000f;
                pp1 = in[6 + i] * 0.866025403f;
                sum = in[0 + i] + pp2;
                t1 = in[0 + i] - in[12 + i];
                t0 = sum + pp1;
                t2 = sum - pp1;
                // End 3 point IDCT on even indices
                // 3 point IDCT on odd indices (for 6 point IDCT)
                pp2 = in[15 + i] * 0.500000000f;
                pp1 = in[9 + i] * 0.866025403f;
                sum = in[3 + i] + pp2;
                t4 = in[3 + i] - in[15 + i];
                t5 = sum + pp1;
                t3 = sum - pp1;
                // End 3 point IDCT on odd indices
                // Twiddle factors on odd indices (for 6 point IDCT)
                t3 *= 1.931851653f;
                t4 *= 0.707106781f;
                t5 *= 0.517638090f;
                // Output butterflies on 2 3 point IDCT's (for 6 point IDCT)
                double save = t0;
                t0 += t5;
                t5 = save - t5;
                save = t1;
                t1 += t4;
                t4 = save - t4;
                save = t2;
                t2 += t3;
                t3 = save - t3;
                // End 6 point IDCT
                // Twiddle factors on indices (for 12 point IDCT)
                t0 *= 0.504314480f;
                t1 *= 0.541196100f;
                t2 *= 0.630236207f;
                t3 *= 0.821339815f;
                t4 *= 1.306562965f;
                t5 *= 3.830648788f;
                // End 12 point IDCT
                // Shift to 12 point modified IDCT, multiply by window type 2
                t8 = -t0 * 0.793353340f;
                t9 = -t0 * 0.608761429f;
                t7 = -t1 * 0.923879532f;
                t10 = -t1 * 0.382683432f;
                t6 = -t2 * 0.991444861f;
                t11 = -t2 * 0.130526192f;
                t0 = t3;
                t1 = t4 * 0.382683432f;
                t2 = t5 * 0.608761429f;
                t3 = -t5 * 0.793353340f;
                t4 = -t4 * 0.923879532f;
                t5 = -t0 * 0.991444861f;
                t0 *= 0.130526192f;
                out[sixI + 6] += t0;
                out[sixI + 7] += t1;
                out[sixI + 8] += t2;
                out[sixI + 9] += t3;
                out[sixI + 10] += t4;
                out[sixI + 11] += t5;
                out[sixI + 12] += t6;
                out[sixI + 13] += t7;
                out[sixI + 14] += t8;
                out[sixI + 15] += t9;
                out[sixI + 16] += t10;
                out[sixI + 17] += t11;
                sixI += 6;
            }
        } else {
            // 36 point IDCT
            // input aliasing for 36 point IDCT
            in[17] += in[16];
            in[16] += in[15];
            in[15] += in[14];
            in[14] += in[13];
            in[13] += in[12];
            in[12] += in[11];
            in[11] += in[10];
            in[10] += in[9];
            in[9] += in[8];
            in[8] += in[7];
            in[7] += in[6];
            in[6] += in[5];
            in[5] += in[4];
            in[4] += in[3];
            in[3] += in[2];
            in[2] += in[1];
            in[1] += in[0];
            // 18 point IDCT for odd indices
            // input aliasing for 18 point IDCT
            in[17] += in[15];
            in[15] += in[13];
            in[13] += in[11];
            in[11] += in[9];
            in[9] += in[7];
            in[7] += in[5];
            in[5] += in[3];
            in[3] += in[1];
            double tmp0, tmp1, tmp2, tmp3, tmp4, tmp0b, tmp1b, tmp2b, tmp3b;
            double tmp0o, tmp1o, tmp2o, tmp3o, tmp4o, tmp0ob, tmp1ob, tmp2ob, tmp3ob;
            // Fast 9 Point Inverse Discrete Cosine Transform
            //
            // By Francois-Raymond Boyer
            // mailto:boyerf@iro.umontreal.ca
            // http://www.iro.umontreal.ca/~boyerf
            //
            // The code has been optimized for Intel processors
            // (takes a lot of time to convert double to and from iternal FPU
            // representation)
            //
            // It is a simple "factorization" of the IDCT matrix.
            // 9 point IDCT on even indices
            // 5 points on odd indices (not really an IDCT)
            double i00 = in[0] + in[0];
            double iip12 = i00 + in[12];
            tmp0 = iip12 + in[4] * 1.8793852415718f + in[8] * 1.532088886238f + in[16] * 0.34729635533386f;
            tmp1 = i00 + in[4] - in[8] - in[12] - in[12] - in[16];
            tmp2 = iip12 - in[4] * 0.34729635533386f - in[8] * 1.8793852415718f + in[16] * 1.532088886238f;
            tmp3 = iip12 - in[4] * 1.532088886238f + in[8] * 0.34729635533386f - in[16] * 1.8793852415718f;
            tmp4 = in[0] - in[4] + in[8] - in[12] + in[16];
            // 4 points on even indices
            double i6s = in[6] * 1.732050808f; // Sqrt[3]
            tmp0b = in[2] * 1.9696155060244f + i6s + in[10] * 1.2855752193731f + in[14] * 0.68404028665134f;
            tmp1b = (in[2] - in[10] - in[14]) * 1.732050808f;
            tmp2b = in[2] * 1.2855752193731f - i6s - in[10] * 0.68404028665134f + in[14] * 1.9696155060244f;
            tmp3b = in[2] * 0.68404028665134f - i6s + in[10] * 1.9696155060244f - in[14] * 1.2855752193731f;
            // 9 point IDCT on odd indices
            // 5 points on odd indices (not really an IDCT)
            double i0 = in[0 + 1] + in[0 + 1];
            double i0p12 = i0 + in[12 + 1];
            tmp0o = i0p12 + in[4 + 1] * 1.8793852415718f + in[8 + 1] * 1.532088886238f + in[16 + 1] * 0.34729635533386f;
            tmp1o = i0 + in[4 + 1] - in[8 + 1] - in[12 + 1] - in[12 + 1] - in[16 + 1];
            tmp2o = i0p12 - in[4 + 1] * 0.34729635533386f - in[8 + 1] * 1.8793852415718f + in[16 + 1] * 1.532088886238f;
            tmp3o = i0p12 - in[4 + 1] * 1.532088886238f + in[8 + 1] * 0.34729635533386f - in[16 + 1] * 1.8793852415718f;
            tmp4o = (in[0 + 1] - in[4 + 1] + in[8 + 1] - in[12 + 1] + in[16 + 1]) * 0.707106781f; // Twiddled
            // 4 points on even indices
            double i7s = in[6 + 1] * 1.732050808f; // Sqrt[3]
            tmp0ob = in[2 + 1] * 1.9696155060244f + i7s + in[10 + 1] * 1.2855752193731f + in[14 + 1]
                    * 0.68404028665134f;
            tmp1ob = (in[2 + 1] - in[10 + 1] - in[14 + 1]) * 1.732050808f;
            tmp2ob = in[2 + 1] * 1.2855752193731f - i7s - in[10 + 1] * 0.68404028665134f + in[14 + 1]
                    * 1.9696155060244f;
            tmp3ob = in[2 + 1] * 0.68404028665134f - i7s + in[10 + 1] * 1.9696155060244f - in[14 + 1]
                    * 1.2855752193731f;
            // Twiddle factors on odd indices and
            // Butterflies on 9 point IDCT's and
            // twiddle factors for 36 point IDCT
            double e, o;
            e = tmp0 + tmp0b;
            o = (tmp0o + tmp0ob) * 0.501909918f;
            t0 = e + o;
            t17 = e - o;
            e = tmp1 + tmp1b;
            o = (tmp1o + tmp1ob) * 0.517638090f;
            t1 = e + o;
            t16 = e - o;
            e = tmp2 + tmp2b;
            o = (tmp2o + tmp2ob) * 0.551688959f;
            t2 = e + o;
            t15 = e - o;
            e = tmp3 + tmp3b;
            o = (tmp3o + tmp3ob) * 0.610387294f;
            t3 = e + o;
            t14 = e - o;
            t4 = tmp4 + tmp4o;
            t13 = tmp4 - tmp4o;
            e = tmp3 - tmp3b;
            o = (tmp3o - tmp3ob) * 0.871723397f;
            t5 = e + o;
            t12 = e - o;
            e = tmp2 - tmp2b;
            o = (tmp2o - tmp2ob) * 1.183100792f;
            t6 = e + o;
            t11 = e - o;
            e = tmp1 - tmp1b;
            o = (tmp1o - tmp1ob) * 1.931851653f;
            t7 = e + o;
            t10 = e - o;
            e = tmp0 - tmp0b;
            o = (tmp0o - tmp0ob) * 5.736856623f;
            t8 = e + o;
            t9 = e - o;
            // end 36 point IDCT */
            // shift to modified IDCT
            double[] win = Constants.WIN[blockType];
            out[0] = -t9 * win[0];
            out[1] = -t10 * win[1];
            out[2] = -t11 * win[2];
            out[3] = -t12 * win[3];
            out[4] = -t13 * win[4];
            out[5] = -t14 * win[5];
            out[6] = -t15 * win[6];
            out[7] = -t16 * win[7];
            out[8] = -t17 * win[8];
            out[9] = t17 * win[9];
            out[10] = t16 * win[10];
            out[11] = t15 * win[11];
            out[12] = t14 * win[12];
            out[13] = t13 * win[13];
            out[14] = t12 * win[14];
            out[15] = t11 * win[15];
            out[16] = t10 * win[16];
            out[17] = t9 * win[17];
            out[18] = t8 * win[18];
            out[19] = t7 * win[19];
            out[20] = t6 * win[20];
            out[21] = t5 * win[21];
            out[22] = t4 * win[22];
            out[23] = t3 * win[23];
            out[24] = t2 * win[24];
            out[25] = t1 * win[25];
            out[26] = t0 * win[26];
            out[27] = t0 * win[27];
            out[28] = t1 * win[28];
            out[29] = t2 * win[29];
            out[30] = t3 * win[30];
            out[31] = t4 * win[31];
            out[32] = t5 * win[32];
            out[33] = t6 * win[33];
            out[34] = t7 * win[34];
            out[35] = t8 * win[35];
        }
    }
}

final class SynthesisFilter {
    private double[] v1 = new double[512];
    private double[] v2 = new double[512];
    private double[] actualV = v1; // v1 or v2
    private int actualWritePos = 15; // 0-15
    private double[] samples = new double[32]; // 32 new subband samples
    private int channel;
    private double scaleFactor;
    private double[] tmpOutBuffer = new double[32];
    // DOUBLE
    private static final double MY_PI = 3.14159265358979323846;
    private static final double COS1_64 = divCos(MY_PI / 64.0);
    private static final double COS3_64 = divCos(MY_PI * 3.0 / 64.0);
    private static final double COS5_64 = divCos(MY_PI * 5.0 / 64.0);
    private static final double COS7_64 = divCos(MY_PI * 7.0 / 64.0);
    private static final double COS9_64 = divCos(MY_PI * 9.0 / 64.0);
    private static final double COS11_64 = divCos(MY_PI * 11.0 / 64.0);
    private static final double COS13_64 = divCos(MY_PI * 13.0 / 64.0);
    private static final double COS15_64 = divCos(MY_PI * 15.0 / 64.0);
    private static final double COS17_64 = divCos(MY_PI * 17.0 / 64.0);
    private static final double COS19_64 = divCos(MY_PI * 19.0 / 64.0);
    private static final double COS21_64 = divCos(MY_PI * 21.0 / 64.0);
    private static final double COS23_64 = divCos(MY_PI * 23.0 / 64.0);
    private static final double COS25_64 = divCos(MY_PI * 25.0 / 64.0);
    private static final double COS27_64 = divCos(MY_PI * 27.0 / 64.0);
    private static final double COS29_64 = divCos(MY_PI * 29.0 / 64.0);
    private static final double COS31_64 = divCos(MY_PI * 31.0 / 64.0);
    private static final double COS1_32 = divCos(MY_PI / 32.0);
    private static final double COS3_32 = divCos(MY_PI * 3.0 / 32.0);
    private static final double COS5_32 = divCos(MY_PI * 5.0 / 32.0);
    private static final double COS7_32 = divCos(MY_PI * 7.0 / 32.0);
    private static final double COS9_32 = divCos(MY_PI * 9.0 / 32.0);
    private static final double COS11_32 = divCos(MY_PI * 11.0 / 32.0);
    private static final double COS13_32 = divCos(MY_PI * 13.0 / 32.0);
    private static final double COS15_32 = divCos(MY_PI * 15.0 / 32.0);
    private static final double COS1_16 = divCos(MY_PI / 16.0);
    private static final double COS3_16 = divCos(MY_PI * 3.0 / 16.0);
    private static final double COS5_16 = divCos(MY_PI * 5.0 / 16.0);
    private static final double COS7_16 = divCos(MY_PI * 7.0 / 16.0);
    private static final double COS1_8 = divCos(MY_PI / 8.0);
    private static final double COS3_8 = divCos(MY_PI * 3.0 / 8.0);
    private static final double COS1_4 = divCos(MY_PI / 4.0);
    private static final double[][] D16 = Constants.D16;

    /**
     * Contructor. The scalefactor scales the calculated double pcm samples to short values (raw pcm samples are in
     * [-1.0, 1.0], if no violations occur).
     */
    SynthesisFilter(int channelNumber, double factor) {
        channel = channelNumber;
        scaleFactor = factor;
    }

    private static double divCos(double a) {
        return (1.0 / (2.0 * Math.cos(a)));
    }

    /**
     * Compute new values via a fast cosine transform.
     */
    private void computeNewV() {
        double nv0, nv1, nv2, nv3, nv4, nv5, nv6, nv7, nv8, nv9;
        double nv10, nv11, nv12, nv13, nv14, nv15, nv16, nv17, nv18, nv19;
        double nv20, nv21, nv22, nv23, nv24, nv25, nv26, nv27, nv28, nv29;
        double nv30, nv31;
        double[] s = samples;
        double s0 = s[0];
        double s1 = s[1];
        double s2 = s[2];
        double s3 = s[3];
        double s4 = s[4];
        double s5 = s[5];
        double s6 = s[6];
        double s7 = s[7];
        double s8 = s[8];
        double s9 = s[9];
        double s10 = s[10];
        double s11 = s[11];
        double s12 = s[12];
        double s13 = s[13];
        double s14 = s[14];
        double s15 = s[15];
        double s16 = s[16];
        double s17 = s[17];
        double s18 = s[18];
        double s19 = s[19];
        double s20 = s[20];
        double s21 = s[21];
        double s22 = s[22];
        double s23 = s[23];
        double s24 = s[24];
        double s25 = s[25];
        double s26 = s[26];
        double s27 = s[27];
        double s28 = s[28];
        double s29 = s[29];
        double s30 = s[30];
        double s31 = s[31];
        double p0 = s0 + s31;
        double p1 = s1 + s30;
        double p2 = s2 + s29;
        double p3 = s3 + s28;
        double p4 = s4 + s27;
        double p5 = s5 + s26;
        double p6 = s6 + s25;
        double p7 = s7 + s24;
        double p8 = s8 + s23;
        double p9 = s9 + s22;
        double p10 = s10 + s21;
        double p11 = s11 + s20;
        double p12 = s12 + s19;
        double p13 = s13 + s18;
        double p14 = s14 + s17;
        double p15 = s15 + s16;
        double pp0 = p0 + p15;
        double pp1 = p1 + p14;
        double pp2 = p2 + p13;
        double pp3 = p3 + p12;
        double pp4 = p4 + p11;
        double pp5 = p5 + p10;
        double pp6 = p6 + p9;
        double pp7 = p7 + p8;
        double pp8 = (p0 - p15) * COS1_32;
        double pp9 = (p1 - p14) * COS3_32;
        double pp10 = (p2 - p13) * COS5_32;
        double pp11 = (p3 - p12) * COS7_32;
        double pp12 = (p4 - p11) * COS9_32;
        double pp13 = (p5 - p10) * COS11_32;
        double pp14 = (p6 - p9) * COS13_32;
        double pp15 = (p7 - p8) * COS15_32;
        p0 = pp0 + pp7;
        p1 = pp1 + pp6;
        p2 = pp2 + pp5;
        p3 = pp3 + pp4;
        p4 = (pp0 - pp7) * COS1_16;
        p5 = (pp1 - pp6) * COS3_16;
        p6 = (pp2 - pp5) * COS5_16;
        p7 = (pp3 - pp4) * COS7_16;
        p8 = pp8 + pp15;
        p9 = pp9 + pp14;
        p10 = pp10 + pp13;
        p11 = pp11 + pp12;
        p12 = (pp8 - pp15) * COS1_16;
        p13 = (pp9 - pp14) * COS3_16;
        p14 = (pp10 - pp13) * COS5_16;
        p15 = (pp11 - pp12) * COS7_16;
        pp0 = p0 + p3;
        pp1 = p1 + p2;
        pp2 = (p0 - p3) * COS1_8;
        pp3 = (p1 - p2) * COS3_8;
        pp4 = p4 + p7;
        pp5 = p5 + p6;
        pp6 = (p4 - p7) * COS1_8;
        pp7 = (p5 - p6) * COS3_8;
        pp8 = p8 + p11;
        pp9 = p9 + p10;
        pp10 = (p8 - p11) * COS1_8;
        pp11 = (p9 - p10) * COS3_8;
        pp12 = p12 + p15;
        pp13 = p13 + p14;
        pp14 = (p12 - p15) * COS1_8;
        pp15 = (p13 - p14) * COS3_8;
        p0 = pp0 + pp1;
        p1 = (pp0 - pp1) * COS1_4;
        p2 = pp2 + pp3;
        p3 = (pp2 - pp3) * COS1_4;
        p4 = pp4 + pp5;
        p5 = (pp4 - pp5) * COS1_4;
        p6 = pp6 + pp7;
        p7 = (pp6 - pp7) * COS1_4;
        p8 = pp8 + pp9;
        p9 = (pp8 - pp9) * COS1_4;
        p10 = pp10 + pp11;
        p11 = (pp10 - pp11) * COS1_4;
        p12 = pp12 + pp13;
        p13 = (pp12 - pp13) * COS1_4;
        p14 = pp14 + pp15;
        p15 = (pp14 - pp15) * COS1_4;
        // this is pretty insane coding
        double tmp1;
        nv19 = -(nv4 = (nv12 = p7) + p5) - p6; // 36-17
        nv27 = -p6 - p7 - p4; // 44-17
        nv6 = (nv10 = (nv14 = p15) + p11) + p13;
        nv17 = -(nv2 = p15 + p13 + p9) - p14; // 34-17
        nv21 = (tmp1 = -p14 - p15 - p10 - p11) - p13; // 38-17
        nv29 = -p14 - p15 - p12 - p8; // 46-17
        nv25 = tmp1 - p12; // 42-17
        nv31 = -p0; // 48-17
        nv0 = p1;
        nv23 = -(nv8 = p3) - p2; // 40-17
        p0 = (s0 - s31) * COS1_64;
        p1 = (s1 - s30) * COS3_64;
        p2 = (s2 - s29) * COS5_64;
        p3 = (s3 - s28) * COS7_64;
        p4 = (s4 - s27) * COS9_64;
        p5 = (s5 - s26) * COS11_64;
        p6 = (s6 - s25) * COS13_64;
        p7 = (s7 - s24) * COS15_64;
        p8 = (s8 - s23) * COS17_64;
        p9 = (s9 - s22) * COS19_64;
        p10 = (s10 - s21) * COS21_64;
        p11 = (s11 - s20) * COS23_64;
        p12 = (s12 - s19) * COS25_64;
        p13 = (s13 - s18) * COS27_64;
        p14 = (s14 - s17) * COS29_64;
        p15 = (s15 - s16) * COS31_64;
        pp0 = p0 + p15;
        pp1 = p1 + p14;
        pp2 = p2 + p13;
        pp3 = p3 + p12;
        pp4 = p4 + p11;
        pp5 = p5 + p10;
        pp6 = p6 + p9;
        pp7 = p7 + p8;
        pp8 = (p0 - p15) * COS1_32;
        pp9 = (p1 - p14) * COS3_32;
        pp10 = (p2 - p13) * COS5_32;
        pp11 = (p3 - p12) * COS7_32;
        pp12 = (p4 - p11) * COS9_32;
        pp13 = (p5 - p10) * COS11_32;
        pp14 = (p6 - p9) * COS13_32;
        pp15 = (p7 - p8) * COS15_32;
        p0 = pp0 + pp7;
        p1 = pp1 + pp6;
        p2 = pp2 + pp5;
        p3 = pp3 + pp4;
        p4 = (pp0 - pp7) * COS1_16;
        p5 = (pp1 - pp6) * COS3_16;
        p6 = (pp2 - pp5) * COS5_16;
        p7 = (pp3 - pp4) * COS7_16;
        p8 = pp8 + pp15;
        p9 = pp9 + pp14;
        p10 = pp10 + pp13;
        p11 = pp11 + pp12;
        p12 = (pp8 - pp15) * COS1_16;
        p13 = (pp9 - pp14) * COS3_16;
        p14 = (pp10 - pp13) * COS5_16;
        p15 = (pp11 - pp12) * COS7_16;
        pp0 = p0 + p3;
        pp1 = p1 + p2;
        pp2 = (p0 - p3) * COS1_8;
        pp3 = (p1 - p2) * COS3_8;
        pp4 = p4 + p7;
        pp5 = p5 + p6;
        pp6 = (p4 - p7) * COS1_8;
        pp7 = (p5 - p6) * COS3_8;
        pp8 = p8 + p11;
        pp9 = p9 + p10;
        pp10 = (p8 - p11) * COS1_8;
        pp11 = (p9 - p10) * COS3_8;
        pp12 = p12 + p15;
        pp13 = p13 + p14;
        pp14 = (p12 - p15) * COS1_8;
        pp15 = (p13 - p14) * COS3_8;
        p0 = pp0 + pp1;
        p1 = (pp0 - pp1) * COS1_4;
        p2 = pp2 + pp3;
        p3 = (pp2 - pp3) * COS1_4;
        p4 = pp4 + pp5;
        p5 = (pp4 - pp5) * COS1_4;
        p6 = pp6 + pp7;
        p7 = (pp6 - pp7) * COS1_4;
        p8 = pp8 + pp9;
        p9 = (pp8 - pp9) * COS1_4;
        p10 = pp10 + pp11;
        p11 = (pp10 - pp11) * COS1_4;
        p12 = pp12 + pp13;
        p13 = (pp12 - pp13) * COS1_4;
        p14 = pp14 + pp15;
        p15 = (pp14 - pp15) * COS1_4;
        // manually doing something that a compiler should handle sucks
        // coding like this is hard to read
        double tmp2;
        nv5 = (nv11 = (nv13 = (nv15 = p15) + p7) + p11) + p5 + p13;
        nv7 = (nv9 = p15 + p11 + p3) + p13;
        nv16 = -(nv1 = (tmp1 = p13 + p15 + p9) + p1) - p14; // 33-17
        nv18 = -(nv3 = tmp1 + p5 + p7) - p6 - p14; // 35-17
        nv22 = (tmp1 = -p10 - p11 - p14 - p15) - p13 - p2 - p3; // 39-17
        nv20 = tmp1 - p13 - p5 - p6 - p7; // 37-17
        nv24 = tmp1 - p12 - p2 - p3; // 41-17
        nv26 = tmp1 - p12 - (tmp2 = p4 + p6 + p7); // 43-17
        nv30 = (tmp1 = -p8 - p12 - p14 - p15) - p0; // 47-17
        nv28 = tmp1 - tmp2; // 45-17
        // insert V[0-15] (== nv[0-15]) into actual v:
        // double[] x2 = actual_v + actual_write_pos;
        double[] dest = actualV;
        int pos = actualWritePos;
        dest[0 + pos] = nv0;
        dest[16 + pos] = nv1;
        dest[32 + pos] = nv2;
        dest[48 + pos] = nv3;
        dest[64 + pos] = nv4;
        dest[80 + pos] = nv5;
        dest[96 + pos] = nv6;
        dest[112 + pos] = nv7;
        dest[128 + pos] = nv8;
        dest[144 + pos] = nv9;
        dest[160 + pos] = nv10;
        dest[176 + pos] = nv11;
        dest[192 + pos] = nv12;
        dest[208 + pos] = nv13;
        dest[224 + pos] = nv14;
        dest[240 + pos] = nv15;
        // V[16] is always 0.0:
        dest[256 + pos] = 0.0f;
        // insert V[17-31] (== -nv[15-1]) into actual v:
        dest[272 + pos] = -nv15;
        dest[288 + pos] = -nv14;
        dest[304 + pos] = -nv13;
        dest[320 + pos] = -nv12;
        dest[336 + pos] = -nv11;
        dest[352 + pos] = -nv10;
        dest[368 + pos] = -nv9;
        dest[384 + pos] = -nv8;
        dest[400 + pos] = -nv7;
        dest[416 + pos] = -nv6;
        dest[432 + pos] = -nv5;
        dest[448 + pos] = -nv4;
        dest[464 + pos] = -nv3;
        dest[480 + pos] = -nv2;
        dest[496 + pos] = -nv1;
        // insert V[32] (== -nv[0]) into other v:
        dest = (actualV == v1) ? v2 : v1;
        dest[0 + pos] = -nv0;
        // insert V[33-48] (== nv[16-31]) into other v:
        dest[16 + pos] = nv16;
        dest[32 + pos] = nv17;
        dest[48 + pos] = nv18;
        dest[64 + pos] = nv19;
        dest[80 + pos] = nv20;
        dest[96 + pos] = nv21;
        dest[112 + pos] = nv22;
        dest[128 + pos] = nv23;
        dest[144 + pos] = nv24;
        dest[160 + pos] = nv25;
        dest[176 + pos] = nv26;
        dest[192 + pos] = nv27;
        dest[208 + pos] = nv28;
        dest[224 + pos] = nv29;
        dest[240 + pos] = nv30;
        dest[256 + pos] = nv31;
        // insert V[49-63] (== nv[30-16]) into other v:
        dest[272 + pos] = nv30;
        dest[288 + pos] = nv29;
        dest[304 + pos] = nv28;
        dest[320 + pos] = nv27;
        dest[336 + pos] = nv26;
        dest[352 + pos] = nv25;
        dest[368 + pos] = nv24;
        dest[384 + pos] = nv23;
        dest[400 + pos] = nv22;
        dest[416 + pos] = nv21;
        dest[432 + pos] = nv20;
        dest[448 + pos] = nv19;
        dest[464 + pos] = nv18;
        dest[480 + pos] = nv17;
        dest[496 + pos] = nv16;
    }

    private void computePcmSamples0() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[0 + dvp] * dp[0]) + (vp[15 + dvp] * dp[1]) + (vp[14 + dvp] * dp[2]) + (vp[13 + dvp] * dp[3]) + (vp[12 + dvp] * dp[4]) + (vp[11 + dvp] * dp[5])
                    + (vp[10 + dvp] * dp[6]) + (vp[9 + dvp] * dp[7]) + (vp[8 + dvp] * dp[8]) + (vp[7 + dvp] * dp[9]) + (vp[6 + dvp] * dp[10]) + (vp[5 + dvp] * dp[11])
                    + (vp[4 + dvp] * dp[12]) + (vp[3 + dvp] * dp[13]) + (vp[2 + dvp] * dp[14]) + (vp[1 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples1() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[1 + dvp] * dp[0]) + (vp[0 + dvp] * dp[1]) + (vp[15 + dvp] * dp[2]) + (vp[14 + dvp] * dp[3]) + (vp[13 + dvp] * dp[4]) + (vp[12 + dvp] * dp[5])
                    + (vp[11 + dvp] * dp[6]) + (vp[10 + dvp] * dp[7]) + (vp[9 + dvp] * dp[8]) + (vp[8 + dvp] * dp[9]) + (vp[7 + dvp] * dp[10]) + (vp[6 + dvp] * dp[11])
                    + (vp[5 + dvp] * dp[12]) + (vp[4 + dvp] * dp[13]) + (vp[3 + dvp] * dp[14]) + (vp[2 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples2() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[2 + dvp] * dp[0]) + (vp[1 + dvp] * dp[1]) + (vp[0 + dvp] * dp[2]) + (vp[15 + dvp] * dp[3]) + (vp[14 + dvp] * dp[4]) + (vp[13 + dvp] * dp[5])
                    + (vp[12 + dvp] * dp[6]) + (vp[11 + dvp] * dp[7]) + (vp[10 + dvp] * dp[8]) + (vp[9 + dvp] * dp[9]) + (vp[8 + dvp] * dp[10]) + (vp[7 + dvp] * dp[11])
                    + (vp[6 + dvp] * dp[12]) + (vp[5 + dvp] * dp[13]) + (vp[4 + dvp] * dp[14]) + (vp[3 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples3() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[3 + dvp] * dp[0]) + (vp[2 + dvp] * dp[1]) + (vp[1 + dvp] * dp[2]) + (vp[0 + dvp] * dp[3]) + (vp[15 + dvp] * dp[4]) + (vp[14 + dvp] * dp[5])
                    + (vp[13 + dvp] * dp[6]) + (vp[12 + dvp] * dp[7]) + (vp[11 + dvp] * dp[8]) + (vp[10 + dvp] * dp[9]) + (vp[9 + dvp] * dp[10]) + (vp[8 + dvp] * dp[11])
                    + (vp[7 + dvp] * dp[12]) + (vp[6 + dvp] * dp[13]) + (vp[5 + dvp] * dp[14]) + (vp[4 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples4() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[4 + dvp] * dp[0]) + (vp[3 + dvp] * dp[1]) + (vp[2 + dvp] * dp[2]) + (vp[1 + dvp] * dp[3]) + (vp[0 + dvp] * dp[4]) + (vp[15 + dvp] * dp[5])
                    + (vp[14 + dvp] * dp[6]) + (vp[13 + dvp] * dp[7]) + (vp[12 + dvp] * dp[8]) + (vp[11 + dvp] * dp[9]) + (vp[10 + dvp] * dp[10]) + (vp[9 + dvp] * dp[11])
                    + (vp[8 + dvp] * dp[12]) + (vp[7 + dvp] * dp[13]) + (vp[6 + dvp] * dp[14]) + (vp[5 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples5() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[5 + dvp] * dp[0]) + (vp[4 + dvp] * dp[1]) + (vp[3 + dvp] * dp[2]) + (vp[2 + dvp] * dp[3]) + (vp[1 + dvp] * dp[4]) + (vp[0 + dvp] * dp[5])
                    + (vp[15 + dvp] * dp[6]) + (vp[14 + dvp] * dp[7]) + (vp[13 + dvp] * dp[8]) + (vp[12 + dvp] * dp[9]) + (vp[11 + dvp] * dp[10]) + (vp[10 + dvp] * dp[11])
                    + (vp[9 + dvp] * dp[12]) + (vp[8 + dvp] * dp[13]) + (vp[7 + dvp] * dp[14]) + (vp[6 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples6() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[6 + dvp] * dp[0]) + (vp[5 + dvp] * dp[1]) + (vp[4 + dvp] * dp[2]) + (vp[3 + dvp] * dp[3]) + (vp[2 + dvp] * dp[4]) + (vp[1 + dvp] * dp[5])
                    + (vp[0 + dvp] * dp[6]) + (vp[15 + dvp] * dp[7]) + (vp[14 + dvp] * dp[8]) + (vp[13 + dvp] * dp[9]) + (vp[12 + dvp] * dp[10]) + (vp[11 + dvp] * dp[11])
                    + (vp[10 + dvp] * dp[12]) + (vp[9 + dvp] * dp[13]) + (vp[8 + dvp] * dp[14]) + (vp[7 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples7() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[7 + dvp] * dp[0]) + (vp[6 + dvp] * dp[1]) + (vp[5 + dvp] * dp[2]) + (vp[4 + dvp] * dp[3]) + (vp[3 + dvp] * dp[4]) + (vp[2 + dvp] * dp[5])
                    + (vp[1 + dvp] * dp[6]) + (vp[0 + dvp] * dp[7]) + (vp[15 + dvp] * dp[8]) + (vp[14 + dvp] * dp[9]) + (vp[13 + dvp] * dp[10]) + (vp[12 + dvp] * dp[11])
                    + (vp[11 + dvp] * dp[12]) + (vp[10 + dvp] * dp[13]) + (vp[9 + dvp] * dp[14]) + (vp[8 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples8() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[8 + dvp] * dp[0]) + (vp[7 + dvp] * dp[1]) + (vp[6 + dvp] * dp[2]) + (vp[5 + dvp] * dp[3]) + (vp[4 + dvp] * dp[4]) + (vp[3 + dvp] * dp[5])
                    + (vp[2 + dvp] * dp[6]) + (vp[1 + dvp] * dp[7]) + (vp[0 + dvp] * dp[8]) + (vp[15 + dvp] * dp[9]) + (vp[14 + dvp] * dp[10]) + (vp[13 + dvp] * dp[11])
                    + (vp[12 + dvp] * dp[12]) + (vp[11 + dvp] * dp[13]) + (vp[10 + dvp] * dp[14]) + (vp[9 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples9() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[9 + dvp] * dp[0]) + (vp[8 + dvp] * dp[1]) + (vp[7 + dvp] * dp[2]) + (vp[6 + dvp] * dp[3]) + (vp[5 + dvp] * dp[4]) + (vp[4 + dvp] * dp[5])
                    + (vp[3 + dvp] * dp[6]) + (vp[2 + dvp] * dp[7]) + (vp[1 + dvp] * dp[8]) + (vp[0 + dvp] * dp[9]) + (vp[15 + dvp] * dp[10]) + (vp[14 + dvp] * dp[11])
                    + (vp[13 + dvp] * dp[12]) + (vp[12 + dvp] * dp[13]) + (vp[11 + dvp] * dp[14]) + (vp[10 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples10() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[10 + dvp] * dp[0]) + (vp[9 + dvp] * dp[1]) + (vp[8 + dvp] * dp[2]) + (vp[7 + dvp] * dp[3]) + (vp[6 + dvp] * dp[4]) + (vp[5 + dvp] * dp[5])
                    + (vp[4 + dvp] * dp[6]) + (vp[3 + dvp] * dp[7]) + (vp[2 + dvp] * dp[8]) + (vp[1 + dvp] * dp[9]) + (vp[0 + dvp] * dp[10]) + (vp[15 + dvp] * dp[11])
                    + (vp[14 + dvp] * dp[12]) + (vp[13 + dvp] * dp[13]) + (vp[12 + dvp] * dp[14]) + (vp[11 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples11() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[11 + dvp] * dp[0]) + (vp[10 + dvp] * dp[1]) + (vp[9 + dvp] * dp[2]) + (vp[8 + dvp] * dp[3]) + (vp[7 + dvp] * dp[4]) + (vp[6 + dvp] * dp[5])
                    + (vp[5 + dvp] * dp[6]) + (vp[4 + dvp] * dp[7]) + (vp[3 + dvp] * dp[8]) + (vp[2 + dvp] * dp[9]) + (vp[1 + dvp] * dp[10]) + (vp[0 + dvp] * dp[11])
                    + (vp[15 + dvp] * dp[12]) + (vp[14 + dvp] * dp[13]) + (vp[13 + dvp] * dp[14]) + (vp[12 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples12() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[12 + dvp] * dp[0]) + (vp[11 + dvp] * dp[1]) + (vp[10 + dvp] * dp[2]) + (vp[9 + dvp] * dp[3]) + (vp[8 + dvp] * dp[4]) + (vp[7 + dvp] * dp[5])
                    + (vp[6 + dvp] * dp[6]) + (vp[5 + dvp] * dp[7]) + (vp[4 + dvp] * dp[8]) + (vp[3 + dvp] * dp[9]) + (vp[2 + dvp] * dp[10]) + (vp[1 + dvp] * dp[11])
                    + (vp[0 + dvp] * dp[12]) + (vp[15 + dvp] * dp[13]) + (vp[14 + dvp] * dp[14]) + (vp[13 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples13() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[13 + dvp] * dp[0]) + (vp[12 + dvp] * dp[1]) + (vp[11 + dvp] * dp[2]) + (vp[10 + dvp] * dp[3]) + (vp[9 + dvp] * dp[4]) + (vp[8 + dvp] * dp[5])
                    + (vp[7 + dvp] * dp[6]) + (vp[6 + dvp] * dp[7]) + (vp[5 + dvp] * dp[8]) + (vp[4 + dvp] * dp[9]) + (vp[3 + dvp] * dp[10]) + (vp[2 + dvp] * dp[11])
                    + (vp[1 + dvp] * dp[12]) + (vp[0 + dvp] * dp[13]) + (vp[15 + dvp] * dp[14]) + (vp[14 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples14() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[14 + dvp] * dp[0]) + (vp[13 + dvp] * dp[1]) + (vp[12 + dvp] * dp[2]) + (vp[11 + dvp] * dp[3]) + (vp[10 + dvp] * dp[4]) + (vp[9 + dvp] * dp[5])
                    + (vp[8 + dvp] * dp[6]) + (vp[7 + dvp] * dp[7]) + (vp[6 + dvp] * dp[8]) + (vp[5 + dvp] * dp[9]) + (vp[4 + dvp] * dp[10]) + (vp[3 + dvp] * dp[11])
                    + (vp[2 + dvp] * dp[12]) + (vp[1 + dvp] * dp[13]) + (vp[0 + dvp] * dp[14]) + (vp[15 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples15() {
        final double[] vp = actualV;
        final double[] tmpOut = tmpOutBuffer;
        int dvp = 0;
        for (int i = 0; i < 32; i++) {
            final double[] dp = D16[i];
            double pcmSample = (((vp[15 + dvp] * dp[0]) + (vp[14 + dvp] * dp[1]) + (vp[13 + dvp] * dp[2]) + (vp[12 + dvp] * dp[3]) + (vp[11 + dvp] * dp[4])
                    + (vp[10 + dvp] * dp[5]) + (vp[9 + dvp] * dp[6]) + (vp[8 + dvp] * dp[7]) + (vp[7 + dvp] * dp[8]) + (vp[6 + dvp] * dp[9]) + (vp[5 + dvp] * dp[10])
                    + (vp[4 + dvp] * dp[11]) + (vp[3 + dvp] * dp[12]) + (vp[2 + dvp] * dp[13]) + (vp[1 + dvp] * dp[14]) + (vp[0 + dvp] * dp[15])) * scaleFactor);
            tmpOut[i] = pcmSample;
            dvp += 16;
        }
    }

    private void computePcmSamples() {
        switch (actualWritePos) {
        case 0:
            computePcmSamples0();
            break;
        case 1:
            computePcmSamples1();
            break;
        case 2:
            computePcmSamples2();
            break;
        case 3:
            computePcmSamples3();
            break;
        case 4:
            computePcmSamples4();
            break;
        case 5:
            computePcmSamples5();
            break;
        case 6:
            computePcmSamples6();
            break;
        case 7:
            computePcmSamples7();
            break;
        case 8:
            computePcmSamples8();
            break;
        case 9:
            computePcmSamples9();
            break;
        case 10:
            computePcmSamples10();
            break;
        case 11:
            computePcmSamples11();
            break;
        case 12:
            computePcmSamples12();
            break;
        case 13:
            computePcmSamples13();
            break;
        case 14:
            computePcmSamples14();
            break;
        case 15:
            computePcmSamples15();
            break;
        default:
        }
    }

    /**
     * Calculate 32 PCM samples and write them
     */
    void calculatePcmSamples(double[] s, Decoder player) {
        for (int i = 0; i < 32; i++) {
            samples[i] = s[i];
        }
        computeNewV();
        computePcmSamples();
        player.appendSamples(channel, tmpOutBuffer);
        actualWritePos = (actualWritePos + 1) & 0xf;
        actualV = (actualV == v1) ? v2 : v1;
    }
}