/*
 * Druid - a distributed column store.
 * Copyright (C) 2012  Metamarkets Group Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.metamx.druid.client.cache;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.CachedData;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.NodeLocator;
import net.spy.memcached.internal.BulkFuture;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 */
public class MemcachedCacheTest
{
  private static final byte[] HI = "hiiiiiiiiiiiiiiiiiii".getBytes();
  private static final byte[] HO = "hooooooooooooooooooo".getBytes();
  private MemcachedCache cache;

  @Before
  public void setUp() throws Exception
  {
    MemcachedClientIF client = new MockMemcachedClient();
    cache = new MemcachedCache(client, "druid-memcached-test", 500, 3600);
  }

  @Test
  public void testSanity() throws Exception
  {
    Assert.assertNull(cache.get(new Cache.NamedKey("a", HI)));
    put(cache, "a", HI, 1);
    Assert.assertEquals(1, get(cache, "a", HI));
    Assert.assertNull(cache.get(new Cache.NamedKey("the", HI)));

    put(cache, "the", HI, 2);
    Assert.assertEquals(1, get(cache, "a", HI));
    Assert.assertEquals(2, get(cache, "the", HI));

    put(cache, "the", HO, 10);
    Assert.assertEquals(1, get(cache, "a", HI));
    Assert.assertNull(cache.get(new Cache.NamedKey("a", HO)));
    Assert.assertEquals(2, get(cache, "the", HI));
    Assert.assertEquals(10, get(cache, "the", HO));

    cache.close("the");
    Assert.assertEquals(1, get(cache, "a", HI));
    Assert.assertNull(cache.get(new Cache.NamedKey("a", HO)));

    cache.close("a");
  }

  @Test
  public void testGetBulk() throws Exception
  {
    Assert.assertNull(cache.get(new Cache.NamedKey("the", HI)));

    put(cache, "the", HI, 2);
    put(cache, "the", HO, 10);

    Cache.NamedKey key1 = new Cache.NamedKey("the", HI);
    Cache.NamedKey key2 = new Cache.NamedKey("the", HO);

    Map<Cache.NamedKey, byte[]> result = cache.getBulk(
        Lists.newArrayList(
            key1,
            key2
        )
    );

    Assert.assertEquals(2, Ints.fromByteArray(result.get(key1)));
    Assert.assertEquals(10, Ints.fromByteArray(result.get(key2)));
  }

  public void put(Cache cache, String namespace, byte[] key, Integer value)
  {
    cache.put(new Cache.NamedKey(namespace, key), Ints.toByteArray(value));
  }

  public int get(Cache cache, String namespace, byte[] key)
  {
    return Ints.fromByteArray(cache.get(new Cache.NamedKey(namespace, key)));
  }
}

class MockMemcachedClient implements MemcachedClientIF
{
  private final ConcurrentMap<String, CachedData> theMap = new ConcurrentHashMap<String, CachedData>();
  private final SerializingTranscoder transcoder;

  public MockMemcachedClient()
  {
    transcoder = new LZ4Transcoder();
    transcoder.setCompressionThreshold(0);
  }

  @Override
  public Collection<SocketAddress> getAvailableServers()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Collection<SocketAddress> getUnavailableServers()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Transcoder<Object> getTranscoder()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public NodeLocator getNodeLocator()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Future<Boolean> append(long cas, String key, Object val)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> Future<Boolean> append(long cas, String key, T val, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Future<Boolean> prepend(long cas, String key, Object val)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> Future<Boolean> prepend(long cas, String key, T val, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> Future<CASResponse> asyncCAS(String key, long casId, T value, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Future<CASResponse> asyncCAS(String key, long casId, Object value)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> CASResponse cas(String key, long casId, int exp, T value, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public CASResponse cas(String key, long casId, Object value)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> Future<Boolean> add(String key, int exp, T o, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Future<Boolean> add(String key, int exp, Object o)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> Future<Boolean> set(String key, int exp, T o, Transcoder<T> tc)
  {
    theMap.put(key, tc.encode(o));

    return new Future<Boolean>()
    {
      @Override
      public boolean cancel(boolean b)
      {
        return false;
      }

      @Override
      public boolean isCancelled()
      {
        return false;
      }

      @Override
      public boolean isDone()
      {
        return true;
      }

      @Override
      public Boolean get() throws InterruptedException, ExecutionException
      {
        return true;
      }

      @Override
      public Boolean get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException
      {
        return true;
      }
    };
  }

  @Override
  public Future<Boolean> set(String key, int exp, Object o)
  {
    return set(key, exp, o, transcoder);
  }

  @Override
  public <T> Future<Boolean> replace(String key, int exp, T o, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Future<Boolean> replace(String key, int exp, Object o)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> Future<T> asyncGet(String key, final Transcoder<T> tc)
  {
    CachedData data = theMap.get(key);
    final T theValue = data != null ? tc.decode(data) : null;

    return new Future<T>()
    {
      @Override
      public boolean cancel(boolean b)
      {
        return false;
      }

      @Override
      public boolean isCancelled()
      {
        return false;
      }

      @Override
      public boolean isDone()
      {
        return true;
      }

      @Override
      public T get() throws InterruptedException, ExecutionException
      {
        return theValue;
      }

      @Override
      public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException
      {
        return theValue;
      }
    };
  }

  @Override
  public Future<Object> asyncGet(String key)
  {
    return asyncGet(key, transcoder);
  }

  @Override
  public Future<CASValue<Object>> asyncGetAndTouch(String key, int exp)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> Future<CASValue<T>> asyncGetAndTouch(String key, int exp, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public CASValue<Object> getAndTouch(String key, int exp)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> CASValue<T> getAndTouch(String key, int exp, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> Future<CASValue<T>> asyncGets(String key, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Future<CASValue<Object>> asyncGets(String key)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> CASValue<T> gets(String key, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public CASValue<Object> gets(String key)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> T get(String key, Transcoder<T> tc)
  {
    CachedData data = theMap.get(key);
    return data != null ? tc.decode(data) : null;
  }

  @Override
  public Object get(String key)
  {
    return get(key, transcoder);
  }

  @Override
  public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keys, Iterator<Transcoder<T>> tcs)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys, Iterator<Transcoder<T>> tcs)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> BulkFuture<Map<String, T>> asyncGetBulk(final Iterator<String> keys, final Transcoder<T> tc)
  {
    return new BulkFuture<Map<String, T>>()
        {
          @Override
          public boolean isTimeout()
          {
            return false;
          }

          @Override
          public Map<String, T> getSome(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException
          {
            return get();
          }

          @Override
          public OperationStatus getStatus()
          {
            return null;
          }

          @Override
          public boolean cancel(boolean b)
          {
            return false;
          }

          @Override
          public boolean isCancelled()
          {
            return false;
          }

          @Override
          public boolean isDone()
          {
            return true;
          }

          @Override
          public Map<String, T> get() throws InterruptedException, ExecutionException
          {
            Map<String, T> retVal = Maps.newHashMap();

            while(keys.hasNext()) {
              String key = keys.next();
              CachedData data = theMap.get(key);
              retVal.put(key, data != null ? tc.decode(data) : null);
            }

            return retVal;
          }

          @Override
          public Map<String, T> get(long l, TimeUnit timeUnit)
              throws InterruptedException, ExecutionException, TimeoutException
          {
            return get();
          }
        };
  }

  @Override
  public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public BulkFuture<Map<String, Object>> asyncGetBulk(Iterator<String> keys)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public BulkFuture<Map<String, Object>> asyncGetBulk(final Collection<String> keys)
  {
    return asyncGetBulk(keys.iterator(), transcoder);
  }

  @Override
  public <T> BulkFuture<Map<String, T>> asyncGetBulk(Transcoder<T> tc, String... keys)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public BulkFuture<Map<String, Object>> asyncGetBulk(String... keys)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> Map<String, T> getBulk(Iterator<String> keys, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> Map<String, T> getBulk(Collection<String> keys, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Map<String, Object> getBulk(Iterator<String> keys)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Map<String, Object> getBulk(Collection<String> keys)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> Map<String, T> getBulk(Transcoder<T> tc, String... keys)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Map<String, Object> getBulk(String... keys)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> Future<Boolean> touch(String key, int exp, Transcoder<T> tc)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public <T> Future<Boolean> touch(String key, int exp)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Map<SocketAddress, String> getVersions()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Map<SocketAddress, Map<String, String>> getStats()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Map<SocketAddress, Map<String, String>> getStats(String prefix)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public long incr(String key, long by)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public long incr(String key, int by)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public long decr(String key, long by)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public long decr(String key, int by)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public long incr(String key, long by, long def, int exp)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public long incr(String key, int by, long def, int exp)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public long decr(String key, long by, long def, int exp)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public long decr(String key, int by, long def, int exp)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Future<Long> asyncIncr(String key, long by)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Future<Long> asyncIncr(String key, int by)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Future<Long> asyncDecr(String key, long by)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Future<Long> asyncDecr(String key, int by)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public long incr(String key, long by, long def)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public long incr(String key, int by, long def)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public long decr(String key, long by, long def)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public long decr(String key, int by, long def)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Future<Boolean> delete(String key)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Future<Boolean> flush(int delay)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Future<Boolean> flush()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void shutdown()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public boolean shutdown(long timeout, TimeUnit unit)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public boolean waitForQueues(long timeout, TimeUnit unit)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public boolean addObserver(ConnectionObserver obs)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public boolean removeObserver(ConnectionObserver obs)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<String> listSaslMechanisms()
  {
    throw new UnsupportedOperationException("not implemented");
  }
};
