package com.cedarsoftware.util.io;

import com.cedarsoftware.util.PrintStyle;
import com.cedarsoftware.util.reflect.Accessor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.io.ArgumentHelper.isTrue;
import static com.cedarsoftware.util.io.JsonWriter.CLASSLOADER;
import static com.cedarsoftware.util.io.JsonWriter.CUSTOM_WRITER_MAP;
import static com.cedarsoftware.util.io.JsonWriter.ENUM_PUBLIC_ONLY;
import static com.cedarsoftware.util.io.JsonWriter.FIELD_NAME_BLACK_LIST;
import static com.cedarsoftware.util.io.JsonWriter.FIELD_SPECIFIERS;
import static com.cedarsoftware.util.io.JsonWriter.FORCE_MAP_FORMAT_ARRAY_KEYS_ITEMS;
import static com.cedarsoftware.util.io.JsonWriter.NOT_CUSTOM_WRITER_MAP;
import static com.cedarsoftware.util.io.JsonWriter.PRETTY_PRINT;
import static com.cedarsoftware.util.io.JsonWriter.SHORT_META_KEYS;
import static com.cedarsoftware.util.io.JsonWriter.SKIP_NULL_FIELDS;
import static com.cedarsoftware.util.io.JsonWriter.TYPE;
import static com.cedarsoftware.util.io.JsonWriter.TYPE_NAME_MAP;
import static com.cedarsoftware.util.io.JsonWriter.WRITE_LONGS_AS_STRINGS;
import static com.cedarsoftware.util.io.JsonWriter.nullWriter;

/**
 * @author Kenny Partlow (kpartlow@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class WriteOptionsBuilder {

    private static final Map<Class<?>, JsonWriter.JsonClassWriter> BASE_WRITERS;

    private final WriteOptionsImplementation writeOptions;

    private final Map<Class<?>, Collection<String>> excludedFields = new HashMap<>();

    private final Map<Class<?>, Collection<String>> includedFields = new HashMap<>();

    static {
        Map<Class<?>, JsonWriter.JsonClassWriter> temp = new ConcurrentHashMap<>();
        temp.put(String.class, new Writers.JsonStringWriter());
        temp.put(Date.class, new Writers.DateWriter());
        temp.put(BigInteger.class, new Writers.BigIntegerWriter());
        temp.put(BigDecimal.class, new Writers.BigDecimalWriter());
        temp.put(java.sql.Date.class, new Writers.DateWriter());
        temp.put(Timestamp.class, new Writers.TimestampWriter());
        temp.put(Calendar.class, new Writers.CalendarWriter());
        temp.put(TimeZone.class, new Writers.TimeZoneWriter());
        temp.put(Locale.class, new Writers.LocaleWriter());
        temp.put(Class.class, new Writers.ClassWriter());
        temp.put(UUID.class, new Writers.UUIDWriter());
        temp.put(LocalDate.class, new Writers.LocalDateWriter());
        temp.put(LocalTime.class, new Writers.LocalTimeWriter());
        temp.put(LocalDateTime.class, new Writers.LocalDateTimeWriter());
        temp.put(ZonedDateTime.class, new Writers.ZonedDateTimeWriter());
        temp.put(OffsetDateTime.class, new Writers.OffsetDateTimeWriter());
        temp.put(YearMonth.class, new Writers.YearMonthWriter());
        temp.put(Year.class, new Writers.YearWriter());
        temp.put(ZoneOffset.class, new Writers.ZoneOffsetWriter());

        JsonWriter.JsonClassWriter stringWriter = new Writers.PrimitiveUtf8StringWriter();
        temp.put(StringBuilder.class, stringWriter);
        temp.put(StringBuffer.class, stringWriter);
        temp.put(URL.class, stringWriter);
        temp.put(ZoneOffset.class, stringWriter);

        JsonWriter.JsonClassWriter primitiveValueWriter = new Writers.PrimitiveValueWriter();
        temp.put(AtomicBoolean.class, primitiveValueWriter);
        temp.put(AtomicInteger.class, primitiveValueWriter);
        temp.put(AtomicLong.class, primitiveValueWriter);

        Class<?> zoneInfoClass = MetaUtils.classForName("sun.util.calendar.ZoneInfo", WriteOptions.class.getClassLoader());
        if (zoneInfoClass != null) {
            temp.put(zoneInfoClass, new Writers.TimeZoneWriter());
        }

        BASE_WRITERS = temp;
    }
    
    public WriteOptionsBuilder() {
        this.writeOptions = new WriteOptionsImplementation();
    }

    public WriteOptionsBuilder withDefaultOptimizations() {
        return withIsoDateTimeFormat()
                .withShortMetaKeys()
                .skipNullFields();
    }

    public WriteOptionsBuilder skipNullFields() {
        writeOptions.skippingNullFields = true;
        return this;
    }

    public WriteOptionsBuilder withPrettyPrint() {
        writeOptions.printStyle = PrintStyle.PRETTY_PRINT;
        return this;
    }

    public WriteOptionsBuilder withPrintStyle(PrintStyle style) {
        writeOptions.printStyle = style;
        return this;
    }

    public WriteOptionsBuilder writeLongsAsStrings() {
        writeOptions.writingLongsAsStrings = true;
        return this;
    }

    /**
     * This option only applies when you're writing enums as objects so we
     * force the enum write to be ENUMS_AS_OBJECTS
     *
     * @return
     */
    public WriteOptionsBuilder writeEnumsAsObject() {
        writeOptions.enumWriter = nullWriter;
        return this;
    }

    public WriteOptionsBuilder doNotWritePrivateEnumFields() {
        writeOptions.enumPublicOnly = true;
        writeOptions.enumWriter = nullWriter;
        return this;
    }

    public WriteOptionsBuilder writePrivateEnumFields() {
        writeOptions.enumPublicOnly = false;
        writeOptions.enumWriter = nullWriter;
        return this;
    }

    public WriteOptionsBuilder writeEnumsAsPrimitives() {
        writeOptions.enumPublicOnly = false;
        writeOptions.enumWriter = new Writers.EnumsAsStringWriter();
        return this;
    }

    // Map with all String keys, will still output in the @keys/@items approach
    public WriteOptionsBuilder forceMapOutputAsKeysAndValues() {
        writeOptions.forcingMapFormatWithKeyArrays = true;
        return this;
    }

    // Map with all String keys, will output as a JSON object, with the keys of the Map being the keys of the JSON
    // Object, which is the default and more natural.
    public WriteOptionsBuilder doNotForceMapOutputAsKeysAndValues() {
        writeOptions.forcingMapFormatWithKeyArrays = false;
        return this;
    }

    public WriteOptionsBuilder withClassLoader(ClassLoader classLoader) {
        writeOptions.classLoader = classLoader;
        return this;
    }

    public WriteOptionsBuilder withLogicalPrimitive(Class<?> c) {
        this.writeOptions.logicalPrimitives.add(c);
        return this;
    }

    public WriteOptionsBuilder withLogicalPrimitives(Collection<Class<?>> collection) {
        this.writeOptions.logicalPrimitives.addAll(collection);
        return this;
    }

    public WriteOptionsBuilder writeLocalDateAsTimeStamp() {
        return withCustomWriter(LocalDate.class, new Writers.LocalDateAsTimestamp());
    }

    public WriteOptionsBuilder writeLocalDateWithFormat(DateTimeFormatter formatter) {
        return withCustomWriter(LocalDate.class, new Writers.LocalDateWriter(formatter));
    }

    public WriteOptionsBuilder withIsoDateTimeFormat() {
        return withDateFormat(JsonWriter.ISO_DATE_TIME_FORMAT);
    }

    public WriteOptionsBuilder withIsoDateFormat() {
        return withDateFormat(JsonWriter.ISO_DATE_FORMAT);
    }

    public WriteOptionsBuilder withDateFormat(String format) {
        writeOptions.dateFormat = format;
        return this;
    }

    public WriteOptionsBuilder withShortMetaKeys() {
        this.writeOptions.usingShortMetaKeys = true;
        return this;
    }

    public WriteOptionsBuilder neverShowTypeInfo() {
        this.writeOptions.typeWriter = TypeWriter.NEVER;
        return this;
    }

    public WriteOptionsBuilder alwaysShowTypeInfo() {
        this.writeOptions.typeWriter = TypeWriter.ALWAYS;
        return this;
    }

    public WriteOptionsBuilder showMinimalTypeInfo() {
        this.writeOptions.typeWriter = TypeWriter.MINIMAL;
        return this;
    }

    public WriteOptionsBuilder excludedFields(Class<?> c, Collection<String> fields) {
        Collection<String> collection = this.excludedFields.computeIfAbsent(c, f -> new LinkedHashSet<>());
        collection.addAll(fields);
        return this;
    }

    public WriteOptionsBuilder excludedFields(Map<Class<?>, Collection<String>> map) {
        for (Map.Entry<Class<?>, Collection<String>> entry : map.entrySet()) {
            Collection<String> collection = this.excludedFields.computeIfAbsent(entry.getKey(), f -> new LinkedHashSet<>());
            collection.addAll(entry.getValue());
        }
        return this;
    }

    public WriteOptionsBuilder includedFields(Class<?> c, List<String> fields) {
        Collection<String> collection = this.includedFields.computeIfAbsent(c, f -> new LinkedHashSet<>());
        collection.addAll(fields);
        return this;
    }

    public WriteOptionsBuilder includedFields(Map<Class<?>, Collection<String>> map) {
        for (Map.Entry<Class<?>, Collection<String>> entry : map.entrySet()) {
            Collection<String> collection = this.includedFields.computeIfAbsent(entry.getKey(), f -> new LinkedHashSet<>());
            collection.addAll(entry.getValue());
        }
        return this;
    }

    public WriteOptionsBuilder withCustomTypeName(Class<?> type, String newTypeName) {
        return withCustomTypeName(type.getName(), newTypeName);
    }

    public WriteOptionsBuilder withCustomTypeName(String type, String newTypeName) {
        assertTypesAreBeingOutput();
        this.writeOptions.customTypeMap.put(type, newTypeName);
        return this;
    }

    public WriteOptionsBuilder withCustomTypeNames(Map<String, String> map) {
        assertTypesAreBeingOutput();
        this.writeOptions.customTypeMap.putAll(map);
        return this;
    }

    public WriteOptionsBuilder withCustomWriter(Class<?> c, JsonWriter.JsonClassWriter writer) {
        this.writeOptions.customWriters.put(c, writer);
        return this;
    }

    public WriteOptionsBuilder withCustomWriters(Map<? extends Class<?>, ? extends JsonWriter.JsonClassWriter> map) {
        this.writeOptions.customWriters.putAll(map);
        return this;
    }

    public WriteOptionsBuilder withNoCustomizationFor(Class<?> c) {
        this.writeOptions.nonCustomClasses.add(c);
        return this;
    }

    public WriteOptionsBuilder withNoCustomizationsFor(Collection<Class<?>> collection) {
        this.writeOptions.nonCustomClasses.addAll(collection);
        return this;
    }

    public static WriteOptionsBuilder fromMap(Map args) {
        WriteOptionsBuilder builder = new WriteOptionsBuilder();

        if (isTrue(args.get(SHORT_META_KEYS))) {
            builder.withShortMetaKeys();
        }

        Object type = args.get(TYPE);
        if (isTrue(type)) {
            builder.alwaysShowTypeInfo();
        }

        if (Boolean.FALSE.equals(type) || "false".equals(args.get(TYPE))) {
            builder.neverShowTypeInfo();
        }

        Map<String, String> typeNameMap = (Map<String, String>) args.get(TYPE_NAME_MAP);

        if (typeNameMap != null) {
            builder.withCustomTypeNames(typeNameMap);
        }

        if (isTrue(args.get(PRETTY_PRINT))) {
            builder.withPrintStyle(PrintStyle.PRETTY_PRINT);
        }

        if (isTrue(args.get(WRITE_LONGS_AS_STRINGS))) {
            builder.writeLongsAsStrings();
        }

        if (isTrue(args.get(SKIP_NULL_FIELDS))) {
            builder.skipNullFields();
        }

        // eventually let's get rid of this member variable and just use the one being passed into the writer object.
        boolean isEnumPublicOnly = isTrue(args.get(ENUM_PUBLIC_ONLY));

        if (isEnumPublicOnly) {
            builder.doNotWritePrivateEnumFields();
        }
        else {
            builder.writePrivateEnumFields();
        }

        if (isTrue(args.get(FORCE_MAP_FORMAT_ARRAY_KEYS_ITEMS))) {
            builder.forceMapOutputAsKeysAndValues();
        }

        ClassLoader loader = (ClassLoader) args.get(CLASSLOADER);
        builder.withClassLoader(loader == null ? JsonWriter.class.getClassLoader() : loader);

        Map<Class<?>, JsonWriter.JsonClassWriter> customWriters = (Map<Class<?>, JsonWriter.JsonClassWriter>) args.get(CUSTOM_WRITER_MAP);
        if (customWriters != null) {
            builder.withCustomWriters(customWriters);
        }

        Collection<Class<?>> notCustomClasses = (Collection<Class<?>>) args.get(NOT_CUSTOM_WRITER_MAP);
        if (notCustomClasses != null) {
            builder.withNoCustomizationsFor(notCustomClasses);
        }

        // Convert String field names to Java Field instances (makes it easier for user to set this up)
        Map<Class<?>, Collection<String>> stringSpecifiers = (Map<Class<?>, Collection<String>>) args.get(FIELD_SPECIFIERS);

        if (stringSpecifiers != null) {
            builder.includedFields(stringSpecifiers);
        }

        // may have to convert these to juse per class level, but that may be difficult.
        // since the user thinks of all the fields on a class at the class + parents level
        Map<Class<?>, Collection<String>> stringBlackList = (Map<Class<?>, Collection<String>>) args.get(FIELD_NAME_BLACK_LIST);

        if (stringBlackList != null) {
            builder.excludedFields(stringBlackList);
        }

        return builder;
    }

    public WriteOptions build() {
        this.writeOptions.includedFields.putAll(MetaUtils.convertStringFieldNamesToAccessors(this.includedFields));
        this.writeOptions.excludedFields.putAll(MetaUtils.convertStringFieldNamesToAccessors(this.excludedFields));
        return new WriteOptionsImplementation(this.writeOptions);
    }

    private void assertTypesAreBeingOutput() {
        if (writeOptions.typeWriter == TypeWriter.NEVER) {
            throw new IllegalStateException("There is no need to set the type name map when types are never being written");
        }
    }

    private static class WriteOptionsImplementation implements WriteOptions {
        @Getter
        private final Set<Class<?>> logicalPrimitives;

        @Getter
        private boolean usingShortMetaKeys = false;

        @Getter
        private boolean writingLongsAsStrings = false;

        @Getter
        private boolean skippingNullFields = false;

        @Getter
        private boolean forcingMapFormatWithKeyArrays = false;

        @Getter
        private boolean enumPublicOnly = false;

        @Getter
        private ClassLoader classLoader = WriteOptionsImplementation.class.getClassLoader();

        @Getter
        private JsonWriter.JsonClassWriter enumWriter;

        @Getter
        private final Map<Class<?>, JsonWriter.JsonClassWriter> customWriters;

        @Getter
        private final Map<String, String> customTypeMap;

        @Getter
        private final Collection<Class<?>> nonCustomClasses;

        @Getter
        private final Map<Class<?>, Collection<Accessor>> includedFields;

        @Getter
        private final Map<Class<?>, Collection<Accessor>> excludedFields;

        @Deprecated
        @Getter
        private String dateFormat;

        private PrintStyle printStyle;

        private TypeWriter typeWriter;

        private WriteOptionsImplementation() {
            this.logicalPrimitives = new HashSet<>(Primitives.PRIMITIVE_WRAPPERS);
            this.customWriters = new HashMap<>(BASE_WRITERS);
            this.excludedFields = new HashMap<>();
            this.includedFields = new HashMap<>();
            this.customTypeMap = new HashMap<>();
            this.nonCustomClasses = new HashSet<>();
            this.printStyle = PrintStyle.ONE_LINE;
            this.typeWriter = TypeWriter.MINIMAL;
            this.enumWriter = new Writers.EnumsAsStringWriter();
        }

        private WriteOptionsImplementation(WriteOptionsImplementation options) {
            this.usingShortMetaKeys = options.usingShortMetaKeys;
            this.typeWriter = options.typeWriter;
            this.printStyle = options.printStyle;
            this.writingLongsAsStrings = options.writingLongsAsStrings;
            this.skippingNullFields = options.skippingNullFields;
            this.forcingMapFormatWithKeyArrays = options.forcingMapFormatWithKeyArrays;
            this.enumPublicOnly = options.enumPublicOnly;
            this.classLoader = options.classLoader;
            this.enumWriter = options.enumWriter;
            this.customWriters = Collections.unmodifiableMap(options.customWriters);
            this.customTypeMap = Collections.unmodifiableMap(options.customTypeMap);
            this.nonCustomClasses = Collections.unmodifiableCollection(options.nonCustomClasses);
            this.includedFields = Collections.unmodifiableMap(options.includedFields);
            this.excludedFields = Collections.unmodifiableMap(options.excludedFields);
            this.logicalPrimitives = Collections.unmodifiableSet(options.logicalPrimitives);
            this.dateFormat = options.dateFormat;
        }

        @Override
        public boolean isAlwaysShowingType() {
            return typeWriter == TypeWriter.ALWAYS;
        }

        @Override
        public boolean isNeverShowingType() {
            return typeWriter == TypeWriter.NEVER;
        }

        @Override
        public boolean isPrettyPrint() {
            return this.printStyle == PrintStyle.PRETTY_PRINT;
        }

        @Override
        public boolean isLogicalPrimitive(Class<?> c) {
            return Primitives.isLogicalPrimitive(c, logicalPrimitives);
        }
        
        /**
         * We're cheating here because the booleans are needed to be mutable by the builder.
         *
         * @return
         */
        public WriteOptions ensurePrettyPrint() {
            this.printStyle = PrintStyle.PRETTY_PRINT;
            return this;
        }
    }
}
