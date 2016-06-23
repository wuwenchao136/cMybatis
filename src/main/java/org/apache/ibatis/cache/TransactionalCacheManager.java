/*
 *    Copyright 2009-2011 The MyBatis Team
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.cache;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.SerializationException;
import org.apache.commons.lang.SerializationUtils;
import org.apache.ibatis.cache.decorators.TransactionalCache;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

public class TransactionalCacheManager {

	private static final Log logger = LogFactory
			.getLog(TransactionalCacheManager.class);

	private Map<Cache, TransactionalCache> transactionalCaches = new HashMap<Cache, TransactionalCache>();

	public void clear(Cache cache) {
		getTransactionalCache(cache).clear();
	}

	@SuppressWarnings("unchecked")
	public <T> void putObject(Cache cache, CacheKey key, Object value) {
		ArrayList<T> arrayList = new ArrayList<T>();
		boolean flag = true;
		for (Object object : (ArrayList<?>) value)
			try {
				arrayList.add((T) cloneObject(object));
			} catch (CloneNotSupportedException e) {
				logger.error("Encounter CloneNotSupportedException When Cache Object ["
						+ object.toString() + "]");
				flag = false;
				break;
			}
		if (flag)// if false skip cache
			getTransactionalCache(cache).putObject(key, arrayList);
	}

	public void commit() {
		for (TransactionalCache txCache : transactionalCaches.values()) {
			txCache.commit();
		}
	}

	public void rollback() {
		for (TransactionalCache txCache : transactionalCaches.values()) {
			txCache.rollback();
		}
	}

	private TransactionalCache getTransactionalCache(Cache cache) {
		TransactionalCache txCache = transactionalCaches.get(cache);
		if (txCache == null) {
			txCache = new TransactionalCache(cache);
			transactionalCaches.put(cache, txCache);
		}
		return txCache;
	}

	/**
	 * 对象的深拷贝方法
	 * @param 需进行深拷贝的对象
	 * @return	拷贝后得到的对象
	 * @throws CloneNotSupportedException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cloneObject(T obj) throws CloneNotSupportedException {
		if (obj == null) {
			return null;
		}
		if (obj instanceof Cloneable) {//如果对象已implement Cloneable接口则调用对象的clone方法
			Class<? extends Object> clazz = obj.getClass();
			Method m;
			try {
				m = clazz.getMethod("clone", (Class[]) null);
			} catch (NoSuchMethodException ex) {
				throw new NoSuchMethodError(ex.getMessage());
			}
			try {
				Object result = m.invoke(obj, (Object[]) null);
				return (T) result;
			} catch (InvocationTargetException ex) {
				Throwable cause = ex.getCause();
				if (cause instanceof CloneNotSupportedException) {
					throw ((CloneNotSupportedException) cause);
				}
				throw new Error("Unexpected exception", cause);
			} catch (IllegalAccessException ex) {
				throw new IllegalAccessError(ex.getMessage());
			}
		}
		if (obj instanceof Serializable)//如果对象支持序列化则进行反序列化获得对象信息
			return (T) SerializationUtils.clone((Serializable) obj);
		throw new SerializationException();
	}

}
