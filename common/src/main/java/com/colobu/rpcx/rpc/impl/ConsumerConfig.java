package com.colobu.rpcx.rpc.impl;

import com.colobu.rpcx.netty.IClient;
import com.colobu.rpcx.rpc.CglibProxy;
import com.colobu.rpcx.rpc.ReflectUtils;
import com.colobu.rpcx.rpc.Result;
import com.colobu.rpcx.rpc.annotation.Consumer;

import java.util.stream.Stream;

public class ConsumerConfig {

    private IClient client;

    public ConsumerConfig(IClient client) {
        this.client = client;
    }

    public <T> T refer(Class<T> clazz) {
        return new CglibProxy().getProxy(clazz, (method, args) -> {
            Consumer provider = clazz.getAnnotation(Consumer.class);
            RpcInvocation invocation = new RpcInvocation();
            invocation.setClassName(provider.impl());
            invocation.setArguments(args);
            invocation.setMethodName(method.getName());

            Class<?>[] types = method.getParameterTypes();
            invocation.parameterTypeNames = Stream.of(types).map(it -> ReflectUtils.getDesc(it)).toArray(String[]::new);
            invocation.setParameterTypes(types);
            invocation.setResultType(method.getReturnType());

            RpcInvoker invoker = new RpcInvoker(client);
            Result result = invoker.invoke(invocation);
            return result.getValue();
        });
    }

}