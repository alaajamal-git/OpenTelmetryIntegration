package com.otel.project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@RestController
public class Controller {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
    
    @Value("${otel.observed.url:localhost}")
    private String observed_url;

    @Autowired
    private RestTemplate rest;
    
    @Autowired
    OpenTelMonitorService openTelService;

    
    @PostMapping("/call")
    public ResponseEntity<String> getInfo(@RequestBody String msg) {
    	
    	openTelService.registerOtelTracerMeterForService("test-context", "test-service",observed_url);
    	
    	LOGGER.info("service registered for otel observation!");
    	
    	HttpStatusCode code = sendrequest();
    	
        return new ResponseEntity<String>("request received!",
        		code);
    }
    
    
    public HttpStatusCode sendrequest() {
    	ResponseEntity<String> res;
    	try {
    		res = rest.postForEntity(observed_url, "Hi!", String.class);
    	}
    	catch (HttpStatusCodeException e) {
    		e.printStackTrace();
    		return e.getStatusCode();
    	}
    	
    	
    	return res.getStatusCode();
    }
   

}
