package com.fasterxml.jackson.core;

import java.io.Serializable;

public class JsonLocation implements Serializable {
    public static final JsonLocation NA = new JsonLocation("N/A", -1, -1, -1, -1);
    final int _columnNr;
    final int _lineNr;
    final transient Object _sourceRef;
    final long _totalBytes;
    final long _totalChars;

    public JsonLocation(Object srcRef, long totalChars, int lineNr, int colNr) {
        this(srcRef, -1, totalChars, lineNr, colNr);
    }

    public JsonLocation(Object sourceRef, long totalBytes, long totalChars, int lineNr, int columnNr) {
        this._sourceRef = sourceRef;
        this._totalBytes = totalBytes;
        this._totalChars = totalChars;
        this._lineNr = lineNr;
        this._columnNr = columnNr;
    }

    public long getByteOffset() {
        return this._totalBytes;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(80);
        sb.append("[Source: ");
        if (this._sourceRef == null) {
            sb.append("UNKNOWN");
        } else {
            sb.append(this._sourceRef.toString());
        }
        sb.append("; line: ");
        sb.append(this._lineNr);
        sb.append(", column: ");
        sb.append(this._columnNr);
        sb.append(']');
        return sb.toString();
    }

    public int hashCode() {
        return ((((this._sourceRef == null ? 1 : this._sourceRef.hashCode()) ^ this._lineNr) + this._columnNr) ^ ((int) this._totalChars)) + ((int) this._totalBytes);
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof JsonLocation)) {
            return false;
        }
        JsonLocation otherLoc = (JsonLocation) other;
        if (this._sourceRef == null) {
            if (otherLoc._sourceRef != null) {
                return false;
            }
        } else if (!this._sourceRef.equals(otherLoc._sourceRef)) {
            return false;
        }
        if (this._lineNr == otherLoc._lineNr && this._columnNr == otherLoc._columnNr && this._totalChars == otherLoc._totalChars && getByteOffset() == otherLoc.getByteOffset()) {
            return true;
        }
        return false;
    }
}
