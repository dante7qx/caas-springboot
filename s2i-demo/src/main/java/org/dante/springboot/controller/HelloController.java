package org.dante.springboot.controller;

import javax.servlet.http.HttpServletRequest;

import org.dante.springboot.prop.DBProp;
import org.dante.springboot.service.hello.HelloService;
import org.dante.springboot.util.IPUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
	
	@Autowired
	private HelloService helloService;
	@Autowired
	private DBProp dbProp;
	@Value("${hello.msg}")
	private String helloMsg;
	@Autowired
	private Environment env;
	
	@GetMapping("/")
	public String hello(HttpServletRequest request) {
		return helloService.sayHello(helloMsg + " -- 客户端IP：" + IPUtils.getIpAddr(request));
	}
	
	@GetMapping("/db")
	public String db() {
		return dbProp.getName().concat(" - ").concat(dbProp.getUser());
	}
	
	@GetMapping("/healthz")
	public String healthz() {
		return "UP-" + env.getProperty("hello_msg");
	}
	
	
	
}
