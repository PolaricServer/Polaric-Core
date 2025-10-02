/* Simple LRU Cache with thread-safe operations */

package no.polaric.core.util; 
import java.util.*;

public class LRUCache<T> {

    private int _capacity;
    private LinkedHashMap<String, T> _cache;
    
    
    public LRUCache(int capacity)
    {
        _capacity = capacity;
        _cache = new LinkedHashMap<String, T> (capacity + 10, 0.75f, true)  {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > _capacity;
            }
        };
    }


    public synchronized T get(String key)
    {
        return _cache.get(key);
    }
    
    
    public synchronized void remove(String key)
    {
        _cache.remove(key);
    }
    
    
    public synchronized void put(String key, T item)
         { _cache.put(key, item); }
 }

 
