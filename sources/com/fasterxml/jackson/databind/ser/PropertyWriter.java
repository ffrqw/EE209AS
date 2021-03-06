package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.ConcreteBeanPropertyBase;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.Serializable;
import java.lang.annotation.Annotation;

public abstract class PropertyWriter extends ConcreteBeanPropertyBase implements Serializable {
    private static final long serialVersionUID = 1;

    public abstract void depositSchemaProperty(JsonObjectFormatVisitor jsonObjectFormatVisitor, SerializerProvider serializerProvider) throws JsonMappingException;

    @Deprecated
    public abstract void depositSchemaProperty(ObjectNode objectNode, SerializerProvider serializerProvider) throws JsonMappingException;

    public abstract <A extends Annotation> A getAnnotation(Class<A> cls);

    public abstract <A extends Annotation> A getContextAnnotation(Class<A> cls);

    public abstract PropertyName getFullName();

    public abstract String getName();

    public abstract void serializeAsElement(Object obj, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws Exception;

    public abstract void serializeAsField(Object obj, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws Exception;

    public abstract void serializeAsOmittedField(Object obj, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws Exception;

    public abstract void serializeAsPlaceholder(Object obj, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws Exception;

    protected PropertyWriter(PropertyMetadata md) {
        super(md);
    }

    protected PropertyWriter(BeanPropertyDefinition propDef) {
        super(propDef.getMetadata());
    }

    protected PropertyWriter(PropertyWriter base) {
        super((ConcreteBeanPropertyBase) base);
    }

    public <A extends Annotation> A findAnnotation(Class<A> acls) {
        A ann = getAnnotation(acls);
        if (ann == null) {
            return getContextAnnotation(acls);
        }
        return ann;
    }
}
