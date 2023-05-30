package com.otel.project;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

@Service
public class OpenTelMonitorServiceImpl implements OpenTelMonitorService {

	private Resource resource;
	private SdkTracerProvider sdkTracerProvider;
	private SdkMeterProvider sdkMeterProvider;
	private OpenTelemetrySdk openTelemetrySdk;

	private List<ClientHttpRequestInterceptor> interceptors = new LinkedList<>();

	private Map<String, ClientHttpRequestInterceptor> otelInterceptors = new HashMap<>();

	private Logger LOGGER = LoggerFactory.getLogger(OpenTelMonitorServiceImpl.class);

	@Autowired
	RestTemplate rest;

	public OpenTelMonitorServiceImpl(@Value("${otel.protocol}") String collectorProtocol,
			@Value("${otel.host}") String collectorHost, @Value("${otel.port}") String collectorPort) {

		this.resource = Resource.getDefault()
				.merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "otel-service")));

		this.sdkTracerProvider = SdkTracerProvider.builder()
				.addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder()
						.setEndpoint(String.format("%s://%s:%s", collectorProtocol, collectorHost, collectorPort))
						.build()).build())
				.setResource(resource).build();

		this.sdkMeterProvider = SdkMeterProvider.builder()
				.registerMetricReader(PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder()
						.setEndpoint(String.format("%s://%s:%s", collectorProtocol, collectorHost, collectorPort))
						.build()).build())
				.setResource(resource).build();

		this.openTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
				.setMeterProvider(sdkMeterProvider)
				.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
				.buildAndRegisterGlobal();
	}

	@Override
	public void registerOtelTracerMeterForService(String context, String name, String observedExtensionUri) {

		String TripleId = String.format("%s.%s.%s", context, name, observedExtensionUri);

		if (!otelInterceptors.containsKey(TripleId)) {

			ClientHttpRequestInterceptor otelInteceptor = new OpenTelInterceptor(this.openTelemetrySdk, context, name,
					observedExtensionUri);

			otelInterceptors.put(TripleId, otelInteceptor);

			interceptors.add(otelInteceptor);

			rest.setInterceptors(interceptors);
		}
		
		else {
			LOGGER.error("service already registered!");
		}
	}

	@Override
	public void unregisterOtelTracerMeterForService(String context, String name, String observedExtensionUri) {
		String TripleId = String.format("%s.%s.%s", context, name, observedExtensionUri);

		if (otelInterceptors.containsKey(TripleId)) {

			interceptors.remove(otelInterceptors.get(TripleId));

			rest.setInterceptors(interceptors);

			LOGGER.debug("unregister operation succeeded!");
		} else
			LOGGER.debug("unregister operation faild: interceptor not found!");

	}

	public OpenTelemetrySdk getOpenTelemetrySdk() {
		return openTelemetrySdk;
	}

	public Map<String, ClientHttpRequestInterceptor> getOtelInterceptors() {
		return otelInterceptors;
	}

	public void setOtelInterceptors(Map<String, ClientHttpRequestInterceptor> otelInterceptors) {
		this.otelInterceptors = otelInterceptors;
	}

	public List<ClientHttpRequestInterceptor> getInterceptors() {
		return interceptors;
	}

	public void setInterceptors(List<ClientHttpRequestInterceptor> interceptors) {
		this.interceptors = interceptors;
	}
}