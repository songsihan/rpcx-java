package com.colobu.rpcx.spring;

import com.colobu.rpcx.client.IServiceDiscovery;
import com.colobu.rpcx.client.NettyClient;
import com.colobu.rpcx.client.ZkServiceDiscovery;
import com.colobu.rpcx.netty.IClient;
import com.colobu.rpcx.rpc.RpcException;
import com.colobu.rpcx.rpc.annotation.Provider;
import com.colobu.rpcx.server.IServiceRegister;
import com.colobu.rpcx.server.NettyServer;
import com.colobu.rpcx.server.ZkServiceRegister;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.UUID;

/**
 * @author goodjava@qq.com
 */
@Configuration
@Aspect
@ConditionalOnClass(RpcxConsumer.class)
public class RpcxAutoConfigure {

    private static final Logger logger = LoggerFactory.getLogger(RpcxAutoConfigure.class);

    @Autowired
    private ApplicationContext context;

    @Value("${rpcx.package.path}")
    private String rpcxPackagePath;

    @Value("${rpcx.base.path}")
    private String rpcxBasePath;


    @PostConstruct
    private void init() {
        Reflections reflections = new Reflections(rpcxPackagePath);
        Set<Class<?>> providerSet = reflections.getTypesAnnotatedWith(Provider.class, true);
        providerSet.stream().forEach(it -> {
            logger.info("provider:{}", it);
        });

        NettyServer server = new NettyServer();
        server.setGetBeanFunc((clazz) -> context.getBean(clazz));
        server.start();
        IServiceRegister reg = new ZkServiceRegister(rpcxBasePath, server.getAddr() + ":" + server.getPort(), rpcxPackagePath);
        reg.register();
        reg.start();
    }

    @Bean
    @ConditionalOnMissingBean
    public RpcxConsumer rpcxConsumer() {
        IServiceDiscovery serviceDiscovery = new ZkServiceDiscovery(rpcxBasePath);
        IClient client = new NettyClient(serviceDiscovery);
        return new RpcxConsumer(client);
    }


    /**
     * 所有provider 的执行,这里都会被拦截到
     * @param joinPoint
     * @return
     */
    @Around("execution(public * *(..)) && within(@com.colobu.rpcx.rpc.annotation.Provider *)")
    public Object aroundProvider(ProceedingJoinPoint joinPoint) {
        Provider provider = joinPoint.getTarget().getClass().getAnnotation(Provider.class);
        String uuid = UUID.randomUUID().toString();
        Object[] o = joinPoint.getArgs();
        logger.info("provider execute begin name:{} version:{}  id:{} params:{}", provider.name(), provider.version(), uuid, joinPoint.getArgs());
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long useTime = System.currentTimeMillis() - startTime;
            logger.info("provider execute finish name:{} version:{} id:{} result:{} useTime:{}", provider.name(), provider.version(), uuid, result, useTime);
            return result;
        } catch (Throwable throwable) {
            logger.warn("provider execute finish name:{} version:{} id:{} error:{}", provider.name(), provider.version(), uuid, throwable.getMessage());
            return new RpcException(throwable);//包装成自己的异常
        }
    }

}
