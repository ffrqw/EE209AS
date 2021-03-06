package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.NumberOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class WriterBasedJsonGenerator extends JsonGeneratorImpl {
    protected static final char[] HEX_CHARS = CharTypes.copyHexChars();
    protected SerializableString _currentEscape;
    protected char[] _entityBuffer;
    protected char[] _outputBuffer;
    protected int _outputEnd;
    protected int _outputHead;
    protected int _outputTail;
    protected char _quoteChar = '\"';
    protected final Writer _writer;

    public WriterBasedJsonGenerator(IOContext ctxt, int features, ObjectCodec codec, Writer w) {
        super(ctxt, features, codec);
        this._writer = w;
        this._outputBuffer = ctxt.allocConcatBuffer();
        this._outputEnd = this._outputBuffer.length;
    }

    public final Object getOutputTarget() {
        return this._writer;
    }

    public final int getOutputBuffered() {
        return Math.max(0, this._outputTail - this._outputHead);
    }

    public final boolean canWriteFormattedNumbers() {
        return true;
    }

    public final void writeFieldName(String name) throws IOException {
        boolean z = true;
        int status = this._writeContext.writeFieldName(name);
        if (status == 4) {
            _reportError("Can not write a field name, expecting a value");
        }
        if (status != 1) {
            z = false;
        }
        _writeFieldName(name, z);
    }

    public final void writeFieldName(SerializableString name) throws IOException {
        boolean z = true;
        int status = this._writeContext.writeFieldName(name.getValue());
        if (status == 4) {
            _reportError("Can not write a field name, expecting a value");
        }
        if (status != 1) {
            z = false;
        }
        _writeFieldName(name, z);
    }

    protected final void _writeFieldName(String name, boolean commaBefore) throws IOException {
        if (this._cfgPrettyPrinter != null) {
            _writePPFieldName(name, commaBefore);
            return;
        }
        if (this._outputTail + 1 >= this._outputEnd) {
            _flushBuffer();
        }
        if (commaBefore) {
            char[] cArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            cArr[i] = ',';
        }
        if (this._cfgUnqNames) {
            _writeString(name);
            return;
        }
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        _writeString(name);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
    }

    protected final void _writeFieldName(SerializableString name, boolean commaBefore) throws IOException {
        if (this._cfgPrettyPrinter != null) {
            _writePPFieldName(name, commaBefore);
            return;
        }
        if (this._outputTail + 1 >= this._outputEnd) {
            _flushBuffer();
        }
        if (commaBefore) {
            char[] cArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            cArr[i] = ',';
        }
        char[] quoted = name.asQuotedChars();
        if (this._cfgUnqNames) {
            writeRaw(quoted, 0, quoted.length);
            return;
        }
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        int qlen = quoted.length;
        if ((this._outputTail + qlen) + 1 >= this._outputEnd) {
            writeRaw(quoted, 0, qlen);
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            cArr = this._outputBuffer;
            i = this._outputTail;
            this._outputTail = i + 1;
            cArr[i] = this._quoteChar;
            return;
        }
        System.arraycopy(quoted, 0, this._outputBuffer, this._outputTail, qlen);
        this._outputTail += qlen;
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
    }

    public final void writeStartArray() throws IOException {
        _verifyValueWrite("start an array");
        this._writeContext = this._writeContext.createChildArrayContext();
        if (this._cfgPrettyPrinter != null) {
            this._cfgPrettyPrinter.writeStartArray(this);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = '[';
    }

    public final void writeEndArray() throws IOException {
        if (!this._writeContext.inArray()) {
            _reportError("Current context not Array but " + this._writeContext.typeDesc());
        }
        if (this._cfgPrettyPrinter != null) {
            this._cfgPrettyPrinter.writeEndArray(this, this._writeContext.getEntryCount());
        } else {
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            char[] cArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            cArr[i] = ']';
        }
        this._writeContext = this._writeContext.clearAndGetParent();
    }

    public final void writeStartObject(Object forValue) throws IOException {
        _verifyValueWrite("start an object");
        JsonWriteContext ctxt = this._writeContext.createChildObjectContext();
        this._writeContext = ctxt;
        if (forValue != null) {
            ctxt.setCurrentValue(forValue);
        }
        if (this._cfgPrettyPrinter != null) {
            this._cfgPrettyPrinter.writeStartObject(this);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = '{';
    }

    public final void writeStartObject() throws IOException {
        _verifyValueWrite("start an object");
        this._writeContext = this._writeContext.createChildObjectContext();
        if (this._cfgPrettyPrinter != null) {
            this._cfgPrettyPrinter.writeStartObject(this);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = '{';
    }

    public final void writeEndObject() throws IOException {
        if (!this._writeContext.inObject()) {
            _reportError("Current context not Object but " + this._writeContext.typeDesc());
        }
        if (this._cfgPrettyPrinter != null) {
            this._cfgPrettyPrinter.writeEndObject(this, this._writeContext.getEntryCount());
        } else {
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            char[] cArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            cArr[i] = '}';
        }
        this._writeContext = this._writeContext.clearAndGetParent();
    }

    protected final void _writePPFieldName(String name, boolean commaBefore) throws IOException {
        if (commaBefore) {
            this._cfgPrettyPrinter.writeObjectEntrySeparator(this);
        } else {
            this._cfgPrettyPrinter.beforeObjectEntries(this);
        }
        if (this._cfgUnqNames) {
            _writeString(name);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        _writeString(name);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
    }

    protected final void _writePPFieldName(SerializableString name, boolean commaBefore) throws IOException {
        if (commaBefore) {
            this._cfgPrettyPrinter.writeObjectEntrySeparator(this);
        } else {
            this._cfgPrettyPrinter.beforeObjectEntries(this);
        }
        char[] quoted = name.asQuotedChars();
        if (this._cfgUnqNames) {
            writeRaw(quoted, 0, quoted.length);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        writeRaw(quoted, 0, quoted.length);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
    }

    public final void writeString(String text) throws IOException {
        _verifyValueWrite("write a string");
        if (text == null) {
            _writeNull();
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        _writeString(text);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
    }

    public final void writeString(char[] text, int offset, int len) throws IOException {
        _verifyValueWrite("write a string");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        _writeString(text, offset, len);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
    }

    public final void writeString(SerializableString sstr) throws IOException {
        _verifyValueWrite("write a string");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        char[] text = sstr.asQuotedChars();
        int len = text.length;
        if (len < 32) {
            if (len > this._outputEnd - this._outputTail) {
                _flushBuffer();
            }
            System.arraycopy(text, 0, this._outputBuffer, this._outputTail, len);
            this._outputTail += len;
        } else {
            _flushBuffer();
            this._writer.write(text, 0, len);
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
    }

    public final void writeRawUTF8String(byte[] text, int offset, int length) throws IOException {
        _reportUnsupportedOperation();
    }

    public final void writeUTF8String(byte[] text, int offset, int length) throws IOException {
        _reportUnsupportedOperation();
    }

    public final void writeRaw(String text) throws IOException {
        int len = text.length();
        int room = this._outputEnd - this._outputTail;
        if (room == 0) {
            _flushBuffer();
            room = this._outputEnd - this._outputTail;
        }
        if (room >= len) {
            text.getChars(0, len, this._outputBuffer, this._outputTail);
            this._outputTail += len;
            return;
        }
        writeRawLong(text);
    }

    public final void writeRaw(String text, int start, int len) throws IOException {
        int room = this._outputEnd - this._outputTail;
        if (room < len) {
            _flushBuffer();
            room = this._outputEnd - this._outputTail;
        }
        if (room >= len) {
            text.getChars(start, start + len, this._outputBuffer, this._outputTail);
            this._outputTail += len;
            return;
        }
        writeRawLong(text.substring(start, start + len));
    }

    public final void writeRaw(SerializableString text) throws IOException {
        writeRaw(text.getValue());
    }

    public final void writeRaw(char[] text, int offset, int len) throws IOException {
        if (len < 32) {
            if (len > this._outputEnd - this._outputTail) {
                _flushBuffer();
            }
            System.arraycopy(text, offset, this._outputBuffer, this._outputTail, len);
            this._outputTail += len;
            return;
        }
        _flushBuffer();
        this._writer.write(text, offset, len);
    }

    public final void writeRaw(char c) throws IOException {
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = c;
    }

    private void writeRawLong(String text) throws IOException {
        int room = this._outputEnd - this._outputTail;
        text.getChars(0, room, this._outputBuffer, this._outputTail);
        this._outputTail += room;
        _flushBuffer();
        int offset = room;
        int len = text.length() - room;
        while (len > this._outputEnd) {
            int amount = this._outputEnd;
            text.getChars(offset, offset + amount, this._outputBuffer, 0);
            this._outputHead = 0;
            this._outputTail = amount;
            _flushBuffer();
            offset += amount;
            len -= amount;
        }
        text.getChars(offset, offset + len, this._outputBuffer, 0);
        this._outputHead = 0;
        this._outputTail = len;
    }

    public final void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException, JsonGenerationException {
        _verifyValueWrite("write a binary value");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        _writeBinary(b64variant, data, offset, offset + len);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
    }

    public final int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws IOException, JsonGenerationException {
        int bytes;
        _verifyValueWrite("write a binary value");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        byte[] encodingBuffer = this._ioContext.allocBase64Buffer();
        if (dataLength < 0) {
            try {
                bytes = _writeBinary(b64variant, data, encodingBuffer);
            } catch (Throwable th) {
                this._ioContext.releaseBase64Buffer(encodingBuffer);
            }
        } else {
            int missing = _writeBinary(b64variant, data, encodingBuffer, dataLength);
            if (missing > 0) {
                _reportError("Too few bytes available: missing " + missing + " bytes (out of " + dataLength + ")");
            }
            bytes = dataLength;
        }
        this._ioContext.releaseBase64Buffer(encodingBuffer);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        return bytes;
    }

    public final void writeNumber(short s) throws IOException {
        _verifyValueWrite("write a number");
        if (this._cfgNumbersAsStrings) {
            _writeQuotedShort(s);
            return;
        }
        if (this._outputTail + 6 >= this._outputEnd) {
            _flushBuffer();
        }
        this._outputTail = NumberOutput.outputInt((int) s, this._outputBuffer, this._outputTail);
    }

    private void _writeQuotedShort(short s) throws IOException {
        if (this._outputTail + 8 >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        this._outputTail = NumberOutput.outputInt((int) s, this._outputBuffer, this._outputTail);
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
    }

    public final void writeNumber(int i) throws IOException {
        _verifyValueWrite("write a number");
        if (this._cfgNumbersAsStrings) {
            _writeQuotedInt(i);
            return;
        }
        if (this._outputTail + 11 >= this._outputEnd) {
            _flushBuffer();
        }
        this._outputTail = NumberOutput.outputInt(i, this._outputBuffer, this._outputTail);
    }

    private void _writeQuotedInt(int i) throws IOException {
        if (this._outputTail + 13 >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr[i2] = this._quoteChar;
        this._outputTail = NumberOutput.outputInt(i, this._outputBuffer, this._outputTail);
        cArr = this._outputBuffer;
        i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr[i2] = this._quoteChar;
    }

    public final void writeNumber(long l) throws IOException {
        _verifyValueWrite("write a number");
        if (this._cfgNumbersAsStrings) {
            _writeQuotedLong(l);
            return;
        }
        if (this._outputTail + 21 >= this._outputEnd) {
            _flushBuffer();
        }
        this._outputTail = NumberOutput.outputLong(l, this._outputBuffer, this._outputTail);
    }

    private void _writeQuotedLong(long l) throws IOException {
        if (this._outputTail + 23 >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        this._outputTail = NumberOutput.outputLong(l, this._outputBuffer, this._outputTail);
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
    }

    public final void writeNumber(BigInteger value) throws IOException {
        _verifyValueWrite("write a number");
        if (value == null) {
            _writeNull();
        } else if (this._cfgNumbersAsStrings) {
            _writeQuotedRaw(value.toString());
        } else {
            writeRaw(value.toString());
        }
    }

    public final void writeNumber(double d) throws IOException {
        if (this._cfgNumbersAsStrings || (isEnabled(Feature.QUOTE_NON_NUMERIC_NUMBERS) && (Double.isNaN(d) || Double.isInfinite(d)))) {
            writeString(String.valueOf(d));
            return;
        }
        _verifyValueWrite("write a number");
        writeRaw(String.valueOf(d));
    }

    public final void writeNumber(float f) throws IOException {
        if (this._cfgNumbersAsStrings || (isEnabled(Feature.QUOTE_NON_NUMERIC_NUMBERS) && (Float.isNaN(f) || Float.isInfinite(f)))) {
            writeString(String.valueOf(f));
            return;
        }
        _verifyValueWrite("write a number");
        writeRaw(String.valueOf(f));
    }

    public final void writeNumber(BigDecimal value) throws IOException {
        _verifyValueWrite("write a number");
        if (value == null) {
            _writeNull();
        } else if (this._cfgNumbersAsStrings) {
            _writeQuotedRaw(_asString(value));
        } else {
            writeRaw(_asString(value));
        }
    }

    public final void writeNumber(String encodedValue) throws IOException {
        _verifyValueWrite("write a number");
        if (this._cfgNumbersAsStrings) {
            _writeQuotedRaw(encodedValue);
        } else {
            writeRaw(encodedValue);
        }
    }

    private void _writeQuotedRaw(String value) throws IOException {
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        writeRaw(value);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        cArr = this._outputBuffer;
        i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
    }

    public final void writeBoolean(boolean state) throws IOException {
        _verifyValueWrite("write a boolean value");
        if (this._outputTail + 5 >= this._outputEnd) {
            _flushBuffer();
        }
        int ptr = this._outputTail;
        char[] buf = this._outputBuffer;
        if (state) {
            buf[ptr] = 't';
            ptr++;
            buf[ptr] = 'r';
            ptr++;
            buf[ptr] = 'u';
            ptr++;
            buf[ptr] = 'e';
        } else {
            buf[ptr] = 'f';
            ptr++;
            buf[ptr] = 'a';
            ptr++;
            buf[ptr] = 'l';
            ptr++;
            buf[ptr] = 's';
            ptr++;
            buf[ptr] = 'e';
        }
        this._outputTail = ptr + 1;
    }

    public final void writeNull() throws IOException {
        _verifyValueWrite("write a null");
        _writeNull();
    }

    protected final void _verifyValueWrite(String typeMsg) throws IOException {
        int status = this._writeContext.writeValue();
        if (this._cfgPrettyPrinter != null) {
            _verifyPrettyValueWrite(typeMsg, status);
            return;
        }
        char c;
        switch (status) {
            case 1:
                c = ',';
                break;
            case 2:
                c = ':';
                break;
            case 3:
                if (this._rootValueSeparator != null) {
                    writeRaw(this._rootValueSeparator.getValue());
                    return;
                }
                return;
            case 5:
                _reportCantWriteValueExpectName(typeMsg);
                return;
            default:
                return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = c;
    }

    public final void flush() throws IOException {
        _flushBuffer();
        if (this._writer != null && isEnabled(Feature.FLUSH_PASSED_TO_STREAM)) {
            this._writer.flush();
        }
    }

    public final void close() throws IOException {
        super.close();
        if (this._outputBuffer != null && isEnabled(Feature.AUTO_CLOSE_JSON_CONTENT)) {
            while (true) {
                JsonStreamContext ctxt = getOutputContext();
                if (!ctxt.inArray()) {
                    if (!ctxt.inObject()) {
                        break;
                    }
                    writeEndObject();
                } else {
                    writeEndArray();
                }
            }
        }
        _flushBuffer();
        this._outputHead = 0;
        this._outputTail = 0;
        if (this._writer != null) {
            if (this._ioContext.isResourceManaged() || isEnabled(Feature.AUTO_CLOSE_TARGET)) {
                this._writer.close();
            } else if (isEnabled(Feature.FLUSH_PASSED_TO_STREAM)) {
                this._writer.flush();
            }
        }
        _releaseBuffers();
    }

    protected final void _releaseBuffers() {
        char[] buf = this._outputBuffer;
        if (buf != null) {
            this._outputBuffer = null;
            this._ioContext.releaseConcatBuffer(buf);
        }
    }

    private void _writeString(String text) throws IOException {
        int len = text.length();
        if (len > this._outputEnd) {
            _writeLongString(text);
            return;
        }
        if (this._outputTail + len > this._outputEnd) {
            _flushBuffer();
        }
        text.getChars(0, len, this._outputBuffer, this._outputTail);
        if (this._characterEscapes != null) {
            _writeStringCustom(len);
        } else if (this._maximumNonEscapedChar != 0) {
            _writeStringASCII(len, this._maximumNonEscapedChar);
        } else {
            _writeString2(len);
        }
    }

    private void _writeString2(int len) throws IOException {
        int end = this._outputTail + len;
        int[] escCodes = this._outputEscapes;
        char escLen = escCodes.length;
        while (this._outputTail < end) {
            char c;
            while (true) {
                c = this._outputBuffer[this._outputTail];
                if (c < escLen && escCodes[c] != 0) {
                    break;
                }
                int i = this._outputTail + 1;
                this._outputTail = i;
                if (i >= end) {
                    return;
                }
            }
            int flushLen = this._outputTail - this._outputHead;
            if (flushLen > 0) {
                this._writer.write(this._outputBuffer, this._outputHead, flushLen);
            }
            char[] cArr = this._outputBuffer;
            int i2 = this._outputTail;
            this._outputTail = i2 + 1;
            c = cArr[i2];
            _prependOrWriteCharacterEscape(c, escCodes[c]);
        }
    }

    private void _writeLongString(String text) throws IOException {
        _flushBuffer();
        int textLen = text.length();
        int offset = 0;
        do {
            int segmentLen;
            int max = this._outputEnd;
            if (offset + max > textLen) {
                segmentLen = textLen - offset;
            } else {
                segmentLen = max;
            }
            text.getChars(offset, offset + segmentLen, this._outputBuffer, 0);
            if (this._characterEscapes != null) {
                _writeSegmentCustom(segmentLen);
            } else if (this._maximumNonEscapedChar != 0) {
                _writeSegmentASCII(segmentLen, this._maximumNonEscapedChar);
            } else {
                _writeSegment(segmentLen);
            }
            offset += segmentLen;
        } while (offset < textLen);
    }

    private void _writeSegment(int end) throws IOException {
        int[] escCodes = this._outputEscapes;
        char escLen = escCodes.length;
        int ptr = 0;
        int start = 0;
        while (ptr < end) {
            char c;
            do {
                c = this._outputBuffer[ptr];
                if (c < escLen && escCodes[c] != 0) {
                    break;
                }
                ptr++;
            } while (ptr < end);
            int flushLen = ptr - start;
            if (flushLen > 0) {
                this._writer.write(this._outputBuffer, start, flushLen);
                if (ptr >= end) {
                    return;
                }
            }
            ptr++;
            start = _prependOrWriteCharacterEscape(this._outputBuffer, ptr, end, c, escCodes[c]);
        }
    }

    private void _writeString(char[] text, int offset, int len) throws IOException {
        if (this._characterEscapes != null) {
            _writeStringCustom(text, offset, len);
        } else if (this._maximumNonEscapedChar != 0) {
            _writeStringASCII(text, offset, len, this._maximumNonEscapedChar);
        } else {
            len += offset;
            int[] escCodes = this._outputEscapes;
            char escLen = escCodes.length;
            while (offset < len) {
                char c;
                int start = offset;
                do {
                    c = text[offset];
                    if (c < escLen && escCodes[c] != 0) {
                        break;
                    }
                    offset++;
                } while (offset < len);
                int offset2 = offset;
                int newAmount = offset2 - start;
                if (newAmount < 32) {
                    if (this._outputTail + newAmount > this._outputEnd) {
                        _flushBuffer();
                    }
                    if (newAmount > 0) {
                        System.arraycopy(text, start, this._outputBuffer, this._outputTail, newAmount);
                        this._outputTail += newAmount;
                    }
                } else {
                    _flushBuffer();
                    this._writer.write(text, start, newAmount);
                }
                if (offset2 < len) {
                    offset = offset2 + 1;
                    c = text[offset2];
                    _appendCharacterEscape(c, escCodes[c]);
                } else {
                    offset = offset2;
                    return;
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void _writeStringASCII(int r10, int r11) throws java.io.IOException, com.fasterxml.jackson.core.JsonGenerationException {
        /*
        r9 = this;
        r6 = r9._outputTail;
        r1 = r6 + r10;
        r3 = r9._outputEscapes;
        r6 = r3.length;
        r7 = r11 + 1;
        r4 = java.lang.Math.min(r6, r7);
    L_0x000d:
        r6 = r9._outputTail;
        if (r6 >= r1) goto L_0x0044;
    L_0x0011:
        r6 = r9._outputBuffer;
        r7 = r9._outputTail;
        r0 = r6[r7];
        if (r0 >= r4) goto L_0x0038;
    L_0x0019:
        r2 = r3[r0];
        if (r2 == 0) goto L_0x003c;
    L_0x001d:
        r6 = r9._outputTail;
        r7 = r9._outputHead;
        r5 = r6 - r7;
        if (r5 <= 0) goto L_0x002e;
    L_0x0025:
        r6 = r9._writer;
        r7 = r9._outputBuffer;
        r8 = r9._outputHead;
        r6.write(r7, r8, r5);
    L_0x002e:
        r6 = r9._outputTail;
        r6 = r6 + 1;
        r9._outputTail = r6;
        r9._prependOrWriteCharacterEscape(r0, r2);
        goto L_0x000d;
    L_0x0038:
        if (r0 <= r11) goto L_0x003c;
    L_0x003a:
        r2 = -1;
        goto L_0x001d;
    L_0x003c:
        r6 = r9._outputTail;
        r6 = r6 + 1;
        r9._outputTail = r6;
        if (r6 < r1) goto L_0x0011;
    L_0x0044:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.fasterxml.jackson.core.json.WriterBasedJsonGenerator._writeStringASCII(int, int):void");
    }

    private void _writeSegmentASCII(int end, int maxNonEscaped) throws IOException, JsonGenerationException {
        int[] escCodes = this._outputEscapes;
        char escLimit = Math.min(escCodes.length, maxNonEscaped + 1);
        int ptr = 0;
        int escCode = 0;
        int start = 0;
        while (ptr < end) {
            char c;
            do {
                c = this._outputBuffer[ptr];
                if (c < escLimit) {
                    escCode = escCodes[c];
                    if (escCode != 0) {
                        break;
                    }
                    ptr++;
                } else {
                    if (c > maxNonEscaped) {
                        escCode = -1;
                        break;
                    }
                    ptr++;
                }
            } while (ptr < end);
            int flushLen = ptr - start;
            if (flushLen > 0) {
                this._writer.write(this._outputBuffer, start, flushLen);
                if (ptr >= end) {
                    return;
                }
            }
            ptr++;
            start = _prependOrWriteCharacterEscape(this._outputBuffer, ptr, end, c, escCode);
        }
    }

    private void _writeStringASCII(char[] text, int offset, int len, int maxNonEscaped) throws IOException, JsonGenerationException {
        len += offset;
        int[] escCodes = this._outputEscapes;
        char escLimit = Math.min(escCodes.length, maxNonEscaped + 1);
        int escCode = 0;
        while (offset < len) {
            int start = offset;
            do {
                char c = text[offset];
                if (c < escLimit) {
                    escCode = escCodes[c];
                    if (escCode != 0) {
                        break;
                    }
                    offset++;
                } else {
                    if (c > maxNonEscaped) {
                        escCode = -1;
                        break;
                    }
                    offset++;
                }
            } while (offset < len);
            int newAmount = offset - start;
            if (newAmount < 32) {
                if (this._outputTail + newAmount > this._outputEnd) {
                    _flushBuffer();
                }
                if (newAmount > 0) {
                    System.arraycopy(text, start, this._outputBuffer, this._outputTail, newAmount);
                    this._outputTail += newAmount;
                }
            } else {
                _flushBuffer();
                this._writer.write(text, start, newAmount);
            }
            if (offset < len) {
                offset++;
                _appendCharacterEscape(c, escCode);
            } else {
                return;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void _writeStringCustom(int r12) throws java.io.IOException, com.fasterxml.jackson.core.JsonGenerationException {
        /*
        r11 = this;
        r8 = r11._outputTail;
        r2 = r8 + r12;
        r4 = r11._outputEscapes;
        r8 = r11._maximumNonEscapedChar;
        if (r8 > 0) goto L_0x0041;
    L_0x000a:
        r7 = 65535; // 0xffff float:9.1834E-41 double:3.23786E-319;
    L_0x000d:
        r8 = r4.length;
        r9 = r7 + 1;
        r5 = java.lang.Math.min(r8, r9);
        r1 = r11._characterEscapes;
    L_0x0016:
        r8 = r11._outputTail;
        if (r8 >= r2) goto L_0x005a;
    L_0x001a:
        r8 = r11._outputBuffer;
        r9 = r11._outputTail;
        r0 = r8[r9];
        if (r0 >= r5) goto L_0x0044;
    L_0x0022:
        r3 = r4[r0];
        if (r3 == 0) goto L_0x0052;
    L_0x0026:
        r8 = r11._outputTail;
        r9 = r11._outputHead;
        r6 = r8 - r9;
        if (r6 <= 0) goto L_0x0037;
    L_0x002e:
        r8 = r11._writer;
        r9 = r11._outputBuffer;
        r10 = r11._outputHead;
        r8.write(r9, r10, r6);
    L_0x0037:
        r8 = r11._outputTail;
        r8 = r8 + 1;
        r11._outputTail = r8;
        r11._prependOrWriteCharacterEscape(r0, r3);
        goto L_0x0016;
    L_0x0041:
        r7 = r11._maximumNonEscapedChar;
        goto L_0x000d;
    L_0x0044:
        if (r0 <= r7) goto L_0x0048;
    L_0x0046:
        r3 = -1;
        goto L_0x0026;
    L_0x0048:
        r8 = r1.getEscapeSequence(r0);
        r11._currentEscape = r8;
        if (r8 == 0) goto L_0x0052;
    L_0x0050:
        r3 = -2;
        goto L_0x0026;
    L_0x0052:
        r8 = r11._outputTail;
        r8 = r8 + 1;
        r11._outputTail = r8;
        if (r8 < r2) goto L_0x001a;
    L_0x005a:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.fasterxml.jackson.core.json.WriterBasedJsonGenerator._writeStringCustom(int):void");
    }

    private void _writeSegmentCustom(int end) throws IOException, JsonGenerationException {
        int[] escCodes = this._outputEscapes;
        char maxNonEscaped = this._maximumNonEscapedChar <= 0 ? '￿' : this._maximumNonEscapedChar;
        char escLimit = Math.min(escCodes.length, maxNonEscaped + 1);
        CharacterEscapes customEscapes = this._characterEscapes;
        int ptr = 0;
        int escCode = 0;
        int start = 0;
        while (ptr < end) {
            char c;
            do {
                c = this._outputBuffer[ptr];
                if (c < escLimit) {
                    escCode = escCodes[c];
                    if (escCode != 0) {
                        break;
                    }
                    ptr++;
                } else if (c > maxNonEscaped) {
                    escCode = -1;
                    break;
                } else {
                    SerializableString escapeSequence = customEscapes.getEscapeSequence(c);
                    this._currentEscape = escapeSequence;
                    if (escapeSequence != null) {
                        escCode = -2;
                        break;
                    }
                    ptr++;
                }
            } while (ptr < end);
            int flushLen = ptr - start;
            if (flushLen > 0) {
                this._writer.write(this._outputBuffer, start, flushLen);
                if (ptr >= end) {
                    return;
                }
            }
            ptr++;
            start = _prependOrWriteCharacterEscape(this._outputBuffer, ptr, end, c, escCode);
        }
    }

    private void _writeStringCustom(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        len += offset;
        int[] escCodes = this._outputEscapes;
        char maxNonEscaped = this._maximumNonEscapedChar <= 0 ? '￿' : this._maximumNonEscapedChar;
        char escLimit = Math.min(escCodes.length, maxNonEscaped + 1);
        CharacterEscapes customEscapes = this._characterEscapes;
        int escCode = 0;
        while (offset < len) {
            int start = offset;
            do {
                char c = text[offset];
                if (c < escLimit) {
                    escCode = escCodes[c];
                    if (escCode != 0) {
                        break;
                    }
                    offset++;
                } else if (c > maxNonEscaped) {
                    escCode = -1;
                    break;
                } else {
                    SerializableString escapeSequence = customEscapes.getEscapeSequence(c);
                    this._currentEscape = escapeSequence;
                    if (escapeSequence != null) {
                        escCode = -2;
                        break;
                    }
                    offset++;
                }
            } while (offset < len);
            int newAmount = offset - start;
            if (newAmount < 32) {
                if (this._outputTail + newAmount > this._outputEnd) {
                    _flushBuffer();
                }
                if (newAmount > 0) {
                    System.arraycopy(text, start, this._outputBuffer, this._outputTail, newAmount);
                    this._outputTail += newAmount;
                }
            } else {
                _flushBuffer();
                this._writer.write(text, start, newAmount);
            }
            if (offset < len) {
                offset++;
                _appendCharacterEscape(c, escCode);
            } else {
                return;
            }
        }
    }

    protected final void _writeBinary(Base64Variant b64variant, byte[] input, int inputPtr, int inputEnd) throws IOException, JsonGenerationException {
        int safeInputEnd = inputEnd - 3;
        int safeOutputEnd = this._outputEnd - 6;
        int chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
        int inputPtr2 = inputPtr;
        while (inputPtr2 <= safeInputEnd) {
            if (this._outputTail > safeOutputEnd) {
                _flushBuffer();
            }
            inputPtr = inputPtr2 + 1;
            inputPtr2 = inputPtr + 1;
            inputPtr = inputPtr2 + 1;
            this._outputTail = b64variant.encodeBase64Chunk((((input[inputPtr2] << 8) | (input[inputPtr] & 255)) << 8) | (input[inputPtr2] & 255), this._outputBuffer, this._outputTail);
            chunksBeforeLF--;
            if (chunksBeforeLF <= 0) {
                char[] cArr = this._outputBuffer;
                int i = this._outputTail;
                this._outputTail = i + 1;
                cArr[i] = '\\';
                cArr = this._outputBuffer;
                i = this._outputTail;
                this._outputTail = i + 1;
                cArr[i] = 'n';
                chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
            }
            inputPtr2 = inputPtr;
        }
        int inputLeft = inputEnd - inputPtr2;
        if (inputLeft > 0) {
            if (this._outputTail > safeOutputEnd) {
                _flushBuffer();
            }
            inputPtr = inputPtr2 + 1;
            int b24 = input[inputPtr2] << 16;
            if (inputLeft == 2) {
                b24 |= (input[inputPtr] & 255) << 8;
            }
            this._outputTail = b64variant.encodeBase64Partial(b24, inputLeft, this._outputBuffer, this._outputTail);
            return;
        }
    }

    protected final int _writeBinary(Base64Variant b64variant, InputStream data, byte[] readBuffer, int bytesLeft) throws IOException, JsonGenerationException {
        int inputPtr = 0;
        int inputEnd = 0;
        int lastFullOffset = -3;
        int safeOutputEnd = this._outputEnd - 6;
        int chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
        while (bytesLeft > 2) {
            if (inputPtr > lastFullOffset) {
                inputEnd = _readMore(data, readBuffer, inputPtr, inputEnd, bytesLeft);
                inputPtr = 0;
                if (inputEnd < 3) {
                    break;
                }
                lastFullOffset = inputEnd - 3;
            }
            if (this._outputTail > safeOutputEnd) {
                _flushBuffer();
            }
            int inputPtr2 = inputPtr + 1;
            inputPtr = inputPtr2 + 1;
            inputPtr2 = inputPtr + 1;
            bytesLeft -= 3;
            this._outputTail = b64variant.encodeBase64Chunk((((readBuffer[inputPtr] << 8) | (readBuffer[inputPtr2] & 255)) << 8) | (readBuffer[inputPtr] & 255), this._outputBuffer, this._outputTail);
            chunksBeforeLF--;
            if (chunksBeforeLF <= 0) {
                char[] cArr = this._outputBuffer;
                int i = this._outputTail;
                this._outputTail = i + 1;
                cArr[i] = '\\';
                cArr = this._outputBuffer;
                i = this._outputTail;
                this._outputTail = i + 1;
                cArr[i] = 'n';
                chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
            }
            inputPtr = inputPtr2;
        }
        if (bytesLeft <= 0) {
            return bytesLeft;
        }
        inputEnd = _readMore(data, readBuffer, inputPtr, inputEnd, bytesLeft);
        if (inputEnd <= 0) {
            return bytesLeft;
        }
        int amount;
        if (this._outputTail > safeOutputEnd) {
            _flushBuffer();
        }
        int b24 = readBuffer[0] << 16;
        if (1 < inputEnd) {
            b24 |= (readBuffer[1] & 255) << 8;
            amount = 2;
        } else {
            amount = 1;
        }
        this._outputTail = b64variant.encodeBase64Partial(b24, amount, this._outputBuffer, this._outputTail);
        return bytesLeft - amount;
    }

    protected final int _writeBinary(Base64Variant b64variant, InputStream data, byte[] readBuffer) throws IOException, JsonGenerationException {
        int inputPtr = 0;
        int inputEnd = 0;
        int lastFullOffset = -3;
        int bytesDone = 0;
        int safeOutputEnd = this._outputEnd - 6;
        int chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
        while (true) {
            if (inputPtr > lastFullOffset) {
                inputEnd = _readMore(data, readBuffer, inputPtr, inputEnd, readBuffer.length);
                inputPtr = 0;
                if (inputEnd < 3) {
                    break;
                }
                lastFullOffset = inputEnd - 3;
            }
            if (this._outputTail > safeOutputEnd) {
                _flushBuffer();
            }
            int inputPtr2 = inputPtr + 1;
            inputPtr = inputPtr2 + 1;
            inputPtr2 = inputPtr + 1;
            bytesDone += 3;
            this._outputTail = b64variant.encodeBase64Chunk((((readBuffer[inputPtr] << 8) | (readBuffer[inputPtr2] & 255)) << 8) | (readBuffer[inputPtr] & 255), this._outputBuffer, this._outputTail);
            chunksBeforeLF--;
            if (chunksBeforeLF <= 0) {
                char[] cArr = this._outputBuffer;
                int i = this._outputTail;
                this._outputTail = i + 1;
                cArr[i] = '\\';
                cArr = this._outputBuffer;
                i = this._outputTail;
                this._outputTail = i + 1;
                cArr[i] = 'n';
                chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
            }
            inputPtr = inputPtr2;
        }
        if (inputEnd <= 0) {
            return bytesDone;
        }
        if (this._outputTail > safeOutputEnd) {
            _flushBuffer();
        }
        int b24 = readBuffer[0] << 16;
        int amount = 1;
        if (1 < inputEnd) {
            b24 |= (readBuffer[1] & 255) << 8;
            amount = 2;
        }
        bytesDone += amount;
        this._outputTail = b64variant.encodeBase64Partial(b24, amount, this._outputBuffer, this._outputTail);
        return bytesDone;
    }

    private int _readMore(InputStream in, byte[] readBuffer, int inputPtr, int inputEnd, int maxRead) throws IOException {
        int i = 0;
        int inputPtr2 = inputPtr;
        while (inputPtr2 < inputEnd) {
            int i2 = i + 1;
            inputPtr = inputPtr2 + 1;
            readBuffer[i] = readBuffer[inputPtr2];
            i = i2;
            inputPtr2 = inputPtr;
        }
        inputEnd = i;
        maxRead = Math.min(maxRead, readBuffer.length);
        do {
            int length = maxRead - inputEnd;
            if (length == 0) {
                break;
            }
            int count = in.read(readBuffer, inputEnd, length);
            if (count < 0) {
                return inputEnd;
            }
            inputEnd += count;
        } while (inputEnd < 3);
        return inputEnd;
    }

    private final void _writeNull() throws IOException {
        if (this._outputTail + 4 >= this._outputEnd) {
            _flushBuffer();
        }
        int ptr = this._outputTail;
        char[] buf = this._outputBuffer;
        buf[ptr] = 'n';
        ptr++;
        buf[ptr] = 'u';
        ptr++;
        buf[ptr] = 'l';
        ptr++;
        buf[ptr] = 'l';
        this._outputTail = ptr + 1;
    }

    private void _prependOrWriteCharacterEscape(char ch, int escCode) throws IOException, JsonGenerationException {
        int ptr;
        char[] buf;
        if (escCode >= 0) {
            if (this._outputTail >= 2) {
                ptr = this._outputTail - 2;
                this._outputHead = ptr;
                int ptr2 = ptr + 1;
                this._outputBuffer[ptr] = '\\';
                this._outputBuffer[ptr2] = (char) escCode;
                return;
            }
            buf = this._entityBuffer;
            if (buf == null) {
                buf = _allocateEntityBuffer();
            }
            this._outputHead = this._outputTail;
            buf[1] = (char) escCode;
            this._writer.write(buf, 0, 2);
        } else if (escCode == -2) {
            String escape;
            if (this._currentEscape == null) {
                escape = this._characterEscapes.getEscapeSequence(ch).getValue();
            } else {
                escape = this._currentEscape.getValue();
                this._currentEscape = null;
            }
            int len = escape.length();
            if (this._outputTail >= len) {
                ptr = this._outputTail - len;
                this._outputHead = ptr;
                escape.getChars(0, len, this._outputBuffer, ptr);
                return;
            }
            this._outputHead = this._outputTail;
            this._writer.write(escape);
        } else if (this._outputTail >= 6) {
            buf = this._outputBuffer;
            ptr = this._outputTail - 6;
            this._outputHead = ptr;
            buf[ptr] = '\\';
            ptr++;
            buf[ptr] = 'u';
            if (ch > 'ÿ') {
                hi = (ch >> 8) & 255;
                ptr++;
                buf[ptr] = HEX_CHARS[hi >> 4];
                ptr++;
                buf[ptr] = HEX_CHARS[hi & 15];
                ch = (char) (ch & 255);
            } else {
                ptr++;
                buf[ptr] = '0';
                ptr++;
                buf[ptr] = '0';
            }
            ptr++;
            buf[ptr] = HEX_CHARS[ch >> 4];
            buf[ptr + 1] = HEX_CHARS[ch & 15];
        } else {
            buf = this._entityBuffer;
            if (buf == null) {
                buf = _allocateEntityBuffer();
            }
            this._outputHead = this._outputTail;
            if (ch > 'ÿ') {
                hi = (ch >> 8) & 255;
                int lo = ch & 255;
                buf[10] = HEX_CHARS[hi >> 4];
                buf[11] = HEX_CHARS[hi & 15];
                buf[12] = HEX_CHARS[lo >> 4];
                buf[13] = HEX_CHARS[lo & 15];
                this._writer.write(buf, 8, 6);
                return;
            }
            buf[6] = HEX_CHARS[ch >> 4];
            buf[7] = HEX_CHARS[ch & 15];
            this._writer.write(buf, 2, 6);
        }
    }

    private int _prependOrWriteCharacterEscape(char[] buffer, int ptr, int end, char ch, int escCode) throws IOException, JsonGenerationException {
        char[] ent;
        if (escCode >= 0) {
            if (ptr <= 1 || ptr >= end) {
                ent = this._entityBuffer;
                if (ent == null) {
                    ent = _allocateEntityBuffer();
                }
                ent[1] = (char) escCode;
                this._writer.write(ent, 0, 2);
            } else {
                ptr -= 2;
                buffer[ptr] = '\\';
                buffer[ptr + 1] = (char) escCode;
            }
            return ptr;
        } else if (escCode != -2) {
            int hi;
            if (ptr <= 5 || ptr >= end) {
                ent = this._entityBuffer;
                if (ent == null) {
                    ent = _allocateEntityBuffer();
                }
                this._outputHead = this._outputTail;
                if (ch > 'ÿ') {
                    hi = (ch >> 8) & 255;
                    int lo = ch & 255;
                    ent[10] = HEX_CHARS[hi >> 4];
                    ent[11] = HEX_CHARS[hi & 15];
                    ent[12] = HEX_CHARS[lo >> 4];
                    ent[13] = HEX_CHARS[lo & 15];
                    this._writer.write(ent, 8, 6);
                } else {
                    ent[6] = HEX_CHARS[ch >> 4];
                    ent[7] = HEX_CHARS[ch & 15];
                    this._writer.write(ent, 2, 6);
                }
            } else {
                ptr -= 6;
                int i = ptr + 1;
                buffer[ptr] = '\\';
                ptr = i + 1;
                buffer[i] = 'u';
                if (ch > 'ÿ') {
                    hi = (ch >> 8) & 255;
                    i = ptr + 1;
                    buffer[ptr] = HEX_CHARS[hi >> 4];
                    ptr = i + 1;
                    buffer[i] = HEX_CHARS[hi & 15];
                    ch = (char) (ch & 255);
                } else {
                    i = ptr + 1;
                    buffer[ptr] = '0';
                    ptr = i + 1;
                    buffer[i] = '0';
                }
                i = ptr + 1;
                buffer[ptr] = HEX_CHARS[ch >> 4];
                buffer[i] = HEX_CHARS[ch & 15];
                ptr = i - 5;
            }
            return ptr;
        } else {
            String escape;
            if (this._currentEscape == null) {
                escape = this._characterEscapes.getEscapeSequence(ch).getValue();
            } else {
                escape = this._currentEscape.getValue();
                this._currentEscape = null;
            }
            int len = escape.length();
            if (ptr < len || ptr >= end) {
                this._writer.write(escape);
            } else {
                ptr -= len;
                escape.getChars(0, len, buffer, ptr);
            }
            return ptr;
        }
    }

    private void _appendCharacterEscape(char ch, int escCode) throws IOException, JsonGenerationException {
        if (escCode >= 0) {
            if (this._outputTail + 2 > this._outputEnd) {
                _flushBuffer();
            }
            char[] cArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            cArr[i] = '\\';
            cArr = this._outputBuffer;
            i = this._outputTail;
            this._outputTail = i + 1;
            cArr[i] = (char) escCode;
        } else if (escCode != -2) {
            if (this._outputTail + 5 >= this._outputEnd) {
                _flushBuffer();
            }
            int ptr = this._outputTail;
            char[] buf = this._outputBuffer;
            int i2 = ptr + 1;
            buf[ptr] = '\\';
            ptr = i2 + 1;
            buf[i2] = 'u';
            if (ch > 'ÿ') {
                int hi = (ch >> 8) & 255;
                i2 = ptr + 1;
                buf[ptr] = HEX_CHARS[hi >> 4];
                ptr = i2 + 1;
                buf[i2] = HEX_CHARS[hi & 15];
                ch = (char) (ch & 255);
            } else {
                i2 = ptr + 1;
                buf[ptr] = '0';
                ptr = i2 + 1;
                buf[i2] = '0';
            }
            i2 = ptr + 1;
            buf[ptr] = HEX_CHARS[ch >> 4];
            ptr = i2 + 1;
            buf[i2] = HEX_CHARS[ch & 15];
            this._outputTail = ptr;
        } else {
            String escape;
            if (this._currentEscape == null) {
                escape = this._characterEscapes.getEscapeSequence(ch).getValue();
            } else {
                escape = this._currentEscape.getValue();
                this._currentEscape = null;
            }
            int len = escape.length();
            if (this._outputTail + len > this._outputEnd) {
                _flushBuffer();
                if (len > this._outputEnd) {
                    this._writer.write(escape);
                    return;
                }
            }
            escape.getChars(0, len, this._outputBuffer, this._outputTail);
            this._outputTail += len;
        }
    }

    private char[] _allocateEntityBuffer() {
        char[] buf = new char[14];
        buf[0] = '\\';
        buf[2] = '\\';
        buf[3] = 'u';
        buf[4] = '0';
        buf[5] = '0';
        buf[8] = '\\';
        buf[9] = 'u';
        this._entityBuffer = buf;
        return buf;
    }

    protected final void _flushBuffer() throws IOException {
        int len = this._outputTail - this._outputHead;
        if (len > 0) {
            int offset = this._outputHead;
            this._outputHead = 0;
            this._outputTail = 0;
            this._writer.write(this._outputBuffer, offset, len);
        }
    }
}
