package com.otel.project;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.Slf4JBaggageEventListener;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleUpDownCounter;
import io.opentelemetry.api.metrics.Meter;


public class OpenTelInterceptor implements ClientHttpRequestInterceptor {

	private String tracerName;
	private String meterName;
	private String context;
	private String observedExtensionUrl;

	private OtelTracer tracer;
	private Meter meter;

	private DoubleUpDownCounter requestSuccessCounter;
	private DoubleUpDownCounter requestFailureCounter;
	private DoubleUpDownCounter totalRequestCounter;
	
	private Logger LOGGER = LoggerFactory.getLogger(OpenTelInterceptor.class);


	public OpenTelInterceptor(OpenTelemetry opentelemetry, String context, String serviceName,
			String observedExtensionUrl) {
		
		this.tracer = getOtelTracer(opentelemetry);
		this.meter = getOtelMeter(opentelemetry);
		this.tracerName = serviceName;
		this.meterName = serviceName;
		this.context = context;
		this.observedExtensionUrl = observedExtensionUrl;
		initMerterCounters();

	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {

		return request.getURI().toString().equals(this.observedExtensionUrl)
				? Observation.createNotStarted("observe.example.operation", createObservationRegistry(tracer))
						.contextualName(this.context).observe(() -> sendrequest(request, body, execution))
				: execution.execute(request, body);

	}

	private ClientHttpResponse sendrequest(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
    	
    	ClientHttpResponse response = null;
    	
    	try {
    		
    		response = execution.execute(request, body);
    		
    		if(!hasError(response))
    			this.requestSuccessCounter.add(1);
    		else
    			this.requestFailureCounter.add(1);
    		
        	tracer.currentSpan().tag("http.response.code",response.getStatusCode().toString());
        	tracer.currentSpan().tag("http.request.url",request.getURI().toString());
        	
    	}
    	
    	catch (IOException e) {
    		this.requestFailureCounter.add(1);
    		e.printStackTrace();
    	}    	
  
    	this.totalRequestCounter.add(1);
    	
		return response;
    }

	private OtelTracer getOtelTracer(OpenTelemetry openTelemetry) {
		io.opentelemetry.api.trace.Tracer otelTracer = openTelemetry.getTracerProvider().get(this.tracerName);

		OtelCurrentTraceContext otelCurrentTraceContext = new OtelCurrentTraceContext();

		Slf4JEventListener slf4JEventListener = new Slf4JEventListener();

		Slf4JBaggageEventListener slf4JBaggageEventListener = new Slf4JBaggageEventListener(Collections.emptyList());

		return new OtelTracer(otelTracer, otelCurrentTraceContext, event -> {
			slf4JEventListener.onEvent(event);
			slf4JBaggageEventListener.onEvent(event);
		}, new OtelBaggageManager(otelCurrentTraceContext, Collections.emptyList(), Collections.emptyList()));
	}

	private Meter getOtelMeter(OpenTelemetry openTelemetry) {
		return openTelemetry.getMeter(this.meterName);
	}

	private ObservationRegistry createObservationRegistry(Tracer tracer) {

		ObservationRegistry registry = ObservationRegistry.create();

		registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer));

		return registry;
	}

	private boolean hasError(ClientHttpResponse clientHttpResponse)
	        throws IOException {
	    return !clientHttpResponse.getStatusCode().is2xxSuccessful();
	}
	private void initMerterCounters() {
		this.requestSuccessCounter = meter.upDownCounterBuilder(String.format("example.%s.%s.request_success_count",context ,meterName)).ofDoubles()
				.setDescription("Request success counter for HTTP client")
				.setUnit("1").build();

		this.requestFailureCounter = meter.upDownCounterBuilder(String.format("example.%s.%s.request_failure_count",context ,meterName)).ofDoubles()
				.setDescription("Request failure counter for HTTP client").setUnit("1").build();

		this.totalRequestCounter = meter.upDownCounterBuilder(String.format("example.%s.%s.total_request_count",context ,meterName)).ofDoubles()
				.setDescription("Total request counter for HTTP client").setUnit("1").build();

	}

	@Override
	public int hashCode() {
		return Objects.hash(context, meterName, observedExtensionUrl, tracerName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OpenTelInterceptor other = (OpenTelInterceptor) obj;
		return Objects.equals(context, other.context) && Objects.equals(meterName, other.meterName)
				&& Objects.equals(observedExtensionUrl, other.observedExtensionUrl)
				&& Objects.equals(tracerName, other.tracerName);
	}
	
	
}