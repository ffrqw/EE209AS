package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.json.JsonReadContext;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.cfg.PackageVersion;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.TreeMap;

public class TokenBuffer extends JsonGenerator {
    protected static final int DEFAULT_GENERATOR_FEATURES = Feature.collectDefaults();
    protected int _appendAt;
    protected boolean _closed;
    protected Segment _first;
    protected boolean _forceBigDecimal;
    protected int _generatorFeatures;
    protected boolean _hasNativeId;
    protected boolean _hasNativeObjectIds;
    protected boolean _hasNativeTypeIds;
    protected Segment _last;
    protected boolean _mayHaveNativeIds;
    protected ObjectCodec _objectCodec;
    protected Object _objectId;
    protected Object _typeId;
    protected JsonWriteContext _writeContext;

    protected static final class Parser extends ParserMinimalBase {
        protected transient ByteArrayBuilder _byteBuilder;
        protected boolean _closed;
        protected ObjectCodec _codec;
        protected final boolean _hasNativeIds;
        protected final boolean _hasNativeObjectIds;
        protected final boolean _hasNativeTypeIds;
        protected JsonLocation _location = null;
        protected JsonReadContext _parsingContext;
        protected Segment _segment;
        protected int _segmentPtr;

        public Parser(Segment firstSeg, ObjectCodec codec, boolean hasNativeTypeIds, boolean hasNativeObjectIds) {
            super(0);
            this._segment = firstSeg;
            this._segmentPtr = -1;
            this._codec = codec;
            this._parsingContext = JsonReadContext.createRootContext(null);
            this._hasNativeTypeIds = hasNativeTypeIds;
            this._hasNativeObjectIds = hasNativeObjectIds;
            this._hasNativeIds = hasNativeTypeIds | hasNativeObjectIds;
        }

        public final void setLocation(JsonLocation l) {
            this._location = l;
        }

        public final ObjectCodec getCodec() {
            return this._codec;
        }

        public final void setCodec(ObjectCodec c) {
            this._codec = c;
        }

        public final Version version() {
            return PackageVersion.VERSION;
        }

        public final JsonToken peekNextToken() throws IOException {
            if (this._closed) {
                return null;
            }
            Segment seg = this._segment;
            int ptr = this._segmentPtr + 1;
            if (ptr >= 16) {
                ptr = 0;
                seg = seg == null ? null : seg.next();
            }
            if (seg != null) {
                return seg.type(ptr);
            }
            return null;
        }

        public final void close() throws IOException {
            if (!this._closed) {
                this._closed = true;
            }
        }

        public final JsonToken nextToken() throws IOException {
            if (this._closed || this._segment == null) {
                return null;
            }
            int i = this._segmentPtr + 1;
            this._segmentPtr = i;
            if (i >= 16) {
                this._segmentPtr = 0;
                this._segment = this._segment.next();
                if (this._segment == null) {
                    return null;
                }
            }
            this._currToken = this._segment.type(this._segmentPtr);
            if (this._currToken == JsonToken.FIELD_NAME) {
                Object ob = _currentObject();
                this._parsingContext.setCurrentName(ob instanceof String ? (String) ob : ob.toString());
            } else if (this._currToken == JsonToken.START_OBJECT) {
                this._parsingContext = this._parsingContext.createChildObjectContext(-1, -1);
            } else if (this._currToken == JsonToken.START_ARRAY) {
                this._parsingContext = this._parsingContext.createChildArrayContext(-1, -1);
            } else if (this._currToken == JsonToken.END_OBJECT || this._currToken == JsonToken.END_ARRAY) {
                this._parsingContext = this._parsingContext.getParent();
                if (this._parsingContext == null) {
                    this._parsingContext = JsonReadContext.createRootContext(null);
                }
            }
            return this._currToken;
        }

        public final String nextFieldName() throws IOException {
            if (this._closed || this._segment == null) {
                return null;
            }
            int ptr = this._segmentPtr + 1;
            if (ptr < 16 && this._segment.type(ptr) == JsonToken.FIELD_NAME) {
                this._segmentPtr = ptr;
                Object ob = this._segment.get(ptr);
                String name = ob instanceof String ? (String) ob : ob.toString();
                this._parsingContext.setCurrentName(name);
                return name;
            } else if (nextToken() == JsonToken.FIELD_NAME) {
                return getCurrentName();
            } else {
                return null;
            }
        }

        public final boolean isClosed() {
            return this._closed;
        }

        public final JsonStreamContext getParsingContext() {
            return this._parsingContext;
        }

        public final JsonLocation getTokenLocation() {
            return getCurrentLocation();
        }

        public final JsonLocation getCurrentLocation() {
            return this._location == null ? JsonLocation.NA : this._location;
        }

        public final String getCurrentName() {
            if (this._currToken == JsonToken.START_OBJECT || this._currToken == JsonToken.START_ARRAY) {
                return this._parsingContext.getParent().getCurrentName();
            }
            return this._parsingContext.getCurrentName();
        }

        public final void overrideCurrentName(String name) {
            JsonReadContext ctxt = this._parsingContext;
            if (this._currToken == JsonToken.START_OBJECT || this._currToken == JsonToken.START_ARRAY) {
                ctxt = ctxt.getParent();
            }
            try {
                ctxt.setCurrentName(name);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public final String getText() {
            Object ob;
            if (this._currToken == JsonToken.VALUE_STRING || this._currToken == JsonToken.FIELD_NAME) {
                ob = _currentObject();
                if (ob instanceof String) {
                    return (String) ob;
                }
                return ob == null ? null : ob.toString();
            } else if (this._currToken == null) {
                return null;
            } else {
                switch (this._currToken) {
                    case VALUE_NUMBER_INT:
                    case VALUE_NUMBER_FLOAT:
                        ob = _currentObject();
                        return ob == null ? null : ob.toString();
                    default:
                        return this._currToken.asString();
                }
            }
        }

        public final char[] getTextCharacters() {
            String str = getText();
            return str == null ? null : str.toCharArray();
        }

        public final int getTextLength() {
            String str = getText();
            return str == null ? 0 : str.length();
        }

        public final int getTextOffset() {
            return 0;
        }

        public final boolean hasTextCharacters() {
            return false;
        }

        public final BigInteger getBigIntegerValue() throws IOException {
            Number n = getNumberValue();
            if (n instanceof BigInteger) {
                return (BigInteger) n;
            }
            if (getNumberType() == NumberType.BIG_DECIMAL) {
                return ((BigDecimal) n).toBigInteger();
            }
            return BigInteger.valueOf(n.longValue());
        }

        public final BigDecimal getDecimalValue() throws IOException {
            Number n = getNumberValue();
            if (n instanceof BigDecimal) {
                return (BigDecimal) n;
            }
            switch (getNumberType()) {
                case INT:
                case LONG:
                    return BigDecimal.valueOf(n.longValue());
                case BIG_INTEGER:
                    return new BigDecimal((BigInteger) n);
                default:
                    return BigDecimal.valueOf(n.doubleValue());
            }
        }

        public final double getDoubleValue() throws IOException {
            return getNumberValue().doubleValue();
        }

        public final float getFloatValue() throws IOException {
            return getNumberValue().floatValue();
        }

        public final int getIntValue() throws IOException {
            if (this._currToken == JsonToken.VALUE_NUMBER_INT) {
                return ((Number) _currentObject()).intValue();
            }
            return getNumberValue().intValue();
        }

        public final long getLongValue() throws IOException {
            return getNumberValue().longValue();
        }

        public final NumberType getNumberType() throws IOException {
            Number n = getNumberValue();
            if (n instanceof Integer) {
                return NumberType.INT;
            }
            if (n instanceof Long) {
                return NumberType.LONG;
            }
            if (n instanceof Double) {
                return NumberType.DOUBLE;
            }
            if (n instanceof BigDecimal) {
                return NumberType.BIG_DECIMAL;
            }
            if (n instanceof BigInteger) {
                return NumberType.BIG_INTEGER;
            }
            if (n instanceof Float) {
                return NumberType.FLOAT;
            }
            if (n instanceof Short) {
                return NumberType.INT;
            }
            return null;
        }

        public final Number getNumberValue() throws IOException {
            _checkIsNumber();
            String value = _currentObject();
            if (value instanceof Number) {
                return (Number) value;
            }
            if (value instanceof String) {
                String str = value;
                if (str.indexOf(46) >= 0) {
                    return Double.valueOf(Double.parseDouble(str));
                }
                return Long.valueOf(Long.parseLong(str));
            } else if (value == null) {
                return null;
            } else {
                throw new IllegalStateException("Internal error: entry should be a Number, but is of type " + value.getClass().getName());
            }
        }

        public final Object getEmbeddedObject() {
            if (this._currToken == JsonToken.VALUE_EMBEDDED_OBJECT) {
                return _currentObject();
            }
            return null;
        }

        public final byte[] getBinaryValue(Base64Variant b64variant) throws IOException, JsonParseException {
            if (this._currToken == JsonToken.VALUE_EMBEDDED_OBJECT) {
                Object ob = _currentObject();
                if (ob instanceof byte[]) {
                    return (byte[]) ob;
                }
            }
            if (this._currToken != JsonToken.VALUE_STRING) {
                throw _constructError("Current token (" + this._currToken + ") not VALUE_STRING (or VALUE_EMBEDDED_OBJECT with byte[]), can not access as binary");
            }
            String str = getText();
            if (str == null) {
                return null;
            }
            ByteArrayBuilder builder = this._byteBuilder;
            if (builder == null) {
                builder = new ByteArrayBuilder(100);
                this._byteBuilder = builder;
            } else {
                this._byteBuilder.reset();
            }
            _decodeBase64(str, builder, b64variant);
            return builder.toByteArray();
        }

        public final int readBinaryValue(Base64Variant b64variant, OutputStream out) throws IOException {
            byte[] data = getBinaryValue(b64variant);
            if (data == null) {
                return 0;
            }
            out.write(data, 0, data.length);
            return data.length;
        }

        public final boolean canReadObjectId() {
            return this._hasNativeObjectIds;
        }

        public final boolean canReadTypeId() {
            return this._hasNativeTypeIds;
        }

        public final Object getTypeId() {
            return this._segment.findTypeId(this._segmentPtr);
        }

        public final Object getObjectId() {
            return this._segment.findObjectId(this._segmentPtr);
        }

        protected final Object _currentObject() {
            return this._segment.get(this._segmentPtr);
        }

        protected final void _checkIsNumber() throws JsonParseException {
            if (this._currToken == null || !this._currToken.isNumeric()) {
                throw _constructError("Current token (" + this._currToken + ") not numeric, can not use numeric value accessors");
            }
        }

        protected final void _handleEOF() throws JsonParseException {
            _throwInternal();
        }
    }

    protected static final class Segment {
        public static final int TOKENS_PER_SEGMENT = 16;
        private static final JsonToken[] TOKEN_TYPES_BY_INDEX = new JsonToken[16];
        protected TreeMap<Integer, Object> _nativeIds;
        protected Segment _next;
        protected long _tokenTypes;
        protected final Object[] _tokens = new Object[16];

        static {
            JsonToken[] t = JsonToken.values();
            System.arraycopy(t, 1, TOKEN_TYPES_BY_INDEX, 1, Math.min(15, t.length - 1));
        }

        public final JsonToken type(int index) {
            long l = this._tokenTypes;
            if (index > 0) {
                l >>= index << 2;
            }
            return TOKEN_TYPES_BY_INDEX[((int) l) & 15];
        }

        public final int rawType(int index) {
            long l = this._tokenTypes;
            if (index > 0) {
                l >>= index << 2;
            }
            return ((int) l) & 15;
        }

        public final Object get(int index) {
            return this._tokens[index];
        }

        public final Segment next() {
            return this._next;
        }

        public final boolean hasIds() {
            return this._nativeIds != null;
        }

        public final Segment append(int index, JsonToken tokenType) {
            if (index < 16) {
                set(index, tokenType);
                return null;
            }
            this._next = new Segment();
            this._next.set(0, tokenType);
            return this._next;
        }

        public final Segment append(int index, JsonToken tokenType, Object objectId, Object typeId) {
            if (index < 16) {
                set(index, tokenType, objectId, typeId);
                return null;
            }
            this._next = new Segment();
            this._next.set(0, tokenType, objectId, typeId);
            return this._next;
        }

        public final Segment append(int index, JsonToken tokenType, Object value) {
            if (index < 16) {
                set(index, tokenType, value);
                return null;
            }
            this._next = new Segment();
            this._next.set(0, tokenType, value);
            return this._next;
        }

        public final Segment append(int index, JsonToken tokenType, Object value, Object objectId, Object typeId) {
            if (index < 16) {
                set(index, tokenType, value, objectId, typeId);
                return null;
            }
            this._next = new Segment();
            this._next.set(0, tokenType, value, objectId, typeId);
            return this._next;
        }

        public final Segment appendRaw(int index, int rawTokenType, Object value) {
            if (index < 16) {
                set(index, rawTokenType, value);
                return null;
            }
            this._next = new Segment();
            this._next.set(0, rawTokenType, value);
            return this._next;
        }

        public final Segment appendRaw(int index, int rawTokenType, Object value, Object objectId, Object typeId) {
            if (index < 16) {
                set(index, rawTokenType, value, objectId, typeId);
                return null;
            }
            this._next = new Segment();
            this._next.set(0, rawTokenType, value, objectId, typeId);
            return this._next;
        }

        private void set(int index, JsonToken tokenType) {
            long typeCode = (long) tokenType.ordinal();
            if (index > 0) {
                typeCode <<= index << 2;
            }
            this._tokenTypes |= typeCode;
        }

        private void set(int index, JsonToken tokenType, Object objectId, Object typeId) {
            long typeCode = (long) tokenType.ordinal();
            if (index > 0) {
                typeCode <<= index << 2;
            }
            this._tokenTypes |= typeCode;
            assignNativeIds(index, objectId, typeId);
        }

        private void set(int index, JsonToken tokenType, Object value) {
            this._tokens[index] = value;
            long typeCode = (long) tokenType.ordinal();
            if (index > 0) {
                typeCode <<= index << 2;
            }
            this._tokenTypes |= typeCode;
        }

        private void set(int index, JsonToken tokenType, Object value, Object objectId, Object typeId) {
            this._tokens[index] = value;
            long typeCode = (long) tokenType.ordinal();
            if (index > 0) {
                typeCode <<= index << 2;
            }
            this._tokenTypes |= typeCode;
            assignNativeIds(index, objectId, typeId);
        }

        private void set(int index, int rawTokenType, Object value) {
            this._tokens[index] = value;
            long typeCode = (long) rawTokenType;
            if (index > 0) {
                typeCode <<= index << 2;
            }
            this._tokenTypes |= typeCode;
        }

        private void set(int index, int rawTokenType, Object value, Object objectId, Object typeId) {
            this._tokens[index] = value;
            long typeCode = (long) rawTokenType;
            if (index > 0) {
                typeCode <<= index << 2;
            }
            this._tokenTypes |= typeCode;
            assignNativeIds(index, objectId, typeId);
        }

        private final void assignNativeIds(int index, Object objectId, Object typeId) {
            if (this._nativeIds == null) {
                this._nativeIds = new TreeMap();
            }
            if (objectId != null) {
                this._nativeIds.put(Integer.valueOf(_objectIdIndex(index)), objectId);
            }
            if (typeId != null) {
                this._nativeIds.put(Integer.valueOf(_typeIdIndex(index)), typeId);
            }
        }

        public final Object findObjectId(int index) {
            return this._nativeIds == null ? null : this._nativeIds.get(Integer.valueOf(_objectIdIndex(index)));
        }

        public final Object findTypeId(int index) {
            return this._nativeIds == null ? null : this._nativeIds.get(Integer.valueOf(_typeIdIndex(index)));
        }

        private final int _typeIdIndex(int i) {
            return i + i;
        }

        private final int _objectIdIndex(int i) {
            return (i + i) + 1;
        }
    }

    @Deprecated
    public TokenBuffer(ObjectCodec codec) {
        this(codec, false);
    }

    public TokenBuffer(ObjectCodec codec, boolean hasNativeIds) {
        this._hasNativeId = false;
        this._objectCodec = codec;
        this._generatorFeatures = DEFAULT_GENERATOR_FEATURES;
        this._writeContext = JsonWriteContext.createRootContext(null);
        Segment segment = new Segment();
        this._last = segment;
        this._first = segment;
        this._appendAt = 0;
        this._hasNativeTypeIds = hasNativeIds;
        this._hasNativeObjectIds = hasNativeIds;
        this._mayHaveNativeIds = this._hasNativeTypeIds | this._hasNativeObjectIds;
    }

    public TokenBuffer(JsonParser p) {
        this(p, null);
    }

    public TokenBuffer(JsonParser p, DeserializationContext ctxt) {
        boolean z = false;
        this._hasNativeId = false;
        this._objectCodec = p.getCodec();
        this._generatorFeatures = DEFAULT_GENERATOR_FEATURES;
        this._writeContext = JsonWriteContext.createRootContext(null);
        Segment segment = new Segment();
        this._last = segment;
        this._first = segment;
        this._appendAt = 0;
        this._hasNativeTypeIds = p.canReadTypeId();
        this._hasNativeObjectIds = p.canReadObjectId();
        this._mayHaveNativeIds = this._hasNativeTypeIds | this._hasNativeObjectIds;
        if (ctxt != null) {
            z = ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        }
        this._forceBigDecimal = z;
    }

    public TokenBuffer forceUseOfBigDecimal(boolean b) {
        this._forceBigDecimal = b;
        return this;
    }

    public Version version() {
        return PackageVersion.VERSION;
    }

    public JsonParser asParser() {
        return asParser(this._objectCodec);
    }

    public JsonParser asParser(ObjectCodec codec) {
        return new Parser(this._first, codec, this._hasNativeTypeIds, this._hasNativeObjectIds);
    }

    public JsonParser asParser(JsonParser src) {
        Parser p = new Parser(this._first, src.getCodec(), this._hasNativeTypeIds, this._hasNativeObjectIds);
        p.setLocation(src.getTokenLocation());
        return p;
    }

    public JsonToken firstToken() {
        if (this._first != null) {
            return this._first.type(0);
        }
        return null;
    }

    public TokenBuffer append(TokenBuffer other) throws IOException {
        if (!this._hasNativeTypeIds) {
            this._hasNativeTypeIds = other.canWriteTypeId();
        }
        if (!this._hasNativeObjectIds) {
            this._hasNativeObjectIds = other.canWriteObjectId();
        }
        this._mayHaveNativeIds = this._hasNativeTypeIds | this._hasNativeObjectIds;
        JsonParser p = other.asParser();
        while (p.nextToken() != null) {
            copyCurrentStructure(p);
        }
        return this;
    }

    public void serialize(JsonGenerator gen) throws IOException {
        boolean hasIds;
        Segment segment = this._first;
        int ptr = -1;
        boolean checkIds = this._mayHaveNativeIds;
        if (checkIds && segment.hasIds()) {
            hasIds = true;
        } else {
            hasIds = false;
        }
        while (true) {
            ptr++;
            if (ptr >= 16) {
                ptr = 0;
                segment = segment.next();
                if (segment == null) {
                    return;
                }
                if (checkIds && segment.hasIds()) {
                    hasIds = true;
                } else {
                    hasIds = false;
                }
            }
            JsonToken t = segment.type(ptr);
            if (t != null) {
                if (hasIds) {
                    Object id = segment.findObjectId(ptr);
                    if (id != null) {
                        gen.writeObjectId(id);
                    }
                    id = segment.findTypeId(ptr);
                    if (id != null) {
                        gen.writeTypeId(id);
                    }
                }
                Object ob;
                Object n;
                switch (t) {
                    case START_OBJECT:
                        gen.writeStartObject();
                        break;
                    case END_OBJECT:
                        gen.writeEndObject();
                        break;
                    case START_ARRAY:
                        gen.writeStartArray();
                        break;
                    case END_ARRAY:
                        gen.writeEndArray();
                        break;
                    case FIELD_NAME:
                        ob = segment.get(ptr);
                        if (!(ob instanceof SerializableString)) {
                            gen.writeFieldName((String) ob);
                            break;
                        } else {
                            gen.writeFieldName((SerializableString) ob);
                            break;
                        }
                    case VALUE_STRING:
                        ob = segment.get(ptr);
                        if (!(ob instanceof SerializableString)) {
                            gen.writeString((String) ob);
                            break;
                        } else {
                            gen.writeString((SerializableString) ob);
                            break;
                        }
                    case VALUE_NUMBER_INT:
                        n = segment.get(ptr);
                        if (!(n instanceof Integer)) {
                            if (!(n instanceof BigInteger)) {
                                if (!(n instanceof Long)) {
                                    if (!(n instanceof Short)) {
                                        gen.writeNumber(((Number) n).intValue());
                                        break;
                                    } else {
                                        gen.writeNumber(((Short) n).shortValue());
                                        break;
                                    }
                                }
                                gen.writeNumber(((Long) n).longValue());
                                break;
                            }
                            gen.writeNumber((BigInteger) n);
                            break;
                        }
                        gen.writeNumber(((Integer) n).intValue());
                        break;
                    case VALUE_NUMBER_FLOAT:
                        n = segment.get(ptr);
                        if (n instanceof Double) {
                            gen.writeNumber(((Double) n).doubleValue());
                            break;
                        } else if (n instanceof BigDecimal) {
                            gen.writeNumber((BigDecimal) n);
                            break;
                        } else if (n instanceof Float) {
                            gen.writeNumber(((Float) n).floatValue());
                            break;
                        } else if (n == null) {
                            gen.writeNull();
                            break;
                        } else if (n instanceof String) {
                            gen.writeNumber((String) n);
                            break;
                        } else {
                            throw new JsonGenerationException(String.format("Unrecognized value type for VALUE_NUMBER_FLOAT: %s, can not serialize", new Object[]{n.getClass().getName()}), gen);
                        }
                    case VALUE_TRUE:
                        gen.writeBoolean(true);
                        break;
                    case VALUE_FALSE:
                        gen.writeBoolean(false);
                        break;
                    case VALUE_NULL:
                        gen.writeNull();
                        break;
                    case VALUE_EMBEDDED_OBJECT:
                        Object value = segment.get(ptr);
                        if (!(value instanceof RawValue)) {
                            if (!(value instanceof JsonSerializable)) {
                                gen.writeEmbeddedObject(value);
                                break;
                            } else {
                                gen.writeObject(value);
                                break;
                            }
                        }
                        ((RawValue) value).serialize(gen);
                        break;
                    default:
                        throw new RuntimeException("Internal error: should never end up through this code path");
                }
            }
            return;
        }
    }

    public TokenBuffer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.getCurrentTokenId() != JsonToken.FIELD_NAME.id()) {
            copyCurrentStructure(p);
        } else {
            JsonToken t;
            writeStartObject();
            do {
                copyCurrentStructure(p);
                t = p.nextToken();
            } while (t == JsonToken.FIELD_NAME);
            if (t != JsonToken.END_OBJECT) {
                ctxt.reportWrongTokenException(p, JsonToken.END_OBJECT, "Expected END_OBJECT after copying contents of a JsonParser into TokenBuffer, got " + t, new Object[0]);
            }
            writeEndObject();
        }
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[TokenBuffer: ");
        JsonParser jp = asParser();
        int count = 0;
        boolean hasNativeIds = this._hasNativeTypeIds || this._hasNativeObjectIds;
        while (true) {
            JsonToken t = jp.nextToken();
            if (t == null) {
                break;
            }
            if (hasNativeIds) {
                try {
                    _appendNativeIds(sb);
                } catch (IOException ioe) {
                    throw new IllegalStateException(ioe);
                }
            }
            if (count < 100) {
                if (count > 0) {
                    sb.append(", ");
                }
                sb.append(t.toString());
                if (t == JsonToken.FIELD_NAME) {
                    sb.append('(');
                    sb.append(jp.getCurrentName());
                    sb.append(')');
                }
            }
            count++;
        }
        if (count >= 100) {
            sb.append(" ... (truncated ").append(count - 100).append(" entries)");
        }
        sb.append(']');
        return sb.toString();
    }

    private final void _appendNativeIds(StringBuilder sb) {
        Object objectId = this._last.findObjectId(this._appendAt - 1);
        if (objectId != null) {
            sb.append("[objectId=").append(String.valueOf(objectId)).append(']');
        }
        Object typeId = this._last.findTypeId(this._appendAt - 1);
        if (typeId != null) {
            sb.append("[typeId=").append(String.valueOf(typeId)).append(']');
        }
    }

    public JsonGenerator enable(Feature f) {
        this._generatorFeatures |= f.getMask();
        return this;
    }

    public JsonGenerator disable(Feature f) {
        this._generatorFeatures &= f.getMask() ^ -1;
        return this;
    }

    public boolean isEnabled(Feature f) {
        return (this._generatorFeatures & f.getMask()) != 0;
    }

    public int getFeatureMask() {
        return this._generatorFeatures;
    }

    @Deprecated
    public JsonGenerator setFeatureMask(int mask) {
        this._generatorFeatures = mask;
        return this;
    }

    public JsonGenerator overrideStdFeatures(int values, int mask) {
        this._generatorFeatures = ((mask ^ -1) & getFeatureMask()) | (values & mask);
        return this;
    }

    public JsonGenerator useDefaultPrettyPrinter() {
        return this;
    }

    public JsonGenerator setCodec(ObjectCodec oc) {
        this._objectCodec = oc;
        return this;
    }

    public ObjectCodec getCodec() {
        return this._objectCodec;
    }

    public final JsonWriteContext getOutputContext() {
        return this._writeContext;
    }

    public boolean canWriteBinaryNatively() {
        return true;
    }

    public void flush() throws IOException {
    }

    public void close() throws IOException {
        this._closed = true;
    }

    public boolean isClosed() {
        return this._closed;
    }

    public final void writeStartArray() throws IOException {
        this._writeContext.writeValue();
        _append(JsonToken.START_ARRAY);
        this._writeContext = this._writeContext.createChildArrayContext();
    }

    public final void writeEndArray() throws IOException {
        _append(JsonToken.END_ARRAY);
        JsonWriteContext c = this._writeContext.getParent();
        if (c != null) {
            this._writeContext = c;
        }
    }

    public final void writeStartObject() throws IOException {
        this._writeContext.writeValue();
        _append(JsonToken.START_OBJECT);
        this._writeContext = this._writeContext.createChildObjectContext();
    }

    public void writeStartObject(Object forValue) throws IOException {
        this._writeContext.writeValue();
        _append(JsonToken.START_OBJECT);
        JsonWriteContext ctxt = this._writeContext.createChildObjectContext();
        this._writeContext = ctxt;
        if (forValue != null) {
            ctxt.setCurrentValue(forValue);
        }
    }

    public final void writeEndObject() throws IOException {
        _append(JsonToken.END_OBJECT);
        JsonWriteContext c = this._writeContext.getParent();
        if (c != null) {
            this._writeContext = c;
        }
    }

    public final void writeFieldName(String name) throws IOException {
        this._writeContext.writeFieldName(name);
        _append(JsonToken.FIELD_NAME, name);
    }

    public void writeFieldName(SerializableString name) throws IOException {
        this._writeContext.writeFieldName(name.getValue());
        _append(JsonToken.FIELD_NAME, name);
    }

    public void writeString(String text) throws IOException {
        if (text == null) {
            writeNull();
        } else {
            _appendValue(JsonToken.VALUE_STRING, text);
        }
    }

    public void writeString(char[] text, int offset, int len) throws IOException {
        writeString(new String(text, offset, len));
    }

    public void writeString(SerializableString text) throws IOException {
        if (text == null) {
            writeNull();
        } else {
            _appendValue(JsonToken.VALUE_STRING, text);
        }
    }

    public void writeRawUTF8String(byte[] text, int offset, int length) throws IOException {
        _reportUnsupportedOperation();
    }

    public void writeUTF8String(byte[] text, int offset, int length) throws IOException {
        _reportUnsupportedOperation();
    }

    public void writeRaw(String text) throws IOException {
        _reportUnsupportedOperation();
    }

    public void writeRaw(String text, int offset, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    public void writeRaw(SerializableString text) throws IOException {
        _reportUnsupportedOperation();
    }

    public void writeRaw(char[] text, int offset, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    public void writeRaw(char c) throws IOException {
        _reportUnsupportedOperation();
    }

    public void writeRawValue(String text) throws IOException {
        _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, new RawValue(text));
    }

    public void writeRawValue(String text, int offset, int len) throws IOException {
        if (offset > 0 || len != text.length()) {
            text = text.substring(offset, offset + len);
        }
        _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, new RawValue(text));
    }

    public void writeRawValue(char[] text, int offset, int len) throws IOException {
        _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, new String(text, offset, len));
    }

    public void writeNumber(short i) throws IOException {
        _appendValue(JsonToken.VALUE_NUMBER_INT, Short.valueOf(i));
    }

    public void writeNumber(int i) throws IOException {
        _appendValue(JsonToken.VALUE_NUMBER_INT, Integer.valueOf(i));
    }

    public void writeNumber(long l) throws IOException {
        _appendValue(JsonToken.VALUE_NUMBER_INT, Long.valueOf(l));
    }

    public void writeNumber(double d) throws IOException {
        _appendValue(JsonToken.VALUE_NUMBER_FLOAT, Double.valueOf(d));
    }

    public void writeNumber(float f) throws IOException {
        _appendValue(JsonToken.VALUE_NUMBER_FLOAT, Float.valueOf(f));
    }

    public void writeNumber(BigDecimal dec) throws IOException {
        if (dec == null) {
            writeNull();
        } else {
            _appendValue(JsonToken.VALUE_NUMBER_FLOAT, dec);
        }
    }

    public void writeNumber(BigInteger v) throws IOException {
        if (v == null) {
            writeNull();
        } else {
            _appendValue(JsonToken.VALUE_NUMBER_INT, v);
        }
    }

    public void writeNumber(String encodedValue) throws IOException {
        _appendValue(JsonToken.VALUE_NUMBER_FLOAT, encodedValue);
    }

    public void writeBoolean(boolean state) throws IOException {
        _appendValue(state ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE);
    }

    public void writeNull() throws IOException {
        _appendValue(JsonToken.VALUE_NULL);
    }

    public void writeObject(Object value) throws IOException {
        if (value == null) {
            writeNull();
        } else if (value.getClass() == byte[].class || (value instanceof RawValue)) {
            _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, value);
        } else if (this._objectCodec == null) {
            _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, value);
        } else {
            this._objectCodec.writeValue(this, value);
        }
    }

    public void writeTree(TreeNode node) throws IOException {
        if (node == null) {
            writeNull();
        } else if (this._objectCodec == null) {
            _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, node);
        } else {
            this._objectCodec.writeTree(this, node);
        }
    }

    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException {
        byte[] copy = new byte[len];
        System.arraycopy(data, offset, copy, 0, len);
        writeObject(copy);
    }

    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) {
        throw new UnsupportedOperationException();
    }

    public boolean canWriteTypeId() {
        return this._hasNativeTypeIds;
    }

    public boolean canWriteObjectId() {
        return this._hasNativeObjectIds;
    }

    public void writeTypeId(Object id) {
        this._typeId = id;
        this._hasNativeId = true;
    }

    public void writeObjectId(Object id) {
        this._objectId = id;
        this._hasNativeId = true;
    }

    public void writeEmbeddedObject(Object object) throws IOException {
        _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, object);
    }

    public void copyCurrentEvent(JsonParser p) throws IOException {
        if (this._mayHaveNativeIds) {
            _checkNativeIds(p);
        }
        switch (p.getCurrentToken()) {
            case START_OBJECT:
                writeStartObject();
                return;
            case END_OBJECT:
                writeEndObject();
                return;
            case START_ARRAY:
                writeStartArray();
                return;
            case END_ARRAY:
                writeEndArray();
                return;
            case FIELD_NAME:
                writeFieldName(p.getCurrentName());
                return;
            case VALUE_STRING:
                if (p.hasTextCharacters()) {
                    writeString(p.getTextCharacters(), p.getTextOffset(), p.getTextLength());
                    return;
                } else {
                    writeString(p.getText());
                    return;
                }
            case VALUE_NUMBER_INT:
                switch (p.getNumberType()) {
                    case INT:
                        writeNumber(p.getIntValue());
                        return;
                    case BIG_INTEGER:
                        writeNumber(p.getBigIntegerValue());
                        return;
                    default:
                        writeNumber(p.getLongValue());
                        return;
                }
            case VALUE_NUMBER_FLOAT:
                if (this._forceBigDecimal) {
                    writeNumber(p.getDecimalValue());
                    return;
                }
                switch (p.getNumberType()) {
                    case BIG_DECIMAL:
                        writeNumber(p.getDecimalValue());
                        return;
                    case FLOAT:
                        writeNumber(p.getFloatValue());
                        return;
                    default:
                        writeNumber(p.getDoubleValue());
                        return;
                }
            case VALUE_TRUE:
                writeBoolean(true);
                return;
            case VALUE_FALSE:
                writeBoolean(false);
                return;
            case VALUE_NULL:
                writeNull();
                return;
            case VALUE_EMBEDDED_OBJECT:
                writeObject(p.getEmbeddedObject());
                return;
            default:
                throw new RuntimeException("Internal error: should never end up through this code path");
        }
    }

    public void copyCurrentStructure(JsonParser p) throws IOException {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.FIELD_NAME) {
            if (this._mayHaveNativeIds) {
                _checkNativeIds(p);
            }
            writeFieldName(p.getCurrentName());
            t = p.nextToken();
        }
        if (this._mayHaveNativeIds) {
            _checkNativeIds(p);
        }
        switch (t) {
            case START_OBJECT:
                writeStartObject();
                while (p.nextToken() != JsonToken.END_OBJECT) {
                    copyCurrentStructure(p);
                }
                writeEndObject();
                return;
            case START_ARRAY:
                writeStartArray();
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    copyCurrentStructure(p);
                }
                writeEndArray();
                return;
            default:
                copyCurrentEvent(p);
                return;
        }
    }

    private final void _checkNativeIds(JsonParser jp) throws IOException {
        Object typeId = jp.getTypeId();
        this._typeId = typeId;
        if (typeId != null) {
            this._hasNativeId = true;
        }
        typeId = jp.getObjectId();
        this._objectId = typeId;
        if (typeId != null) {
            this._hasNativeId = true;
        }
    }

    protected final void _append(JsonToken type) {
        Segment next = this._hasNativeId ? this._last.append(this._appendAt, type, this._objectId, this._typeId) : this._last.append(this._appendAt, type);
        if (next == null) {
            this._appendAt++;
            return;
        }
        this._last = next;
        this._appendAt = 1;
    }

    protected final void _append(JsonToken type, Object value) {
        Segment next;
        if (this._hasNativeId) {
            next = this._last.append(this._appendAt, type, value, this._objectId, this._typeId);
        } else {
            next = this._last.append(this._appendAt, type, value);
        }
        if (next == null) {
            this._appendAt++;
            return;
        }
        this._last = next;
        this._appendAt = 1;
    }

    protected final void _appendValue(JsonToken type) {
        this._writeContext.writeValue();
        Segment next = this._hasNativeId ? this._last.append(this._appendAt, type, this._objectId, this._typeId) : this._last.append(this._appendAt, type);
        if (next == null) {
            this._appendAt++;
            return;
        }
        this._last = next;
        this._appendAt = 1;
    }

    protected final void _appendValue(JsonToken type, Object value) {
        Segment next;
        this._writeContext.writeValue();
        if (this._hasNativeId) {
            next = this._last.append(this._appendAt, type, value, this._objectId, this._typeId);
        } else {
            next = this._last.append(this._appendAt, type, value);
        }
        if (next == null) {
            this._appendAt++;
            return;
        }
        this._last = next;
        this._appendAt = 1;
    }

    protected final void _appendRaw(int rawType, Object value) {
        Segment next;
        if (this._hasNativeId) {
            next = this._last.appendRaw(this._appendAt, rawType, value, this._objectId, this._typeId);
        } else {
            next = this._last.appendRaw(this._appendAt, rawType, value);
        }
        if (next == null) {
            this._appendAt++;
            return;
        }
        this._last = next;
        this._appendAt = 1;
    }

    protected void _reportUnsupportedOperation() {
        throw new UnsupportedOperationException("Called operation not supported for TokenBuffer");
    }
}
