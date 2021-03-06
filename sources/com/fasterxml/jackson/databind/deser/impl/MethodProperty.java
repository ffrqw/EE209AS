package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.Annotations;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public final class MethodProperty extends SettableBeanProperty {
    private static final long serialVersionUID = 1;
    protected final AnnotatedMethod _annotated;
    protected final transient Method _setter;

    public MethodProperty(BeanPropertyDefinition propDef, JavaType type, TypeDeserializer typeDeser, Annotations contextAnnotations, AnnotatedMethod method) {
        super(propDef, type, typeDeser, contextAnnotations);
        this._annotated = method;
        this._setter = method.getAnnotated();
    }

    protected MethodProperty(MethodProperty src, JsonDeserializer<?> deser) {
        super((SettableBeanProperty) src, (JsonDeserializer) deser);
        this._annotated = src._annotated;
        this._setter = src._setter;
    }

    protected MethodProperty(MethodProperty src, PropertyName newName) {
        super((SettableBeanProperty) src, newName);
        this._annotated = src._annotated;
        this._setter = src._setter;
    }

    protected MethodProperty(MethodProperty src, Method m) {
        super(src);
        this._annotated = src._annotated;
        this._setter = m;
    }

    public final MethodProperty withName(PropertyName newName) {
        return new MethodProperty(this, newName);
    }

    public final MethodProperty withValueDeserializer(JsonDeserializer<?> deser) {
        return new MethodProperty(this, (JsonDeserializer) deser);
    }

    public final void fixAccess(DeserializationConfig config) {
        this._annotated.fixAccess(config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
    }

    public final <A extends Annotation> A getAnnotation(Class<A> acls) {
        return this._annotated == null ? null : this._annotated.getAnnotation(acls);
    }

    public final AnnotatedMember getMember() {
        return this._annotated;
    }

    public final void deserializeAndSet(JsonParser p, DeserializationContext ctxt, Object instance) throws IOException {
        Object value = deserialize(p, ctxt);
        try {
            this._setter.invoke(instance, new Object[]{value});
        } catch (Exception e) {
            _throwAsIOE(p, e, value);
        }
    }

    public final Object deserializeSetAndReturn(JsonParser p, DeserializationContext ctxt, Object instance) throws IOException {
        try {
            Object result = this._setter.invoke(instance, new Object[]{deserialize(p, ctxt)});
            return result == null ? instance : result;
        } catch (Exception e) {
            _throwAsIOE(p, e, value);
            return null;
        }
    }

    public final void set(Object instance, Object value) throws IOException {
        try {
            this._setter.invoke(instance, new Object[]{value});
        } catch (Exception e) {
            _throwAsIOE(e, value);
        }
    }

    public final Object setAndReturn(Object instance, Object value) throws IOException {
        try {
            Object result = this._setter.invoke(instance, new Object[]{value});
            return result == null ? instance : result;
        } catch (Exception e) {
            _throwAsIOE(e, value);
            return null;
        }
    }

    final Object readResolve() {
        return new MethodProperty(this, this._annotated.getAnnotated());
    }
}
