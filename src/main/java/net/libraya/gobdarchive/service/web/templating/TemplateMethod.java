package net.libraya.gobdarchive.service.web.templating;

import java.lang.reflect.Method;

public class TemplateMethod {
	
	protected Object owner;
	protected Method method;
	
	public TemplateMethod(Object owner, Method method) {
		this.owner = owner;
		this.method = method;
	}
}
