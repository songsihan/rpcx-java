package com.colobu.rpcx.service;

import com.colobu.rpcx.rpc.annotation.Consumer;

/**
 * @author goodjava@qq.com
 */
@Consumer(impl = "com.colobu.rpcx.service.TestService")
public interface ITestService {

    String hi(String str);

    String $echo(String str);

    byte[] golangHi(byte[] data);


    int sum(int a,int b);

}
