package com.colobu.rpcx.filter.impl;


import com.colobu.rpcx.cache.Cache;
import com.colobu.rpcx.cache.LruCacheFactory;
import com.colobu.rpcx.common.StringUtils;
import com.colobu.rpcx.config.Constants;
import com.colobu.rpcx.filter.Filter;
import com.colobu.rpcx.rpc.Invoker;
import com.colobu.rpcx.rpc.Result;
import com.colobu.rpcx.rpc.RpcException;
import com.colobu.rpcx.rpc.annotation.RpcFilter;
import com.colobu.rpcx.rpc.impl.RpcInvocation;
import com.colobu.rpcx.rpc.impl.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodjava@qq.com
 */
@RpcFilter(order = -998, group = {Constants.PROVIDER})
public class CacheFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CacheFilter.class);

    private static LruCacheFactory factory = new LruCacheFactory();

    @Override
    public Result invoke(Invoker<?> invoker, RpcInvocation invocation) throws RpcException {
        logger.debug("CacheFilter begin");
        if ("true".equals(invoker.getUrl().getParameter(Constants.CACHE_KEY))) {
            Cache cache = factory.getCache(invoker.getUrl());
            if (cache != null) {
                String key = StringUtils.toArgumentString(invocation.getArguments());
                if (cache != null && key != null) {
                    Object value = cache.get(key);
                    if (value != null) {
                        logger.info("CacheFilter end get from cache");
                        return new RpcResult(value);
                    }
                    Result result = invoker.invoke(invocation);
                    if (!result.hasException()) {
                        cache.put(key, result.getValue());
                    }
                    logger.debug("CacheFilter end");
                    return result;
                }
            }
        }
        logger.debug("CacheFilter end");
        return invoker.invoke(invocation);
    }

}
