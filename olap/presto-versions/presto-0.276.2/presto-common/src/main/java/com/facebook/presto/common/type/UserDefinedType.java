/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.common.type;

import com.facebook.presto.common.QualifiedObjectName;

import static java.util.Objects.requireNonNull;

/**
 * UserDefinedType represents an enum type, or a distinct type.
 * Type definition is defined by user and is extracted at runtime.
 */
public class UserDefinedType
{
    private final QualifiedObjectName name;
    private final TypeSignature representation;

    public UserDefinedType(QualifiedObjectName name, TypeSignature representation)
    {
        this.name = requireNonNull(name, "name is null");
        this.representation = requireNonNull(representation, "representation is null");
    }

    public QualifiedObjectName getUserDefinedTypeName()
    {
        return name;
    }

    public TypeSignature getPhysicalTypeSignature()
    {
        return representation;
    }

    public boolean isDistinctType()
    {
        return representation.isDistinctType();
    }
}
