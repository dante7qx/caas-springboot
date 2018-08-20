package org.dante.springboot.service.hello;

import org.springframework.stereotype.Service;

@Service
public class HelloService {

	public String sayHello(String msg) {
		return "您好 --> " + msg;
	}
	
}
