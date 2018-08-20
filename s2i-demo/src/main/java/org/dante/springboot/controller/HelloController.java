package org.dante.springboot.controller;

import javax.servlet.http.HttpServletRequest;

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
	@Value("${hello.msg}")
	private String helloMsg;
	@Autowired
	private Environment env;
	
	@GetMapping("/")
	public String hello(HttpServletRequest request) {
		return helloService.sayHello(helloMsg + " -- " + IPUtils.getIpAddr(request));
	}
	
	@GetMapping("/healthz")
	public String healthz() {
		return "UP-" + env.getProperty("hello.msg");
	}
}
