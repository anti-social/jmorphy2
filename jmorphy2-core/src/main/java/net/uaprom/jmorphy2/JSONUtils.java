package net.uaprom.jmorphy2;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Deque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

import org.noggit.JSONParser;


public class JSONUtils {
    @SuppressWarnings("unchecked")
    public static Object parseJSON(InputStream stream) throws IOException {
        JSONParser parser = new JSONParser(new BufferedReader(new InputStreamReader(stream, "UTF-8")));
        Deque<Object> stack = new LinkedList<Object>();
        Object obj = null, prevObj = null, container = null;
        int event;
        
        while ((event = parser.nextEvent()) != JSONParser.EOF) {
            switch (event) {
            case JSONParser.ARRAY_START:
                obj = new ArrayList<Object>();
                stack.addFirst(obj);
                continue;
            case JSONParser.OBJECT_START:
                obj = new HashMap<Object,Object>();
                stack.addFirst(obj);
                continue;
            case JSONParser.STRING:
                obj = parser.getString();
                break;
            case JSONParser.LONG:
                obj = parser.getLong();
                break;
            case JSONParser.NUMBER:
                obj = parser.getDouble();
                break;
            case JSONParser.BOOLEAN:
                obj = parser.getBoolean();
                break;
            case JSONParser.NULL:
                parser.getNull();
                obj = null;
                break;
            case JSONParser.ARRAY_END:
            case JSONParser.OBJECT_END:
                obj = stack.removeFirst();
                if (stack.isEmpty()) {
                    return obj;
                }
                break;
            }

            container = stack.peekFirst();
            if (container instanceof List) {
                ((List<Object>) container).add(obj);
            }
            else if (container instanceof Map) {
                if (obj != null && prevObj != null) {
                    ((Map<Object,Object>) container).put(prevObj, obj);
                }
                else {
                    prevObj = obj;
                    obj = null;
                }
            }
        }
        return obj;
    }
}
