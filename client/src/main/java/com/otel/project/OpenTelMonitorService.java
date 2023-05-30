package com.otel.project;

public interface OpenTelMonitorService {
	
	public void registerOtelTracerMeterForService(String context, String name, String observedUri);
	public void unregisterOtelTracerMeterForService(String context, String name, String observedUri);
}
