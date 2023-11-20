package com.cedarsoftware.util.io;

import com.cedarsoftware.util.DeepEquals;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License")
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
class JsonObjectTest
{
    @Test
    void testGetId()
    {
        JsonObject jObj = new JsonObject();
        assert -1L == jObj.getId();
    }

    @Test
    void testGetPrimitiveValue()
    {
        JsonObject jObj = new JsonObject();
        jObj.setType("long");
        jObj.setValue(10L);
        assertEquals(jObj.getPrimitiveValue(), 10L);

        jObj.setType("phoney");
        try
        {
            jObj.getPrimitiveValue();
        }
        catch (JsonIoException e)
        {
            assert e.getMessage().toLowerCase().contains("invalid primitive type");
        }

        try
        {
            jObj.getLength();
        }
        catch (JsonIoException e)
        {
            assert e.getMessage().toLowerCase().contains("called");
            assert e.getMessage().toLowerCase().contains("non-collection");
        }

        jObj.moveCharsToMate();
    }

    @Test
    void testMultipleReadWritesRefInsideArray()
    {
        Map jsonObj1 = new LinkedHashMap<>();
        Map jsonObj2 = new LinkedHashMap<>();

        jsonObj1.put("name", "Batman");
        jsonObj1.put("partners", new Object[]{jsonObj2});
        jsonObj2.put("name", "Robin");
        jsonObj2.put("partners", new Object[]{jsonObj1});

        String json = TestUtil.toJson(jsonObj1, new WriteOptions().showTypeInfo(WriteOptions.ShowType.NEVER));
        TestUtil.printLine("json = " + json);
        Object o = TestUtil.toObjects(json, new ReadOptionsBuilder().returnAsMaps().build(), null);
        json = TestUtil.toJson(o);
        TestUtil.printLine("json = " + json);
        o = TestUtil.toObjects(json, null);
        json = TestUtil.toJson(o); // Before JsonWriter.adjustIfReferenced, it did not write "@id":1 for Batman's Map, so bad reference is written
        assert TestUtil.count(json, "@id") == 1;
        TestUtil.printLine("json = " + json);
        o = TestUtil.toObjects(json, new ReadOptionsBuilder().returnAsMaps().build(), null);
        assert o instanceof JsonObject;
    }

    @Test
    void testMultipleReadWritesRefInsideCollection()
    {
        Map jsonObj1 = new LinkedHashMap<>();
        Map jsonObj2 = new LinkedHashMap<>();

        Map<String, Map> batFriends = new LinkedHashMap<>();
        batFriends.put("Robski", jsonObj2);
        Map<String, Map> robFriends = new LinkedHashMap<>();
        robFriends.put("Batski", jsonObj1);
        jsonObj1.put("name", "Batman");
        jsonObj1.put("partners", batFriends);
        jsonObj2.put("name", "Robin");
        jsonObj2.put("partners", robFriends);

        String json = TestUtil.toJson(jsonObj1, new WriteOptions().showTypeInfo(WriteOptions.ShowType.NEVER));
        TestUtil.printLine("json = " + json);
        Object o = TestUtil.toObjects(json, new ReadOptionsBuilder().returnAsMaps().build(), null);
        json = TestUtil.toJson(o);
        TestUtil.printLine("json = " + json);
        o = TestUtil.toObjects(json, null);
        json = TestUtil.toJson(o); // Before JsonWriter.adjustIfReferenced, it did not write "@id":1 for Batman's Map, so bad reference is written
        assert TestUtil.count(json, "@id") == 1;
        TestUtil.printLine("json = " + json);
        o = TestUtil.toObjects(json, new ReadOptionsBuilder().returnAsMaps().build(), null);
        assert o instanceof JsonObject;
    }

    @Test
    public void testAsArray()
    {
        JsonObject jObj = new JsonObject();
        assert !jObj.isArray();
        assert !jObj.isCollection();
        assert !jObj.isMap;
        jObj.put(JsonObject.ITEMS, new Object[] {"hello", "goodbye"});
        assert jObj.isArray();
        assert !jObj.isCollection();
        assert !jObj.isMap;
        JsonObject jObj2 = new JsonObject();
        jObj2.put(JsonObject.ITEMS, new Object[] {"hello", "goodbye"});
        assert DeepEquals.deepEquals(jObj, jObj2);
    }
}
