package link.locutus.discord.util.io;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

public class BitBuffer {

    private static final long[] MASK = new long[Long.SIZE + 1];
    static {
        for (int i = 0; i < Long.SIZE; i++) {
            MASK[i] = (1L << i) - 1;
        }
        MASK[Long.SIZE] = -1L;
    }

    private long buffer;
    private int bitsInBuffer;
    public final ByteBuffer byteBuffer;

    public BitBuffer(ByteBuffer byteBuffer) {
        this.buffer = 0L;
        this.bitsInBuffer = 0;
        this.byteBuffer = byteBuffer;
    }

    public void writeBits(long value, int count) {
        int remainingBits = 64 - bitsInBuffer;

        // If the new value fits entirely within the buffer, write it directly.
        if (count <= remainingBits) {
            long mask = MASK[count];
            buffer |= (value & mask) << bitsInBuffer;
            bitsInBuffer += count;
            if (bitsInBuffer == 64) {
                flush();
            }
        } else {
            // Write as much as possible to the current buffer.
            buffer |= (value & MASK[remainingBits]) << bitsInBuffer;
            flush();

            // Write the remaining bits in another iteration.
            int remainingCount = count - remainingBits;
            buffer |= (value >>> remainingBits) & MASK[remainingCount];
            bitsInBuffer = remainingCount;
        }
    }

    public long readBits(int count) {
        long result;
        if (count <= bitsInBuffer) {
            result = buffer & MASK[count];
            buffer >>>= count;
            bitsInBuffer -= count;
        } else {
            int remainingCount = count - bitsInBuffer;
            result = buffer & MASK[bitsInBuffer];
            buffer = byteBuffer.getLong();
            result |= (buffer & MASK[remainingCount]) << bitsInBuffer;
            buffer >>>= remainingCount;
            bitsInBuffer = 64 - remainingCount;
        }

        return result;
    }

    private void fill() {
        buffer = byteBuffer.getLong();
        bitsInBuffer = 64;
    }

    private void flush() {
        byteBuffer.putLong(buffer);
        buffer = 0;
        bitsInBuffer = 0;
    }

    public byte[] getWrittenBytes() {
        int numBytesInBuffer = (bitsInBuffer + 7) / 8;
        byte[] bytes = new byte[byteBuffer.position() + numBytesInBuffer];
        System.arraycopy(byteBuffer.array(), 0, bytes, 0, byteBuffer.position());
        if (bitsInBuffer > 0) {
            int lastByteIndex = byteBuffer.position();
            for (int i = 0; i < numBytesInBuffer; i++) {
                bytes[lastByteIndex + i] = (byte) buffer;
                buffer >>>= 8;
            }
        }
        return bytes;
    }

    public long readLong() {
        return readBits(Long.SIZE);
    }

    public void setBytes(byte[] data) {
        byteBuffer.clear();
        byteBuffer.put(data);
        byteBuffer.position(0);
        buffer = 0;
        bitsInBuffer = 0;
    }

    public boolean readBit() {
        return readBits(1) == 1;
    }

    public void writeBit(boolean value) {
        writeBits(value ? 1 : 0, 1);
    }

    public int readInt() {
        return (int) readBits(Integer.SIZE);
    }

    public int readByte() {
        return (int) readBits(Byte.SIZE);
    }

    public char readChar() {
        return (char) readBits(Character.SIZE);
    }

    public short readShort() {
        return (short) readBits(Short.SIZE);
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public void writeInt(int value) {
        writeBits(value, Integer.SIZE);
    }

    public void writeByte(int value) {
        writeBits(value, Byte.SIZE);
    }

    public void writeChar(char value) {
        writeBits(value, Character.SIZE);
    }

    public void writeShort(short value) {
        writeBits(value, Short.SIZE);
    }

    public void writeLong(long value) {
        writeBits(value, Long.SIZE);
    }

    public void writeFloat(float value) {
        writeInt(Float.floatToIntBits(value));
    }

    public void writeDouble(double value) {
        writeLong(Double.doubleToLongBits(value));
    }

    public BitBuffer reset() {
        byteBuffer.position(0);
        buffer = 0;
        bitsInBuffer = 0;
        return this;
    }

    public void writeVarInt(int value) {
        while ((value & -128) != 0) {
            writeByte(value & 127 | 128);
            value >>>= 7;
        }
        writeByte(value);
    }

    public int readVarInt() {
        int value = 0;
        int i = 0;
        int b;
        while (((b = readByte()) & 128) != 0) {
            value |= (b & 127) << i;
            i += 7;
            if (i > 35)
                throw new RuntimeException("VarInt too big");
        }
        return value | b << i;
    }

    public void writeVarLong(long value) {
        while ((value & -128L) != 0L) {
            writeByte((int) (value & 127L) | 128);
            value >>>= 7;
        }
        writeByte((int) value);
    }

    public long readVarLong() {
        long value = 0L;
        int i = 0;
        long b;
        while (((b = readByte()) & 128L) != 0L) {
            value |= (b & 127L) << i;
            i += 7;
            if (i > 63)
                throw new RuntimeException("VarLong too big");
        }
        return value | b << i;
    }
}
